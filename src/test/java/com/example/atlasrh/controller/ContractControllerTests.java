package com.example.atlasrh.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.dto.ContractRequest;
import com.example.atlasrh.enums.ContractType;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.ContractService;

@WebMvcTest(ContractController.class)
class ContractControllerTests {
  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private ContractService service;
  private final String url = "/api/employees/1/contracts";
  private final ContractRequest request = new ContractRequest(ContractType.PERMANENT,
      LocalDate.of(2026, 1, 1), null, new BigDecimal("12000.00"));
  private final ContractDto contract = new ContractDto(2L, request.type(), request.startDate(), null,
      request.grossSalary(), 1L);
  private final String json = """
      {"type":"PERMANENT","startDate":"2026-01-01","grossSalary":12000.00}
      """;

  @Test
  void createsJsonContractWithLocation() throws Exception {
    when(service.create(1L, request)).thenReturn(contract);
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated()).andExpect(header().string("Location", url + "/2"))
        .andExpect(jsonPath("$.employeeId").value(1)).andExpect(jsonPath("$.type").value("PERMANENT"))
        .andExpect(jsonPath("$.employee").doesNotExist());
  }

  @Test
  void createsMultipartAndUrlencodedContracts() throws Exception {
    when(service.create(1L, request)).thenReturn(contract);
    mvc.perform(multipart(url).param("type", "PERMANENT").param("startDate", "2026-01-01")
        .param("grossSalary", "12000.00")).andExpect(status().isCreated());
    mvc.perform(post(url).contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("type", "PERMANENT").param("startDate", "2026-01-01").param("grossSalary", "12000.00"))
        .andExpect(status().isCreated());
  }

  @Test
  void bindsFixedTermEndDateFromForm() throws Exception {
    var fixed = new ContractRequest(ContractType.FIXED_TERM, request.startDate(), LocalDate.of(2026, 12, 31),
        request.grossSalary());
    when(service.create(1L, fixed)).thenReturn(contract);
    mvc.perform(multipart(url).param("type", "FIXED_TERM").param("startDate", "2026-01-01")
        .param("endDate", "2026-12-31").param("grossSalary", "12000.00"))
        .andExpect(status().isCreated());
    verify(service).create(1L, fixed);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"type\":\"UNKNOWN\"}",
      "{\"type\":\"PERMANENT\",\"startDate\":\"invalid\",\"grossSalary\":1}",
      "{\"type\":\"PERMANENT\",\"startDate\":\"2026-01-01\",\"grossSalary\":-1}",
      "{\"type\":\"PERMANENT\",\"startDate\":\"2026-01-01\",\"grossSalary\":1.234}"})
  void invalidJsonReturns400(String body) throws Exception {
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"type=UNKNOWN", "startDate=invalid", "grossSalary=-1", "grossSalary=1.234"})
  void invalidFormReturns400(String field) throws Exception {
    var form = multipart(url).param("type", "PERMANENT").param("startDate", "2026-01-01")
        .param("grossSalary", "12000.00");
    String[] parts = field.split("=");
    form.with(request -> {
      request.setParameter(parts[0], parts[1]);
      return request;
    });
    mvc.perform(form).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "invalid"})
  void invalidEmployeeIdReturns400(String id) throws Exception {
    mvc.perform(post("/api/employees/" + id + "/contracts").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void businessFailuresKeepTheirStatuses() throws Exception {
    when(service.create(1L, request)).thenThrow(new BadRequestException("Invalid dates"),
        new ResourceNotFoundException("Employee not found"), new ConflictException("Overlapping contract"));
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isNotFound());
    mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isConflict());
  }
}
