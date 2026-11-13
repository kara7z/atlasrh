package com.example.atlasrh.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;

import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.dto.DepartmentRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.exception.DepartmentConflictException;
import com.example.atlasrh.exception.DepartmentNotFoundException;
import com.example.atlasrh.mapper.DepartmentMapper;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {
  @Mock
  private DepartmentRepository repository;
  @Mock
  private DepartmentMapper mapper;
  @Mock
  private EmployeeRepository employeeRepository;
  @InjectMocks
  private DepartmentServiceImpl service;

  private final Department department = new Department();
  private final DepartmentRequest request = new DepartmentRequest("Engineering", "Development team");
  private final DepartmentDto departmentDto = new DepartmentDto(1L, "Engineering", "Development team");

  @BeforeEach
  void printTestMethodName(TestInfo testInfo) {
    System.out.println("\nRunning test: " + testInfo.getTestMethod().orElseThrow().getName());
  }

  @Nested
  class ReadOperations {
    @Test
    void findAllReturnsDepartments() {
      when(repository.findAll()).thenReturn(List.of(department));
      when(mapper.toDto(department)).thenReturn(departmentDto);

      List<DepartmentDto> result = service.findAll();
      System.out.println("findAll result: " + result);

      assertThat(result).containsExactly(departmentDto);
      verify(repository).findAll();
    }

    @Test
    void findByIdReturnsDepartment() {
      when(repository.findById(1L)).thenReturn(Optional.of(department));
      when(mapper.toDto(department)).thenReturn(departmentDto);

      Optional<DepartmentDto> result = service.findById(1L);
      System.out.println("findById result: " + result);

      assertThat(result).contains(departmentDto);
      verify(repository).findById(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
      when(repository.findById(99L)).thenReturn(Optional.empty());

      Optional<DepartmentDto> result = service.findById(99L);
      System.out.println("findById missing result: " + result);

      assertThat(result).isEmpty();
      verify(repository).findById(99L);
    }
  }

  @Nested
  class CreateOperations {
    @Test
    void createSavesDepartment() {
      DepartmentDto newDepartmentDto = new DepartmentDto(null, request.name(), request.description());
      when(mapper.toEntity(newDepartmentDto)).thenReturn(department);
      when(repository.save(department)).thenReturn(department);
      when(mapper.toDto(department)).thenReturn(departmentDto);

      DepartmentDto result = service.create(request);
      System.out.println("create result: " + result);

      assertThat(result).isEqualTo(departmentDto);
      verify(repository).existsByName(request.name());
      verify(repository).save(department);
    }

    @Test
    void createRejectsDuplicateName() {
      when(repository.existsByName(request.name())).thenReturn(true);

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(DepartmentConflictException.class)
          .hasMessageContaining("department with this name already exists");
      verify(repository, never()).save(any());
    }

    @Test
    void createTranslatesDuplicateKeyConstraint() {
      when(mapper.toEntity(any(DepartmentDto.class))).thenReturn(department);
      when(repository.save(department))
          .thenThrow(new DataIntegrityViolationException("duplicate", new DuplicateKeyException("duplicate")));

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(DepartmentConflictException.class)
          .hasMessageContaining("department with this name already exists");
    }
  }

  @Nested
  class UpdateOperations {
    @Test
    void updateChangesDepartmentDetails() {
      department.setName("Old name");
      department.setDescription("Old description");
      when(repository.findById(1L)).thenReturn(Optional.of(department));
      when(repository.save(department)).thenReturn(department);
      when(mapper.toDto(department)).thenReturn(departmentDto);

      DepartmentDto result = service.update(1L, request);
      System.out.println("update result: " + result);

      assertThat(result).isEqualTo(departmentDto);
      assertThat(department.getName()).isEqualTo(request.name());
      assertThat(department.getDescription()).isEqualTo(request.description());
      verify(repository).existsByNameAndIdNot(request.name(), 1L);
      verify(repository).save(department);
    }

    @Test
    void updateRejectsMissingDepartment() {
      when(repository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.update(99L, request))
          .isInstanceOf(DepartmentNotFoundException.class)
          .hasMessageContaining("Department 99 not found");
    }

    @Test
    void updateRejectsDuplicateName() {
      when(repository.findById(1L)).thenReturn(Optional.of(department));
      when(repository.existsByNameAndIdNot(request.name(), 1L)).thenReturn(true);

      assertThatThrownBy(() -> service.update(1L, request))
          .isInstanceOf(DepartmentConflictException.class)
          .hasMessageContaining("department with this name already exists");
      verify(repository, never()).save(any());
    }

    @Test
    void updateTranslatesDuplicateKeyConstraint() {
      when(repository.findById(1L)).thenReturn(Optional.of(department));
      when(repository.save(department))
          .thenThrow(new DataIntegrityViolationException("duplicate", new DuplicateKeyException("duplicate")));

      assertThatThrownBy(() -> service.update(1L, request))
          .isInstanceOf(DepartmentConflictException.class)
          .hasMessageContaining("department with this name already exists");
    }
  }

  @Nested
  class DeleteOperations {
    @Test
    void deleteRemovesDepartmentWithoutEmployees() {
      when(repository.findById(1L)).thenReturn(Optional.of(department));
      when(employeeRepository.existsByDepartmentId(1L)).thenReturn(false);

      service.delete(1L);

      verify(employeeRepository).existsByDepartmentId(1L);
      verify(repository).delete(department);
      System.out.println("delete result: Department 1 deleted successfully");
    }

    @Test
    void deleteRejectsMissingDepartment() {
      when(repository.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.delete(99L))
          .isInstanceOf(DepartmentNotFoundException.class)
          .hasMessageContaining("Department 99 not found");
      verify(repository, never()).delete(any());
    }

    @Test
    void deleteRejectsDepartmentWithEmployees() {
      when(repository.findById(1L)).thenReturn(Optional.of(department));
      when(employeeRepository.existsByDepartmentId(1L)).thenReturn(true);

      assertThatThrownBy(() -> service.delete(1L))
          .isInstanceOf(DepartmentConflictException.class)
          .hasMessageContaining("Cannot delete a department with employees");
      verify(repository, never()).delete(any());
    }

    @Test
    void deleteTranslatesEmployeeReferenceConstraint() {
      when(repository.findById(1L)).thenReturn(Optional.of(department));
      when(employeeRepository.existsByDepartmentId(1L)).thenReturn(false);
      org.mockito.Mockito.doThrow(new DataIntegrityViolationException(
          "reference", new DataIntegrityViolationException("foreign key")))
          .when(repository).delete(department);

      assertThatThrownBy(() -> service.delete(1L))
          .isInstanceOf(DepartmentConflictException.class)
          .hasMessageContaining("Cannot delete a department with employees");
    }

    @Test
    void deletePropagatesUnrelatedIntegrityFailure() {
      when(repository.findById(1L)).thenReturn(Optional.of(department));
      when(employeeRepository.existsByDepartmentId(1L)).thenReturn(false);
      DbActionExecutionException failure = new DbActionExecutionException(
          null, new IllegalStateException("unrelated"));
      org.mockito.Mockito.doThrow(failure).when(repository).delete(department);

      assertThatThrownBy(() -> service.delete(1L)).isSameAs(failure);
    }
  }
}
