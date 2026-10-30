package com.example.atlasrh.repository;

import java.time.LocalDate;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import com.example.atlasrh.entity.Contract;

public interface ContractRepository extends ListCrudRepository<Contract, Long> {
  @Query("""
      SELECT COUNT(*) > 0 FROM contracts
      WHERE employee_id = :employeeId
        AND (:endDate IS NULL OR start_date <= :endDate)
        AND (end_date IS NULL OR end_date >= :startDate)
      """)
  boolean existsOverlapping(Long employeeId, LocalDate startDate, LocalDate endDate);
}
