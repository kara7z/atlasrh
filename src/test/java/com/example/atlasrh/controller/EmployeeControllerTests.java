package com.example.atlasrh.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.EmployeeService;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTests {
  @Autowired
  private MockMvc mvc;
  @MockitoBean
  private EmployeeService service;

  private final EmployeeDto employee = new EmployeeDto(2L, "EMP-1", "Sara", "Amrani", "sara@example.com",
      "Developer", LocalDate.of(2026, 1, 1), 1L, null);
  private final EmployeeRequest request = new EmployeeRequest("EMP-1", "Sara", "Amrani", "sara@example.com",
      "Developer", LocalDate.of(2026, 1, 1), 1L, null);
  private final String json = """
      {"employeeNumber":"EMP-1","firstName":"Sara","lastName":"Amrani","email":"sara@example.com",
       "position":"Developer","hireDate":"2026-01-01","departmentId":1}
      """;

  @Test
  void createsEmployeeWithLocationHeader() throws Exception {
    when(service.create(request)).thenReturn(employee);
    mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated()).andExpect(header().string("Location", "/api/employees/2"))
        .andExpect(jsonPath("$.id").value(2));
  }

  @Test
  void updatesEmployee() throws Exception {
    when(service.update(2L, request)).thenReturn(employee);
    mvc.perform(put("/api/employees/2").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(2));
  }

  @Test
  void findsEmployeeWithoutExposingEntityGraph() throws Exception {
    when(service.findById(2L)).thenReturn(Optional.of(employee));
    mvc.perform(get("/api/employees/2")).andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("sara@example.com"))
        .andExpect(jsonPath("$.departmentId").value(1)).andExpect(jsonPath("$.team").doesNotExist());
  }

  @Test
  void missingEmployeeReturns404() throws Exception {
    mvc.perform(get("/api/employees/99")).andExpect(status().isNotFound());
    when(service.update(99L, request)).thenThrow(new ResourceNotFoundException("Employee not found"));
    mvc.perform(put("/api/employees/99").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isNotFound());
  }

  @Test
  void duplicatesReturn409OnCreateAndUpdate() throws Exception {
    when(service.create(request)).thenThrow(new ConflictException("Duplicate employee"));
    when(service.update(2L, request)).thenThrow(new ConflictException("Duplicate employee"));
    mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    mvc.perform(put("/api/employees/2").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isConflict());
  }

  @Test
  void missingReferenceReturns404() throws Exception {
    when(service.create(request)).thenThrow(new ResourceNotFoundException("Department not found"));
    mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isNotFound());
  }

  @Test
  void selfManagementReturns400() throws Exception {
    when(service.update(2L, request)).thenThrow(new BadRequestException("An employee cannot be their own manager"));
    mvc.perform(put("/api/employees/2").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"departmentId\":1}", "{\"email\":\"invalid\"}"})
  void rejectsInvalidRequestsBeforeCallingService(String invalid) throws Exception {
    mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(invalid))
        .andExpect(status().isBadRequest());
    mvc.perform(put("/api/employees/2").contentType(MediaType.APPLICATION_JSON).content(invalid))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "invalid"})
  void rejectsInvalidIds(String id) throws Exception {
    mvc.perform(get("/api/employees/" + id)).andExpect(status().isBadRequest());
    mvc.perform(put("/api/employees/" + id).contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void rejectsInvalidEmailAndReferenceIdsInOtherwiseValidRequests() throws Exception {
    for (String invalid : new String[] {json.replace("sara@example.com", "invalid"),
        json.replace("\"departmentId\":1", "\"departmentId\":0"),
        json.replace("\"departmentId\":1", "\"departmentId\":1,\"managerId\":-1"),
        json.replace("Sara", "a".repeat(256))}) {
      mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(invalid))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(service);
  }
}
