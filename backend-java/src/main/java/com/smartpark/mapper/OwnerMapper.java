package com.smartpark.mapper;

import com.smartpark.entity.Owner;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OwnerMapper {
    int insert(Owner owner);
    int deleteById(@Param("id") Long id);
    int update(Owner owner);
    Owner selectById(@Param("id") Long id);
    List<Owner> selectAll();
}
