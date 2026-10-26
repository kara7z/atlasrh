package com.example.atlasrh.exception;

public class DepartmentNotFoundException extends RuntimeException {
  public DepartmentNotFoundException(Long id) {
    super("Department " + id + " not found");
  }
}
