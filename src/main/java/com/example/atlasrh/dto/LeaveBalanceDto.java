package com.example.atlasrh.dto;

import com.example.atlasrh.enums.LeaveType;

public record LeaveBalanceDto(LeaveType leaveType, int daysPerYear, long approvedDays, long remainingDays) {
}
