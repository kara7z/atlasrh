package com.example.atlasrh.service.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.dto.LeaveBalanceDto;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.repository.LeaveTypeRepository;
import com.example.atlasrh.util.WorkingDays;

@Component
public class LeaveBalancePolicy {
  private final LeaveTypeRepository types;
  private final LeaveRequestRepository requests;

  public LeaveBalancePolicy(LeaveTypeRepository types, LeaveRequestRepository requests) {
    this.types = types;
    this.requests = requests;
  }

  public void check(Long employeeId, LeaveType type, LocalDate start, LocalDate end) {
    for (int year = start.getYear(); year <= end.getYear(); year++) {
      LocalDate yearStart = LocalDate.of(year, 1, 1);
      LocalDate yearEnd = LocalDate.of(year, 12, 31);
      LocalDate from = start.isAfter(yearStart) ? start : yearStart;
      LocalDate to = end.isBefore(yearEnd) ? end : yearEnd;
      long requested = WorkingDays.between(from, to);
      if (requested > balance(employeeId, type, year).remainingDays()) {
        throw exceeded(year);
      }
    }
  }

  public LeaveBalanceDto balance(Long employeeId, LeaveType type, int year) {
    int allowance = types.findById(type.name())
        .orElseThrow(() -> new BadRequestException("Leave type allowance is not configured"))
        .getDaysPerYear();
    LocalDate yearStart = LocalDate.of(year, 1, 1);
    LocalDate yearEnd = LocalDate.of(year, 12, 31);
    long used = requests.findApprovedForYear(employeeId, type, yearStart, yearEnd).stream()
        .mapToLong(leave -> WorkingDays.between(
            leave.getStartDate().isAfter(yearStart) ? leave.getStartDate() : yearStart,
            leave.getEndDate().isBefore(yearEnd) ? leave.getEndDate() : yearEnd)).sum();
    return new LeaveBalanceDto(type, allowance, used, allowance - used);
  }

  private BadRequestException exceeded(int year) {
    return new BadRequestException("Leave request exceeds the remaining balance for " + year);
  }
}
