package com.example.atlasrh.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.atlasrh.entity.Department;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, Long id);
}
