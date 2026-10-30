package com.example.atlasrh.controller;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.dto.DepartmentRequest;
import com.example.atlasrh.exception.DepartmentNotFoundException;
import com.example.atlasrh.service.DepartmentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@Tag(name = "Departments", description = "HR department CRUD and unique-name rules")
@RequestMapping("/api/departments")
public class DepartmentController {
  private final DepartmentService service;

  public DepartmentController(DepartmentService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "List departments")
  public List<DepartmentDto> findAll() {
    return service.findAll();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a department", description = "Returns 404 if the department does not exist.")
  public DepartmentDto findById(@PathVariable @Positive(message = "Department ID must be positive") Long id) {
    return service.findById(id).orElseThrow(() -> new DepartmentNotFoundException(id));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Create a department", description = "Name is required and unique (409 on duplicate). Description is optional. Accepts JSON or forms.")
  public ResponseEntity<DepartmentDto> create(@Valid @RequestBody DepartmentRequest request) {
    DepartmentDto department = service.create(request);
    return ResponseEntity.created(URI.create("/api/departments/" + department.id())).body(department);
  }

  @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @Hidden
  public ResponseEntity<DepartmentDto> createForm(@Valid @ModelAttribute DepartmentRequest request) {
    return create(request);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Update a department", description = "Replace name and description. Duplicate names return 409.")
  public DepartmentDto update(@PathVariable @Positive(message = "Department ID must be positive") Long id,
      @Valid @RequestBody DepartmentRequest request) {
    return service.update(id, request);
  }

  @PutMapping(value = "/{id}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @Hidden
  public DepartmentDto updateForm(@PathVariable @Positive(message = "Department ID must be positive") Long id,
      @Valid @ModelAttribute DepartmentRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a department", description = "Returns 204. A department with employees cannot be deleted (409).")
  public ResponseEntity<Void> delete(@PathVariable @Positive(message = "Department ID must be positive") Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
