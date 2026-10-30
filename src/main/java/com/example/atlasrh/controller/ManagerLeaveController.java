package com.example.atlasrh.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.service.LeaveRequestService;

import jakarta.validation.constraints.Positive;

@RestController
@Tag(name = "Manager leave", description = "Pending direct-report requests and manager decisions")
@RequestMapping("/api/managers/{managerId}/leave-requests")
public class ManagerLeaveController {
  private final LeaveRequestService service;

  public ManagerLeaveController(LeaveRequestService service) {
    this.service = service;
  }

  @GetMapping("/pending")
  @Operation(summary = "List a manager's pending team requests", description = "Only PENDING requests from direct reports, ordered by start date and ID. Excludes manager's own, indirect and unrelated requests. Existing manager with no matches returns [].")
  public List<LeaveRequestDto> pending(
      @PathVariable @Positive(message = "Manager ID must be positive") Long managerId) {
    return service.pendingForManager(managerId);
  }
}
