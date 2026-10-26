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

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table("leave_requests")
public class LeaveRequest {
  @Id
  @Setter(AccessLevel.NONE)
  private Long id;

  @Column("employee_id")
  private Long employeeId;

  @Transient
  private Employee employee;

  private LeaveType leaveType;
  private LocalDate startDate;
  private LocalDate endDate;
  private String reason;
  private RequestStatus status;
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
