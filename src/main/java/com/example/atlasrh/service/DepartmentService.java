package com.example.atlasrh.service;

import java.util.List;
import java.util.Optional;

import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.dto.DepartmentRequest;

public interface DepartmentService {
  List<DepartmentDto> findAll();

  Optional<DepartmentDto> findById(Long id);

  DepartmentDto create(DepartmentRequest request);

  DepartmentDto update(Long id, DepartmentRequest request);

  void delete(Long id);
}
