package com.example.atlasrh.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import io.swagger.v3.oas.annotations.Hidden;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTests {
  @Autowired
  private MockMvc mvc;
  @Autowired
  private JsonMapper json;
  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  private RequestMappingHandlerMapping mappings;

  @Test
  void swaggerUiAndConfigurationAreAvailable() throws Exception {
    mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
    mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value("/v3/api-docs"));
  }

  @Test
  void everyApplicationRouteHasASummaryAndTagAndUniqueOperationId() throws Exception {
    var document = document();
    assertThat(document.at("/info/title").asText()).isEqualTo("AtlasRH API");
    assertThat(document.at("/info/description").asText()).contains("No authentication");
    var operationIds = new HashSet<String>();
    mappings.getHandlerMethods().forEach((mapping, handler) -> {
      if (!handler.getBeanType().getPackageName().equals("com.example.atlasrh.controller")
          || handler.getMethod().isAnnotationPresent(Hidden.class)) {
        return;
      }
      for (String path : mapping.getPatternValues()) {
        for (var method : mapping.getMethodsCondition().getMethods()) {
          var operation = document.path("paths").path(path).path(method.name().toLowerCase(Locale.ROOT));
          assertThat(operation.isMissingNode()).as("Document %s %s", method, path).isFalse();
          assertThat(operation.path("summary").asText()).as("Summary for %s %s", method, path).isNotBlank();
          assertThat(operation.path("tags").isEmpty()).isFalse();
          assertThat(operationIds.add(operation.path("operationId").asText())).as("Unique operation ID").isTrue();
        }
      }
    });
    assertThat(operationIds.size()).isGreaterThanOrEqualTo(20);
  }

  @Test
  void writeBodiesDescribeJsonAndBothFormFormatsWithTheSameDto() throws Exception {
    var paths = document().path("paths");
    String[][] writes = {
        {"/api/departments", "post", "DepartmentRequest"},
        {"/api/departments/{id}", "put", "DepartmentRequest"},
        {"/api/employees", "post", "EmployeeRequest"},
        {"/api/employees/{id}", "put", "EmployeeRequest"},
        {"/api/employees/{id}/contracts", "post", "ContractRequest"},
        {"/api/employees/{id}/contracts/{contractId}/end", "put", "EndContractRequest"},
        {"/api/employees/{employeeId}/leave-requests", "post", "LeaveCreateRequest"},
        {"/api/leave-requests/{id}/decision", "put", "LeaveDecisionRequest"}
    };
    for (String[] write : writes) {
      var operation = paths.path(write[0]).path(write[1]);
      var body = operation.path("requestBody").path("content");
      for (String format : new String[] {"application/json", "multipart/form-data", "application/x-www-form-urlencoded"}) {
        assertThat(body.path(format).path("schema").path("$ref").asText())
            .as("%s %s supports %s", write[1], write[0], format)
            .isEqualTo("#/components/schemas/" + write[2]);
      }
      assertThat(operation.path("responses").has(write[1].equals("post") ? "201" : "200")).isTrue();
    }
  }

  @Test
  void errorsAndSuccessCodesMatchActualApiBehavior() throws Exception {
    var paths = document().path("paths");
    var responses = paths.path("/api/employees").path("post").path("responses");
    assertThat(responses.has("201")).isTrue();
    assertThat(responses.has("200")).isFalse();
    for (String code : new String[] {"400", "404", "409", "415"}) {
      assertThat(responses.path(code).path("content").path("application/problem+json")
          .path("schema").path("$ref").asText()).isEqualTo("#/components/schemas/ApiError");
    }
    var deleted = paths.path("/api/departments/{id}").path("delete").path("responses");
    assertThat(deleted.has("204")).isTrue();
    assertThat(deleted.path("204").has("content")).isFalse();
    assertThat(paths.path("/api/employees/{employeeId}/leave-requests").path("post")
        .path("responses").has("409")).isFalse();
  }

  @Test
  void schemasAndPaginationUseDtosInsteadOfEntitiesOrOpaquePageableObjects() throws Exception {
    var document = document();
    var schemas = document.path("components").path("schemas");
    assertThat(schemas.has("EmployeeDto")).isTrue();
    assertThat(schemas.has("LeaveRequestDto")).isTrue();
    for (String entity : new String[] {"Employee", "Department", "Contract", "LeaveRequest"}) {
      assertThat(schemas.has(entity)).as("Do not expose entity %s", entity).isFalse();
    }
    assertThat(schemas.path("EmployeeRequest").path("example").isMissingNode()).isFalse();
    assertThat(schemas.path("LeaveCreateRequest").path("required").toString()).contains("leaveType", "reason");
    var operation = document.path("paths").path("/api/employees").path("get");
    var parameters = operation.path("parameters");
    var names = new HashSet<String>();
    parameters.forEach(parameter -> names.add(parameter.path("name").asText()));
    assertThat(names).contains("page", "size", "sort", "name", "departmentId").doesNotContain("pageable");
  }

  private JsonNode document() throws Exception {
    var response = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse();
    return json.readTree(response.getContentAsString());
  }
}
