package com.example.atlasrh.service;

import java.util.List;
import java.util.Optional;

import com.example.atlasrh.dto.ContractDto;

public interface ContractService {
  List<ContractDto> findAll();

  Optional<ContractDto> findById(Long id);
}
