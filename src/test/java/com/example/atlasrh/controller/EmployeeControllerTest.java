package com.example.atlasrh.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.service.EmployeeService;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {
  private static final String BASE_PATH = "/api/employees";
  private static final String EMPLOYEE_JSON = """
      {
        "employeeNumber": "EMP-001",
        "firstName": "Sara",
        "lastName": "Amrani",
        "email": "sara@example.com",
        "position": "Developer",
        "hireDate": "2026-01-01",
        "departmentId": 10
      }
      """;

  @Mock
  private EmployeeService service;
  @InjectMocks
  private EmployeeController controller;

  private MockMvc mockMvc;

  private final EmployeeDto employeeDto = new EmployeeDto(
      1L, "EMP-001", "Sara", "Amrani", "sara@example.com", "Developer",
      LocalDate.of(2026, 1, 1), 10L, null);
  private final EmployeeRequest request = new EmployeeRequest(
      "EMP-001", "Sara", "Amrani", "sara@example.com", "Developer",
      LocalDate.of(2026, 1, 1), 10L, null);

  @BeforeEach
  void setUp(TestInfo testInfo) {
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new ApiExceptionHandler())
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
        .build();
    System.out.println("\nRunning test: " + testInfo.getTestMethod().orElseThrow().getName());
  }

  @Nested
  class SearchEndpoint {
    @Test
    void searchReturnsPagedEmployees() throws Exception {
      Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
      when(service.search("sara", 10L, pageable))
          .thenReturn(new PageImpl<>(List.of(employeeDto), pageable, 1));

      mockMvc.perform(get(BASE_PATH)
          .param("name", "sara")
          .param("departmentId", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content[0].id").value(1))
          .andExpect(jsonPath("$.content[0].firstName").value("Sara"))
          .andExpect(jsonPath("$.number").value(0))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.totalPages").value(1))
          .andExpect(jsonPath("$.first").value(true))
          .andExpect(jsonPath("$.last").value(true));

      verify(service).search("sara", 10L, pageable);
    }

    @Test
    void searchAppliesPagingAndSortParameters() throws Exception {
      Pageable pageable = PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "lastName"));
      when(service.search(isNull(), isNull(), eq(pageable)))
          .thenReturn(new PageImpl<>(List.of(), pageable, 0));

      mockMvc.perform(get(BASE_PATH)
          .param("page", "2")
          .param("size", "5")
          .param("sort", "lastName,asc"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.number").value(2))
          .andExpect(jsonPath("$.size").value(5));

      verify(service).search(null, null, pageable);
    }

    @Test
    void searchRejectsSizeAboveMaximum() throws Exception {
      mockMvc.perform(get(BASE_PATH).param("size", "101"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Request validation failed"))
          .andExpect(jsonPath("$.errors[0].field").value("size"))
          .andExpect(jsonPath("$.errors[0].message").value("Size must not exceed 100"));
    }

    @Test
    void searchRejectsNonPositiveDepartmentId() throws Exception {
      mockMvc.perform(get(BASE_PATH).param("departmentId", "0"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Request validation failed"))
          .andExpect(jsonPath("$.errors[0].field").value("departmentId"))
          .andExpect(jsonPath("$.errors[0].message").value("Department ID must be positive"));
    }
  }

  @Nested
  class LookupEndpoint {
    @Test
    void findByIdReturnsEmployee() throws Exception {
      when(service.findById(1L)).thenReturn(Optional.of(employeeDto));

      mockMvc.perform(get(BASE_PATH + "/1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.employeeNumber").value("EMP-001"))
          .andExpect(jsonPath("$.email").value("sara@example.com"));

      verify(service).findById(1L);
    }

    @Test
    void findByIdReturnsNotFoundWhenEmployeeIsMissing() throws Exception {
      when(service.findById(99L)).thenReturn(Optional.empty());

      mockMvc.perform(get(BASE_PATH + "/99"))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.detail").value("Employee not found: 99"))
          .andExpect(jsonPath("$.errors").isEmpty());
    }
  }

  @Nested
  class CreateEndpoint {
    @Test
    void createReturnsCreatedEmployeeWithLocation() throws Exception {
      when(service.create(request)).thenReturn(employeeDto);

      mockMvc.perform(post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(EMPLOYEE_JSON))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", "/api/employees/1"))
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.employeeNumber").value("EMP-001"));

      verify(service).create(request);
    }

    @Test
    void createFormReturnsCreatedEmployeeWithLocation() throws Exception {
      when(service.create(any(EmployeeRequest.class))).thenReturn(employeeDto);

      mockMvc.perform(post(BASE_PATH)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .param("employeeNumber", "EMP-001")
          .param("firstName", "Sara")
          .param("lastName", "Amrani")
          .param("email", "sara@example.com")
          .param("position", "Developer")
          .param("hireDate", "2026-01-01")
          .param("departmentId", "10"))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", "/api/employees/1"))
          .andExpect(jsonPath("$.id").value(1));

      verify(service).create(request);
    }

    @Test
    void createRejectsInvalidPayload() throws Exception {
      mockMvc.perform(post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {
                "employeeNumber": "",
                "firstName": "Sara",
                "lastName": "Amrani",
                "email": "not-an-email",
                "position": "Developer",
                "hireDate": "2026-01-01",
                "departmentId": 10
              }
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.detail").value("Request validation failed"))
          .andExpect(jsonPath("$.errors.length()").value(2));
    }
  }

  @Nested
  class UpdateEndpoint {
    @Test
    void updateReturnsUpdatedEmployee() throws Exception {
      when(service.update(eq(1L), any(EmployeeRequest.class))).thenReturn(employeeDto);

      mockMvc.perform(put(BASE_PATH + "/1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(EMPLOYEE_JSON))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.lastName").value("Amrani"));

      verify(service).update(1L, request);
    }

    @Test
    void updateFormReturnsUpdatedEmployee() throws Exception {
      when(service.update(eq(1L), any(EmployeeRequest.class))).thenReturn(employeeDto);

      mockMvc.perform(put(BASE_PATH + "/1")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .param("employeeNumber", "EMP-001")
          .param("firstName", "Sara")
          .param("lastName", "Amrani")
          .param("email", "sara@example.com")
          .param("position", "Developer")
          .param("hireDate", "2026-01-01")
          .param("departmentId", "10"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1));

      verify(service).update(1L, request);
    }

    @Test
    void updateReturnsNotFoundWhenEmployeeIsMissing() throws Exception {
      when(service.update(eq(99L), any(EmployeeRequest.class)))
          .thenThrow(new ResourceNotFoundException("Employee not found: 99"));

      mockMvc.perform(put(BASE_PATH + "/99")
          .contentType(MediaType.APPLICATION_JSON)
          .content(EMPLOYEE_JSON))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.detail").value("Employee not found: 99"));
    }
  }
}
