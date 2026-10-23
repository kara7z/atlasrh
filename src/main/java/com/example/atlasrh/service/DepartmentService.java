package com.example.atlasrh.service;

import java.util.List;
import java.util.Optional;

import com.example.atlasrh.dto.DepartmentDto;

public interface DepartmentService {
  List<DepartmentDto> findAll();

  Optional<DepartmentDto> findById(Long id);
}
