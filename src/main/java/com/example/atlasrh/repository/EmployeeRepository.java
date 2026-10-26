package com.example.atlasrh.repository;

import org.springframework.data.repository.ListCrudRepository;

import com.example.atlasrh.entity.Employee;

public interface EmployeeRepository extends ListCrudRepository<Employee, Long> {
  boolean existsByDepartmentId(Long departmentId);
}
