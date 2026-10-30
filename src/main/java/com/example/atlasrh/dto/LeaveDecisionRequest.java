package com.example.atlasrh.dto;

import com.example.atlasrh.enums.RequestStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LeaveDecisionRequest(
    @NotNull(message = "Manager ID is required")
    @Positive(message = "Manager ID must be positive") Long managerId,
    @NotNull(message = "Decision is required") RequestStatus decision,
    String comment) {
}
