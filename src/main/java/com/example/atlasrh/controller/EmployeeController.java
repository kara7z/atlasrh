package com.example.atlasrh.controller;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.entity.Employee;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * EmployeeController
 */
@RestController
@RequestMapping("/employee")
public class EmployeeController {
  @PostMapping
  public ResponseEntity<Employee> postMethodName() {
    Employee employee = Employee.builder()
        .firstName("Kara")
        .lastName("Oussama")
        .employeeNumber("jf")
        .email("kara.oussama@example.com")
        .position("Developer")
        .hireDate(LocalDate.of(2026, 10, 26))
        .departmentId(23L)
        .build();
    return ResponseEntity.ok(employee);

  }

}
