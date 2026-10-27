package com.example.atlasrh.exception;

public class DepartmentNotFoundException extends ResourceNotFoundException {
  public DepartmentNotFoundException(Long id) {
    super("Department " + id + " not found");
  }
}
