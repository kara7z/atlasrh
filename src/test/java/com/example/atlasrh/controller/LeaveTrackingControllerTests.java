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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.LeaveRequestService;

@WebMvcTest(EmployeeLeaveController.class)
class LeaveTrackingControllerTests {
  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private LeaveRequestService service;
  private final String url = "/api/employees/1/leave-requests";
  private final LeaveRequestDto leave = new LeaveRequestDto(3L, 1L, LeaveType.ANNUAL,
      LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), "Holiday", RequestStatus.CANCELLED, null);

  @Test
  void listsWithDefaultPageAndStableMetadata() throws Exception {
    var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startDate", "id"));
    when(service.listForEmployee(1L, null, pageable)).thenReturn(new PageImpl<>(List.of(leave), pageable, 1));
    mvc.perform(get(url)).andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(3))
        .andExpect(jsonPath("$.content[0].employee").doesNotExist())
        .andExpect(jsonPath("$.number").value(0)).andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.totalPages").value(1));
  }

  @Test
  void passesStatusAndPagingAndSortToService() throws Exception {
    var pageable = PageRequest.of(1, 2, Sort.by("endDate"));
    when(service.listForEmployee(1L, RequestStatus.CANCELLED, pageable))
        .thenReturn(new PageImpl<>(List.of(leave), pageable, 3));
    mvc.perform(get(url).param("status", "CANCELLED").param("page", "1").param("size", "2")
        .param("sort", "endDate,asc")).andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].status").value("CANCELLED"))
        .andExpect(jsonPath("$.number").value(1)).andExpect(jsonPath("$.totalElements").value(3));
  }

  @ParameterizedTest
  @ValueSource(strings = {"status=UNKNOWN", "page=-1", "page=invalid", "size=0", "size=101"})
  void invalidListParametersReturn400(String query) throws Exception {
    String[] parts = query.split("=");
    mvc.perform(get(url).param(parts[0], parts[1])).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void cancellationAcceptsNoBodyOrMultipartWithoutRequiredFields() throws Exception {
    when(service.cancel(1L, 3L)).thenReturn(leave);
    mvc.perform(put(url + "/3/cancel")).andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    mvc.perform(multipart(HttpMethod.PUT, url + "/3/cancel")).andExpect(status().isOk());
  }

  @Test
  void missingResourcesAndProcessedRequestsKeepTheirErrorStatuses() throws Exception {
    var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startDate", "id"));
    when(service.listForEmployee(1L, null, pageable)).thenThrow(new ResourceNotFoundException("Employee not found"));
    mvc.perform(get(url)).andExpect(status().isNotFound());
    when(service.cancel(1L, 3L)).thenThrow(new ResourceNotFoundException("Not owned"),
        new ConflictException("Already processed"));
    mvc.perform(put(url + "/3/cancel")).andExpect(status().isNotFound());
    mvc.perform(put(url + "/3/cancel")).andExpect(status().isConflict());
  }

  @Test
  void invalidPathIdsReturn400() throws Exception {
    mvc.perform(get("/api/employees/0/leave-requests")).andExpect(status().isBadRequest());
    mvc.perform(put(url + "/0/cancel")).andExpect(status().isBadRequest());
    mvc.perform(put("/api/employees/-1/leave-requests/3/cancel")).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
