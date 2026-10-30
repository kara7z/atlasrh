package com.example.atlasrh.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.dto.LeaveCreateRequest;
import com.example.atlasrh.dto.LeaveCreatedDto;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.LeaveRequestMapper;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.LeaveRequestService;
import com.example.atlasrh.util.WorkingDays;

@Service
@Transactional(readOnly = true)
public class LeaveRequestServiceImpl implements LeaveRequestService {
  private final LeaveRequestRepository repository;
  private final LeaveRequestMapper mapper;
  private final EmployeeRepository employeeRepository;
  private final LeaveBalancePolicy balancePolicy;

  public LeaveRequestServiceImpl(LeaveRequestRepository repository, LeaveRequestMapper mapper,
      EmployeeRepository employeeRepository, LeaveBalancePolicy balancePolicy) {
    this.repository = repository;
    this.mapper = mapper;
    this.employeeRepository = employeeRepository;
    this.balancePolicy = balancePolicy;
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
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public LeaveCreatedDto create(Long employeeId, LeaveCreateRequest request) {
    if (request.endDate().isBefore(request.startDate())) {
      throw new BadRequestException("End date must not be before start date");
    }
    if (WorkingDays.between(request.startDate(), request.endDate()) == 0) {
      throw new BadRequestException("Leave request must include at least one working day");
    }
    employeeRepository.lockById(employeeId)
        .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    if (repository.existsOverlapping(employeeId, request.startDate(), request.endDate())) {
      throw new BadRequestException("Leave request overlaps an existing request");
    }
    balancePolicy.check(employeeId, request.leaveType(), request.startDate(), request.endDate());
    var dto = new LeaveRequestDto(null, employeeId, request.leaveType(), request.startDate(), request.endDate(),
        request.reason().strip(), RequestStatus.PENDING, null);
    return mapper.toCreatedDto(repository.save(mapper.toEntity(dto)));
  }

  @Override
  public List<LeaveRequestDto> pendingForManager(Long managerId) {
    requireManager(managerId);
    return repository.findPendingByManagerId(managerId).stream().map(mapper::toDto).toList();
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
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
    if (request.decision() == RequestStatus.APPROVED) {
      employeeRepository.lockById(leave.getEmployeeId())
          .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + leave.getEmployeeId()));
      balancePolicy.check(leave.getEmployeeId(), leave.getLeaveType(), leave.getStartDate(), leave.getEndDate());
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
