package com.example.atlasrh.controller;

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
@RequestMapping("/api/leave-requests")
public class LeaveRequestController {
  private final LeaveRequestService service;

  public LeaveRequestController(LeaveRequestService service) {
    this.service = service;
  }

  @PutMapping(value = "/{id}/decision", consumes = MediaType.APPLICATION_JSON_VALUE)
  public LeaveRequestDto decide(
      @PathVariable @Positive(message = "Leave request ID must be positive") Long id,
      @Valid @RequestBody LeaveDecisionRequest request) {
    return service.decide(id, request);
  }

  @PutMapping(value = "/{id}/decision",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  public LeaveRequestDto decideForm(
      @PathVariable @Positive(message = "Leave request ID must be positive") Long id,
      @Valid @ModelAttribute LeaveDecisionRequest request) {
    return service.decide(id, request);
  }
}
