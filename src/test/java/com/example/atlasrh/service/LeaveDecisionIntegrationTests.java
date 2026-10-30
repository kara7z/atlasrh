package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.repository.LeaveRequestRepository;

@SpringBootTest
@Transactional
class LeaveDecisionIntegrationTests {
  @Autowired
  private LeaveRequestService service;
  @Autowired
  private EmployeeService employeeService;
  @Autowired
  private EmployeeRepository employeeRepository;
  @Autowired
  private DepartmentRepository departmentRepository;
  @Autowired
  private LeaveRequestRepository repository;

  @Test
  void pendingListContainsOnlyDirectReportsAndPendingStatusesInDateOrder() {
    Long department = department();
    Long manager = employee(department, null);
    Long report = employee(department, manager);
    Long otherReport = employee(department(), manager);
    Long indirect = employee(department, report);
    var later = leave(report, RequestStatus.PENDING, LocalDate.of(2026, 12, 2));
    var earlier = leave(otherReport, RequestStatus.PENDING, LocalDate.of(2026, 11, 2));
    leave(manager, RequestStatus.PENDING, earlier.getStartDate());
    leave(indirect, RequestStatus.PENDING, earlier.getStartDate());
    leave(employee(department, null), RequestStatus.PENDING, earlier.getStartDate());
    leave(report, RequestStatus.APPROVED, earlier.getStartDate());
    leave(report, RequestStatus.REJECTED, earlier.getStartDate());
    leave(report, RequestStatus.CANCELLED, earlier.getStartDate());

    assertThat(service.pendingForManager(manager)).extracting(item -> item.id())
        .containsExactly(earlier.getId(), later.getId());
    assertThat(service.pendingForManager(otherReport)).isEmpty();
    assertThatThrownBy(() -> service.pendingForManager(Long.MAX_VALUE)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void decisionPersistsAndDisappearsFromPendingList() {
    Long department = department();
    Long manager = employee(department, null);
    var leave = leave(employee(department, manager), RequestStatus.PENDING, LocalDate.of(2026, 11, 2));
    var result = service.decide(leave.getId(), new LeaveDecisionRequest(manager, RequestStatus.REJECTED, " Coverage "));
    assertThat(result.status()).isEqualTo(RequestStatus.REJECTED);
    assertThat(result.comment()).isEqualTo("Coverage");
    var saved = repository.findById(leave.getId()).orElseThrow();
    assertThat(saved.getComment()).isEqualTo("Coverage");
    assertThat(saved.getStatus()).isEqualTo(RequestStatus.REJECTED);
    assertThat(service.pendingForManager(manager)).isEmpty();
    assertThatThrownBy(() -> service.decide(leave.getId(), new LeaveDecisionRequest(manager, RequestStatus.APPROVED, null)))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void rejectsMissingCommentAndNonTeamDecisionsWithoutChangingRequest() {
    Long department = department();
    Long manager = employee(department, null);
    Long otherManager = employee(department, null);
    var leave = leave(employee(department, manager), RequestStatus.PENDING, LocalDate.of(2026, 11, 2));
    assertThatThrownBy(() -> service.decide(leave.getId(), new LeaveDecisionRequest(manager, RequestStatus.REJECTED, " ")))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> service.decide(leave.getId(), new LeaveDecisionRequest(otherManager, RequestStatus.APPROVED, null)))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(repository.findById(leave.getId()).orElseThrow().getStatus()).isEqualTo(RequestStatus.PENDING);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentApproveAndRejectYieldOneDecisionAndOneConflict() throws Exception {
    Long department = department();
    Long manager = employee(department, null);
    Long employee = employee(department, manager);
    var leave = leave(employee, RequestStatus.PENDING, LocalDate.of(2026, 11, 2));
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      CountDownLatch start = new CountDownLatch(1);
      Callable<String> approve = () -> decideAfterStart(start, leave.getId(), manager, RequestStatus.APPROVED);
      Callable<String> reject = () -> decideAfterStart(start, leave.getId(), manager, RequestStatus.REJECTED);
      var first = executor.submit(approve);
      var second = executor.submit(reject);
      start.countDown();
      assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder("decided", "conflict");
      assertThat(repository.findById(leave.getId()).orElseThrow().getStatus())
          .isIn(RequestStatus.APPROVED, RequestStatus.REJECTED);
    } finally {
      repository.deleteById(leave.getId());
      employeeRepository.deleteById(employee);
      employeeRepository.deleteById(manager);
      departmentRepository.deleteById(department);
    }
  }

  private String decideAfterStart(CountDownLatch start, Long id, Long manager, RequestStatus status) throws Exception {
    if (!start.await(10, TimeUnit.SECONDS)) {
      throw new IllegalStateException("Concurrent test did not start");
    }
    try {
      service.decide(id, new LeaveDecisionRequest(manager, status, "Decision reason"));
      return "decided";
    } catch (ConflictException exception) {
      return "conflict";
    }
  }

  private Long department() {
    return departmentRepository.save(Department.builder().name("ATLAS-18-" + UUID.randomUUID()).build()).getId();
  }

  private Long employee(Long department, Long manager) {
    String number = UUID.randomUUID().toString();
    return employeeService.create(new EmployeeRequest(number, "Sara", "Amrani", number + "@example.com",
        "Developer", LocalDate.of(2025, 1, 1), department, manager)).id();
  }

  private LeaveRequest leave(Long employee, RequestStatus status, LocalDate start) {
    return repository.save(LeaveRequest.builder().employeeId(employee).leaveType(LeaveType.ANNUAL)
        .startDate(start).endDate(start.plusDays(1)).reason("Holiday").status(status).build());
  }
}
