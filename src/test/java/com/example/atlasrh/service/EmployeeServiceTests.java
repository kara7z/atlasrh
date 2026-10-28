package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.relational.core.conversion.DbAction;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.data.relational.core.conversion.IdValueSource;

import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.EmployeeMapper;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.impl.EmployeeServiceImpl;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTests {
  @Mock
  private EmployeeRepository repository;
  @Mock
  private DepartmentRepository departmentRepository;
  private EmployeeService service;

  @BeforeEach
  void setUp() {
    service = new EmployeeServiceImpl(repository, Mappers.getMapper(EmployeeMapper.class), departmentRepository);
  }

  @Test
  void createsEmployeeWithGeneratedIdAndOptionalManager() {
    when(departmentRepository.existsById(1L)).thenReturn(true);
    when(repository.save(any(Employee.class))).thenAnswer(invocation -> {
      Employee employee = invocation.getArgument(0);
      assertThat(employee.getId()).isNull();
      assertThat(employee.getEmployeeNumber()).isEqualTo("EMP-1");
      assertThat(employee.getManagerId()).isNull();
      return Employee.builder().id(2L).employeeNumber(employee.getEmployeeNumber()).build();
    });
    assertThat(service.create(request(null)).id()).isEqualTo(2L);
  }

  @Test
  void updatesAllFieldsAndCanClearManagerWithoutChangingId() {
    when(repository.existsById(2L)).thenReturn(true);
    when(departmentRepository.existsById(1L)).thenReturn(true);
    when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var result = service.update(2L, request(null));
    assertThat(result.id()).isEqualTo(2L);
    assertThat(result.employeeNumber()).isEqualTo("EMP-1");
    assertThat(result.firstName()).isEqualTo("Sara");
    assertThat(result.lastName()).isEqualTo("Amrani");
    assertThat(result.email()).isEqualTo("sara@example.com");
    assertThat(result.position()).isEqualTo("Developer");
    assertThat(result.hireDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(result.departmentId()).isEqualTo(1L);
    assertThat(result.managerId()).isNull();
    verify(repository).existsByEmployeeNumberAndIdNot("EMP-1", 2L);
    verify(repository).existsByEmailAndIdNot("sara@example.com", 2L);
  }

  @Test
  void acceptsAnExistingManager() {
    when(departmentRepository.existsById(1L)).thenReturn(true);
    when(repository.existsById(3L)).thenReturn(true);
    when(repository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));
    assertThat(service.create(request(3L)).managerId()).isEqualTo(3L);
  }

  @Test
  void rejectsDuplicateNumberOnCreate() {
    when(repository.existsByEmployeeNumber("EMP-1")).thenReturn(true);
    assertThatThrownBy(() -> service.create(request(null))).isInstanceOf(ConflictException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsDuplicateEmailOnCreate() {
    when(repository.existsByEmail("sara@example.com")).thenReturn(true);
    assertThatThrownBy(() -> service.create(request(null))).isInstanceOf(ConflictException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsDuplicateNumberOnUpdate() {
    when(repository.existsById(2L)).thenReturn(true);
    when(repository.existsByEmployeeNumberAndIdNot("EMP-1", 2L)).thenReturn(true);
    assertThatThrownBy(() -> service.update(2L, request(null))).isInstanceOf(ConflictException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsDuplicateEmailOnUpdate() {
    when(repository.existsById(2L)).thenReturn(true);
    when(repository.existsByEmailAndIdNot("sara@example.com", 2L)).thenReturn(true);
    assertThatThrownBy(() -> service.update(2L, request(null))).isInstanceOf(ConflictException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsMissingEmployeeOnUpdate() {
    assertThatThrownBy(() -> service.update(99L, request(null))).isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsSelfManagement() {
    when(repository.existsById(2L)).thenReturn(true);
    assertThatThrownBy(() -> service.update(2L, request(2L))).isInstanceOf(BadRequestException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsMissingDepartmentOnCreateAndUpdate() {
    assertThatThrownBy(() -> service.create(request(null))).isInstanceOf(ResourceNotFoundException.class);
    when(repository.existsById(2L)).thenReturn(true);
    assertThatThrownBy(() -> service.update(2L, request(null))).isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsMissingManagerOnCreateAndUpdate() {
    when(departmentRepository.existsById(1L)).thenReturn(true);
    assertThatThrownBy(() -> service.create(request(3L))).isInstanceOf(ResourceNotFoundException.class);
    when(repository.existsById(2L)).thenReturn(true);
    assertThatThrownBy(() -> service.update(2L, request(3L))).isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void translatesDuplicateKeyRaces() {
    when(departmentRepository.existsById(1L)).thenReturn(true);
    when(repository.save(any(Employee.class))).thenThrow(new DuplicateKeyException("duplicate"));
    assertThatThrownBy(() -> service.create(request(null))).isInstanceOf(ConflictException.class);
  }

  @Test
  void translatesWrappedJdbcDuplicateKeyRaces() {
    when(departmentRepository.existsById(1L)).thenReturn(true);
    when(repository.save(any(Employee.class))).thenThrow(new DbActionExecutionException(
        new DbAction.InsertRoot<>(new Employee(), IdValueSource.GENERATED), new DuplicateKeyException("duplicate")));
    assertThatThrownBy(() -> service.create(request(null))).isInstanceOf(ConflictException.class);
  }

  @Test
  void doesNotHideUnrelatedDatabaseErrors() {
    when(departmentRepository.existsById(1L)).thenReturn(true);
    var failure = new DataIntegrityViolationException("unrelated");
    when(repository.save(any(Employee.class))).thenThrow(failure);
    assertThatThrownBy(() -> service.create(request(null))).isSameAs(failure);
  }

  private EmployeeRequest request(Long managerId) {
    return new EmployeeRequest("EMP-1", "Sara", "Amrani", "sara@example.com", "Developer",
        LocalDate.of(2026, 1, 1), 1L, managerId);
  }
}
