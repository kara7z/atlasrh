package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.atlasrh.enums.LeaveType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Leave creation fields. Employee comes from URL; status, ID and weekday count are server-controlled. Dates are inclusive.",
    example = "{\"leaveType\":\"ANNUAL\",\"startDate\":\"2026-11-06\",\"endDate\":\"2026-11-09\",\"reason\":\"Family holiday\"}")
public record LeaveCreateRequest(
    @NotNull(message = "Leave type is required") LeaveType leaveType,
    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
    @NotBlank(message = "Reason is required") String reason) {
}
