package com.smartpark.controller;

import com.smartpark.dto.Response;
import com.smartpark.entity.AccessRecord;
import com.smartpark.service.AccessRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access-records")
public class AccessRecordController {

    @Autowired
    private AccessRecordService accessRecordService;

    @GetMapping
    public Response<List<AccessRecord>> getAllRecords() {
        List<AccessRecord> records = accessRecordService.selectAll();
        return Response.success(records);
    }

    @GetMapping("/{id}")
    public Response<AccessRecord> getRecordById(@PathVariable Long id) {
        AccessRecord record = accessRecordService.selectById(id);
        return Response.success(record);
    }

    @GetMapping("/plate/{plateNumber}")
    public Response<List<AccessRecord>> getRecordsByPlateNumber(@PathVariable String plateNumber) {
        List<AccessRecord> records = accessRecordService.selectByPlateNumber(plateNumber);
        return Response.success(records);
    }

    @GetMapping("/space/{spaceId}")
    public Response<List<AccessRecord>> getRecordsBySpaceId(@PathVariable Long spaceId) {
        List<AccessRecord> records = accessRecordService.selectBySpaceId(spaceId);
        return Response.success(records);
    }

    @PostMapping
    public Response<Integer> createRecord(@RequestBody AccessRecord accessRecord) {
        int result = accessRecordService.insert(accessRecord);
        return Response.success(result);
    }

    @DeleteMapping("/{id}")
    public Response<Integer> deleteRecord(@PathVariable Long id) {
        int result = accessRecordService.deleteById(id);
        return Response.success(result);
    }
}
