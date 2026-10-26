package com.example.atlasrh.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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
import com.example.atlasrh.exception.DepartmentConflictException;
import com.example.atlasrh.exception.DepartmentNotFoundException;
import com.example.atlasrh.service.DepartmentService;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTests {
  @Autowired
  private MockMvc mvc;

  @MockitoBean
  private DepartmentService service;

  private final DepartmentDto department = new DepartmentDto(1L, "Engineering", "Product development");
  private final DepartmentRequest request = new DepartmentRequest("Engineering", "Product development");
  private final String json = """
      {"name":"Engineering","description":"Product development"}
      """;

  @Test
  void listsDepartments() throws Exception {
    when(service.findAll()).thenReturn(List.of(department));

    mvc.perform(get("/api/departments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].name").value("Engineering"))
        .andExpect(jsonPath("$[0].employees").doesNotExist());
  }

  @Test
  void returnsAnEmptyList() throws Exception {
    when(service.findAll()).thenReturn(List.of());

    mvc.perform(get("/api/departments")).andExpect(status().isOk()).andExpect(content().json("[]"));
  }

  @Test
  void findsADepartment() throws Exception {
    when(service.findById(1L)).thenReturn(Optional.of(department));

    mvc.perform(get("/api/departments/1"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Engineering"));
  }

  @Test
  void missingDepartmentReturns404() throws Exception {
    mvc.perform(get("/api/departments/99"))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void createsADepartmentWithLocationHeader() throws Exception {
    when(service.create(request)).thenReturn(department);

    mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/departments/1"))
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void duplicateNameReturns409() throws Exception {
    when(service.create(request)).thenThrow(new DepartmentConflictException("Duplicate name"));

    mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void updatesADepartment() throws Exception {
    when(service.update(1L, request)).thenReturn(department);

    mvc.perform(put("/api/departments/1").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
  }

  @Test
  void updatingMissingDepartmentReturns404() throws Exception {
    when(service.update(99L, request)).thenThrow(new DepartmentNotFoundException(99L));

    mvc.perform(put("/api/departments/99").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isNotFound());
  }

  @Test
  void updatingToDuplicateNameReturns409() throws Exception {
    when(service.update(1L, request)).thenThrow(new DepartmentConflictException("Duplicate name"));

    mvc.perform(put("/api/departments/1").contentType(MediaType.APPLICATION_JSON).content(json))
        .andExpect(status().isConflict());
  }

  @Test
  void deletesADepartmentWithoutAResponseBody() throws Exception {
    mvc.perform(delete("/api/departments/1"))
        .andExpect(status().isNoContent()).andExpect(content().string(""));
    verify(service).delete(1L);
  }

  @Test
  void deletingMissingDepartmentReturns404() throws Exception {
    doThrow(new DepartmentNotFoundException(99L)).when(service).delete(99L);

    mvc.perform(delete("/api/departments/99")).andExpect(status().isNotFound());
  }

  @Test
  void deletingOccupiedDepartmentReturns409() throws Exception {
    doThrow(new DepartmentConflictException("Department has employees")).when(service).delete(1L);

    mvc.perform(delete("/api/departments/1")).andExpect(status().isConflict());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"name\":\"   \"}"})
  void rejectsInvalidNamesOnCreateAndUpdate(String invalid) throws Exception {
    mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON).content(invalid))
        .andExpect(status().isBadRequest());
    mvc.perform(put("/api/departments/1").contentType(MediaType.APPLICATION_JSON).content(invalid))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void rejectsNamesLongerThanTheDatabaseColumn() throws Exception {
    String invalid = "{\"name\":\"" + "a".repeat(256) + "\"}";
    mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON).content(invalid))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void descriptionIsOptional() throws Exception {
    when(service.create(any())).thenReturn(new DepartmentDto(1L, "Engineering", null));

    mvc.perform(post("/api/departments").contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\":\"Engineering\"}"))
        .andExpect(status().isCreated());
    verify(service).create(new DepartmentRequest("Engineering", null));
  }
}
