package com.example.atlasrh.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.atlasrh.entity.Contract;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.enums.ContractType;

@DataJpaTest
class ContractRepositoryTest {
  @Autowired
  private ContractRepository repository;
  @Autowired
  private EmployeeRepository employeeRepository;
  @Autowired
  private DepartmentRepository departmentRepository;

  @Test
  void shouldDetectAnOverlappingContract() {
    Long employeeId = persistEmployee();
    persistContract(employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30));

    boolean overlapping = repository.existsOverlapping(employeeId,
        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));

    assertThat(overlapping).isTrue();
  }

  @Test
  void shouldDetectAnOpenEndedContract() {
    Long employeeId = persistEmployee();
    persistContract(employeeId, LocalDate.of(2026, 1, 1), null);

    boolean overlapping = repository.existsOverlapping(employeeId,
        LocalDate.of(2026, 7, 1), null);

    assertThat(overlapping).isTrue();
  }

  @Test
  void shouldIgnoreAContractEndingBeforeThePeriod() {
    Long employeeId = persistEmployee();
    persistContract(employeeId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31));

    boolean overlapping = repository.existsOverlapping(employeeId,
        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31));

    assertThat(overlapping).isFalse();
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

  private void persistContract(Long employeeId, LocalDate startDate, LocalDate endDate) {
    repository.save(Contract.builder()
        .type(ContractType.PERMANENT)
        .startDate(startDate)
        .endDate(endDate)
        .grossSalary(new BigDecimal("3000.00"))
        .employeeId(employeeId)
        .build());
  }
}
