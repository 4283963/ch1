package com.smartpark.service;

import com.smartpark.entity.PlateWhitelist;
import com.smartpark.mapper.PlateWhitelistMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PlateWhitelistService {

    @Autowired
    private PlateWhitelistMapper plateWhitelistMapper;

    public int insert(PlateWhitelist plateWhitelist) {
        return plateWhitelistMapper.insert(plateWhitelist);
    }

    public int deleteById(Long id) {
        return plateWhitelistMapper.deleteById(id);
    }

    public int update(PlateWhitelist plateWhitelist) {
        return plateWhitelistMapper.update(plateWhitelist);
    }

    public PlateWhitelist selectById(Long id) {
        return plateWhitelistMapper.selectById(id);
    }

    public List<PlateWhitelist> selectAll() {
        return plateWhitelistMapper.selectAll();
    }

    public PlateWhitelist selectByPlateNumber(String plateNumber) {
        return plateWhitelistMapper.selectByPlateNumber(plateNumber);
    }

    public List<Map<String, Object>> selectWhitelistWithDetail(String plateNumber) {
        return plateWhitelistMapper.selectWhitelistWithDetail(plateNumber);
    }

    public int updateActive(Long id, Integer isActive) {
        return plateWhitelistMapper.updateActive(id, isActive);
    }
}
