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
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.dto.LeaveCreateRequest;
import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.repository.LeaveTypeRepository;

@SpringBootTest
@Transactional
class LeaveCreationIntegrationTests {
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
  @Autowired
  private LeaveTypeRepository types;

  @Test
  void seedsAllowancesAndCreatesPendingRequestWithWeekdayCount() {
    assertThat(types.findById("ANNUAL").orElseThrow().getDaysPerYear()).isEqualTo(22);
    assertThat(types.findById("EXCEPTIONAL").orElseThrow().getDaysPerYear()).isEqualTo(5);
    Long employee = employee(department(), null);
    var created = service.create(employee, request(LeaveType.ANNUAL, "2026-11-06", "2026-11-09"));
    assertThat(created.workingDays()).isEqualTo(2);
    assertThat(created.status()).isEqualTo(RequestStatus.PENDING);
    assertThat(requests.findById(created.id()).orElseThrow().getEmployeeId()).isEqualTo(employee);
  }

  @ParameterizedTest
  @EnumSource(RequestStatus.class)
  void inclusiveOverlapIsBlockedAcrossTypesAndStatuses(RequestStatus status) {
    Long employee = employee(department(), null);
    seed(employee, LeaveType.ANNUAL, status, "2026-11-02", "2026-11-04");
    assertThatThrownBy(() -> service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-11-04", "2026-11-05")))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("overlaps");
    assertThat(service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-11-05", "2026-11-06")).id())
        .isNotNull();
  }

  @Test
  void annualAndExceptionalHaveIndependentBudgetsBasedOnlyOnApprovedDays() {
    Long employee = employee(department(), null);
    seed(employee, LeaveType.ANNUAL, RequestStatus.APPROVED, "2026-11-02", "2026-12-01");
    assertThatThrownBy(() -> service.create(employee, request(LeaveType.ANNUAL, "2026-12-02", "2026-12-02")))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("balance");
    assertThat(service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-12-07", "2026-12-11")).workingDays())
        .isEqualTo(5);
    assertThatThrownBy(() -> service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-12-14", "2026-12-21")))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("balance");
  }

  @Test
  void pendingRejectedAndCancelledRequestsDoNotConsumeBalance() {
    Long employee = employee(department(), null);
    seed(employee, LeaveType.EXCEPTIONAL, RequestStatus.PENDING, "2026-10-05", "2026-10-09");
    seed(employee, LeaveType.EXCEPTIONAL, RequestStatus.REJECTED, "2026-10-12", "2026-10-16");
    seed(employee, LeaveType.EXCEPTIONAL, RequestStatus.CANCELLED, "2026-10-19", "2026-10-23");
    assertThat(service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-11-02", "2026-11-06")).workingDays())
        .isEqualTo(5);
  }

  @Test
  void crossYearRequestsSplitApprovedUsageAtCalendarBoundaries() {
    Long employee = employee(department(), null);
    var crossYear = service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-12-28", "2027-01-05"));
    assertThat(crossYear.workingDays()).isEqualTo(7);
    var saved = requests.findById(crossYear.id()).orElseThrow();
    saved.setStatus(RequestStatus.APPROVED);
    requests.save(saved);
    assertThatThrownBy(() -> service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-12-24", "2026-12-25")))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("2026");
    assertThat(service.create(employee, request(LeaveType.EXCEPTIONAL, "2027-01-06", "2027-01-07")).workingDays())
        .isEqualTo(2);
  }

  @Test
  void approvalRechecksBalanceWithoutChangingPendingStatusWhenExhausted() {
    Long department = department();
    Long manager = employee(department, null);
    Long employee = employee(department, manager);
    var first = service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-11-02", "2026-11-04"));
    var second = service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-11-09", "2026-11-11"));
    service.decide(first.id(), new LeaveDecisionRequest(manager, RequestStatus.APPROVED, null));
    assertThatThrownBy(() -> service.decide(second.id(), new LeaveDecisionRequest(manager, RequestStatus.APPROVED, null)))
        .isInstanceOf(BadRequestException.class).hasMessageContaining("balance");
    assertThat(requests.findById(second.id()).orElseThrow().getStatus()).isEqualTo(RequestStatus.PENDING);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void simultaneousOverlappingCreationsCannotBothSucceed() throws Exception {
    Long department = department();
    Long employee = employee(department, null);
    try {
      Callable<String> create = () -> {
        try {
          service.create(employee, request(LeaveType.ANNUAL, "2026-11-02", "2026-11-04"));
          return "created";
        } catch (BadRequestException exception) {
          return "rejected";
        }
      };
      assertThat(race(create, create)).containsExactlyInAnyOrder("created", "rejected");
    } finally {
      cleanup(employee);
      departments.deleteById(department);
    }
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void simultaneousApprovalsCannotOverspendYearlyAllowance() throws Exception {
    Long department = department();
    Long manager = employee(department, null);
    Long employee = employee(department, manager);
    try {
      var first = service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-11-02", "2026-11-04"));
      var second = service.create(employee, request(LeaveType.EXCEPTIONAL, "2026-11-09", "2026-11-11"));
      assertThat(race(() -> approve(first.id(), manager), () -> approve(second.id(), manager)))
          .containsExactlyInAnyOrder("approved", "rejected");
      assertThat(requests.findApprovedForYear(employee, LeaveType.EXCEPTIONAL,
          LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).hasSize(1);
    } finally {
      cleanup(employee);
      employees.deleteById(manager);
      departments.deleteById(department);
    }
  }

  private String approve(Long id, Long manager) {
    try {
      service.decide(id, new LeaveDecisionRequest(manager, RequestStatus.APPROVED, null));
      return "approved";
    } catch (BadRequestException exception) {
      return "rejected";
    }
  }

  private List<String> race(Callable<String> first, Callable<String> second) throws Exception {
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var a = executor.submit(() -> { start.await(); return first.call(); });
      var b = executor.submit(() -> { start.await(); return second.call(); });
      start.countDown();
      return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
    }
  }

  private void cleanup(Long employee) {
    requests.findAll().stream().filter(leave -> employee.equals(leave.getEmployeeId())).forEach(requests::delete);
    employees.deleteById(employee);
  }

  private Long department() {
    return departments.save(Department.builder().name("ATLAS-19-" + UUID.randomUUID()).build()).getId();
  }

  private Long employee(Long department, Long manager) {
    String number = UUID.randomUUID().toString();
    return employeeService.create(new EmployeeRequest(number, "Sara", "Amrani", number + "@example.com",
        "Developer", LocalDate.of(2025, 1, 1), department, manager)).id();
  }

  private LeaveCreateRequest request(LeaveType type, String start, String end) {
    return new LeaveCreateRequest(type, LocalDate.parse(start), LocalDate.parse(end), "Holiday");
  }

  private void seed(Long employee, LeaveType type, RequestStatus status, String start, String end) {
    requests.save(LeaveRequest.builder().employeeId(employee).leaveType(type).status(status)
        .startDate(LocalDate.parse(start)).endDate(LocalDate.parse(end)).reason("Holiday").build());
  }
}
