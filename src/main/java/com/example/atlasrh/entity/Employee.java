package com.example.atlasrh.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employees")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Employee {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Setter(AccessLevel.NONE)
  private Long id;

  @Column(name = "employee_number", nullable = false, unique = true)
  private String employeeNumber;

  @Column(name = "first_name", nullable = false)
  private String firstName;

  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String position;

  @Column(name = "hire_date", nullable = false)
  private LocalDate hireDate;

  @Column(name = "department_id", nullable = false)
  private Long departmentId;

  @Column(name = "manager_id")
  private Long managerId;

  @Transient
  private Department department;
  @Transient
  private Employee manager;
  @Transient
  @Builder.Default
  private List<Employee> team = new ArrayList<>();
  @Transient
  @Builder.Default
  private List<Contract> contracts = new ArrayList<>();
  @Transient
  @Builder.Default
  private List<LeaveRequest> leaveRequests = new ArrayList<>();

  public void setDepartmentId(Long departmentId) {
    this.departmentId = departmentId;
    this.department = null;
  }

  public void setManagerId(Long managerId) {
    this.managerId = managerId;
    this.manager = null;
  }

  public void setDepartment(Department department) {
    this.department = department;
    this.departmentId = department == null ? null : department.getId();
  }

  public void setManager(Employee manager) {
    this.manager = manager;
    this.managerId = manager == null ? null : manager.getId();
  }

}
