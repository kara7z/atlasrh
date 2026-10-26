package com.example.atlasrh.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.atlasrh.exception.DepartmentConflictException;
import com.example.atlasrh.exception.DepartmentNotFoundException;

@RestControllerAdvice(assignableTypes = DepartmentController.class)
public class DepartmentExceptionHandler {
  @ExceptionHandler(DepartmentNotFoundException.class)
  public ProblemDetail notFound(DepartmentNotFoundException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(DepartmentConflictException.class)
  public ProblemDetail conflict(DepartmentConflictException exception) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
  }
}
