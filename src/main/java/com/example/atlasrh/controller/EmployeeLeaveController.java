package com.example.atlasrh.controller;

import java.net.URI;

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
@RequestMapping("/api/employees/{employeeId}/leave-requests")
public class EmployeeLeaveController {
  private final LeaveRequestService service;

  public EmployeeLeaveController(LeaveRequestService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponse<LeaveRequestDto> list(
      @PathVariable @Positive(message = "Employee ID must be positive") Long employeeId,
      @RequestParam(required = false) RequestStatus status,
      @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must not be negative") int page,
      @RequestParam(defaultValue = "10") @Min(value = 1, message = "Size must be positive")
      @Max(value = 100, message = "Size must not exceed 100") int size,
      @PageableDefault(sort = {"startDate", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
    return PageResponse.from(service.listForEmployee(employeeId, status,
        PageRequest.of(page, size, pageable.getSort())));
  }

  @PutMapping("/{id}/cancel")
  public LeaveRequestDto cancel(
      @PathVariable @Positive(message = "Employee ID must be positive") Long employeeId,
      @PathVariable @Positive(message = "Leave request ID must be positive") Long id) {
    return service.cancel(employeeId, id);
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
