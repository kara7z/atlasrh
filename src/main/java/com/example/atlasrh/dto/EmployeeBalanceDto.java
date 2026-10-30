package com.example.atlasrh.dto;

import java.time.LocalDate;
import java.util.List;

public record EmployeeBalanceDto(Long employeeId, int year, LocalDate asOf, LocalDate hireDate,
    SeniorityDto seniority, List<LeaveBalanceDto> balances) {
}
