package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.atlasrh.enums.LeaveType;

@Schema(description = "Calculated allowance minus approved weekday usage. A negative remainder exposes legacy overdraw.")
public record LeaveBalanceDto(LeaveType leaveType, int daysPerYear, long approvedDays, long remainingDays) {
}
