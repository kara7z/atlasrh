package com.example.atlasrh.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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

@SpringJUnitConfig(MapperTests.Config.class)
class MapperTests {
  @TestConfiguration(proxyBeanMethods = false)
  @ComponentScan(basePackageClasses = MappingConfig.class)
  static class Config {
  }

  @Autowired
  private DepartmentMapper departmentMapper;
  @Autowired
  private EmployeeMapper employeeMapper;
  @Autowired
  private ContractMapper contractMapper;
  @Autowired
  private LeaveRequestMapper leaveRequestMapper;

  @Test
  void departmentMapsBothWaysWithoutTraversingEmployees() {
    DepartmentDto dto = new DepartmentDto(1L, "Engineering", "Product development");
    Department entity = departmentMapper.toEntity(dto);

    assertThat(entity).extracting("id", "name", "description")
        .containsExactly(1L, "Engineering", "Product development");
    assertThat(entity.getEmployees()).isEmpty();

    Employee employee = new Employee();
    employee.setDepartment(entity);
    entity.setEmployees(List.of(employee));
    assertThat(departmentMapper.toDto(entity)).isEqualTo(dto);
  }

  @Test
  void employeeMapsBothWaysUsingIdsInsteadOfCyclicRelationships() {
    LocalDate hireDate = LocalDate.of(2024, 1, 15);
    EmployeeDto dto = new EmployeeDto(2L, "EMP-002", "Sara", "Amrani",
        "sara@example.com", "Developer", hireDate, 1L, 3L);
    Employee entity = employeeMapper.toEntity(dto);

    assertThat(entity).extracting("id", "employeeNumber", "firstName", "lastName",
        "email", "position", "hireDate", "departmentId", "managerId")
        .containsExactly(2L, "EMP-002", "Sara", "Amrani", "sara@example.com",
            "Developer", hireDate, 1L, 3L);
    assertThat(entity.getDepartment()).isNull();
    assertThat(entity.getManager()).isNull();
    assertThat(entity.getTeam()).isEmpty();
    assertThat(entity.getContracts()).isEmpty();
    assertThat(entity.getLeaveRequests()).isEmpty();

    Department department = new Department();
    department.setId(1L);
    department.setEmployees(List.of(entity));
    entity.setDepartment(department);
    Employee manager = new Employee();
    manager.setId(3L);
    manager.setTeam(List.of(entity));
    entity.setManager(manager);
    entity.setTeam(List.of(manager));
    Contract contract = new Contract();
    contract.setEmployee(entity);
    entity.setContracts(List.of(contract));
    LeaveRequest request = new LeaveRequest();
    request.setEmployee(entity);
    entity.setLeaveRequests(List.of(request));

    assertThat(employeeMapper.toDto(entity)).isEqualTo(dto);
  }

  @ParameterizedTest
  @EnumSource(ContractType.class)
  void contractMapsBothWaysWithoutLosingDatesSalaryOrEmployeeId(ContractType type) {
    LocalDate start = LocalDate.of(2026, 1, 1);
    LocalDate end = LocalDate.of(2026, 12, 31);
    BigDecimal salary = new BigDecimal("12345.67");
    ContractDto dto = new ContractDto(4L, type, start, end, salary, 2L);
    Contract entity = contractMapper.toEntity(dto);

    assertThat(entity).extracting("id", "type", "startDate", "endDate", "grossSalary", "employeeId")
        .containsExactly(4L, type, start, end, salary, 2L);
    assertThat(entity.getEmployee()).isNull();

    Employee employee = new Employee();
    employee.setId(2L);
    employee.setContracts(List.of(entity));
    entity.setEmployee(employee);
    assertThat(contractMapper.toDto(entity)).isEqualTo(dto);
  }

  @ParameterizedTest
  @EnumSource(RequestStatus.class)
  void leaveRequestMapsBothWaysForEveryStatus(RequestStatus status) {
    LocalDate start = LocalDate.of(2026, 11, 2);
    LocalDate end = LocalDate.of(2026, 11, 6);
    LeaveRequestDto dto = new LeaveRequestDto(5L, 2L, LeaveType.ANNUAL,
        start, end, "Vacation", status, "Manager comment");
    LeaveRequest entity = leaveRequestMapper.toEntity(dto);

    assertThat(entity).extracting("id", "employeeId", "leaveType", "startDate", "endDate",
        "reason", "status", "comment")
        .containsExactly(5L, 2L, LeaveType.ANNUAL, start, end, "Vacation", status, "Manager comment");
    assertThat(entity.getEmployee()).isNull();

    Employee employee = new Employee();
    employee.setId(2L);
    employee.setLeaveRequests(List.of(entity));
    entity.setEmployee(employee);
    assertThat(leaveRequestMapper.toDto(entity)).isEqualTo(dto);
  }

  @Test
  void nullableFieldsArePreserved() {
    DepartmentDto department = new DepartmentDto(1L, "Engineering", null);
    EmployeeDto employee = new EmployeeDto(2L, "EMP-002", "Sara", "Amrani",
        "sara@example.com", "Developer", LocalDate.of(2024, 1, 15), 1L, null);
    ContractDto contract = new ContractDto(4L, ContractType.PERMANENT,
        LocalDate.of(2026, 1, 1), null, new BigDecimal("10000.00"), 2L);
    LeaveRequestDto request = new LeaveRequestDto(5L, 2L, LeaveType.EXCEPTIONAL,
        LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), null, RequestStatus.PENDING, null);

    assertThat(departmentMapper.toDto(departmentMapper.toEntity(department))).isEqualTo(department);
    assertThat(employeeMapper.toDto(employeeMapper.toEntity(employee))).isEqualTo(employee);
    assertThat(contractMapper.toDto(contractMapper.toEntity(contract))).isEqualTo(contract);
    assertThat(leaveRequestMapper.toDto(leaveRequestMapper.toEntity(request))).isEqualTo(request);
  }

  @Test
  void nullInputsMapToNullInBothDirections() {
    assertThat(departmentMapper.toDto(null)).isNull();
    assertThat(departmentMapper.toEntity(null)).isNull();
    assertThat(employeeMapper.toDto(null)).isNull();
    assertThat(employeeMapper.toEntity(null)).isNull();
    assertThat(contractMapper.toDto(null)).isNull();
    assertThat(contractMapper.toEntity(null)).isNull();
    assertThat(leaveRequestMapper.toDto(null)).isNull();
    assertThat(leaveRequestMapper.toEntity(null)).isNull();
  }
}
