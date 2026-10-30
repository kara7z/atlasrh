package com.example.atlasrh.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.atlasrh.enums.ContractStatus;
import com.example.atlasrh.enums.ContractType;

@Schema(description = "Contract history/ending response with SCHEDULED, ACTIVE or ENDED status as of today UTC.")
public record ContractHistoryDto(Long id, ContractType type, LocalDate startDate, LocalDate endDate,
    BigDecimal grossSalary, Long employeeId, ContractStatus status) {
}
