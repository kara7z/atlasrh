package com.example.atlasrh.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.EmployeeBalanceDto;
import com.example.atlasrh.dto.SeniorityDto;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.EmployeeBalanceService;

@Service
@Transactional(readOnly = true)
public class EmployeeBalanceServiceImpl implements EmployeeBalanceService {
  private final EmployeeRepository employees;
  private final LeaveBalancePolicy policy;
  private final Clock clock;

  public EmployeeBalanceServiceImpl(EmployeeRepository employees, LeaveBalancePolicy policy, Clock clock) {
    this.employees = employees;
    this.policy = policy;
    this.clock = clock;
  }

  @Override
  public EmployeeBalanceDto balance(Long employeeId, Integer year) {
    LocalDate today = LocalDate.now(clock);
    int selectedYear = year == null ? today.getYear() : year;
    if (selectedYear < 1000 || selectedYear > 9999) {
      throw new BadRequestException("Year must be between 1000 and 9999");
    }
    var employee = employees.findById(employeeId)
        .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    Period elapsed = employee.getHireDate().isAfter(today)
        ? Period.ZERO : Period.between(employee.getHireDate(), today);
    var seniority = new SeniorityDto(elapsed.getYears(), elapsed.getMonths());
    var balances = Arrays.stream(LeaveType.values())
        .map(type -> policy.balance(employeeId, type, selectedYear)).toList();
    return new EmployeeBalanceDto(employeeId, selectedYear, today, employee.getHireDate(), seniority, balances);
  }
}
