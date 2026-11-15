package com.example.atlasrh.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.dto.DepartmentRequest;
import com.example.atlasrh.exception.DepartmentConflictException;
import com.example.atlasrh.exception.DepartmentNotFoundException;
import com.example.atlasrh.service.DepartmentService;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {
  private static final String BASE_PATH = "/api/departments";
  private static final String DEPARTMENT_JSON = """
      {
        "name": "Engineering",
        "description": "Development team"
      }
      """;
  private static final DepartmentDto DEPARTMENT = new DepartmentDto(1L, "Engineering", "Development team");

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DepartmentService service;

  @Nested
  class ReadEndpoints {
    @Test
    void findAllReturnsTheDepartmentList() throws Exception {
      when(service.findAll()).thenReturn(List.of(DEPARTMENT));

      mockMvc.perform(get(BASE_PATH))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].id").value(1))
          .andExpect(jsonPath("$[0].name").value("Engineering"))
          .andExpect(jsonPath("$[0].description").value("Development team"));

      verify(service).findAll();
    }

    @Test
    void findByIdReturnsTheDepartment() throws Exception {
      when(service.findById(1L)).thenReturn(Optional.of(DEPARTMENT));

      mockMvc.perform(get(BASE_PATH + "/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.name").value("Engineering"));

      verify(service).findById(1L);
    }

    @Test
    void findByIdReturnsNotFoundWhenTheDepartmentIsMissing() throws Exception {
      when(service.findById(99L)).thenReturn(Optional.empty());

      mockMvc.perform(get(BASE_PATH + "/99"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.detail").value("Department 99 not found"))
          .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void findByIdRejectsANonPositiveId() throws Exception {
      mockMvc.perform(get(BASE_PATH + "/0"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Request validation failed"))
          .andExpect(jsonPath("$.errors[0].message").value("Department ID must be positive"));

      verify(service, never()).findById(any());
    }
  }

  @Nested
  class CreateEndpoint {
    @Test
    void createReturnsCreatedWithLocationHeader() throws Exception {
      when(service.create(new DepartmentRequest("Engineering", "Development team"))).thenReturn(DEPARTMENT);

      mockMvc.perform(post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(DEPARTMENT_JSON))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", "/api/departments/1"))
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.name").value("Engineering"));
    }

    @Test
    void createRejectsABlankName() throws Exception {
      mockMvc.perform(post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "name": "  ",
                "description": "Development team"
              }
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Request validation failed"))
          .andExpect(jsonPath("$.errors[0].field").value("name"))
          .andExpect(jsonPath("$.errors[0].message").value("Department name is required"));

      verify(service, never()).create(any());
    }

    @Test
    void createReturnsConflictWhenTheNameAlreadyExists() throws Exception {
      when(service.create(any(DepartmentRequest.class)))
          .thenThrow(new DepartmentConflictException("A department with this name already exists"));

      mockMvc.perform(post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(DEPARTMENT_JSON))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.status").value(409))
          .andExpect(jsonPath("$.detail").value("A department with this name already exists"));
    }
  }

  @Nested
  class UpdateEndpoint {
    @Test
    void updateReturnsTheUpdatedDepartment() throws Exception {
      when(service.update(eq(1L), any(DepartmentRequest.class))).thenReturn(DEPARTMENT);

      mockMvc.perform(put(BASE_PATH + "/1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(DEPARTMENT_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.name").value("Engineering"));

      verify(service).update(1L, new DepartmentRequest("Engineering", "Development team"));
    }

    @Test
    void updateReturnsNotFoundWhenTheDepartmentIsMissing() throws Exception {
      when(service.update(eq(99L), any(DepartmentRequest.class)))
          .thenThrow(new DepartmentNotFoundException(99L));

      mockMvc.perform(put(BASE_PATH + "/99")
          .contentType(MediaType.APPLICATION_JSON)
          .content(DEPARTMENT_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value("Department 99 not found"));
    }
  }

  @Nested
  class DeleteEndpoint {
    @Test
    void deleteReturnsNoContent() throws Exception {
      mockMvc.perform(delete(BASE_PATH + "/1"))
          .andExpect(status().isNoContent());

      verify(service).delete(1L);
    }

    @Test
    void deleteReturnsConflictWhenTheDepartmentHasEmployees() throws Exception {
      doThrow(new DepartmentConflictException("Cannot delete a department with employees"))
          .when(service).delete(1L);

      mockMvc.perform(delete(BASE_PATH + "/1"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.detail").value("Cannot delete a department with employees"));
    }

    @Test
    void deleteReturnsNotFoundWhenTheDepartmentIsMissing() throws Exception {
      doThrow(new DepartmentNotFoundException(99L)).when(service).delete(99L);

      mockMvc.perform(delete(BASE_PATH + "/99"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.detail").value("Department 99 not found"));
    }
  }
}
