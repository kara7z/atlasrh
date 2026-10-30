package com.example.atlasrh.service;

import java.util.List;
import java.util.Optional;

import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.dto.LeaveDecisionRequest;

public interface LeaveRequestService {
  List<LeaveRequestDto> findAll();

  Optional<LeaveRequestDto> findById(Long id);

  List<LeaveRequestDto> pendingForManager(Long managerId);

  LeaveRequestDto decide(Long id, LeaveDecisionRequest request);
}
