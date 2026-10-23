package com.example.atlasrh.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.mapper.ContractMapper;
import com.example.atlasrh.repository.ContractRepository;
import com.example.atlasrh.service.ContractService;

@Service
@Transactional(readOnly = true)
public class ContractServiceImpl implements ContractService {
  private final ContractRepository repository;
  private final ContractMapper mapper;

  public ContractServiceImpl(ContractRepository repository, ContractMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<ContractDto> findAll() {
    return repository.findAll().stream().map(mapper::toDto).toList();
  }

  @Override
  public Optional<ContractDto> findById(Long id) {
    return repository.findById(id).map(mapper::toDto);
  }
}
