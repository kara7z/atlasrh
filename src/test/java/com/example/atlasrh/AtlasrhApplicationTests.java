package com.example.atlasrh;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.atlasrh.repository.ContractRepository;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.service.ContractService;
import com.example.atlasrh.service.DepartmentService;
import com.example.atlasrh.service.EmployeeService;
import com.example.atlasrh.service.LeaveRequestService;

@SpringBootTest
class AtlasrhApplicationTests {
  @Autowired
  private DepartmentRepository departmentRepository;
  @Autowired
  private EmployeeRepository employeeRepository;
  @Autowired
  private ContractRepository contractRepository;
  @Autowired
  private LeaveRequestRepository leaveRequestRepository;
  @Autowired
  private DepartmentService departmentService;
  @Autowired
  private EmployeeService employeeService;
  @Autowired
  private ContractService contractService;
  @Autowired
  private LeaveRequestService leaveRequestService;

  @Test
  @Transactional
  void jdbcRepositoriesAndDtoServicesWorkTogether() {
    Department department = new Department();
    department.setName("ATLAS-11 integration department");
    department.setDescription("Rolled back after the test");
    department = departmentRepository.save(department);

    Employee manager = employeeRepository.save(employee("ATLAS-11-MGR", department.getId(), null));
    Employee employee = employeeRepository.save(employee("ATLAS-11-EMP", department.getId(), manager.getId()));

    Contract contract = new Contract();
    contract.setEmployeeId(employee.getId());
    contract.setType(ContractType.PERMANENT);
    contract.setStartDate(LocalDate.of(2026, 1, 1));
    contract.setGrossSalary(new BigDecimal("12345.67"));
    contract = contractRepository.save(contract);

    LeaveRequest request = new LeaveRequest();
    request.setEmployeeId(employee.getId());
    request.setLeaveType(LeaveType.ANNUAL);
    request.setStartDate(LocalDate.of(2026, 11, 2));
    request.setEndDate(LocalDate.of(2026, 11, 6));
    request.setReason("Vacation");
    request.setStatus(RequestStatus.PENDING);
    request = leaveRequestRepository.save(request);

    DepartmentDto expectedDepartment = new DepartmentDto(department.getId(),
        "ATLAS-11 integration department", "Rolled back after the test");
    EmployeeDto expectedEmployee = new EmployeeDto(employee.getId(), "ATLAS-11-EMP",
        "Sara", "Amrani", "ATLAS-11-EMP@example.com", "Developer", LocalDate.of(2024, 1, 15),
        department.getId(), manager.getId());
    ContractDto expectedContract = new ContractDto(contract.getId(), ContractType.PERMANENT,
        LocalDate.of(2026, 1, 1), null, new BigDecimal("12345.67"), employee.getId());
    LeaveRequestDto expectedRequest = new LeaveRequestDto(request.getId(), employee.getId(),
        LeaveType.ANNUAL, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6),
        "Vacation", RequestStatus.PENDING, null);

    assertThat(departmentService.findById(department.getId())).contains(expectedDepartment);
    assertThat(employeeService.findById(employee.getId())).contains(expectedEmployee);
    assertThat(contractService.findById(contract.getId())).contains(expectedContract);
    assertThat(leaveRequestService.findById(request.getId())).contains(expectedRequest);
    assertThat(departmentService.findAll()).contains(expectedDepartment);
    assertThat(employeeService.findAll()).contains(expectedEmployee);
    assertThat(contractService.findAll()).contains(expectedContract);
    assertThat(leaveRequestService.findAll()).contains(expectedRequest);
  }

  private Employee employee(String number, Long departmentId, Long managerId) {
    Employee employee = new Employee();
    employee.setEmployeeNumber(number);
    employee.setFirstName("Sara");
    employee.setLastName("Amrani");
    employee.setEmail(number + "@example.com");
    employee.setPosition("Developer");
    employee.setHireDate(LocalDate.of(2024, 1, 15));
    employee.setDepartmentId(departmentId);
    employee.setManagerId(managerId);
    return employee;
  }
}
