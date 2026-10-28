package com.example.atlasrh.service;

import java.util.List;
import java.util.Optional;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.dto.EmployeeRequest;

public interface EmployeeService {
  List<EmployeeDto> findAll();

  Optional<EmployeeDto> findById(Long id);

  EmployeeDto create(EmployeeRequest request);

  EmployeeDto update(Long id, EmployeeRequest request);
}
