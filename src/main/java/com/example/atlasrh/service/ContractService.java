package com.example.atlasrh.service;

import java.util.List;
import java.util.Optional;

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.dto.ContractRequest;
import com.example.atlasrh.dto.ContractHistoryDto;
import com.example.atlasrh.dto.EndContractRequest;

public interface ContractService {
  List<ContractDto> findAll();

  Optional<ContractDto> findById(Long id);

  ContractDto create(Long employeeId, ContractRequest request);

  List<ContractHistoryDto> history(Long employeeId);

  ContractHistoryDto end(Long employeeId, Long contractId, EndContractRequest request);
}
