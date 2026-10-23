package com.example.atlasrh.dto;

import java.time.LocalDate;

public record EmployeeDto(
    Long id,
    String employeeNumber,
    String firstName,
    String lastName,
    String email,
    String position,
    LocalDate hireDate,
    Long departmentId,
    Long managerId) {
}
