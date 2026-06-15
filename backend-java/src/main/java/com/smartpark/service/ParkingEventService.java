package com.smartpark.service;

import com.alibaba.fastjson2.JSON;
import com.smartpark.dto.SpaceStatusDTO;
import com.smartpark.entity.ParkingSpace;
import com.smartpark.entity.PlateWhitelist;
import com.smartpark.websocket.ParkingWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ParkingEventService {

    private static final Logger log = LoggerFactory.getLogger(ParkingEventService.class);

    private static final long PLATE_DEDUPLICATION_WINDOW_MS = 5000L;
    private static final long SPACE_COOLDOWN_MS = 3000L;

    @Autowired
    private PlateWhitelistService plateWhitelistService;

    @Autowired
    private ParkingSpaceService parkingSpaceService;

    @Autowired
    private GoGatewayService goGatewayService;

    @Autowired
    private AccessRecordService accessRecordService;

    @Autowired
    private ParkingWebSocketHandler parkingWebSocketHandler;

    private final Map<String, Boolean> vehicleInMap = new ConcurrentHashMap<>();

    private final Map<String, PlateProcessRecord> plateRecentRecord = new ConcurrentHashMap<>();

    private final Map<Long, SpaceExecutionLock> spaceLockMap = new ConcurrentHashMap<>();

    private static class PlateProcessRecord {
        final long timestamp;
        final Map<String, Object> result;
        PlateProcessRecord(long timestamp, Map<String, Object> result) {
            this.timestamp = timestamp;
            this.result = result;
        }
    }

    private static class SpaceExecutionLock {
        final AtomicBoolean executing = new AtomicBoolean(false);
        final AtomicLong lastExecuteTime = new AtomicLong(0);
        final AtomicReference<String> lastCommand = new AtomicReference<>(null);
    }

    private SpaceExecutionLock getSpaceLock(Long spaceId) {
        return spaceLockMap.computeIfAbsent(spaceId, k -> new SpaceExecutionLock());
    }

    public Map<String, Object> processPlateRecognition(String plateNumber) {
        log.info("[入口] 接收到车牌识别结果: {}", plateNumber);
        long now = System.currentTimeMillis();

        PlateProcessRecord recent = plateRecentRecord.get(plateNumber);
        if (recent != null && (now - recent.timestamp) < PLATE_DEDUPLICATION_WINDOW_MS) {
            log.warn("[防抖] 车牌 {} 在 {}ms 内重复识别，直接返回上次结果，距上次: {}ms",
                    plateNumber, PLATE_DEDUPLICATION_WINDOW_MS, now - recent.timestamp);
            Map<String, Object> cached = new HashMap<>(recent.result);
            cached.put("cached", true);
            cached.put("dedupHint", "防抖窗口内重复请求，已忽略");
            return cached;
        }

        PlateWhitelist whitelist = plateWhitelistService.selectByPlateNumber(plateNumber);
        if (whitelist == null) {
            log.warn("[校验] 车牌 {} 不在白名单中", plateNumber);
            Map<String, Object> result = new HashMap<>();
            result.put("plateNumber", plateNumber);
            result.put("success", false);
            result.put("message", "车牌不在白名单中");
            return result;
        }

        Long spaceId = whitelist.getSpaceId();
        if (spaceId == null) {
            log.warn("[校验] 车牌 {} 未分配车位", plateNumber);
            Map<String, Object> result = new HashMap<>();
            result.put("plateNumber", plateNumber);
            result.put("success", false);
            result.put("message", "车牌未分配车位");
            return result;
        }

        ParkingSpace space = parkingSpaceService.selectById(spaceId);
        if (space == null) {
            log.warn("[校验] 车位 {} 不存在", spaceId);
            Map<String, Object> result = new HashMap<>();
            result.put("plateNumber", plateNumber);
            result.put("success", false);
            result.put("message", "车位不存在");
            return result;
        }

        SpaceExecutionLock lock = getSpaceLock(spaceId);
        long lastExec = lock.lastExecuteTime.get();
        long timeSinceLast = now - lastExec;
        if (lastExec > 0 && timeSinceLast < SPACE_COOLDOWN_MS) {
            log.warn("[冷却] 车位 {} 距上次指令仅 {}ms (<{}ms)，拒绝本次请求防止液压过载",
                    space.getSpaceCode(), timeSinceLast, SPACE_COOLDOWN_MS);
            Map<String, Object> result = new HashMap<>();
            result.put("plateNumber", plateNumber);
            result.put("success", false);
            result.put("message", "车位指令冷却中，请稍后再试（防止液压泵过载）");
            result.put("cooldownRemainMs", SPACE_COOLDOWN_MS - timeSinceLast);
            return result;
        }

        if (!lock.executing.compareAndSet(false, true)) {
            log.warn("[并发锁] 车位 {} 已有指令正在执行，拒绝并发请求", space.getSpaceCode());
            Map<String, Object> result = new HashMap<>();
            result.put("plateNumber", plateNumber);
            result.put("success", false);
            result.put("message", "车位指令执行中，请勿重复操作");
            return result;
        }

        try {
            boolean isIn = vehicleInMap.getOrDefault(plateNumber, false);
            Map<String, Object> result = new HashMap<>();
            result.put("plateNumber", plateNumber);

            Map<String, Object> finalResult;
            if (!isIn) {
                finalResult = processVehicleIn(plateNumber, spaceId, space, result);
            } else {
                finalResult = processVehicleOut(plateNumber, spaceId, space, result);
            }

            lock.lastExecuteTime.set(System.currentTimeMillis());
            lock.lastCommand.set(isIn ? "OUT" : "IN");

            if (Boolean.TRUE.equals(finalResult.get("success"))) {
                plateRecentRecord.put(plateNumber, new PlateProcessRecord(System.currentTimeMillis(), finalResult));
            }

            return finalResult;

        } finally {
            lock.executing.set(false);
        }
    }

    private Map<String, Object> processVehicleIn(String plateNumber, Long spaceId, ParkingSpace space, Map<String, Object> result) {
        log.info("[进场] 车牌={}, 车位={}, 当前DB状态={}", plateNumber, space.getSpaceCode(), space.getStatus());

        if (space.getStatus() == 1) {
            log.warn("[进场-校验] 车位 {} DB状态已为占用，拒绝进场指令", space.getSpaceCode());
            result.put("success", false);
            result.put("message", "车位已被占用");
            return result;
        }

        if (vehicleInMap.putIfAbsent(plateNumber, true) != null) {
            log.warn("[进场-竞态] 车牌 {} 已有并发请求标记为在场，已拦截", plateNumber);
            result.put("success", false);
            result.put("message", "车辆正在进场处理中");
            return result;
        }

        int updated = parkingSpaceService.updateStatusWithExpect(spaceId, 2, space.getStatus());
        if (updated == 0) {
            log.warn("[进场-乐观锁] 车位 {} 状态已被其他线程修改(DB原值={})，CAS失败，回滚内存标记",
                    space.getSpaceCode(), space.getStatus());
            vehicleInMap.remove(plateNumber);
            result.put("success", false);
            result.put("message", "车位状态已变化，请稍后重试");
            return result;
        }

        String guidingColor = "#059669";
        parkingWebSocketHandler.sendSpaceStatusUpdate(spaceId, space.getSpaceCode(), "guiding", guidingColor,
                space.getXPos(), space.getYPos(), space.getWidth(), space.getHeight());
        sendEventLog(plateNumber, space.getSpaceCode(), "IN", "车辆识别，地锚下降中，引导停车");

        boolean downResult = goGatewayService.sendDownCommand(space.getSpaceCode());
        if (!downResult) {
            log.warn("[进场] Go网关降地锚调用失败，但继续状态流转");
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[进场] 液压等待被中断");
        }

        parkingSpaceService.updateStatus(spaceId, 1);
        String occupiedColor = "#2563eb";
        parkingWebSocketHandler.sendSpaceStatusUpdate(spaceId, space.getSpaceCode(), "occupied", occupiedColor,
                space.getXPos(), space.getYPos(), space.getWidth(), space.getHeight());
        sendEventLog(plateNumber, space.getSpaceCode(), "IN", "车辆已入场，车位已占用");

        accessRecordService.recordIn(plateNumber, spaceId);

        result.put("success", true);
        result.put("message", "车辆进场成功，路闸已放行");
        result.put("action", "IN");
        result.put("spaceId", spaceId);
        result.put("spaceCode", space.getSpaceCode());
        log.info("[进场-完成] {}", JSON.toJSONString(result));
        return result;
    }

    private Map<String, Object> processVehicleOut(String plateNumber, Long spaceId, ParkingSpace space, Map<String, Object> result) {
        log.info("[离场] 车牌={}, 车位={}, 当前DB状态={}", plateNumber, space.getSpaceCode(), space.getStatus());

        if (space.getStatus() == 0) {
            log.warn("[离场-校验] 车位 {} DB状态已为空闲，拒绝离场指令", space.getSpaceCode());
            vehicleInMap.remove(plateNumber);
            result.put("success", false);
            result.put("message", "车位已空闲，无需离场操作");
            return result;
        }

        if (!vehicleInMap.remove(plateNumber)) {
            log.warn("[离场-竞态] 车牌 {} 内存标记已被其他线程清除，已拦截", plateNumber);
            result.put("success", false);
            result.put("message", "车辆正在离场处理中");
            return result;
        }

        int updated = parkingSpaceService.updateStatusWithExpect(spaceId, 2, space.getStatus());
        if (updated == 0) {
            log.warn("[离场-乐观锁] 车位 {} 状态已被其他线程修改(DB原值={})，CAS失败",
                    space.getSpaceCode(), space.getStatus());
            vehicleInMap.put(plateNumber, true);
            result.put("success", false);
            result.put("message", "车位状态已变化，请稍后重试");
            return result;
        }

        String warningColor = "#ff9800";
        parkingWebSocketHandler.sendSpaceStatusUpdate(spaceId, space.getSpaceCode(), "guiding", warningColor,
                space.getXPos(), space.getYPos(), space.getWidth(), space.getHeight());
        sendEventLog(plateNumber, space.getSpaceCode(), "OUT", "车辆离场，地锚升起中");

        boolean upResult = goGatewayService.sendUpCommand(space.getSpaceCode());
        if (!upResult) {
            log.warn("[离场] Go网关升地锚调用失败，但继续状态流转");
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[离场] 液压等待被中断");
        }

        parkingSpaceService.updateStatus(spaceId, 0);
        String freeColor = "#4b5563";
        parkingWebSocketHandler.sendSpaceStatusUpdate(spaceId, space.getSpaceCode(), "free", freeColor,
                space.getXPos(), space.getYPos(), space.getWidth(), space.getHeight());
        sendEventLog(plateNumber, space.getSpaceCode(), "OUT", "车辆已离场，车位已释放");

        accessRecordService.recordOut(plateNumber, spaceId);

        result.put("success", true);
        result.put("message", "车辆离场成功");
        result.put("action", "OUT");
        result.put("spaceId", spaceId);
        result.put("spaceCode", space.getSpaceCode());
        log.info("[离场-完成] {}", JSON.toJSONString(result));
        return result;
    }

    private void sendEventLog(String plateNumber, String spaceCode, String type, String desc) {
        Map<String, Object> logMsg = new HashMap<>();
        logMsg.put("type", "event_log");
        Map<String, Object> data = new HashMap<>();
        data.put("plateNumber", plateNumber);
        data.put("spaceCode", spaceCode);
        data.put("eventType", type);
        data.put("description", desc);
        data.put("time", System.currentTimeMillis());
        logMsg.put("data", data);
        parkingWebSocketHandler.broadcast(JSON.toJSONString(logMsg));
    }

    public void broadcastAllSpaces() {
        List<ParkingSpace> spaces = parkingSpaceService.selectAll();
        for (ParkingSpace space : spaces) {
            String statusStr = SpaceStatusDTO.mapStatus(space.getStatus());
            String color = "#4b5563";
            if ("occupied".equals(statusStr)) color = "#2563eb";
            else if ("guiding".equals(statusStr)) color = "#059669";
            parkingWebSocketHandler.sendSpaceStatusUpdate(
                    space.getId(),
                    space.getSpaceCode(),
                    statusStr,
                    color,
                    space.getXPos(),
                    space.getYPos(),
                    space.getWidth(),
                    space.getHeight()
            );
        }
    }
}
