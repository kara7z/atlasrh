package com.example.atlasrh.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.example.atlasrh.entity.Department;
import com.example.atlasrh.entity.Employee;

@DataJpaTest
class EmployeeRepositoryTest {
  @Autowired
  private EmployeeRepository repository;
  @Autowired
  private DepartmentRepository departmentRepository;

  @Test
  void shouldSearchEmployeesByNameIgnoringCase() {
    persistEmployees();

    Page<Employee> page = repository.search("amra", null, PageRequest.of(0, 20));

    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent()).extracting(Employee::getLastName).containsExactly("Amrani");
  }

  @Test
  void shouldFilterEmployeesByDepartment() {
    List<Employee> employees = persistEmployees();

    Page<Employee> page = repository.search(null, employees.get(2).getDepartmentId(),
        PageRequest.of(0, 20));

    assertThat(page.getContent()).extracting(Employee::getEmployeeNumber).containsExactly("EMP-003");
  }

  @Test
  void shouldApplyPaginationAndSorting() {
    persistEmployees();

    Page<Employee> page = repository.search(null, null,
        PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "lastName")));

    assertThat(page.getTotalElements()).isEqualTo(3);
    assertThat(page.getContent()).extracting(Employee::getLastName).containsExactly("Zeroual", "Chakir");
  }

  private List<Employee> persistEmployees() {
    Long engineeringId = departmentRepository.save(Department.builder().name("Engineering").build()).getId();
    Long financeId = departmentRepository.save(Department.builder().name("Finance").build()).getId();
    return repository.saveAll(List.of(
        employee("EMP-001", "Sara", "Amrani", engineeringId),
        employee("EMP-002", "Omar", "Chakir", engineeringId),
        employee("EMP-003", "Nadia", "Zeroual", financeId)));
  }

  private Employee employee(String number, String firstName, String lastName, Long departmentId) {
    return Employee.builder()
        .employeeNumber(number)
        .firstName(firstName)
        .lastName(lastName)
        .email(number.toLowerCase() + "@example.com")
        .position("Developer")
        .hireDate(LocalDate.of(2025, 1, 6))
        .departmentId(departmentId)
        .build();
  }
}
