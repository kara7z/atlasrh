package com.example.atlasrh.entity;

import com.example.atlasrh.enums.ContractType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@NoArgsConstructor
@Getter
@Setter
@Table("contracts")
public class Contract {
  @Id
  @Setter(AccessLevel.NONE)
  private Long id;
  private ContractType type;
  private LocalDate startDate;
  private LocalDate endDate;
  private BigDecimal grossSalary;

  @Column("employee_id")
  private Long employeeId;

  @Transient
  private Employee employee;

  public void setEmployee(Employee employee) {
    this.employee = employee;
    this.employeeId = employee == null ? null : employee.getId();
  }
}
