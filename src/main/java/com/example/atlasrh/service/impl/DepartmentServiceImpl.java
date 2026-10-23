package com.example.atlasrh.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.mapper.DepartmentMapper;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.service.DepartmentService;

@Service
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {
  private final DepartmentRepository repository;
  private final DepartmentMapper mapper;

  public DepartmentServiceImpl(DepartmentRepository repository, DepartmentMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<DepartmentDto> findAll() {
    return repository.findAll().stream().map(mapper::toDto).toList();
  }

  @Override
  public Optional<DepartmentDto> findById(Long id) {
    return repository.findById(id).map(mapper::toDto);
  }
}
