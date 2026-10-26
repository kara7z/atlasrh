package com.example.atlasrh.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table("employees")
public class Employee {
  @Id
  @Setter(AccessLevel.NONE)
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
