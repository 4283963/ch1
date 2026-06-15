package com.smartpark.mapper;

import com.smartpark.entity.ParkingSpace;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ParkingSpaceMapper {
    int insert(ParkingSpace parkingSpace);
    int deleteById(@Param("id") Long id);
    int update(ParkingSpace parkingSpace);
    ParkingSpace selectById(@Param("id") Long id);
    List<ParkingSpace> selectAll();
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);
    ParkingSpace selectBySpaceCode(@Param("spaceCode") String spaceCode);
}
