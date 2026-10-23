package com.example.atlasrh.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.mapper.EmployeeMapper;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.EmployeeService;

@Service
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {
  private final EmployeeRepository repository;
  private final EmployeeMapper mapper;

  public EmployeeServiceImpl(EmployeeRepository repository, EmployeeMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<EmployeeDto> findAll() {
    return repository.findAll().stream().map(mapper::toDto).toList();
  }

  @Override
  public Optional<EmployeeDto> findById(Long id) {
    return repository.findById(id).map(mapper::toDto);
  }
}
