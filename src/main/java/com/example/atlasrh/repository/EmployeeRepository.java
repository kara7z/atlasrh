package com.example.atlasrh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.atlasrh.entity.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, EmployeeSearchRepository {
  @Query(value = "select id from employees where id = :id for update", nativeQuery = true)
  Optional<Long> lockById(@Param("id") Long id);

  List<Employee> findByManagerId(Long managerId);

  boolean existsByDepartmentId(Long departmentId);

  boolean existsByEmployeeNumber(String employeeNumber);

  boolean existsByEmail(String email);

  boolean existsByEmployeeNumberAndIdNot(String employeeNumber, Long id);

  boolean existsByEmailAndIdNot(String email, Long id);
}
