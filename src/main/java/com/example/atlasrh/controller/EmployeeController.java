package com.example.atlasrh.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.mapper.EmployeeMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;

/**
 * EmployeeController
 */
@RestController
@RequestMapping("/employee")
public class EmployeeController {
  private final EmployeeMapper employeeMapper;

  public EmployeeController(EmployeeMapper employeeMapper) {
    this.employeeMapper = employeeMapper;
  }

  @PostMapping
  public ResponseEntity<EmployeeDto> create(@Valid @RequestBody EmployeeDto request) {
    Employee employee = Employee.builder()
        .firstName(request.firstName())
        .lastName(request.lastName())
        .employeeNumber(request.employeeNumber())
        .email(request.email())
        .position(request.position())
        .hireDate(request.hireDate())
        .departmentId(request.departmentId())
        .managerId(request.managerId())
        .build();
    return ResponseEntity.ok(employeeMapper.toDto(employee));

  }

}
