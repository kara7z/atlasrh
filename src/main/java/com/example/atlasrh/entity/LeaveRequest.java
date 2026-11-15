package com.example.atlasrh.entity;

import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
@Table(name = "leave_requests")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class LeaveRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Setter(AccessLevel.NONE)
  private Long id;

  @Column(name = "employee_id", nullable = false)
  private Long employeeId;

  @Transient
  private Employee employee;

  @Enumerated(EnumType.STRING)
  @Column(name = "leave_type", nullable = false)
  private LeaveType leaveType;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RequestStatus status;

  @Column(columnDefinition = "TEXT")
  private String comment;

  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
    this.employee = null;
  }

  public void setEmployee(Employee employee) {
    this.employee = employee;
    this.employeeId = employee == null ? null : employee.getId();
  }

}
