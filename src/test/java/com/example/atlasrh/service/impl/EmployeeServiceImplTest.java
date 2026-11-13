package com.example.atlasrh.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.EmployeeMapper;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {
  @Mock
  private EmployeeRepository repository;
  @Mock
  private EmployeeMapper mapper;
  @Mock
  private DepartmentRepository departmentRepository;
  @InjectMocks
  private EmployeeServiceImpl service;

  private final Employee employee = new Employee();
  private final EmployeeRequest request = new EmployeeRequest(
      "EMP-001", "Sara", "Amrani", "sara@example.com", "Developer",
      LocalDate.of(2026, 1, 1), 10L, null);
  private final EmployeeDto employeeDto = new EmployeeDto(
      1L, "EMP-001", "Sara", "Amrani", "sara@example.com", "Developer",
      LocalDate.of(2026, 1, 1), 10L, null);

  @BeforeEach
  void printTestMethodName(TestInfo testInfo) {
    System.out.println("\nRunning test: " + testInfo.getTestMethod().orElseThrow().getName());
  }

  private EmployeeRequest requestWithManager(Long managerId) {
    return new EmployeeRequest(
        "EMP-001", "Sara", "Amrani", "sara@example.com", "Developer",
        LocalDate.of(2026, 1, 1), 10L, managerId);
  }

  @Nested
  class ReadOperations {
    @Test
    void findAllReturnsEmployees() {
      when(repository.findAll()).thenReturn(List.of(employee));
      when(mapper.toDto(employee)).thenReturn(employeeDto);

      List<EmployeeDto> result = service.findAll();
      System.out.println("findAll result: " + result);

      assertThat(result).containsExactly(employeeDto);
      verify(repository).findAll();
    }

    @Test
    void findByIdReturnsEmployee() {
      when(repository.findById(1L)).thenReturn(Optional.of(employee));
      when(mapper.toDto(employee)).thenReturn(employeeDto);

      Optional<EmployeeDto> result = service.findById(1L);
      System.out.println("findById result: " + result);

      assertThat(result).contains(employeeDto);
      verify(repository).findById(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
      when(repository.findById(99L)).thenReturn(Optional.empty());

      Optional<EmployeeDto> result = service.findById(99L);
      System.out.println("findById missing result: " + result);

      assertThat(result).isEmpty();
      verify(repository).findById(99L);
    }

    @Test
    void findTeamReturnsTeamMembers() {
      when(repository.existsById(2L)).thenReturn(true);
      when(repository.findByManagerId(2L)).thenReturn(List.of(employee));
      when(mapper.toDto(employee)).thenReturn(employeeDto);

      List<EmployeeDto> result = service.findTeam(2L);
      System.out.println("findTeam result: " + result);

      assertThat(result).containsExactly(employeeDto);
      verify(repository).existsById(2L);
      verify(repository).findByManagerId(2L);
    }

    @Test
    void findTeamRejectsMissingManager() {
      when(repository.existsById(404L)).thenReturn(false);

      assertThatThrownBy(() -> service.findTeam(404L))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Manager not found: 404");
    }
  }

  @Nested
  class SearchOperations {
    @Test
    void searchAddsIdForStableSort() {
      Pageable requested = PageRequest.of(0, 10, Sort.by("lastName"));
      Pageable stable = PageRequest.of(0, 10, Sort.by("lastName").and(Sort.by("id")));
      when(repository.search("Sara", 10L, stable))
          .thenReturn(new PageImpl<>(List.of(employee), stable, 1));
      when(mapper.toDto(employee)).thenReturn(employeeDto);

      Page<EmployeeDto> result = service.search("Sara", 10L, requested);
      System.out.println("search result: " + result.getContent());

      assertThat(result.getContent()).containsExactly(employeeDto);
      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getSort()).isEqualTo(stable.getSort());
      verify(repository).search("Sara", 10L, stable);
    }

    @Test
    void searchKeepsExplicitIdSort() {
      Pageable requested = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "id"));
      when(repository.search(null, null, requested))
          .thenReturn(new PageImpl<>(List.of(), requested, 0));

      Page<EmployeeDto> result = service.search(null, null, requested);
      System.out.println("search explicit id sort result: " + result.getSort());

      assertThat(result.getSort()).isEqualTo(requested.getSort());
      verify(repository).search(null, null, requested);
    }

    @Test
    void searchRejectsUnpagedRequests() {
      assertThatThrownBy(() -> service.search(null, null, Pageable.unpaged()))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("page size");
    }

    @Test
    void searchRejectsPageSizeAboveOneHundred() {
      Pageable pageable = PageRequest.of(0, 101, Sort.by("lastName"));

      assertThatThrownBy(() -> service.search(null, null, pageable))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("page size");
    }

    @Test
    void searchRejectsNonPositiveDepartmentId() {
      Pageable pageable = PageRequest.of(0, 10, Sort.by("lastName"));

      assertThatThrownBy(() -> service.search(null, 0L, pageable))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Department ID must be positive");
    }

    @Test
    void searchRejectsUnsupportedSortField() {
      Pageable pageable = PageRequest.of(0, 10, Sort.by("salary"));

      assertThatThrownBy(() -> service.search(null, null, pageable))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Unsupported employee sort field: salary");
    }
  }

  @Nested
  class CreateOperations {
    @Test
    void createSavesEmployee() {
      EmployeeDto newEmployeeDto = new EmployeeDto(
          null, request.employeeNumber(), request.firstName(), request.lastName(), request.email(),
          request.position(), request.hireDate(), request.departmentId(), request.managerId());
      when(departmentRepository.existsById(10L)).thenReturn(true);
      when(mapper.toEntity(newEmployeeDto)).thenReturn(employee);
      when(repository.save(employee)).thenReturn(employee);
      when(mapper.toDto(employee)).thenReturn(employeeDto);

      EmployeeDto result = service.create(request);
      System.out.println("create result: " + result);

      assertThat(result).isEqualTo(employeeDto);
      verify(repository).existsByEmployeeNumber(request.employeeNumber());
      verify(repository).existsByEmail(request.email());
      verify(repository).save(employee);
    }

    @Test
    void createRejectsDuplicateEmployeeNumber() {
      when(repository.existsByEmployeeNumber(request.employeeNumber())).thenReturn(true);

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("employee number or email already exists");
    }

    @Test
    void createRejectsDuplicateEmail() {
      when(repository.existsByEmployeeNumber(request.employeeNumber())).thenReturn(false);
      when(repository.existsByEmail(request.email())).thenReturn(true);

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("employee number or email already exists");
    }

    @Test
    void createRejectsMissingDepartment() {
      when(departmentRepository.existsById(10L)).thenReturn(false);

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Department not found: 10");
    }

    @Test
    void createRejectsMissingManager() {
      when(departmentRepository.existsById(10L)).thenReturn(true);
      when(repository.existsById(99L)).thenReturn(false);

      assertThatThrownBy(() -> service.create(requestWithManager(99L)))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Manager not found: 99");
    }

    @Test
    void createTranslatesDuplicateKeyConstraint() {
      when(departmentRepository.existsById(10L)).thenReturn(true);
      when(mapper.toEntity(org.mockito.ArgumentMatchers.any(EmployeeDto.class))).thenReturn(employee);
      when(repository.save(employee))
          .thenThrow(new DataIntegrityViolationException("duplicate", new DuplicateKeyException("duplicate")));

      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("employee number or email already exists");
    }
  }

  @Nested
  class UpdateOperations {
    @Test
    void updateSavesEmployeeWithExistingId() {
      EmployeeDto updatedDto = new EmployeeDto(
          1L, request.employeeNumber(), request.firstName(), request.lastName(), request.email(),
          request.position(), request.hireDate(), request.departmentId(), request.managerId());
      when(repository.existsById(1L)).thenReturn(true);
      when(departmentRepository.existsById(10L)).thenReturn(true);
      when(mapper.toEntity(updatedDto)).thenReturn(employee);
      when(repository.save(employee)).thenReturn(employee);
      when(mapper.toDto(employee)).thenReturn(employeeDto);

      EmployeeDto result = service.update(1L, request);
      System.out.println("update result: " + result);

      assertThat(result).isEqualTo(employeeDto);
      verify(repository).existsByEmployeeNumberAndIdNot(request.employeeNumber(), 1L);
      verify(repository).existsByEmailAndIdNot(request.email(), 1L);
      verify(repository).save(employee);
    }

    @Test
    void updateRejectsMissingEmployee() {
      when(repository.existsById(99L)).thenReturn(false);

      assertThatThrownBy(() -> service.update(99L, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Employee not found: 99");
    }

    @Test
    void updateRejectsDuplicateEmployeeNumber() {
      when(repository.existsById(1L)).thenReturn(true);
      when(repository.existsByEmployeeNumberAndIdNot(request.employeeNumber(), 1L)).thenReturn(true);

      assertThatThrownBy(() -> service.update(1L, request))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("employee number or email already exists");
    }

    @Test
    void updateRejectsDuplicateEmail() {
      when(repository.existsById(1L)).thenReturn(true);
      when(repository.existsByEmployeeNumberAndIdNot(request.employeeNumber(), 1L)).thenReturn(false);
      when(repository.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(true);

      assertThatThrownBy(() -> service.update(1L, request))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("employee number or email already exists");
    }

    @Test
    void updateRejectsEmployeeAsTheirOwnManager() {
      when(repository.existsById(1L)).thenReturn(true);
      when(repository.existsByEmployeeNumberAndIdNot(request.employeeNumber(), 1L)).thenReturn(false);
      when(repository.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);

      assertThatThrownBy(() -> service.update(1L, requestWithManager(1L)))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("cannot be their own manager");
    }

    @Test
    void updateRejectsMissingDepartment() {
      when(repository.existsById(1L)).thenReturn(true);
      when(repository.existsByEmployeeNumberAndIdNot(request.employeeNumber(), 1L)).thenReturn(false);
      when(repository.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
      when(departmentRepository.existsById(10L)).thenReturn(false);

      assertThatThrownBy(() -> service.update(1L, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Department not found: 10");
    }

    @Test
    void updateRejectsMissingManager() {
      when(repository.existsById(1L)).thenReturn(true);
      when(repository.existsByEmployeeNumberAndIdNot(request.employeeNumber(), 1L)).thenReturn(false);
      when(repository.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
      when(departmentRepository.existsById(10L)).thenReturn(true);
      when(repository.existsById(99L)).thenReturn(false);

      assertThatThrownBy(() -> service.update(1L, requestWithManager(99L)))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Manager not found: 99");
    }
  }

  @Nested
  class PersistenceOperations {
    @Test
    void updatePropagatesNonDuplicateIntegrityViolation() {
      when(repository.existsById(1L)).thenReturn(true);
      when(repository.existsByEmployeeNumberAndIdNot(request.employeeNumber(), 1L)).thenReturn(false);
      when(repository.existsByEmailAndIdNot(request.email(), 1L)).thenReturn(false);
      when(departmentRepository.existsById(10L)).thenReturn(true);
      when(mapper.toEntity(org.mockito.ArgumentMatchers.any(EmployeeDto.class))).thenReturn(employee);
      DataIntegrityViolationException failure = new DataIntegrityViolationException("constraint failed");
      when(repository.save(employee)).thenThrow(failure);

      assertThatThrownBy(() -> service.update(1L, request)).isSameAs(failure);
    }
  }
}
