package com.example.atlasrh.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
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

@WebMvcTest({ManagerLeaveController.class, LeaveRequestController.class})
class LeaveDecisionControllerTests {
  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private LeaveRequestService service;
  private final String url = "/api/leave-requests/3/decision";
  private final LeaveDecisionRequest decision = new LeaveDecisionRequest(1L, RequestStatus.REJECTED, "Team coverage");
  private final LeaveRequestDto leave = new LeaveRequestDto(3L, 2L, LeaveType.ANNUAL,
      LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), "Holiday", RequestStatus.REJECTED, "Team coverage");
  private final String json = """
      {"managerId":1,"decision":"REJECTED","comment":"Team coverage"}
      """;

  @Test
  void listsPendingTeamRequestsAndEmptyTeam() throws Exception {
    when(service.pendingForManager(1L)).thenReturn(List.of(new LeaveRequestDto(leave.id(), leave.employeeId(),
        leave.leaveType(), leave.startDate(), leave.endDate(), leave.reason(), RequestStatus.PENDING, null)), List.of());
    mvc.perform(get("/api/managers/1/leave-requests/pending"))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("PENDING"))
        .andExpect(jsonPath("$[0].employee").doesNotExist());
    mvc.perform(get("/api/managers/1/leave-requests/pending"))
        .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void decisionsAcceptJsonMultipartAndUrlencodedForms() throws Exception {
    when(service.decide(3L, decision)).thenReturn(leave);
    mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.comment").value("Team coverage"));
    mvc.perform(multipart(HttpMethod.PUT, url).param("managerId", "1").param("decision", "REJECTED")
        .param("comment", "Team coverage")).andExpect(status().isOk());
    mvc.perform(put(url).contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("managerId", "1").param("decision", "REJECTED").param("comment", "Team coverage"))
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"managerId\":0,\"decision\":\"APPROVED\"}",
      "{\"managerId\":1,\"decision\":\"UNKNOWN\"}"})
  void invalidJsonReturns400(String body) throws Exception {
    mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "invalid", "null"})
  void invalidFormManagerReturns400(String managerId) throws Exception {
    mvc.perform(multipart(HttpMethod.PUT, url).param("managerId", managerId).param("decision", "APPROVED"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void invalidPathsReturn400() throws Exception {
    mvc.perform(get("/api/managers/0/leave-requests/pending")).andExpect(status().isBadRequest());
    mvc.perform(put("/api/leave-requests/-1/decision").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void businessStatusesArePreserved() throws Exception {
    when(service.pendingForManager(99L)).thenThrow(new ResourceNotFoundException("Manager not found"));
    mvc.perform(get("/api/managers/99/leave-requests/pending")).andExpect(status().isNotFound());
    when(service.decide(3L, decision)).thenThrow(new BadRequestException("Missing comment"),
        new ResourceNotFoundException("Not in team"), new ConflictException("Already processed"));
    mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
    mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isNotFound());
    mvc.perform(put(url).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isConflict());
  }
}
