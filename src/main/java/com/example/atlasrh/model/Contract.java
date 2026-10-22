package com.example.atlasrh.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("contracts")
public class Contract {
  @Id
  private Long id;
  private ContractType type;
  private LocalDate startDate;
  private LocalDate endDate;
  private BigDecimal grossSalary;

  @Column("employee_id")
  private Long employeeId;

  @Transient
  private Employee employee;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ContractType getType() {
    return type;
  }

  public void setType(ContractType type) {
    this.type = type;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public BigDecimal getGrossSalary() {
    return grossSalary;
  }

  public void setGrossSalary(BigDecimal grossSalary) {
    this.grossSalary = grossSalary;
  }

  public Long getEmployeeId() {
    return employeeId;
  }

  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
    this.employee = null;
  }

  public Employee getEmployee() {
    return employee;
  }

  public void setEmployee(Employee employee) {
    this.employee = employee;
    this.employeeId = employee == null ? null : employee.getId();
  }
}
