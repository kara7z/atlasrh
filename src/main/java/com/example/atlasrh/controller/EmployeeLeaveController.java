package com.example.atlasrh.controller;

import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.LeaveCreateRequest;
import com.example.atlasrh.dto.LeaveCreatedDto;
import com.example.atlasrh.service.LeaveRequestService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/employees/{employeeId}/leave-requests")
public class EmployeeLeaveController {
  private final LeaveRequestService service;

  public EmployeeLeaveController(LeaveRequestService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<LeaveCreatedDto> create(
      @PathVariable @Positive(message = "Employee ID must be positive") Long employeeId,
      @Valid @RequestBody LeaveCreateRequest request) {
    var leave = service.create(employeeId, request);
    return ResponseEntity.created(URI.create("/api/leave-requests/" + leave.id())).body(leave);
  }

  @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  public ResponseEntity<LeaveCreatedDto> createForm(
      @PathVariable @Positive(message = "Employee ID must be positive") Long employeeId,
      @Valid @ModelAttribute LeaveCreateRequest request) {
    return create(employeeId, request);
  }
}
