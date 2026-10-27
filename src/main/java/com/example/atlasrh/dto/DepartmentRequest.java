package com.example.atlasrh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
    @NotBlank(message = "Department name is required")
    @Size(max = 255, message = "Department name must not exceed 255 characters") String name,
    String description) {
}
