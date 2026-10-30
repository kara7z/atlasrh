package com.example.atlasrh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import com.example.atlasrh.entity.Employee;

public interface EmployeeRepository extends ListCrudRepository<Employee, Long>, EmployeeSearchRepository {
  @Query("SELECT id FROM employees WHERE id = :id FOR UPDATE")
  Optional<Long> lockById(Long id);

  List<Employee> findByManagerId(Long managerId);

  boolean existsByDepartmentId(Long departmentId);

  boolean existsByEmployeeNumber(String employeeNumber);

  boolean existsByEmail(String email);

  boolean existsByEmployeeNumberAndIdNot(String employeeNumber, Long id);

  boolean existsByEmailAndIdNot(String email, Long id);
}
