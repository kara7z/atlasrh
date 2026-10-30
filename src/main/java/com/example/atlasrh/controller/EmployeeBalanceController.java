package com.example.atlasrh.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.EmployeeBalanceDto;
import com.example.atlasrh.service.EmployeeBalanceService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController
@Tag(name = "Leave balance", description = "Calculated yearly leave balance and seniority")
@RequestMapping("/api/employees/{employeeId}/leave-balance")
public class EmployeeBalanceController {
  private final EmployeeBalanceService service;

  public EmployeeBalanceController(EmployeeBalanceService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Get employee leave balance and seniority", description = "Defaults to current UTC year; optional year 1000-9999. Remaining days = configured allowance minus approved weekdays in the selected year, clipping cross-year requests. Seniority is completed years/months as of today, independent of selected year; future hires have zero seniority.")
  public EmployeeBalanceDto balance(
      @PathVariable @Positive(message = "Employee ID must be positive") Long employeeId,
      @RequestParam(required = false) @Min(value = 1000, message = "Year must be at least 1000")
      @Max(value = 9999, message = "Year must not exceed 9999") Integer year) {
    return service.balance(employeeId, year);
  }
}
