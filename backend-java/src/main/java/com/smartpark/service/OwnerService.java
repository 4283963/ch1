package com.smartpark.service;

import com.smartpark.entity.Owner;
import com.smartpark.mapper.OwnerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OwnerService {

    @Autowired
    private OwnerMapper ownerMapper;

    public int insert(Owner owner) {
        return ownerMapper.insert(owner);
    }

    public int deleteById(Long id) {
        return ownerMapper.deleteById(id);
    }

    public int update(Owner owner) {
        return ownerMapper.update(owner);
    }

    public Owner selectById(Long id) {
        return ownerMapper.selectById(id);
    }

    public List<Owner> selectAll() {
        return ownerMapper.selectAll();
    }
}
