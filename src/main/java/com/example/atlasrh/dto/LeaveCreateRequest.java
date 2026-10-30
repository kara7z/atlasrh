package com.example.atlasrh.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.atlasrh.enums.LeaveType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LeaveCreateRequest(
    @NotNull(message = "Leave type is required") LeaveType leaveType,
    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
    @NotBlank(message = "Reason is required") String reason) {
}
