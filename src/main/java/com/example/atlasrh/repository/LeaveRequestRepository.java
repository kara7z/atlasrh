package com.example.atlasrh.repository;

import org.springframework.data.repository.ListCrudRepository;

import com.example.atlasrh.entity.LeaveRequest;

public interface LeaveRequestRepository extends ListCrudRepository<LeaveRequest, Long> {
}
