package com.smartpark.controller;

import com.smartpark.dto.Response;
import com.smartpark.entity.ParkingSpace;
import com.smartpark.service.ParkingEventService;
import com.smartpark.service.ParkingSpaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spaces")
public class ParkingSpaceController {

    @Autowired
    private ParkingSpaceService parkingSpaceService;

    @Autowired
    private ParkingEventService parkingEventService;

    @GetMapping
    public Response<List<ParkingSpace>> getAllSpaces() {
        List<ParkingSpace> spaces = parkingSpaceService.selectAll();
        return Response.success(spaces);
    }

    @GetMapping("/{id}")
    public Response<ParkingSpace> getSpaceById(@PathVariable Long id) {
        ParkingSpace space = parkingSpaceService.selectById(id);
        return Response.success(space);
    }

    @PostMapping
    public Response<Integer> createSpace(@RequestBody ParkingSpace parkingSpace) {
        int result = parkingSpaceService.insert(parkingSpace);
        return Response.success(result);
    }

    @PutMapping("/{id}")
    public Response<Integer> updateSpace(@PathVariable Long id, @RequestBody ParkingSpace parkingSpace) {
        parkingSpace.setId(id);
        int result = parkingSpaceService.update(parkingSpace);
        return Response.success(result);
    }

    @PutMapping("/{id}/status")
    public Response<Integer> updateSpaceStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null) {
            return Response.fail("状态不能为空");
        }
        int result = parkingSpaceService.updateStatus(id, status);
        if (result > 0) {
            parkingEventService.broadcastAllSpaces();
        }
        return Response.success(result);
    }

    @DeleteMapping("/{id}")
    public Response<Integer> deleteSpace(@PathVariable Long id) {
        int result = parkingSpaceService.deleteById(id);
        return Response.success(result);
    }

    @PostMapping("/broadcast")
    public Response<Void> broadcastAllSpaces() {
        parkingEventService.broadcastAllSpaces();
        return Response.success();
    }
}
