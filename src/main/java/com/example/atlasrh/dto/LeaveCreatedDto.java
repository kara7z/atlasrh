package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;

@Schema(description = "New PENDING leave request, including the computed inclusive Monday-Friday workingDays count.")
public record LeaveCreatedDto(Long id, Long employeeId, LeaveType leaveType, LocalDate startDate,
    LocalDate endDate, String reason, RequestStatus status, String comment, long workingDays) {
}
