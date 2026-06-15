package com.smartpark.service;

import com.smartpark.entity.ParkingSpace;
import com.smartpark.mapper.ParkingSpaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParkingSpaceService {

    @Autowired
    private ParkingSpaceMapper parkingSpaceMapper;

    public int insert(ParkingSpace parkingSpace) {
        return parkingSpaceMapper.insert(parkingSpace);
    }

    public int deleteById(Long id) {
        return parkingSpaceMapper.deleteById(id);
    }

    public int update(ParkingSpace parkingSpace) {
        return parkingSpaceMapper.update(parkingSpace);
    }

    public ParkingSpace selectById(Long id) {
        return parkingSpaceMapper.selectById(id);
    }

    public List<ParkingSpace> selectAll() {
        return parkingSpaceMapper.selectAll();
    }

    public int updateStatus(Long id, Integer status) {
        return parkingSpaceMapper.updateStatus(id, status);
    }

    public int updateStatusWithExpect(Long id, Integer status, Integer expectStatus) {
        return parkingSpaceMapper.updateStatusWithExpect(id, status, expectStatus);
    }

    public ParkingSpace selectBySpaceCode(String spaceCode) {
        return parkingSpaceMapper.selectBySpaceCode(spaceCode);
    }
}
