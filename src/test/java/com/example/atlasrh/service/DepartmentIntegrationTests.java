package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.dto.DepartmentRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.exception.DepartmentConflictException;
import com.example.atlasrh.exception.DepartmentNotFoundException;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;

@SpringBootTest
@Transactional
class DepartmentIntegrationTests {
  @Autowired
  private DepartmentService service;
  @Autowired
  private DepartmentRepository repository;
  @Autowired
  private EmployeeRepository employeeRepository;

  @Test
  void crudPersistsAndReturnsMappedDtos() {
    DepartmentDto created = service.create(new DepartmentRequest(name(), "Original"));
    assertThat(created.id()).isNotNull();
    assertThat(service.findById(created.id())).contains(created);
    assertThat(service.findAll()).contains(created);

    DepartmentDto updated = service.update(created.id(), new DepartmentRequest(name(), "Updated"));
    assertThat(updated.id()).isEqualTo(created.id());
    assertThat(service.findById(created.id())).contains(updated);

    service.delete(created.id());
    assertThat(service.findById(created.id())).isEmpty();
    assertThatThrownBy(() -> service.delete(created.id())).isInstanceOf(DepartmentNotFoundException.class);
  }

  @Test
  void duplicateNamesAreRejectedOnCreateAndUpdate() {
    DepartmentDto first = service.create(new DepartmentRequest(name(), null));
    DepartmentDto second = service.create(new DepartmentRequest(name(), null));

    assertThatThrownBy(() -> service.create(new DepartmentRequest(first.name(), null)))
        .isInstanceOf(DepartmentConflictException.class);
    assertThatThrownBy(() -> service.update(second.id(), new DepartmentRequest(first.name(), null)))
        .isInstanceOf(DepartmentConflictException.class);
  }

  @Test
  void unchangedNamesRemainValid() {
    DepartmentDto created = service.create(new DepartmentRequest(name(), "Original"));

    assertThat(service.update(created.id(), new DepartmentRequest(created.name(), null)))
        .isEqualTo(new DepartmentDto(created.id(), created.name(), null));
  }

  @Test
  void employeeReferencesBlockDeletion() {
    DepartmentDto department = service.create(new DepartmentRequest(name(), null));
    String number = UUID.randomUUID().toString();
    employeeRepository.save(Employee.builder()
        .employeeNumber(number)
        .firstName("Sara")
        .lastName("Amrani")
        .email(number + "@example.com")
        .position("Developer")
        .hireDate(LocalDate.of(2026, 1, 1))
        .departmentId(department.id())
        .build());

    assertThatThrownBy(() -> service.delete(department.id())).isInstanceOf(DepartmentConflictException.class);
    assertThat(repository.existsById(department.id())).isTrue();
  }

  @Test
  void databaseEnforcesNameUniquenessIndependentlyOfTheService() {
    String name = name();
    repository.save(Department.builder().name(name).build());

    assertThatThrownBy(() -> repository.save(Department.builder().name(name).build()))
        .isInstanceOf(DuplicateKeyException.class);
  }

  private String name() {
    return "ATLAS-9-" + UUID.randomUUID();
  }
}
