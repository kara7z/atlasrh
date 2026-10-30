package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Department create/update fields. Name is required and unique; description is optional.",
    example = "{\"name\":\"Engineering\",\"description\":\"Development team\"}")
public record DepartmentRequest(
    @NotBlank(message = "Department name is required")
    @Size(max = 255, message = "Department name must not exceed 255 characters") String name,
    String description) {
}
