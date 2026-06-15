package com.smartpark.controller;

import com.smartpark.dto.PlateRecognitionRequest;
import com.smartpark.dto.Response;
import com.smartpark.entity.Owner;
import com.smartpark.entity.PlateWhitelist;
import com.smartpark.service.GoGatewayService;
import com.smartpark.service.OwnerService;
import com.smartpark.service.ParkingEventService;
import com.smartpark.service.PlateWhitelistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plate")
public class PlateController {

    @Autowired
    private PlateWhitelistService plateWhitelistService;

    @Autowired
    private ParkingEventService parkingEventService;

    @Autowired
    private GoGatewayService goGatewayService;

    @Autowired
    private OwnerService ownerService;

    @PostMapping("/recognize")
    public Response<Map<String, Object>> recognize(@RequestBody PlateRecognitionRequest request) {
        if (request.getPlateNumber() == null || request.getPlateNumber().trim().isEmpty()) {
            return Response.fail("车牌号不能为空");
        }
        Map<String, Object> result = parkingEventService.processPlateRecognition(request.getPlateNumber().trim());
        Boolean success = (Boolean) result.get("success");
        if (success != null && success) {
            return Response.success(result);
        } else {
            return Response.fail((String) result.getOrDefault("message", "处理失败"));
        }
    }

    @GetMapping("/whitelist")
    public Response<List<PlateWhitelist>> getAllWhitelist() {
        List<PlateWhitelist> list = plateWhitelistService.selectAll();
        return Response.success(list);
    }

    @GetMapping("/whitelist/detail")
    public Response<List<Map<String, Object>>> getWhitelistWithDetail(
            @RequestParam(required = false) String plateNumber) {
        List<Map<String, Object>> list = plateWhitelistService.selectWhitelistWithDetail(plateNumber);
        return Response.success(list);
    }

    @GetMapping("/whitelist/{id}")
    public Response<PlateWhitelist> getWhitelistById(@PathVariable Long id) {
        PlateWhitelist whitelist = plateWhitelistService.selectById(id);
        return Response.success(whitelist);
    }

    @PostMapping("/whitelist")
    public Response<Integer> createWhitelist(@RequestBody PlateWhitelist plateWhitelist) {
        int result = plateWhitelistService.insert(plateWhitelist);
        return Response.success(result);
    }

    @PutMapping("/whitelist/{id}")
    public Response<Integer> updateWhitelist(@PathVariable Long id, @RequestBody PlateWhitelist plateWhitelist) {
        plateWhitelist.setId(id);
        int result = plateWhitelistService.update(plateWhitelist);
        return Response.success(result);
    }

    @DeleteMapping("/whitelist/{id}")
    public Response<Integer> deleteWhitelist(@PathVariable Long id) {
        int result = plateWhitelistService.deleteById(id);
        return Response.success(result);
    }

    @PutMapping("/whitelist/{id}/active")
    public Response<Integer> updateActive(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer isActive = body.get("isActive");
        if (isActive == null) {
            return Response.fail("isActive不能为空");
        }
        int result = plateWhitelistService.updateActive(id, isActive);
        return Response.success(result);
    }

    @GetMapping("/owners")
    public Response<List<Owner>> getAllOwners() {
        List<Owner> list = ownerService.selectAll();
        return Response.success(list);
    }

    @PostMapping("/owners")
    public Response<Integer> createOwner(@RequestBody Owner owner) {
        int result = ownerService.insert(owner);
        return Response.success(result);
    }

    @PutMapping("/owners/{id}")
    public Response<Integer> updateOwner(@PathVariable Long id, @RequestBody Owner owner) {
        owner.setId(id);
        int result = ownerService.update(owner);
        return Response.success(result);
    }

    @DeleteMapping("/owners/{id}")
    public Response<Integer> deleteOwner(@PathVariable Long id) {
        int result = ownerService.deleteById(id);
        return Response.success(result);
    }

    @PostMapping("/anchor/up")
    public Response<Boolean> anchorUp(@RequestBody Map<String, Object> body) {
        String spaceCode = body.get("spaceId") != null ? String.valueOf(body.get("spaceId")) : null;
        if (spaceCode == null || spaceCode.isEmpty()) {
            return Response.fail("spaceId不能为空");
        }
        boolean result = goGatewayService.sendUpCommand(spaceCode);
        return Response.success(result);
    }

    @PostMapping("/anchor/down")
    public Response<Boolean> anchorDown(@RequestBody Map<String, Object> body) {
        String spaceCode = body.get("spaceId") != null ? String.valueOf(body.get("spaceId")) : null;
        if (spaceCode == null || spaceCode.isEmpty()) {
            return Response.fail("spaceId不能为空");
        }
        boolean result = goGatewayService.sendDownCommand(spaceCode);
        return Response.success(result);
    }
}
