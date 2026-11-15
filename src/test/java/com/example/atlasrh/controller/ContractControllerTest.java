package com.example.atlasrh.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.dto.ContractHistoryDto;
import com.example.atlasrh.dto.ContractRequest;
import com.example.atlasrh.dto.EndContractRequest;
import com.example.atlasrh.enums.ContractStatus;
import com.example.atlasrh.enums.ContractType;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.ContractService;

@WebMvcTest(ContractController.class)
class ContractControllerTest {
  private static final String BASE_PATH = "/api/employees/2/contracts";
  private static final LocalDate START_DATE = LocalDate.of(2026, 11, 2);
  private static final BigDecimal SALARY = new BigDecimal("12000.00");
  private static final String CONTRACT_JSON = """
      {
        "type": "PERMANENT",
        "startDate": "2026-11-02",
        "grossSalary": 12000.00
      }
      """;
  private static final ContractDto CONTRACT = new ContractDto(
      1L, ContractType.PERMANENT, START_DATE, null, SALARY, 2L);

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ContractService service;

  @Nested
  class HistoryEndpoint {
    @Test
    void historyReturnsTheEmployeeContracts() throws Exception {
      ContractHistoryDto history = new ContractHistoryDto(
          1L, ContractType.PERMANENT, START_DATE, null, SALARY, 2L, ContractStatus.ACTIVE);
      when(service.history(2L)).thenReturn(List.of(history));

      mockMvc.perform(get(BASE_PATH))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].id").value(1))
          .andExpect(jsonPath("$[0].type").value("PERMANENT"))
          .andExpect(jsonPath("$[0].employeeId").value(2))
          .andExpect(jsonPath("$[0].status").value("ACTIVE"));

      verify(service).history(2L);
    }

    @Test
    void historyReturnsNotFoundWhenTheEmployeeIsMissing() throws Exception {
      when(service.history(99L)).thenThrow(new ResourceNotFoundException("Employee not found: 99"));

      mockMvc.perform(get("/api/employees/99/contracts"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.detail").value("Employee not found: 99"));
    }

    @Test
    void historyRejectsANonPositiveEmployeeId() throws Exception {
      mockMvc.perform(get("/api/employees/0/contracts"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Request validation failed"))
          .andExpect(jsonPath("$.errors[0].message").value("Employee ID must be positive"));

      verify(service, never()).history(any());
    }
  }

  @Nested
  class CreateEndpoint {
    @Test
    void createReturnsCreatedWithLocationHeader() throws Exception {
      when(service.create(eq(2L), any(ContractRequest.class))).thenReturn(CONTRACT);

      mockMvc.perform(post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(CONTRACT_JSON))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", "/api/employees/2/contracts/1"))
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.type").value("PERMANENT"))
          .andExpect(jsonPath("$.employeeId").value(2));

      verify(service).create(2L, new ContractRequest(
          ContractType.PERMANENT, START_DATE, null, SALARY));
    }

    @Test
    void createRejectsAMissingContractType() throws Exception {
      mockMvc.perform(post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "startDate": "2026-11-02",
                "grossSalary": 12000.00
              }
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Request validation failed"))
          .andExpect(jsonPath("$.errors[0].field").value("type"))
          .andExpect(jsonPath("$.errors[0].message").value("Contract type is required"));

      verify(service, never()).create(any(), any());
    }

    @Test
    void createReturnsBadRequestWhenTheServiceRejectsTheDates() throws Exception {
      when(service.create(eq(2L), any(ContractRequest.class)))
          .thenThrow(new BadRequestException("End date must not be before start date"));

      mockMvc.perform(post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(CONTRACT_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("End date must not be before start date"));
    }

    @Test
    void createReturnsNotFoundWhenTheEmployeeIsMissing() throws Exception {
      when(service.create(eq(99L), any(ContractRequest.class)))
          .thenThrow(new ResourceNotFoundException("Employee not found: 99"));

      mockMvc.perform(post("/api/employees/99/contracts")
          .contentType(MediaType.APPLICATION_JSON)
          .content(CONTRACT_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value("Employee not found: 99"));
    }

    @Test
    void createReturnsConflictWhenAContractAlreadyCoversThePeriod() throws Exception {
      when(service.create(eq(2L), any(ContractRequest.class)))
          .thenThrow(new ConflictException("Employee already has a contract covering this period"));

      mockMvc.perform(post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(CONTRACT_JSON))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(jsonPath("$.detail").value("Employee already has a contract covering this period"));
    }
  }

  @Nested
  class EndEndpoint {
    @Test
    void endReturnsTheEndedContract() throws Exception {
      LocalDate endDate = LocalDate.of(2026, 10, 30);
      ContractHistoryDto ended = new ContractHistoryDto(
          1L, ContractType.PERMANENT, START_DATE, endDate, SALARY, 2L, ContractStatus.ENDED);
      when(service.end(eq(2L), eq(1L), any(EndContractRequest.class))).thenReturn(ended);

      mockMvc.perform(put(BASE_PATH + "/1/end")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "endDate": "2026-10-30"
              }
              """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("ENDED"))
          .andExpect(jsonPath("$.endDate").value("2026-10-30"));

      verify(service).end(2L, 1L, new EndContractRequest(endDate));
    }

    @Test
    void endRejectsAMissingEndDate() throws Exception {
      mockMvc.perform(put(BASE_PATH + "/1/end")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Request validation failed"))
          .andExpect(jsonPath("$.errors[0].field").value("endDate"))
          .andExpect(jsonPath("$.errors[0].message").value("End date is required"));

      verify(service, never()).end(any(), any(), any());
    }

    @Test
    void endReturnsNotFoundWhenTheContractIsMissing() throws Exception {
      when(service.end(eq(2L), eq(9L), any(EndContractRequest.class)))
          .thenThrow(new ResourceNotFoundException("Contract not found: 9"));

      mockMvc.perform(put(BASE_PATH + "/9/end")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "endDate": "2026-10-30"
              }
              """))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value("Contract not found: 9"));
    }

    @Test
    void endReturnsConflictWhenTheContractIsNotActive() throws Exception {
      when(service.end(eq(2L), eq(1L), any(EndContractRequest.class)))
          .thenThrow(new ConflictException("Only an active contract can be ended"));

      mockMvc.perform(put(BASE_PATH + "/1/end")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "endDate": "2026-10-30"
              }
              """))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.detail").value("Only an active contract can be ended"));
    }
  }
}
