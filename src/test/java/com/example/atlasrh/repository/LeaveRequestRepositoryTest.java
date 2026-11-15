package com.example.atlasrh.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.atlasrh.entity.Department;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;

@DataJpaTest
class LeaveRequestRepositoryTest {
  @Autowired
  private LeaveRequestRepository repository;
  @Autowired
  private EmployeeRepository employeeRepository;
  @Autowired
  private DepartmentRepository departmentRepository;

  @Test
  void shouldReturnOnlyApprovedLeaveOfTheRequestedYear() {
    Long employeeId = persistEmployee();
    persistLeave(employeeId, LeaveType.ANNUAL, RequestStatus.APPROVED, "Approved annual");
    persistLeave(employeeId, LeaveType.EXCEPTIONAL, RequestStatus.APPROVED, "Approved exceptional");
    persistLeave(employeeId, LeaveType.ANNUAL, RequestStatus.REJECTED, "Rejected annual");

    List<LeaveRequest> result = repository.findApprovedForYear(employeeId, LeaveType.ANNUAL,
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    assertThat(result).extracting(LeaveRequest::getReason).containsExactly("Approved annual");
  }

  @Test
  void shouldIncludeLeaveOverlappingTheStartOfTheYear() {
    Long employeeId = persistEmployee();
    repository.save(LeaveRequest.builder()
        .employeeId(employeeId)
        .leaveType(LeaveType.ANNUAL)
        .startDate(LocalDate.of(2025, 12, 29))
        .endDate(LocalDate.of(2026, 1, 2))
        .reason("Year boundary")
        .status(RequestStatus.APPROVED)
        .build());

    List<LeaveRequest> result = repository.findApprovedForYear(employeeId, LeaveType.ANNUAL,
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

    assertThat(result).extracting(LeaveRequest::getReason).containsExactly("Year boundary");
  }

  private Long persistEmployee() {
    Long departmentId = departmentRepository.save(Department.builder().name("Engineering").build()).getId();
    return employeeRepository.save(Employee.builder()
        .employeeNumber("EMP-001")
        .firstName("Sara")
        .lastName("Amrani")
        .email("sara@example.com")
        .position("Developer")
        .hireDate(LocalDate.of(2025, 1, 6))
        .departmentId(departmentId)
        .build()).getId();
  }

  private void persistLeave(Long employeeId, LeaveType type, RequestStatus status, String reason) {
    repository.save(LeaveRequest.builder()
        .employeeId(employeeId)
        .leaveType(type)
        .startDate(LocalDate.of(2026, 3, 2))
        .endDate(LocalDate.of(2026, 3, 6))
        .reason(reason)
        .status(status)
        .build());
  }
}
