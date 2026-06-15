package com.smartpark.mapper;

import com.smartpark.entity.PlateWhitelist;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PlateWhitelistMapper {
    int insert(PlateWhitelist plateWhitelist);
    int deleteById(@Param("id") Long id);
    int update(PlateWhitelist plateWhitelist);
    PlateWhitelist selectById(@Param("id") Long id);
    List<PlateWhitelist> selectAll();
    PlateWhitelist selectByPlateNumber(@Param("plateNumber") String plateNumber);
    List<Map<String, Object>> selectWhitelistWithDetail(@Param("plateNumber") String plateNumber);
    int updateActive(@Param("id") Long id, @Param("isActive") Integer isActive);
}
