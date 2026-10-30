package com.example.atlasrh.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.service.LeaveRequestService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@Tag(name = "Manager leave", description = "Pending direct-report requests and manager decisions")
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {
  private final LeaveRequestService service;

  public LeaveRequestController(LeaveRequestService service) {
    this.service = service;
  }

  @PutMapping(value = "/{id}/decision", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Approve or reject a team leave request", description = "Supply managerId and APPROVED/REJECTED. Rejection requires a nonblank comment (400). Only direct reports and PENDING requests can be decided; processed requests return 409. Approval rechecks balance (400 if exhausted). managerId is caller-supplied, not authenticated identity.")
  public LeaveRequestDto decide(
      @PathVariable @Positive(message = "Leave request ID must be positive") Long id,
      @Valid @RequestBody LeaveDecisionRequest request) {
    return service.decide(id, request);
  }

  @PutMapping(value = "/{id}/decision",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @Hidden
  public LeaveRequestDto decideForm(
      @PathVariable @Positive(message = "Leave request ID must be positive") Long id,
      @Valid @ModelAttribute LeaveDecisionRequest request) {
    return service.decide(id, request);
  }
}
