package com.example.atlasrh.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.dto.EmployeeRequest;

public interface EmployeeService {
  List<EmployeeDto> findAll();

  Page<EmployeeDto> search(String name, Long departmentId, Pageable pageable);

  Optional<EmployeeDto> findById(Long id);

  List<EmployeeDto> findTeam(Long managerId);

  EmployeeDto create(EmployeeRequest request);

  EmployeeDto update(Long id, EmployeeRequest request);
}
