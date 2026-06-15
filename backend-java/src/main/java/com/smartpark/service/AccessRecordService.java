package com.smartpark.service;

import com.smartpark.entity.AccessRecord;
import com.smartpark.mapper.AccessRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessRecordService {

    @Autowired
    private AccessRecordMapper accessRecordMapper;

    public int insert(AccessRecord accessRecord) {
        return accessRecordMapper.insert(accessRecord);
    }

    public int deleteById(Long id) {
        return accessRecordMapper.deleteById(id);
    }

    public AccessRecord selectById(Long id) {
        return accessRecordMapper.selectById(id);
    }

    public List<AccessRecord> selectAll() {
        return accessRecordMapper.selectAll();
    }

    public List<AccessRecord> selectByPlateNumber(String plateNumber) {
        return accessRecordMapper.selectByPlateNumber(plateNumber);
    }

    public List<AccessRecord> selectBySpaceId(Long spaceId) {
        return accessRecordMapper.selectBySpaceId(spaceId);
    }

    public int recordIn(String plateNumber, Long spaceId) {
        AccessRecord record = new AccessRecord();
        record.setPlateNumber(plateNumber);
        record.setSpaceId(spaceId);
        record.setEventType("IN");
        return accessRecordMapper.insert(record);
    }

    public int recordOut(String plateNumber, Long spaceId) {
        AccessRecord record = new AccessRecord();
        record.setPlateNumber(plateNumber);
        record.setSpaceId(spaceId);
        record.setEventType("OUT");
        return accessRecordMapper.insert(record);
    }
}
