package com.example.atlasrh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
    @NotBlank @Size(max = 255) String name,
    String description) {
}
