package com.example.atlasrh.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.atlasrh.entity.Contract;

public interface ContractRepository extends JpaRepository<Contract, Long> {
  List<Contract> findByEmployeeIdOrderByStartDateDescIdDesc(Long employeeId);

  @Query("""
      select count(c) > 0 from Contract c
      where c.employeeId = :employeeId
        and (:endDate is null or c.startDate <= :endDate)
        and (c.endDate is null or c.endDate >= :startDate)
      """)
  boolean existsOverlapping(@Param("employeeId") Long employeeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
