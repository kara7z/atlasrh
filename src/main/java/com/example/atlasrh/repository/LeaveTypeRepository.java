package com.example.atlasrh.repository;

import org.springframework.data.repository.ListCrudRepository;

import com.example.atlasrh.entity.LeaveType;

public interface LeaveTypeRepository extends ListCrudRepository<LeaveType, String> {
}
