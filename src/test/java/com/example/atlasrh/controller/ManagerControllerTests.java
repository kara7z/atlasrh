package com.example.atlasrh.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.EmployeeService;

@WebMvcTest(ManagerController.class)
class ManagerControllerTests {
  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private EmployeeService service;

  @Test
  void returnsTeamAsEmployeeDtos() throws Exception {
    when(service.findTeam(3L)).thenReturn(List.of(new EmployeeDto(2L, "EMP-1", "Sara", "Amrani",
        "sara@example.com", "Developer", LocalDate.of(2026, 1, 1), 1L, 3L)));

    mvc.perform(get("/api/managers/3/team"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(2)).andExpect(jsonPath("$[0].managerId").value(3))
        .andExpect(jsonPath("$[0].departmentId").value(1))
        .andExpect(jsonPath("$[0].team").doesNotExist())
        .andExpect(jsonPath("$[0].manager").doesNotExist());
  }

  @Test
  void returnsEmptyArrayForExistingEmployeeWithoutReports() throws Exception {
    when(service.findTeam(3L)).thenReturn(List.of());
    mvc.perform(get("/api/managers/3/team")).andExpect(status().isOk()).andExpect(content().json("[]"));
  }

  @Test
  void missingManagerReturnsUniform404() throws Exception {
    when(service.findTeam(99L)).thenThrow(new ResourceNotFoundException("Manager not found: 99"));
    mvc.perform(get("/api/managers/99/team"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.detail").value("Manager not found: 99"))
        .andExpect(jsonPath("$.instance").value("/api/managers/99/team"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "invalid"})
  void invalidManagerIdsReturn400BeforeCallingService(String id) throws Exception {
    mvc.perform(get("/api/managers/" + id + "/team")).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
