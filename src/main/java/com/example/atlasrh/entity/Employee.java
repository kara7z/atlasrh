package com.example.atlasrh.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("employees")
public class Employee {
  @Id
  private Long id;
  private String employeeNumber;
  private String firstName;
  private String lastName;
  private String email;
  private String position;
  private LocalDate hireDate;

  @Column("department_id")
  private Long departmentId;

  @Column("manager_id")
  private Long managerId;

  @Transient
  private Department department;
  @Transient
  private Employee manager;
  @Transient
  private List<Employee> team = new ArrayList<>();
  @Transient
  private List<Contract> contracts = new ArrayList<>();
  @Transient
  private List<LeaveRequest> leaveRequests = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmployeeNumber() {
    return employeeNumber;
  }

  public void setEmployeeNumber(String employeeNumber) {
    this.employeeNumber = employeeNumber;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPosition() {
    return position;
  }

  public void setPosition(String position) {
    this.position = position;
  }

  public LocalDate getHireDate() {
    return hireDate;
  }

  public void setHireDate(LocalDate hireDate) {
    this.hireDate = hireDate;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(Long departmentId) {
    this.departmentId = departmentId;
    this.department = null;
  }

  public Long getManagerId() {
    return managerId;
  }

  public void setManagerId(Long managerId) {
    this.managerId = managerId;
    this.manager = null;
  }

  public Department getDepartment() {
    return department;
  }

  public void setDepartment(Department department) {
    this.department = department;
    this.departmentId = department == null ? null : department.getId();
  }

  public Employee getManager() {
    return manager;
  }

  public void setManager(Employee manager) {
    this.manager = manager;
    this.managerId = manager == null ? null : manager.getId();
  }

  public List<Employee> getTeam() {
    return team;
  }

  public void setTeam(List<Employee> team) {
    this.team = team;
  }

  public List<Contract> getContracts() {
    return contracts;
  }

  public void setContracts(List<Contract> contracts) {
    this.contracts = contracts;
  }

  public List<LeaveRequest> getLeaveRequests() {
    return leaveRequests;
  }

  public void setLeaveRequests(List<LeaveRequest> leaveRequests) {
    this.leaveRequests = leaveRequests;
  }
}
