package com.example.atlasrh.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.atlasrh.enums.ContractStatus;
import com.example.atlasrh.enums.ContractType;

public record ContractHistoryDto(Long id, ContractType type, LocalDate startDate, LocalDate endDate,
    BigDecimal grossSalary, Long employeeId, ContractStatus status) {
}
