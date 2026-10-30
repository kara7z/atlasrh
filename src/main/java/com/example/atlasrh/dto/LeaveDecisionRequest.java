package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.atlasrh.enums.RequestStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Manager decision for a direct report. managerId is a development actor ID, not authenticated identity. Rejection requires a nonblank comment.",
    example = "{\"managerId\":1,\"decision\":\"REJECTED\",\"comment\":\"Insufficient team coverage\"}")
public record LeaveDecisionRequest(
    @NotNull(message = "Manager ID is required")
    @Positive(message = "Manager ID must be positive") Long managerId,
    @NotNull(message = "Decision is required")
    @Schema(allowableValues = {"APPROVED", "REJECTED"}) RequestStatus decision,
    String comment) {
}
