package com.example.atlasrh.dto;

import java.time.LocalDate;

import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;

public record LeaveRequestDto(
    Long id,
    Long employeeId,
    LeaveType leaveType,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    RequestStatus status,
    String comment) {
}
