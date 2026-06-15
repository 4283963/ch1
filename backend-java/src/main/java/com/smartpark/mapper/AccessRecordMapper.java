package com.smartpark.mapper;

import com.smartpark.entity.AccessRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccessRecordMapper {
    int insert(AccessRecord accessRecord);
    int deleteById(@Param("id") Long id);
    AccessRecord selectById(@Param("id") Long id);
    List<AccessRecord> selectAll();
    List<AccessRecord> selectByPlateNumber(@Param("plateNumber") String plateNumber);
    List<AccessRecord> selectBySpaceId(@Param("spaceId") Long spaceId);
}
