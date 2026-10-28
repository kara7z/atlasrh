package com.example.atlasrh.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EmployeeRequest(
    @NotBlank(message = "Employee number is required")
    @Size(max = 255, message = "Employee number must not exceed 255 characters") String employeeNumber,
    @NotBlank(message = "First name is required")
    @Size(max = 255, message = "First name must not exceed 255 characters") String firstName,
    @NotBlank(message = "Last name is required")
    @Size(max = 255, message = "Last name must not exceed 255 characters") String lastName,
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters") String email,
    @NotBlank(message = "Position is required")
    @Size(max = 255, message = "Position must not exceed 255 characters") String position,
    @NotNull(message = "Hire date is required") LocalDate hireDate,
    @NotNull(message = "Department ID is required")
    @Positive(message = "Department ID must be positive") Long departmentId,
    @Positive(message = "Manager ID must be positive") Long managerId) {
}
