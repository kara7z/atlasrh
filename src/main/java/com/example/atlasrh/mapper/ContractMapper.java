package com.example.atlasrh.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Context;

import java.time.LocalDate;

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.dto.ContractHistoryDto;
import com.example.atlasrh.entity.Contract;
import com.example.atlasrh.enums.ContractStatus;

@Mapper(config = MappingConfig.class)
public interface ContractMapper {
  ContractDto toDto(Contract contract);

  @Mapping(target = "status", expression = "java(status(contract, today))")
  ContractHistoryDto toHistoryDto(Contract contract, @Context LocalDate today);

  default ContractStatus status(Contract contract, LocalDate today) {
    if (contract.isEnded() || (contract.getEndDate() != null && contract.getEndDate().isBefore(today))) {
      return ContractStatus.ENDED;
    }
    return contract.getStartDate().isAfter(today) ? ContractStatus.SCHEDULED : ContractStatus.ACTIVE;
  }

  @Mapping(target = "employee", ignore = true)
  @Mapping(target = "ended", ignore = true)
  Contract toEntity(ContractDto dto);
}
