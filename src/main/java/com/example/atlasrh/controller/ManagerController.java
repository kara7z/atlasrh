package com.example.atlasrh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.service.EmployeeService;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/managers")
public class ManagerController {
  private final EmployeeService service;

  public ManagerController(EmployeeService service) {
    this.service = service;
  }

  @GetMapping("/{id}/team")
  public List<EmployeeDto> findTeam(@PathVariable @Positive(message = "Manager ID must be positive") Long id) {
    return service.findTeam(id);
  }
}
