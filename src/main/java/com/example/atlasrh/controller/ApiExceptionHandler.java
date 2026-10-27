package com.example.atlasrh.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.atlasrh.dto.ValidationError;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Object> badRequest(BadRequestException exception, WebRequest request) {
    return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Object> notFound(ResourceNotFoundException exception, WebRequest request) {
    return problem(HttpStatus.NOT_FOUND, exception.getMessage(), request);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Object> conflict(ConflictException exception, WebRequest request) {
    return problem(HttpStatus.CONFLICT, exception.getMessage(), request);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
    problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
        .map(error -> new ValidationError(error.getField(), error.getDefaultMessage())).toList());
    return handleExceptionInternal(exception, problem, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    if (exception.isForReturnValue()) {
      return super.handleHandlerMethodValidationException(exception, headers, status, request);
    }
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request validation failed");
    problem.setProperty("errors", exception.getParameterValidationResults().stream()
        .flatMap(result -> result.getResolvableErrors().stream()
            .map(error -> new ValidationError(error instanceof FieldError fieldError
                ? fieldError.getField() : result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
        .toList());
    return handleExceptionInternal(exception, problem, headers, status, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> constraintViolation(ConstraintViolationException exception, WebRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    problem.setProperty("errors", exception.getConstraintViolations().stream()
        .map(violation -> new ValidationError(violation.getPropertyPath().toString(), violation.getMessage()))
        .sorted(java.util.Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message))
        .toList());
    return handleExceptionInternal(exception, problem, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
        "Request body is missing or contains invalid JSON, types, dates, or enum values");
    return handleExceptionInternal(exception, problem, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
      HttpStatusCode status, WebRequest request) {
    if (body instanceof ProblemDetail problem) {
      if (problem.getType() == null) {
        problem.setType(URI.create("about:blank"));
      }
      if (problem.getProperties() == null || !problem.getProperties().containsKey("errors")) {
        problem.setProperty("errors", List.of());
      }
      if (request instanceof ServletWebRequest servletRequest) {
        problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
      }
    }
    return super.createResponseEntity(body, headers, status, request);
  }

  private ResponseEntity<Object> problem(HttpStatus status, String detail, WebRequest request) {
    return createResponseEntity(ProblemDetail.forStatusAndDetail(status, detail), new HttpHeaders(), status, request);
  }
}
