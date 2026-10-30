package com.example.atlasrh.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.LeaveRequestMapper;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.LeaveRequestService;

@Service
@Transactional(readOnly = true)
public class LeaveRequestServiceImpl implements LeaveRequestService {
  private final LeaveRequestRepository repository;
  private final LeaveRequestMapper mapper;
  private final EmployeeRepository employeeRepository;

  public LeaveRequestServiceImpl(LeaveRequestRepository repository, LeaveRequestMapper mapper,
      EmployeeRepository employeeRepository) {
    this.repository = repository;
    this.mapper = mapper;
    this.employeeRepository = employeeRepository;
  }

  @Override
  public List<LeaveRequestDto> findAll() {
    return repository.findAll().stream().map(mapper::toDto).toList();
  }

  @Override
  public Optional<LeaveRequestDto> findById(Long id) {
    return repository.findById(id).map(mapper::toDto);
  }

  @Override
  public List<LeaveRequestDto> pendingForManager(Long managerId) {
    requireManager(managerId);
    return repository.findPendingByManagerId(managerId).stream().map(mapper::toDto).toList();
  }

  @Override
  @Transactional
  public LeaveRequestDto decide(Long id, LeaveDecisionRequest request) {
    if (request.decision() != RequestStatus.APPROVED && request.decision() != RequestStatus.REJECTED) {
      throw new BadRequestException("Decision must be APPROVED or REJECTED");
    }
    String comment = request.comment() == null ? null : request.comment().strip();
    if (request.decision() == RequestStatus.REJECTED && (comment == null || comment.isBlank())) {
      throw new BadRequestException("A comment is required when rejecting a leave request");
    }
    requireManager(request.managerId());
    var leave = repository.lockById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + id));
    employeeRepository.findById(leave.getEmployeeId())
        .filter(value -> request.managerId().equals(value.getManagerId())
            && !request.managerId().equals(value.getId()))
        .orElseThrow(() -> new ResourceNotFoundException("Leave request not found in manager's team"));
    if (leave.getStatus() != RequestStatus.PENDING) {
      throw new ConflictException("Only PENDING leave requests can be decided");
    }
    leave.setStatus(request.decision());
    leave.setComment(comment == null || comment.isBlank() ? null : comment);
    return mapper.toDto(repository.save(leave));
  }

  private void requireManager(Long managerId) {
    if (!employeeRepository.existsById(managerId)) {
      throw new ResourceNotFoundException("Manager not found: " + managerId);
    }
  }
}
