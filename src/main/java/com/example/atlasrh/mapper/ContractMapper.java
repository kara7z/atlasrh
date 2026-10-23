package com.example.atlasrh.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.entity.Contract;

@Mapper(config = MappingConfig.class)
public interface ContractMapper {
  ContractDto toDto(Contract contract);

  @Mapping(target = "employee", ignore = true)
  Contract toEntity(ContractDto dto);
}
