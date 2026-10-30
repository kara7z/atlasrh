package com.example.atlasrh.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.dto.EmployeeBalanceDto;
import com.example.atlasrh.dto.LeaveBalanceDto;
import com.example.atlasrh.dto.SeniorityDto;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.EmployeeBalanceService;

@WebMvcTest(EmployeeBalanceController.class)
class EmployeeBalanceControllerTests {
  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private EmployeeBalanceService service;
  private final String url = "/api/employees/1/leave-balance";
  private final EmployeeBalanceDto result = new EmployeeBalanceDto(1L, 2026, LocalDate.of(2026, 10, 30),
      LocalDate.of(2020, 8, 15), new SeniorityDto(6, 2), List.of(
          new LeaveBalanceDto(LeaveType.ANNUAL, 22, 7, 15),
          new LeaveBalanceDto(LeaveType.EXCEPTIONAL, 5, 0, 5)));

  @Test
  void returnsBalanceForCurrentYearByDefault() throws Exception {
    when(service.balance(1L, null)).thenReturn(result);
    mvc.perform(get(url)).andExpect(status().isOk())
        .andExpect(jsonPath("$.employeeId").value(1)).andExpect(jsonPath("$.year").value(2026))
        .andExpect(jsonPath("$.asOf").value("2026-10-30"))
        .andExpect(jsonPath("$.seniority.years").value(6)).andExpect(jsonPath("$.seniority.months").value(2))
        .andExpect(jsonPath("$.balances[0].leaveType").value("ANNUAL"))
        .andExpect(jsonPath("$.balances[0].approvedDays").value(7))
        .andExpect(jsonPath("$.balances[0].remainingDays").value(15))
        .andExpect(jsonPath("$.balances[1].daysPerYear").value(5));
  }

  @Test
  void passesExplicitYearToService() throws Exception {
    when(service.balance(1L, 2026)).thenReturn(result);
    mvc.perform(get(url).param("year", "2026")).andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(strings = {"999", "10000", "-1", "invalid"})
  void invalidYearsReturn400(String year) throws Exception {
    mvc.perform(get(url).param("year", year)).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "invalid"})
  void invalidEmployeeIdsReturn400(String id) throws Exception {
    mvc.perform(get("/api/employees/" + id + "/leave-balance")).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void missingEmployeeReturns404() throws Exception {
    when(service.balance(1L, null)).thenThrow(new ResourceNotFoundException("Employee not found"));
    mvc.perform(get(url)).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
  }
}
