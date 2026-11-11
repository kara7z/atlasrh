package com.example.atlasrh.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.entity.Employee;
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
  void searchReturnsEmployeePageWithStableSort() {
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
  void findByIdReturnsEmployee() {
    when(repository.findById(1L)).thenReturn(Optional.of(employee));
    when(mapper.toDto(employee)).thenReturn(employeeDto);

    Optional<EmployeeDto> result = service.findById(1L);
    System.out.println("findById result: " + result);
    assertThat(result).contains(employeeDto);
    verify(repository).findById(1L);
  }

  @Test
  void findTeamReturnsTeamMembers() {
    when(repository.existsById(2L)).thenReturn(true);
    when(repository.findByManagerId(2L)).thenReturn(List.of(employee));
    when(mapper.toDto(employee)).thenReturn(employeeDto);

    List<EmployeeDto> result = service.findTeam(2L);
    System.out.println("findTeam result: " + result);
    assertThat(result).containsExactly(employeeDto);
    verify(repository).findByManagerId(2L);
  }

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
  void updateSavesEmployeeWithExistingId() {
    when(repository.existsById(1L)).thenReturn(true);
    when(departmentRepository.existsById(10L)).thenReturn(true);
    when(mapper.toEntity(employeeDto)).thenReturn(employee);
    when(repository.save(employee)).thenReturn(employee);
    when(mapper.toDto(employee)).thenReturn(employeeDto);

    EmployeeDto result = service.update(1L, request);
    System.out.println("update result: " + result);
    assertThat(result).isEqualTo(employeeDto);
    verify(repository).existsByEmployeeNumberAndIdNot(request.employeeNumber(), 1L);
    verify(repository).existsByEmailAndIdNot(request.email(), 1L);
    verify(repository).save(employee);
  }
}
