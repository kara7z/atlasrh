package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.dto.EmployeeBalanceDto;
import com.example.atlasrh.dto.LeaveBalanceDto;
import com.example.atlasrh.dto.LeaveCreateRequest;
import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.LeaveRequestRepository;

@SpringBootTest
@Transactional
class EmployeeBalanceIntegrationTests {
  @Autowired
  private EmployeeBalanceService service;
  @Autowired
  private EmployeeService employeeService;
  @Autowired
  private LeaveRequestService leaveService;
  @Autowired
  private DepartmentRepository departments;
  @Autowired
  private LeaveRequestRepository requests;

  @Test
  void employeeWithoutRequestsHasFullAllowanceAndFutureHireHasZeroSeniority() {
    Long employee = employee(department(), null, LocalDate.now(ZoneOffset.UTC).plusYears(1));
    var result = service.balance(employee, null);
    assertThat(result.year()).isEqualTo(result.asOf().getYear());
    assertThat(result.seniority().years()).isZero();
    assertThat(result.seniority().months()).isZero();
    assertThat(balance(result, LeaveType.ANNUAL)).isEqualTo(new LeaveBalanceDto(LeaveType.ANNUAL, 22, 0, 22));
    assertThat(balance(result, LeaveType.EXCEPTIONAL)).isEqualTo(new LeaveBalanceDto(LeaveType.EXCEPTIONAL, 5, 0, 5));
  }

  @Test
  void countsOnlyApprovedDaysOfEachTypeWithinSelectedYearAndEmployee() {
    Long department = department();
    Long employee = employee(department, null, LocalDate.of(2020, 8, 15));
    seed(employee, LeaveType.ANNUAL, RequestStatus.APPROVED, "2026-12-28", "2027-01-05");
    seed(employee, LeaveType.EXCEPTIONAL, RequestStatus.APPROVED, "2026-11-06", "2026-11-09");
    seed(employee, LeaveType.ANNUAL, RequestStatus.PENDING, "2026-10-05", "2026-10-09");
    seed(employee, LeaveType.ANNUAL, RequestStatus.REJECTED, "2026-10-12", "2026-10-16");
    seed(employee, LeaveType.ANNUAL, RequestStatus.CANCELLED, "2026-10-19", "2026-10-23");
    seed(employee, LeaveType.ANNUAL, RequestStatus.APPROVED, "2025-01-06", "2025-01-10");
    seed(employee(department, null, LocalDate.of(2020, 1, 1)), LeaveType.ANNUAL,
        RequestStatus.APPROVED, "2026-11-02", "2026-11-06");

    var current = service.balance(employee, 2026);
    assertThat(balance(current, LeaveType.ANNUAL)).isEqualTo(new LeaveBalanceDto(LeaveType.ANNUAL, 22, 4, 18));
    assertThat(balance(current, LeaveType.EXCEPTIONAL)).isEqualTo(new LeaveBalanceDto(LeaveType.EXCEPTIONAL, 5, 2, 3));
    assertThat(balance(service.balance(employee, 2027), LeaveType.ANNUAL))
        .isEqualTo(new LeaveBalanceDto(LeaveType.ANNUAL, 22, 3, 19));
    assertThat(balance(service.balance(employee, 2025), LeaveType.ANNUAL).approvedDays()).isEqualTo(5);
  }

  @Test
  void balanceReflectsApprovalAndCancellationWithoutManualEditing() {
    Long department = department();
    Long manager = employee(department, null, LocalDate.of(2020, 1, 1));
    Long employee = employee(department, manager, LocalDate.of(2020, 1, 1));
    var leave = leaveService.create(employee, new LeaveCreateRequest(LeaveType.ANNUAL,
        LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 9), "Holiday"));
    assertThat(balance(service.balance(employee, 2026), LeaveType.ANNUAL).remainingDays()).isEqualTo(22);
    leaveService.decide(leave.id(), new LeaveDecisionRequest(manager, RequestStatus.APPROVED, null));
    assertThat(balance(service.balance(employee, 2026), LeaveType.ANNUAL).remainingDays()).isEqualTo(20);
    var pending = leaveService.create(employee, new LeaveCreateRequest(LeaveType.ANNUAL,
        LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11), "Other holiday"));
    leaveService.cancel(employee, pending.id());
    assertThat(balance(service.balance(employee, 2026), LeaveType.ANNUAL).remainingDays()).isEqualTo(20);
  }

  @Test
  void historicalOverdrawIsShownRatherThanHiddenAndMissingEmployeeReturns404() {
    Long employee = employee(department(), null, LocalDate.of(2020, 1, 1));
    seed(employee, LeaveType.EXCEPTIONAL, RequestStatus.APPROVED, "2026-11-02", "2026-11-09");
    assertThat(balance(service.balance(employee, 2026), LeaveType.EXCEPTIONAL).remainingDays()).isEqualTo(-1);
    assertThatThrownBy(() -> service.balance(Long.MAX_VALUE, 2026)).isInstanceOf(ResourceNotFoundException.class);
  }

  private LeaveBalanceDto balance(EmployeeBalanceDto result, LeaveType type) {
    return result.balances().stream().filter(item -> item.leaveType() == type).findFirst().orElseThrow();
  }

  private Long department() {
    return departments.save(Department.builder().name("ATLAS-21-" + UUID.randomUUID()).build()).getId();
  }

  private Long employee(Long department, Long manager, LocalDate hireDate) {
    String number = UUID.randomUUID().toString();
    return employeeService.create(new EmployeeRequest(number, "Sara", "Amrani", number + "@example.com",
        "Developer", hireDate, department, manager)).id();
  }

  private void seed(Long employee, LeaveType type, RequestStatus status, String start, String end) {
    requests.save(LeaveRequest.builder().employeeId(employee).leaveType(type).status(status)
        .startDate(LocalDate.parse(start)).endDate(LocalDate.parse(end)).reason("Holiday").build());
  }
}
