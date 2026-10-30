package com.example.atlasrh.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.dto.LeaveCreateRequest;
import com.example.atlasrh.dto.LeaveCreatedDto;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.LeaveRequestService;

@WebMvcTest(EmployeeLeaveController.class)
class LeaveCreationControllerTests {
  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private LeaveRequestService service;
  private final String url = "/api/employees/1/leave-requests";
  private final LeaveCreateRequest request = new LeaveCreateRequest(LeaveType.ANNUAL,
      LocalDate.of(2026, 11, 6), LocalDate.of(2026, 11, 9), "Holiday");
  private final String json = """
      {"leaveType":"ANNUAL","startDate":"2026-11-06","endDate":"2026-11-09","reason":"Holiday"}
      """;

  @Test
  void createsJsonMultipartAndUrlencodedRequests() throws Exception {
    when(service.create(1L, request)).thenReturn(new LeaveCreatedDto(3L, 1L, request.leaveType(), request.startDate(),
        request.endDate(), request.reason(), RequestStatus.PENDING, null, 2));
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/leave-requests/3"))
        .andExpect(jsonPath("$.workingDays").value(2)).andExpect(jsonPath("$.status").value("PENDING"));
    mvc.perform(multipart(url).param("leaveType", "ANNUAL").param("startDate", "2026-11-06")
        .param("endDate", "2026-11-09").param("reason", "Holiday")).andExpect(status().isCreated());
    mvc.perform(post(url).contentType(MediaType.APPLICATION_FORM_URLENCODED).param("leaveType", "ANNUAL")
        .param("startDate", "2026-11-06").param("endDate", "2026-11-09").param("reason", "Holiday"))
        .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"leaveType\":\"UNKNOWN\"}",
      "{\"leaveType\":\"ANNUAL\",\"startDate\":\"invalid\",\"endDate\":\"2026-11-09\",\"reason\":\"Holiday\"}",
      "{\"leaveType\":\"ANNUAL\",\"startDate\":\"2026-11-06\",\"endDate\":\"2026-11-09\",\"reason\":\" \"}"})
  void rejectsInvalidJson(String body) throws Exception {
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void rejectsMissingFormAndInvalidEmployeeId() throws Exception {
    mvc.perform(multipart(url)).andExpect(status().isBadRequest());
    mvc.perform(post("/api/employees/0/leave-requests").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void preservesBusinessErrorStatuses() throws Exception {
    when(service.create(1L, request)).thenThrow(new BadRequestException("Exceeds balance"),
        new ResourceNotFoundException("Employee not found"));
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isNotFound());
  }
}
