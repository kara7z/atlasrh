package com.example.atlasrh.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/api/departments")
public class DepartmentController {
  private final DepartmentService service;

  public DepartmentController(DepartmentService service) {
    this.service = service;
  }

  @GetMapping
  public List<DepartmentDto> findAll() {
    return service.findAll();
  }

  @GetMapping("/{id}")
  public DepartmentDto findById(@PathVariable @Positive(message = "Department ID must be positive") Long id) {
    return service.findById(id).orElseThrow(() -> new DepartmentNotFoundException(id));
  }

  @PostMapping
  public ResponseEntity<DepartmentDto> create(@Valid @RequestBody DepartmentRequest request) {
    DepartmentDto department = service.create(request);
    return ResponseEntity.created(URI.create("/api/departments/" + department.id())).body(department);
  }

  @PutMapping("/{id}")
  public DepartmentDto update(@PathVariable @Positive(message = "Department ID must be positive") Long id,
      @Valid @RequestBody DepartmentRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @Positive(message = "Department ID must be positive") Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
