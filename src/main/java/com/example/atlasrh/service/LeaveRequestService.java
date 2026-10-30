package com.example.atlasrh.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.dto.LeaveCreateRequest;
import com.example.atlasrh.dto.LeaveCreatedDto;

public interface LeaveRequestService {
  List<LeaveRequestDto> findAll();

  Optional<LeaveRequestDto> findById(Long id);

  List<LeaveRequestDto> pendingForManager(Long managerId);

  LeaveRequestDto decide(Long id, LeaveDecisionRequest request);

  LeaveCreatedDto create(Long employeeId, LeaveCreateRequest request);

  Page<LeaveRequestDto> listForEmployee(Long employeeId, RequestStatus status, Pageable pageable);

  LeaveRequestDto cancel(Long employeeId, Long id);
}
