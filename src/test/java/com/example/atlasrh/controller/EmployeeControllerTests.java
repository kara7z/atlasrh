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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
  void listsEmployeesWithDefaultPagination() throws Exception {
    var pageable = PageRequest.of(0, 20, Sort.by("id"));
    when(service.search(null, null, pageable)).thenReturn(new PageImpl<>(List.of(employee), pageable, 1));
    mvc.perform(get("/api/employees")).andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(2))
        .andExpect(jsonPath("$.content[0].team").doesNotExist())
        .andExpect(jsonPath("$.number").value(0)).andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.first").value(true)).andExpect(jsonPath("$.last").value(true));
  }

  @Test
  void passesFiltersPaginationAndMultipleSortsToService() throws Exception {
    var pageable = PageRequest.of(1, 5, Sort.by(Sort.Order.desc("lastName"), Sort.Order.asc("firstName")));
    when(service.search("Sara", 1L, pageable)).thenReturn(new PageImpl<>(List.of(employee), pageable, 6));
    mvc.perform(get("/api/employees").param("name", "Sara").param("departmentId", "1")
        .param("page", "1").param("size", "5").param("sort", "lastName,desc", "firstName,asc"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.number").value(1))
        .andExpect(jsonPath("$.size").value(5)).andExpect(jsonPath("$.totalElements").value(6))
        .andExpect(jsonPath("$.totalPages").value(2));
  }

  @Test
  void returnsEmptyPage() throws Exception {
    var pageable = PageRequest.of(0, 20, Sort.by("id"));
    when(service.search(null, null, pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));
    mvc.perform(get("/api/employees")).andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty()).andExpect(jsonPath("$.totalElements").value(0));
  }

  @ParameterizedTest
  @ValueSource(strings = {"page=-1", "page=invalid", "size=0", "size=-1", "size=101", "size=invalid",
      "departmentId=0", "departmentId=-1", "departmentId=invalid"})
  void rejectsInvalidListParameters(String query) throws Exception {
    String[] parts = query.split("=");
    mvc.perform(get("/api/employees").param(parts[0], parts[1])).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void unsupportedSortingReturns400() throws Exception {
    var pageable = PageRequest.of(0, 20, Sort.by("unknown"));
    when(service.search(null, null, pageable)).thenThrow(new BadRequestException("Unsupported employee sort field"));
    mvc.perform(get("/api/employees").param("sort", "unknown,asc"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
  }

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
