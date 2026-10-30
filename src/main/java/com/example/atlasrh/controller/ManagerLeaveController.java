package com.example.atlasrh.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.service.LeaveRequestService;

import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/managers/{managerId}/leave-requests")
public class ManagerLeaveController {
  private final LeaveRequestService service;

  public ManagerLeaveController(LeaveRequestService service) {
    this.service = service;
  }

  @GetMapping("/pending")
  public List<LeaveRequestDto> pending(
      @PathVariable @Positive(message = "Manager ID must be positive") Long managerId) {
    return service.pendingForManager(managerId);
  }
}
