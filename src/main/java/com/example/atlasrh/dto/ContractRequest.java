package com.example.atlasrh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.atlasrh.enums.ContractType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record ContractRequest(
    @NotNull(message = "Contract type is required") ContractType type,
    @NotNull(message = "Start date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
    @NotNull(message = "Gross salary is required")
    @DecimalMin(value = "0.0", message = "Gross salary must not be negative")
    @Digits(integer = 17, fraction = 2, message = "Gross salary must have at most 17 integer digits and 2 decimals")
    BigDecimal grossSalary) {
}
