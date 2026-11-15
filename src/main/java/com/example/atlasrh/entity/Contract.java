package com.example.atlasrh.entity;

import com.example.atlasrh.enums.ContractType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "contracts")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Contract {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Setter(AccessLevel.NONE)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContractType type;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "gross_salary", nullable = false, precision = 19, scale = 2)
  private BigDecimal grossSalary;

  @Column(nullable = false)
  private boolean ended;

  @Column(name = "employee_id", nullable = false)
  private Long employeeId;

  @Transient
  private Employee employee;

  public void setEmployee(Employee employee) {
    this.employee = employee;
    this.employeeId = employee == null ? null : employee.getId();
  }
}
