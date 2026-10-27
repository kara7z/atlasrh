package com.example.atlasrh.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.example.atlasrh.enums.ContractType;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class DtoValidationTests {
  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  @Test
  void departmentRequiresABoundedNonblankName() {
    assertThat(fields(new DepartmentRequest(null, null))).containsExactly("name");
    assertThat(fields(new DepartmentRequest(" ", null))).containsExactly("name");
    assertThat(fields(new DepartmentRequest("a".repeat(256), null))).containsExactly("name");
    assertThat(fields(new DepartmentDto(null, "", null))).containsExactly("name");
    assertThat(validator.validate(new DepartmentRequest("a".repeat(255), null))).isEmpty();
    assertThat(validator.validate(new DepartmentDto(null, "Engineering", null))).isEmpty();
  }

  @Test
  void employeeRequiresAllRecordFields() {
    assertThat(fields(new EmployeeDto(null, null, null, null, null, null, null, null, null)))
        .containsExactlyInAnyOrder("employeeNumber", "firstName", "lastName", "email", "position", "hireDate", "departmentId");
  }

  @Test
  void employeeRejectsInvalidEmailIdsAndOversizedStrings() {
    String oversized = "a".repeat(256);
    EmployeeDto dto = new EmployeeDto(-1L, oversized, oversized, oversized, "not-an-email", oversized,
        LocalDate.of(2026, 1, 1), 0L, -1L);

    assertThat(fields(dto)).containsExactlyInAnyOrder("id", "employeeNumber", "firstName", "lastName", "email",
        "position", "departmentId", "managerId");
  }

  @Test
  void employeeAllowsGeneratedIdAndNoManager() {
    EmployeeDto dto = new EmployeeDto(null, "EMP-001", "Sara", "Amrani", "sara@example.com", "Developer",
        LocalDate.of(2026, 1, 1), 1L, null);

    assertThat(validator.validate(dto)).isEmpty();
  }

  @Test
  void contractRequiresTypeStartSalaryAndEmployee() {
    assertThat(fields(new ContractDto(null, null, null, null, null, null)))
        .containsExactlyInAnyOrder("type", "startDate", "grossSalary", "employeeId");
  }

  @Test
  void contractRejectsInvalidSalaryPrecisionAndIds() {
    ContractDto dto = new ContractDto(0L, ContractType.PERMANENT, LocalDate.of(2026, 1, 1), null,
        new BigDecimal("-1.234"), -1L);

    assertThat(fields(dto)).containsExactlyInAnyOrder("id", "grossSalary", "employeeId");
    assertThat(fields(new ContractDto(null, ContractType.PERMANENT, LocalDate.of(2026, 1, 1), null,
        new BigDecimal("100000000000000000.00"), 1L))).containsExactly("grossSalary");
  }

  @Test
  void contractAllowsAnOpenEndedPermanentContract() {
    ContractDto dto = new ContractDto(null, ContractType.PERMANENT, LocalDate.of(2026, 1, 1), null,
        new BigDecimal("12345.67"), 1L);

    assertThat(validator.validate(dto)).isEmpty();
  }

  @Test
  void leaveRequiresEmployeeTypeDatesReasonAndStatus() {
    assertThat(fields(new LeaveRequestDto(null, null, null, null, null, " ", null, null)))
        .containsExactlyInAnyOrder("employeeId", "leaveType", "startDate", "endDate", "reason", "status");
  }

  @Test
  void leaveRejectsNonpositiveIds() {
    LeaveRequestDto dto = new LeaveRequestDto(-1L, 0L, LeaveType.ANNUAL, LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 2), "Vacation", RequestStatus.PENDING, null);

    assertThat(fields(dto)).containsExactlyInAnyOrder("id", "employeeId");
  }

  @Test
  void leaveAllowsOptionalIdAndComment() {
    LeaveRequestDto dto = new LeaveRequestDto(null, 1L, LeaveType.ANNUAL, LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 2), "Vacation", RequestStatus.PENDING, null);

    assertThat(validator.validate(dto)).isEmpty();
  }

  private Set<String> fields(Object dto) {
    return validator.validate(dto).stream().map(violation -> violation.getPropertyPath().toString())
        .collect(Collectors.toSet());
  }
}
