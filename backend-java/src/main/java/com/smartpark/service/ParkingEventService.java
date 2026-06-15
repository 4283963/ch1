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

@Service
public class ParkingEventService {

    private static final Logger log = LoggerFactory.getLogger(ParkingEventService.class);

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

    public Map<String, Object> processPlateRecognition(String plateNumber) {
        log.info("接收到车牌识别结果: {}", plateNumber);
        Map<String, Object> result = new HashMap<>();
        result.put("plateNumber", plateNumber);

        PlateWhitelist whitelist = plateWhitelistService.selectByPlateNumber(plateNumber);
        if (whitelist == null) {
            log.warn("车牌 {} 不在白名单中", plateNumber);
            result.put("success", false);
            result.put("message", "车牌不在白名单中");
            return result;
        }

        Long spaceId = whitelist.getSpaceId();
        if (spaceId == null) {
            log.warn("车牌 {} 未分配车位", plateNumber);
            result.put("success", false);
            result.put("message", "车牌未分配车位");
            return result;
        }

        ParkingSpace space = parkingSpaceService.selectById(spaceId);
        if (space == null) {
            log.warn("车位 {} 不存在", spaceId);
            result.put("success", false);
            result.put("message", "车位不存在");
            return result;
        }

        boolean isIn = vehicleInMap.getOrDefault(plateNumber, false);

        if (!isIn) {
            return processVehicleIn(plateNumber, spaceId, space, result);
        } else {
            return processVehicleOut(plateNumber, spaceId, space, result);
        }
    }

    private Map<String, Object> processVehicleIn(String plateNumber, Long spaceId, ParkingSpace space, Map<String, Object> result) {
        log.info("车辆进场: 车牌={}, 车位={}", plateNumber, space.getSpaceCode());

        if (space.getStatus() == 1) {
            log.warn("车位 {} 已被占用", space.getSpaceCode());
            result.put("success", false);
            result.put("message", "车位已被占用");
            return result;
        }

        parkingSpaceService.updateStatus(spaceId, 2);
        String guidingColor = "#059669";
        parkingWebSocketHandler.sendSpaceStatusUpdate(spaceId, space.getSpaceCode(), "guiding", guidingColor,
                space.getXPos(), space.getYPos(), space.getWidth(), space.getHeight());
        sendEventLog(plateNumber, space.getSpaceCode(), "IN", "车辆识别，地锚下降中，引导停车");

        boolean downResult = goGatewayService.sendDownCommand(space.getSpaceCode());
        if (!downResult) {
            log.warn("Go网关降地锚调用失败，但继续流程");
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        parkingSpaceService.updateStatus(spaceId, 1);
        String occupiedColor = "#2563eb";
        parkingWebSocketHandler.sendSpaceStatusUpdate(spaceId, space.getSpaceCode(), "occupied", occupiedColor,
                space.getXPos(), space.getYPos(), space.getWidth(), space.getHeight());
        sendEventLog(plateNumber, space.getSpaceCode(), "IN", "车辆已入场，车位已占用");

        accessRecordService.recordIn(plateNumber, spaceId);

        vehicleInMap.put(plateNumber, true);

        result.put("success", true);
        result.put("message", "车辆进场成功，路闸已放行");
        result.put("action", "IN");
        result.put("spaceId", spaceId);
        result.put("spaceCode", space.getSpaceCode());
        log.info("车辆进场完成: {}", JSON.toJSONString(result));
        return result;
    }

    private Map<String, Object> processVehicleOut(String plateNumber, Long spaceId, ParkingSpace space, Map<String, Object> result) {
        log.info("车辆离场: 车牌={}, 车位={}", plateNumber, space.getSpaceCode());

        parkingSpaceService.updateStatus(spaceId, 2);
        String warningColor = "#ff9800";
        parkingWebSocketHandler.sendSpaceStatusUpdate(spaceId, space.getSpaceCode(), "guiding", warningColor,
                space.getXPos(), space.getYPos(), space.getWidth(), space.getHeight());
        sendEventLog(plateNumber, space.getSpaceCode(), "OUT", "车辆离场，地锚升起中");

        boolean upResult = goGatewayService.sendUpCommand(space.getSpaceCode());
        if (!upResult) {
            log.warn("Go网关升地锚调用失败，但继续流程");
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        parkingSpaceService.updateStatus(spaceId, 0);
        String freeColor = "#4b5563";
        parkingWebSocketHandler.sendSpaceStatusUpdate(spaceId, space.getSpaceCode(), "free", freeColor,
                space.getXPos(), space.getYPos(), space.getWidth(), space.getHeight());
        sendEventLog(plateNumber, space.getSpaceCode(), "OUT", "车辆已离场，车位已释放");

        accessRecordService.recordOut(plateNumber, spaceId);

        vehicleInMap.remove(plateNumber);

        result.put("success", true);
        result.put("message", "车辆离场成功");
        result.put("action", "OUT");
        result.put("spaceId", spaceId);
        result.put("spaceCode", space.getSpaceCode());
        log.info("车辆离场完成: {}", JSON.toJSONString(result));
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
