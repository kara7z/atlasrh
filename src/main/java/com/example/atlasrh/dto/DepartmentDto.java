package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Saved department record; no nested employee entities.")
public record DepartmentDto(
    @Positive(message = "Department ID must be positive") Long id,
    @NotBlank(message = "Department name is required")
    @Size(max = 255, message = "Department name must not exceed 255 characters") String name,
    String description) {
}
