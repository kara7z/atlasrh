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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.dto.LeaveCreateRequest;
import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.repository.LeaveRequestRepository;

@SpringBootTest
@Transactional
class LeaveTrackingIntegrationTests {
  @Autowired
  private LeaveRequestService service;
  @Autowired
  private EmployeeService employeeService;
  @Autowired
  private EmployeeRepository employees;
  @Autowired
  private DepartmentRepository departments;
  @Autowired
  private LeaveRequestRepository requests;

  @Test
  void filtersAllStatusesWithCorrectEmployeeIsolationPaginationAndOrder() {
    Long department = department();
    Long employee = employee(department, null);
    Long other = employee(department, null);
    for (RequestStatus status : RequestStatus.values()) {
      seed(employee, status);
      seed(other, status);
    }
    var page = service.listForEmployee(employee, null, PageRequest.of(0, 2));
    assertThat(page.getTotalElements()).isEqualTo(4);
    assertThat(page.getTotalPages()).isEqualTo(2);
    assertThat(page.getContent()).allMatch(leave -> employee.equals(leave.employeeId()));
    var second = service.listForEmployee(employee, null, PageRequest.of(1, 2));
    assertThat(second.getContent()).doesNotContainAnyElementsOf(page.getContent());
    assertThat(page.getContent().getLast().id()).isGreaterThan(second.getContent().getFirst().id());
    for (RequestStatus status : RequestStatus.values()) {
      var filtered = service.listForEmployee(employee, status, PageRequest.of(0, 2));
      assertThat(filtered.getTotalElements()).isEqualTo(1);
      assertThat(filtered.getContent()).extracting(item -> item.status()).containsExactly(status);
    }
    var beyond = service.listForEmployee(employee, null, PageRequest.of(9, 2));
    assertThat(beyond.getContent()).isEmpty();
    assertThat(beyond.getTotalElements()).isEqualTo(4);
    var ascending = service.listForEmployee(employee, null, PageRequest.of(0, 10, Sort.by("id")));
    assertThat(ascending.getContent().getFirst().id()).isLessThan(ascending.getContent().getLast().id());
  }

  @Test
  void missingEmployeeIsDifferentFromEmptyHistory() {
    Long employee = employee(department(), null);
    assertThat(service.listForEmployee(employee, null, PageRequest.of(0, 10)).getContent()).isEmpty();
    assertThatThrownBy(() -> service.listForEmployee(Long.MAX_VALUE, null, PageRequest.of(0, 10)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void cancellingOwnPendingRequestPersistsAndUpdatesListsWithoutAlteringOtherFields() {
    Long department = department();
    Long manager = employee(department, null);
    Long employee = employee(department, manager);
    var leave = service.create(employee, request());
    var cancelled = service.cancel(employee, leave.id());
    assertThat(cancelled.status()).isEqualTo(RequestStatus.CANCELLED);
    assertThat(cancelled.startDate()).isEqualTo(leave.startDate());
    assertThat(cancelled.reason()).isEqualTo(leave.reason());
    assertThat(requests.findById(leave.id()).orElseThrow().getStatus()).isEqualTo(RequestStatus.CANCELLED);
    assertThat(service.pendingForManager(manager)).isEmpty();
    assertThat(service.listForEmployee(employee, RequestStatus.CANCELLED, PageRequest.of(0, 10)).getContent())
        .containsExactly(cancelled);
    assertThatThrownBy(() -> service.cancel(employee, leave.id())).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.decide(leave.id(), new LeaveDecisionRequest(manager, RequestStatus.APPROVED, null)))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void cannotCancelAnotherEmployeesRequestOrAlreadyProcessedRequests() {
    Long department = department();
    Long employee = employee(department, null);
    Long other = employee(department, null);
    var pending = seed(employee, RequestStatus.PENDING);
    assertThatThrownBy(() -> service.cancel(other, pending.getId())).isInstanceOf(ResourceNotFoundException.class);
    assertThat(requests.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(RequestStatus.PENDING);
    for (RequestStatus status : List.of(RequestStatus.APPROVED, RequestStatus.REJECTED, RequestStatus.CANCELLED)) {
      var processed = seed(employee, status);
      assertThatThrownBy(() -> service.cancel(employee, processed.getId())).isInstanceOf(ConflictException.class);
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentCancellationAndOtherActionHaveOnlyOneWinner(boolean approve) throws Exception {
    Long department = department();
    Long manager = employee(department, null);
    Long employee = employee(department, manager);
    var leave = service.create(employee, request());
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      CountDownLatch start = new CountDownLatch(1);
      Callable<String> cancel = () -> {
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test start timed out");
        try {
          service.cancel(employee, leave.id());
          return "changed";
        } catch (ConflictException exception) {
          return "conflict";
        }
      };
      Callable<String> other = () -> {
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test start timed out");
        try {
          if (approve) {
            service.decide(leave.id(), new LeaveDecisionRequest(manager, RequestStatus.APPROVED, null));
          } else {
            service.cancel(employee, leave.id());
          }
          return "changed";
        } catch (ConflictException exception) {
          return "conflict";
        }
      };
      var first = executor.submit(cancel);
      var second = executor.submit(other);
      start.countDown();
      assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder("changed", "conflict");
    } finally {
      requests.deleteById(leave.id());
      employees.deleteById(employee);
      employees.deleteById(manager);
      departments.deleteById(department);
    }
  }

  private Long department() {
    return departments.save(Department.builder().name("ATLAS-20-" + UUID.randomUUID()).build()).getId();
  }

  private Long employee(Long department, Long manager) {
    String number = UUID.randomUUID().toString();
    return employeeService.create(new EmployeeRequest(number, "Sara", "Amrani", number + "@example.com",
        "Developer", LocalDate.of(2025, 1, 1), department, manager)).id();
  }

  private LeaveCreateRequest request() {
    return new LeaveCreateRequest(LeaveType.ANNUAL, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), "Holiday");
  }

  private LeaveRequest seed(Long employee, RequestStatus status) {
    return requests.save(LeaveRequest.builder().employeeId(employee).leaveType(LeaveType.ANNUAL).status(status)
        .startDate(LocalDate.of(2026, 11, 2)).endDate(LocalDate.of(2026, 11, 3)).reason("Holiday").build());
  }
}
