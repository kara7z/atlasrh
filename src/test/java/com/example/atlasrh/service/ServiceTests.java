package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.entity.Contract;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.ContractType;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.mapper.ContractMapper;
import com.example.atlasrh.mapper.DepartmentMapper;
import com.example.atlasrh.mapper.EmployeeMapper;
import com.example.atlasrh.mapper.LeaveRequestMapper;
import com.example.atlasrh.repository.ContractRepository;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.service.impl.ContractServiceImpl;
import com.example.atlasrh.service.impl.DepartmentServiceImpl;
import com.example.atlasrh.service.impl.EmployeeServiceImpl;
import com.example.atlasrh.service.impl.LeaveRequestServiceImpl;

@ExtendWith(MockitoExtension.class)
class ServiceTests {
  @Mock
  private DepartmentRepository departmentRepository;
  @Mock
  private EmployeeRepository employeeRepository;
  @Mock
  private ContractRepository contractRepository;
  @Mock
  private LeaveRequestRepository leaveRequestRepository;

  private DepartmentService departmentService;
  private EmployeeService employeeService;
  private ContractService contractService;
  private LeaveRequestService leaveRequestService;

  @BeforeEach
  void setUp() {
    departmentService = new DepartmentServiceImpl(departmentRepository, Mappers.getMapper(DepartmentMapper.class));
    employeeService = new EmployeeServiceImpl(employeeRepository, Mappers.getMapper(EmployeeMapper.class));
    contractService = new ContractServiceImpl(contractRepository, Mappers.getMapper(ContractMapper.class));
    leaveRequestService = new LeaveRequestServiceImpl(leaveRequestRepository, Mappers.getMapper(LeaveRequestMapper.class));
  }

  @Test
  void departmentsAreMappedInsideTheServiceBoundary() {
    Department entity = new Department();
    entity.setId(1L);
    entity.setName("Engineering");
    entity.setDescription("Product development");
    DepartmentDto expected = new DepartmentDto(1L, "Engineering", "Product development");
    when(departmentRepository.findAll()).thenReturn(List.of(entity));
    when(departmentRepository.findById(1L)).thenReturn(Optional.of(entity));

    assertThat(departmentService.findAll()).containsExactly(expected);
    assertThat(departmentService.findById(1L)).contains(expected);
    verify(departmentRepository).findAll();
    verify(departmentRepository).findById(1L);
  }

  @Test
  void employeesAreMappedInsideTheServiceBoundary() {
    Employee entity = new Employee();
    entity.setId(2L);
    entity.setEmployeeNumber("EMP-002");
    entity.setFirstName("Sara");
    entity.setLastName("Amrani");
    entity.setEmail("sara@example.com");
    entity.setPosition("Developer");
    entity.setHireDate(LocalDate.of(2024, 1, 15));
    entity.setDepartmentId(1L);
    entity.setManagerId(3L);
    EmployeeDto expected = new EmployeeDto(2L, "EMP-002", "Sara", "Amrani",
        "sara@example.com", "Developer", LocalDate.of(2024, 1, 15), 1L, 3L);
    when(employeeRepository.findAll()).thenReturn(List.of(entity));
    when(employeeRepository.findById(2L)).thenReturn(Optional.of(entity));

    assertThat(employeeService.findAll()).containsExactly(expected);
    assertThat(employeeService.findById(2L)).contains(expected);
    verify(employeeRepository).findAll();
    verify(employeeRepository).findById(2L);
  }

  @Test
  void contractsAreMappedInsideTheServiceBoundary() {
    Contract entity = new Contract();
    entity.setId(4L);
    entity.setEmployeeId(2L);
    entity.setType(ContractType.PERMANENT);
    entity.setStartDate(LocalDate.of(2026, 1, 1));
    entity.setGrossSalary(new BigDecimal("12345.67"));
    ContractDto expected = new ContractDto(4L, ContractType.PERMANENT,
        LocalDate.of(2026, 1, 1), null, new BigDecimal("12345.67"), 2L);
    when(contractRepository.findAll()).thenReturn(List.of(entity));
    when(contractRepository.findById(4L)).thenReturn(Optional.of(entity));

    assertThat(contractService.findAll()).containsExactly(expected);
    assertThat(contractService.findById(4L)).contains(expected);
    verify(contractRepository).findAll();
    verify(contractRepository).findById(4L);
  }

  @Test
  void leaveRequestsAreMappedInsideTheServiceBoundary() {
    LeaveRequest entity = new LeaveRequest();
    entity.setId(5L);
    entity.setEmployeeId(2L);
    entity.setLeaveType(LeaveType.ANNUAL);
    entity.setStartDate(LocalDate.of(2026, 11, 2));
    entity.setEndDate(LocalDate.of(2026, 11, 6));
    entity.setReason("Vacation");
    entity.setStatus(RequestStatus.PENDING);
    entity.setComment(null);
    LeaveRequestDto expected = new LeaveRequestDto(5L, 2L, LeaveType.ANNUAL,
        LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6), "Vacation", RequestStatus.PENDING, null);
    when(leaveRequestRepository.findAll()).thenReturn(List.of(entity));
    when(leaveRequestRepository.findById(5L)).thenReturn(Optional.of(entity));

    assertThat(leaveRequestService.findAll()).containsExactly(expected);
    assertThat(leaveRequestService.findById(5L)).contains(expected);
    verify(leaveRequestRepository).findAll();
    verify(leaveRequestRepository).findById(5L);
  }

  @Test
  void missingRecordsReturnEmptyOptionals() {
    when(departmentRepository.findById(99L)).thenReturn(Optional.empty());
    when(employeeRepository.findById(99L)).thenReturn(Optional.empty());
    when(contractRepository.findById(99L)).thenReturn(Optional.empty());
    when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

    assertThat(departmentService.findById(99L)).isEmpty();
    assertThat(employeeService.findById(99L)).isEmpty();
    assertThat(contractService.findById(99L)).isEmpty();
    assertThat(leaveRequestService.findById(99L)).isEmpty();
  }

  @Test
  void emptyRepositoriesReturnEmptyLists() {
    when(departmentRepository.findAll()).thenReturn(List.of());
    when(employeeRepository.findAll()).thenReturn(List.of());
    when(contractRepository.findAll()).thenReturn(List.of());
    when(leaveRequestRepository.findAll()).thenReturn(List.of());

    assertThat(departmentService.findAll()).isEmpty();
    assertThat(employeeService.findAll()).isEmpty();
    assertThat(contractService.findAll()).isEmpty();
    assertThat(leaveRequestService.findAll()).isEmpty();
  }
}
