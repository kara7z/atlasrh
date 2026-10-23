package com.example.atlasrh.repository;

import org.springframework.data.repository.ListCrudRepository;

import com.example.atlasrh.entity.Department;

public interface DepartmentRepository extends ListCrudRepository<Department, Long> {
}
