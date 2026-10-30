package com.example.atlasrh.controller;

import java.net.URI;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.LeaveCreateRequest;
import com.example.atlasrh.dto.LeaveCreatedDto;
import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.dto.PageResponse;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.service.LeaveRequestService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

@RestController
@Tag(name = "Employee leave", description = "Create, track and cancel employee leave requests")
@RequestMapping("/api/employees/{employeeId}/leave-requests")
public class EmployeeLeaveController {
  private final LeaveRequestService service;

  public EmployeeLeaveController(LeaveRequestService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "List employee leave requests", description = "Optional status filter. Zero-based pages, size 1-100 (default 10); newest start date and ID first. Sort fields: id, leaveType, startDate, endDate, status. Unknown employee returns 404; no matches returns empty content.")
  public PageResponse<LeaveRequestDto> list(
      @PathVariable @Positive(message = "Employee ID must be positive") Long employeeId,
      @RequestParam(required = false) RequestStatus status,
      @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must not be negative") int page,
      @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be positive")
      @Max(value = 100, message = "Size must not exceed 100") int size,
      @ParameterObject @PageableDefault(sort = {"startDate", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
    return PageResponse.from(service.listForEmployee(employeeId, status,
        PageRequest.of(page, size, pageable.getSort())));
  }

  @PutMapping("/{id}/cancel")
  @Operation(summary = "Cancel an employee's pending request", description = "No request body needed. Only the specified employee's own PENDING request can be cancelled. Other owners return 404; processed requests return 409. Route IDs are development actor identifiers, not authenticated identity.")
  public LeaveRequestDto cancel(
      @PathVariable @Positive(message = "Employee ID must be positive") Long employeeId,
      @PathVariable @Positive(message = "Leave request ID must be positive") Long id) {
    return service.cancel(employeeId, id);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Create a leave request", description = "Returns PENDING with computed workingDays (Monday-Friday inclusive). Invalid dates, weekend-only requests, any overlap or exceeding the yearly balance return 400. Allowances: ANNUAL 22, EXCEPTIONAL 5; approved usage only, checked per calendar year. IDs/status are server-controlled.")
  public ResponseEntity<LeaveCreatedDto> create(
      @PathVariable @Positive(message = "Employee ID must be positive") Long employeeId,
      @Valid @RequestBody LeaveCreateRequest request) {
    var leave = service.create(employeeId, request);
    return ResponseEntity.created(URI.create("/api/leave-requests/" + leave.id())).body(leave);
  }

  @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @Hidden
  public ResponseEntity<LeaveCreatedDto> createForm(
      @PathVariable @Positive(message = "Employee ID must be positive") Long employeeId,
      @Valid @ModelAttribute LeaveCreateRequest request) {
    return create(employeeId, request);
  }
}
