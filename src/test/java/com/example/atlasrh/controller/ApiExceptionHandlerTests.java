package com.example.atlasrh.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.dto.DepartmentRequest;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.EmployeeService;
import com.example.atlasrh.service.DepartmentService;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;

@WebMvcTest({DepartmentController.class, EmployeeController.class})
class ApiExceptionHandlerTests {
  @Autowired
  private MockMvc mvc;

  @MockitoBean
  private DepartmentService service;
  @MockitoBean
  private EmployeeService employeeService;

  @Test
  void fieldValidationReturnsAUniformProblemWithClearMessages() throws Exception {
    mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("about:blank"))
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("Request validation failed"))
        .andExpect(jsonPath("$.instance").value("/api/departments"))
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].message").value("Department name is required"))
        .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
    verifyNoInteractions(service);
  }

  @Test
  void updateValidationHasTheSamePayload() throws Exception {
    mvc.perform(put("/api/departments/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.instance").value("/api/departments/1"))
        .andExpect(jsonPath("$.errors[0].field").value("name"));
    verifyNoInteractions(service);
  }

  @Test
  void employeeValidationRunsBeforeService() throws Exception {
    mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"invalid-email\",\"departmentId\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
        .andExpect(jsonPath("$.errors[*].field", hasItem("departmentId")))
        .andExpect(jsonPath("$.errors[*].field", hasItem("employeeNumber")));
    verifyNoInteractions(employeeService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{", "", "{\"hireDate\":\"not-a-date\"}", "{\"departmentId\":\"not-a-number\"}"})
  void invalidJsonAndTypesReturnASafeUniform400(String json) throws Exception {
    mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Bad Request"))
        .andExpect(jsonPath("$.detail")
            .value("Request body is missing or contains invalid JSON, types, dates, or enum values"))
        .andExpect(jsonPath("$.instance").value("/api/employees"))
        .andExpect(jsonPath("$.errors").isEmpty())
        .andExpect(jsonPath("$.trace").doesNotExist());
    verifyNoInteractions(employeeService);
  }

  @Test
  void invalidPathTypeReturnsAUniform400() throws Exception {
    mvc.perform(get("/api/departments/not-a-number"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.instance").value("/api/departments/not-a-number"))
        .andExpect(jsonPath("$.errors").isEmpty());
    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(longs = {0L, -1L})
  void invalidPathIdsReturnFieldDetails(Long id) throws Exception {
    mvc.perform(get("/api/departments/" + id))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("id"))
        .andExpect(jsonPath("$.errors[0].message").value("Department ID must be positive"));
    verifyNoInteractions(service);
  }

  @Test
  void businessBadRequestsReturn400() throws Exception {
    when(service.findById(1L)).thenThrow(new BadRequestException("Invalid business operation"));

    mvc.perform(get("/api/departments/1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Invalid business operation"))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void businessNotFoundReturns404() throws Exception {
    when(service.findById(1L)).thenThrow(new ResourceNotFoundException("Resource not found"));

    mvc.perform(get("/api/departments/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Not Found"))
        .andExpect(jsonPath("$.detail").value("Resource not found"))
        .andExpect(jsonPath("$.instance").value("/api/departments/1"))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void departmentNotFoundUsesTheGlobalHandler() throws Exception {
    when(service.findById(1L)).thenReturn(Optional.empty());

    mvc.perform(get("/api/departments/1"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("Department 1 not found"))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void businessConflictsReturn409() throws Exception {
    when(service.findById(1L)).thenThrow(new ConflictException("Conflicting operation"));

    mvc.perform(get("/api/departments/1"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value("Conflicting operation"))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void constraintViolationsReturnFieldDetails() throws Exception {
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var violations = factory.getValidator().validate(new DepartmentRequest("", null));
      when(service.findById(1L)).thenThrow(new ConstraintViolationException(violations));

      mvc.perform(get("/api/departments/1"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("name"))
          .andExpect(jsonPath("$.errors[0].message").value("Department name is required"));
    }
  }

  @Test
  void methodNotAllowedPreservesItsStatusAndAllowHeader() throws Exception {
    mvc.perform(post("/api/departments/1"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().exists("Allow"))
        .andExpect(jsonPath("$.status").value(405))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void unknownRoutesReturnAUniform404() throws Exception {
    mvc.perform(get("/api/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.instance").value("/api/does-not-exist"))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void unsupportedContentTypesPreserve415() throws Exception {
    mvc.perform(post("/api/departments").contentType(MediaType.TEXT_PLAIN).content("Engineering"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.status").value(415))
        .andExpect(jsonPath("$.errors").isEmpty());
  }

  @Test
  void successfulResponsesRemainUnchanged() throws Exception {
    DepartmentDto dto = new DepartmentDto(1L, "Engineering", null);
    when(service.findById(1L)).thenReturn(Optional.of(dto));
    when(service.create(new DepartmentRequest("Engineering", null))).thenReturn(dto);

    mvc.perform(get("/api/departments/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Engineering"))
        .andExpect(jsonPath("$.errors").doesNotExist());
    mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Engineering\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.errors").doesNotExist());
  }
}
