package com.example.atlasrh.service;

import com.example.atlasrh.dto.EmployeeBalanceDto;

public interface EmployeeBalanceService {
  EmployeeBalanceDto balance(Long employeeId, Integer year);
}
