package com.example.atlasrh.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.EmployeeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  @GetMapping("/{id}")
  public EmployeeDto findById(@PathVariable @Positive(message = "Employee ID must be positive") Long id) {
    return service.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
  }

  @PostMapping
  public ResponseEntity<EmployeeDto> create(@Valid @RequestBody EmployeeRequest request) {
    EmployeeDto employee = service.create(request);
    return ResponseEntity.created(URI.create("/api/employees/" + employee.id())).body(employee);
  }

  @PutMapping("/{id}")
  public EmployeeDto update(@PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @Valid @RequestBody EmployeeRequest request) {
    return service.update(id, request);
  }
}
