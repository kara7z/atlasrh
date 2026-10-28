package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.repository.DepartmentRepository;

@SpringBootTest
@Transactional
class EmployeeIntegrationTests {
  @Autowired
  private EmployeeService service;
  @Autowired
  private DepartmentRepository departmentRepository;

  @Test
  void persistsCreatesAndUpdatesWithOptionalManager() {
    Long departmentId = departmentId();
    var manager = service.create(request(departmentId, null));
    var request = request(departmentId, manager.id());
    var employee = service.create(request);
    assertThat(employee.id()).isNotNull();
    assertThat(service.findById(employee.id())).contains(employee);

    var updated = service.update(employee.id(), new EmployeeRequest(request.employeeNumber(), "Updated",
        request.lastName(), request.email(), "Lead", request.hireDate(), departmentId, null));
    assertThat(updated.id()).isEqualTo(employee.id());
    assertThat(updated.firstName()).isEqualTo("Updated");
    assertThat(updated.managerId()).isNull();
    assertThat(service.findById(employee.id())).contains(updated);
  }

  @Test
  void rejectsDuplicateNumbersAndEmailsAndSelfManagement() {
    Long departmentId = departmentId();
    var first = service.create(request(departmentId, null));
    var second = service.create(request(departmentId, null));
    var duplicateNumber = new EmployeeRequest(first.employeeNumber(), "Sara", "Amrani", second.email(),
        "Developer", first.hireDate(), departmentId, null);
    var duplicateEmail = new EmployeeRequest(second.employeeNumber(), "Sara", "Amrani", first.email(),
        "Developer", first.hireDate(), departmentId, null);

    assertThatThrownBy(() -> service.create(duplicateNumber)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.create(duplicateEmail)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.update(second.id(), duplicateNumber)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.update(second.id(), duplicateEmail)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.update(second.id(), request(departmentId, second.id())))
        .isInstanceOf(BadRequestException.class);
  }

  private Long departmentId() {
    return departmentRepository.save(Department.builder().name("ATLAS-13-" + UUID.randomUUID()).build()).getId();
  }

  private EmployeeRequest request(Long departmentId, Long managerId) {
    String number = UUID.randomUUID().toString();
    return new EmployeeRequest(number, "Sara", "Amrani", number + "@example.com", "Developer",
        LocalDate.of(2026, 1, 1), departmentId, managerId);
  }
}
