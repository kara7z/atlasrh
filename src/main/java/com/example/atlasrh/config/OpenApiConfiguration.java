package com.example.atlasrh.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;

@Configuration
public class OpenApiConfiguration {
  @Bean
  public OpenAPI atlasOpenApi() {
    return new OpenAPI().info(new Info().title("AtlasRH API").version("v1")
        .description("HR management API: departments, employees, contracts and leave workflows. "
            + "No authentication in this brief: employee/manager IDs are development actor identifiers, "
            + "not authenticated identity. Form dates use YYYY-MM-DD; omit optional IDs instead of sending 'null'."));
  }

  @Bean
  public OpenApiCustomizer atlasApiConventions() {
    return api -> {
      api.getComponents().addSchemas("ApiError", new ObjectSchema()
          .description("Uniform application/problem+json response; validation errors include field details.")
          .addProperty("type", new StringSchema().example("about:blank"))
          .addProperty("title", new StringSchema().example("Bad Request"))
          .addProperty("status", new IntegerSchema().example(400))
          .addProperty("detail", new StringSchema().example("Request validation failed"))
          .addProperty("instance", new StringSchema().example("/api/employees"))
          .addProperty("errors", new ArraySchema().items(new ObjectSchema()
              .addProperty("field", new StringSchema().example("email"))
              .addProperty("message", new StringSchema().example("Email must be valid")))));
      api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
        if (!path.startsWith("/api/")) {
          return;
        }
        operation.getResponses().addApiResponse("400", problem("Invalid fields, parameters, dates or business rule"));
        if (path.contains("{") || path.equals("/api/employees")) {
          operation.getResponses().addApiResponse("404", problem("Resource missing or outside the specified employee/team"));
        }
        boolean conflict = (path.startsWith("/api/departments") && method != PathItem.HttpMethod.GET)
            || (path.startsWith("/api/employees") && !path.contains("leave-")
                && method != PathItem.HttpMethod.GET)
            || path.endsWith("/decision") || path.endsWith("/cancel");
        if (conflict) {
          operation.getResponses().addApiResponse("409", problem("Duplicate resource, overlapping contract or invalid workflow state"));
        }
        var body = operation.getRequestBody();
        if (body != null && body.getContent() != null && body.getContent().containsKey("application/json")) {
          var json = body.getContent().get("application/json");
          body.getContent().addMediaType("multipart/form-data", new MediaType().schema(json.getSchema()))
              .addMediaType("application/x-www-form-urlencoded", new MediaType().schema(json.getSchema()));
          operation.getResponses().addApiResponse("415", problem("Unsupported request Content-Type"));
        }
        if (method == PathItem.HttpMethod.POST) {
          var success = operation.getResponses().remove("200");
          operation.getResponses().addApiResponse("201", success == null
              ? new ApiResponse().description("Created; Location identifies the new record")
              : success.description("Created; Location identifies the new record"));
        } else if (method == PathItem.HttpMethod.DELETE) {
          operation.getResponses().remove("200");
          operation.getResponses().addApiResponse("204", new ApiResponse().description("Deleted; no response body"));
        }
      }));
    };
  }

  private ApiResponse problem(String description) {
    return new ApiResponse().description(description).content(new Content().addMediaType("application/problem+json",
        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError"))));
  }
}
