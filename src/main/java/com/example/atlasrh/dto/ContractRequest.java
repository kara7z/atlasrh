package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.atlasrh.enums.ContractType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Contract create fields. Employee ID comes from the URL. FIXED_TERM/INTERNSHIP require endDate; salary is nonnegative with at most two decimal places.",
    example = "{\"type\":\"FIXED_TERM\",\"startDate\":\"2026-11-01\",\"endDate\":\"2027-10-31\",\"grossSalary\":12000.00}")
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
