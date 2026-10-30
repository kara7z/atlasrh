package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Selected calendar year's leave balances plus seniority as of today's UTC date, not the selected year.")
public record EmployeeBalanceDto(Long employeeId, int year, LocalDate asOf, LocalDate hireDate,
    SeniorityDto seniority, List<LeaveBalanceDto> balances) {
}
