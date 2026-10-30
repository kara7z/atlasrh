package com.example.atlasrh.service;

import java.util.List;
import java.util.Optional;

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.dto.ContractRequest;

public interface ContractService {
  List<ContractDto> findAll();

  Optional<ContractDto> findById(Long id);

  ContractDto create(Long employeeId, ContractRequest request);
}
