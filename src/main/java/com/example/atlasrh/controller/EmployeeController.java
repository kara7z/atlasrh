package com.example.atlasrh.controller;

import java.net.URI;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.dto.PageResponse;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.EmployeeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponse<EmployeeDto> search(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) @Positive(message = "Department ID must be positive") Long departmentId,
      @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must not be negative") int page,
      @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be positive") @Max(value = 100, message = "Size must not exceed 100") int size,
      @PageableDefault(sort = "id") Pageable pageable) {
    return PageResponse.from(service.search(name, departmentId, PageRequest.of(page, size, pageable.getSort())));
  }

  @GetMapping("/{id}")
  public EmployeeDto findById(@PathVariable @Positive(message = "Employee ID must be positive") Long id) {
    return service.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<EmployeeDto> create(@Valid @RequestBody EmployeeRequest request) {
    EmployeeDto employee = service.create(request);
    return ResponseEntity.created(URI.create("/api/employees/" + employee.id())).body(employee);
  }

  @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE })
  public ResponseEntity<EmployeeDto> createForm(@Valid @ModelAttribute EmployeeRequest request) {
    return create(request);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public EmployeeDto update(@PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @Valid @RequestBody EmployeeRequest request) {
    return service.update(id, request);
  }

  @PutMapping(value = "/{id}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  public EmployeeDto updateForm(@PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @Valid @ModelAttribute EmployeeRequest request) {
    return service.update(id, request);
  }
}
