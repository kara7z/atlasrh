package com.example.atlasrh.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.atlasrh.entity.Employee;

public interface EmployeeSearchRepository {
  Page<Employee> search(String name, Long departmentId, Pageable pageable);
}
