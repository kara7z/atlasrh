package com.example.atlasrh.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.service.EmployeeService;

import jakarta.validation.constraints.Positive;

@RestController
@Tag(name = "Teams", description = "Direct-report employee teams")
@RequestMapping("/api/managers")
public class ManagerController {
  private final EmployeeService service;

  public ManagerController(EmployeeService service) {
    this.service = service;
  }

  @GetMapping("/{id}/team")
  @Operation(summary = "Get a manager's direct reports", description = "Returns employee DTOs across departments, excluding indirect reports. Existing employee with no reports returns []; missing manager returns 404.")
  public List<EmployeeDto> findTeam(@PathVariable @Positive(message = "Manager ID must be positive") Long id) {
    return service.findTeam(id);
  }
}
