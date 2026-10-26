package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.data.relational.core.conversion.DbAction;
import org.springframework.data.relational.core.conversion.IdValueSource;

import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.dto.DepartmentRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.exception.DepartmentConflictException;
import com.example.atlasrh.exception.DepartmentNotFoundException;
import com.example.atlasrh.mapper.DepartmentMapper;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.impl.DepartmentServiceImpl;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTests {
  @Mock
  private DepartmentRepository repository;
  @Mock
  private EmployeeRepository employeeRepository;

  private DepartmentService service;
  private final DepartmentRequest request = new DepartmentRequest("Engineering", "Product development");

  @BeforeEach
  void setUp() {
    service = new DepartmentServiceImpl(repository, Mappers.getMapper(DepartmentMapper.class), employeeRepository);
  }

  @Test
  void createsANewDepartmentWithoutAnId() {
    when(repository.save(any(Department.class))).thenAnswer(invocation -> {
      Department entity = invocation.getArgument(0);
      assertThat(entity.getId()).isNull();
      assertThat(entity.getEmployees()).isEmpty();
      return department(1L, entity.getName(), entity.getDescription());
    });

    assertThat(service.create(request)).isEqualTo(new DepartmentDto(1L, request.name(), request.description()));
  }

  @Test
  void rejectsDuplicateNamesOnCreate() {
    when(repository.existsByName(request.name())).thenReturn(true);

    assertThatThrownBy(() -> service.create(request)).isInstanceOf(DepartmentConflictException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void updatesTheExistingDepartmentAndPreservesItsId() {
    when(repository.findById(1L)).thenReturn(Optional.of(department(1L, "Old", null)));
    when(repository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(service.update(1L, request)).isEqualTo(new DepartmentDto(1L, request.name(), request.description()));
    verify(repository).existsByNameAndIdNot(request.name(), 1L);
  }

  @Test
  void permitsKeepingTheSameNameAndClearingDescription() {
    when(repository.findById(1L)).thenReturn(Optional.of(department(1L, request.name(), "Old")));
    when(repository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

    assertThat(service.update(1L, new DepartmentRequest(request.name(), null)))
        .isEqualTo(new DepartmentDto(1L, request.name(), null));
  }

  @Test
  void rejectsDuplicateNamesOnUpdate() {
    when(repository.findById(1L)).thenReturn(Optional.of(department(1L, "Old", null)));
    when(repository.existsByNameAndIdNot(request.name(), 1L)).thenReturn(true);

    assertThatThrownBy(() -> service.update(1L, request)).isInstanceOf(DepartmentConflictException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsUpdatesOfMissingDepartments() {
    assertThatThrownBy(() -> service.update(99L, request)).isInstanceOf(DepartmentNotFoundException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void deletesEmptyDepartments() {
    Department entity = department(1L, request.name(), null);
    when(repository.findById(1L)).thenReturn(Optional.of(entity));

    service.delete(1L);

    verify(employeeRepository).existsByDepartmentId(1L);
    verify(repository).delete(entity);
  }

  @Test
  void rejectsDeletingDepartmentsWithEmployees() {
    when(repository.findById(1L)).thenReturn(Optional.of(department(1L, request.name(), null)));
    when(employeeRepository.existsByDepartmentId(1L)).thenReturn(true);

    assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(DepartmentConflictException.class);
    verify(repository, never()).delete(any());
  }

  @Test
  void rejectsDeletingMissingDepartments() {
    assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(DepartmentNotFoundException.class);
    verify(repository, never()).delete(any());
  }

  @Test
  void translatesDatabaseDuplicateNameRaces() {
    when(repository.save(any(Department.class))).thenThrow(new DuplicateKeyException("duplicate name"));

    assertThatThrownBy(() -> service.create(request)).isInstanceOf(DepartmentConflictException.class);
  }

  @Test
  void translatesWrappedJdbcDuplicateNameRaces() {
    Department entity = department(null, request.name(), null);
    when(repository.save(any(Department.class))).thenThrow(new DbActionExecutionException(
        new DbAction.InsertRoot<>(entity, IdValueSource.GENERATED),
        new DuplicateKeyException("duplicate name")));

    assertThatThrownBy(() -> service.create(request)).isInstanceOf(DepartmentConflictException.class);
  }

  @Test
  void translatesDatabaseForeignKeyRacesOnDelete() {
    Department entity = department(1L, request.name(), null);
    when(repository.findById(1L)).thenReturn(Optional.of(entity));
    doThrow(new DataIntegrityViolationException("employee added concurrently"))
        .when(repository).delete(entity);

    assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(DepartmentConflictException.class);
  }

  @Test
  void doesNotHideOtherDatabaseFailures() {
    DataIntegrityViolationException failure = new DataIntegrityViolationException("unrelated failure");
    when(repository.save(any(Department.class))).thenThrow(failure);

    assertThatThrownBy(() -> service.create(request)).isSameAs(failure);
  }

  private Department department(Long id, String name, String description) {
    return Department.builder().id(id).name(name).description(description).build();
  }
}
