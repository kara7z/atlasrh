package com.example.atlasrh.controller;

import java.net.URI;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

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
@Tag(name = "Employees", description = "Employee records and paginated search")
@RequestMapping("/api/employees")
public class EmployeeController {
  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Search employees", description = "Zero-based pagination, size 1-100. Name is a case-insensitive first/last-name substring; departmentId is an optional AND filter. Sort by id, employeeNumber, firstName, lastName, email, position, hireDate, departmentId or managerId; ID breaks ties.")
  public PageResponse<EmployeeDto> search(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) @Positive(message = "Department ID must be positive") Long departmentId,
      @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must not be negative") int page,
      @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be positive") @Max(value = 100, message = "Size must not exceed 100") int size,
      @ParameterObject @PageableDefault(sort = "id") Pageable pageable) {
    return PageResponse.from(service.search(name, departmentId, PageRequest.of(page, size, pageable.getSort())));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an employee record")
  public EmployeeDto findById(@PathVariable @Positive(message = "Employee ID must be positive") Long id) {
    return service.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Create an employee", description = "Unique employee number/email (409). Department and optional manager must exist (404). Omit managerId for no manager. Accepts JSON or forms.")
  public ResponseEntity<EmployeeDto> create(@Valid @RequestBody EmployeeRequest request) {
    EmployeeDto employee = service.create(request);
    return ResponseEntity.created(URI.create("/api/employees/" + employee.id())).body(employee);
  }

  @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE })
  @Hidden
  public ResponseEntity<EmployeeDto> createForm(@Valid @ModelAttribute EmployeeRequest request) {
    return create(request);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Update an employee", description = "All required record fields must be sent. Omitted managerId clears the manager; an employee cannot manage themselves (400).")
  public EmployeeDto update(@PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @Valid @RequestBody EmployeeRequest request) {
    return service.update(id, request);
  }

  @PutMapping(value = "/{id}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @Hidden
  public EmployeeDto updateForm(@PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @Valid @ModelAttribute EmployeeRequest request) {
    return service.update(id, request);
  }
}
