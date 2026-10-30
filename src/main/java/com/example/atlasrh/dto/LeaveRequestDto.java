package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Saved leave request, with employeeId, workflow status and optional decision comment.")
public record LeaveRequestDto(
    @Positive(message = "Leave request ID must be positive") Long id,
    @NotNull(message = "Employee ID is required")
    @Positive(message = "Employee ID must be positive") Long employeeId,
    @NotNull(message = "Leave type is required") LeaveType leaveType,
    @NotNull(message = "Start date is required") LocalDate startDate,
    @NotNull(message = "End date is required") LocalDate endDate,
    @NotBlank(message = "Reason is required") String reason,
    @NotNull(message = "Request status is required") RequestStatus status,
    String comment) {
}
