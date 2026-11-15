package com.example.atlasrh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.atlasrh.entity.LeaveType;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, String> {
}
