package com.example.atlasrh.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.LeaveRequestService;

@WebMvcTest(LeaveRequestController.class)
class LeaveRequestControllerTest {
  private static final String DECISION_PATH = "/api/leave-requests/1/decision";
  private static final LocalDate START_DATE = LocalDate.of(2026, 11, 2);
  private static final LocalDate END_DATE = LocalDate.of(2026, 11, 6);
  private static final String APPROVED_JSON = """
      {
        "managerId": 5,
        "decision": "APPROVED",
        "comment": "Looks good"
      }
      """;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private LeaveRequestService service;

  private LeaveRequestDto decision(RequestStatus status, String comment) {
    return new LeaveRequestDto(1L, 2L, LeaveType.ANNUAL, START_DATE, END_DATE,
        "Family trip", status, comment);
  }

  @Nested
  class DecisionEndpoint {
    @Test
    void decideReturnsTheApprovedRequest() throws Exception {
      when(service.decide(eq(1L), any(LeaveDecisionRequest.class)))
          .thenReturn(decision(RequestStatus.APPROVED, "Looks good"));

      mockMvc.perform(put(DECISION_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(APPROVED_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.status").value("APPROVED"))
          .andExpect(jsonPath("$.comment").value("Looks good"));

      verify(service).decide(1L, new LeaveDecisionRequest(5L, RequestStatus.APPROVED, "Looks good"));
    }

    @Test
    void decideReturnsTheRejectedRequest() throws Exception {
      when(service.decide(eq(1L), any(LeaveDecisionRequest.class)))
          .thenReturn(decision(RequestStatus.REJECTED, "Insufficient team coverage"));

      mockMvc.perform(put(DECISION_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "managerId": 5,
                "decision": "REJECTED",
                "comment": "Insufficient team coverage"
              }
              """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("REJECTED"))
          .andExpect(jsonPath("$.comment").value("Insufficient team coverage"));
    }

    @Test
    void decideRejectsAMissingManagerId() throws Exception {
      mockMvc.perform(put(DECISION_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "decision": "APPROVED"
              }
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Request validation failed"))
          .andExpect(jsonPath("$.errors[0].field").value("managerId"))
          .andExpect(jsonPath("$.errors[0].message").value("Manager ID is required"));

      verify(service, never()).decide(any(), any());
    }

    @Test
    void decideRejectsANonPositiveManagerId() throws Exception {
      mockMvc.perform(put(DECISION_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "managerId": 0,
                "decision": "APPROVED"
              }
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("managerId"))
          .andExpect(jsonPath("$.errors[0].message").value("Manager ID must be positive"));
    }

    @Test
    void decideRejectsAnUnknownDecisionValue() throws Exception {
      mockMvc.perform(put(DECISION_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "managerId": 5,
                "decision": "UNKNOWN"
              }
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail")
              .value("Request body is missing or contains invalid JSON, types, dates, or enum values"));
    }

    @Test
    void decideReturnsBadRequestWhenRejectionHasNoComment() throws Exception {
      when(service.decide(eq(1L), any(LeaveDecisionRequest.class)))
          .thenThrow(new BadRequestException("A comment is required when rejecting a leave request"));

      mockMvc.perform(put(DECISION_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "managerId": 5,
                "decision": "REJECTED"
              }
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("A comment is required when rejecting a leave request"));
    }

    @Test
    void decideReturnsNotFoundWhenTheRequestIsOutsideTheManagerTeam() throws Exception {
      when(service.decide(eq(1L), any(LeaveDecisionRequest.class)))
          .thenThrow(new ResourceNotFoundException("Leave request not found in manager's team"));

      mockMvc.perform(put(DECISION_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(APPROVED_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.detail").value("Leave request not found in manager's team"));
    }

    @Test
    void decideReturnsConflictWhenTheRequestIsAlreadyProcessed() throws Exception {
      when(service.decide(eq(1L), any(LeaveDecisionRequest.class)))
          .thenThrow(new ConflictException("Only PENDING leave requests can be decided"));

      mockMvc.perform(put(DECISION_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(APPROVED_JSON))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(jsonPath("$.detail").value("Only PENDING leave requests can be decided"));
    }

    @Test
    void decideRejectsANonPositiveLeaveRequestId() throws Exception {
      mockMvc.perform(put("/api/leave-requests/0/decision")
          .contentType(MediaType.APPLICATION_JSON)
          .content(APPROVED_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].message").value("Leave request ID must be positive"));

      verify(service, never()).decide(any(), any());
    }
  }
}
