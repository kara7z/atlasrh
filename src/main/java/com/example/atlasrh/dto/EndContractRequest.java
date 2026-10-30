package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

@Schema(description = "Final end date, between contract start and today (UTC), not extending its original end date.",
    example = "{\"endDate\":\"2026-10-30\"}")
public record EndContractRequest(
    @NotNull(message = "End date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
}
