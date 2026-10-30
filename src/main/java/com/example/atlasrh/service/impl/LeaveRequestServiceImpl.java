package com.example.atlasrh.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
  private static final Set<String> SORT_FIELDS = Set.of("id", "leaveType", "startDate", "endDate", "status");
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
  public Page<LeaveRequestDto> listForEmployee(Long employeeId, RequestStatus status, Pageable pageable) {
    if (pageable.isUnpaged() || pageable.getPageSize() > 100) {
      throw new BadRequestException("Leave request page size must be between 1 and 100");
    }
    for (Sort.Order order : pageable.getSort()) {
      if (!SORT_FIELDS.contains(order.getProperty())) {
        throw new BadRequestException("Unsupported leave request sort field: " + order.getProperty());
      }
    }
    if (!employeeRepository.existsById(employeeId)) {
      throw new ResourceNotFoundException("Employee not found: " + employeeId);
    }
    Sort sort = pageable.getSort();
    if (sort.isUnsorted()) {
      sort = Sort.by(Sort.Direction.DESC, "startDate", "id");
    } else if (sort.getOrderFor("id") == null) {
      sort = sort.and(Sort.by("id"));
    }
    Pageable stablePage = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    Page<com.example.atlasrh.entity.LeaveRequest> result = status == null
        ? repository.findByEmployeeId(employeeId, stablePage)
        : repository.findByEmployeeIdAndStatus(employeeId, status, stablePage);
    return result.map(mapper::toDto);
  }

  @Override
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public LeaveRequestDto cancel(Long employeeId, Long id) {
    if (!employeeRepository.existsById(employeeId)) {
      throw new ResourceNotFoundException("Employee not found: " + employeeId);
    }
    var leave = repository.lockById(id)
        .filter(value -> employeeId.equals(value.getEmployeeId()))
        .orElseThrow(() -> new ResourceNotFoundException("Leave request not found for employee: " + employeeId));
    if (leave.getStatus() != RequestStatus.PENDING) {
      throw new ConflictException("Only PENDING leave requests can be cancelled");
    }
    leave.setStatus(RequestStatus.CANCELLED);
    return mapper.toDto(repository.save(leave));
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
