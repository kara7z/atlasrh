package com.example.atlasrh.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.atlasrh.dto.EmployeeBalanceDto;
import com.example.atlasrh.dto.LeaveBalanceDto;
import com.example.atlasrh.dto.SeniorityDto;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class EmployeeBalanceServiceImplTest {
  @Mock
  private EmployeeRepository employees;
  @Mock
  private LeaveBalancePolicy policy;

  private EmployeeBalanceServiceImpl service;

  private final LocalDate today = LocalDate.of(2026, 11, 13);
  private final Clock clock = Clock.fixed(Instant.parse("2026-11-13T10:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp(TestInfo testInfo) {
    service = new EmployeeBalanceServiceImpl(employees, policy, clock);
    System.out.println("\nRunning test: " + testInfo.getTestMethod().orElseThrow().getName());
  }

  private Employee employee(LocalDate hireDate) {
    return Employee.builder().id(1L).hireDate(hireDate).build();
  }

  private List<LeaveBalanceDto> stubBalances(Long employeeId, int year) {
    LeaveBalanceDto annual = new LeaveBalanceDto(LeaveType.ANNUAL, 20, 3, 17);
    LeaveBalanceDto exceptional = new LeaveBalanceDto(LeaveType.EXCEPTIONAL, 5, 1, 4);
    when(policy.balance(employeeId, LeaveType.ANNUAL, year)).thenReturn(annual);
    when(policy.balance(employeeId, LeaveType.EXCEPTIONAL, year)).thenReturn(exceptional);
    return List.of(annual, exceptional);
  }

  @Nested
  class BalanceOperations {
    @Test
    void balanceUsesCurrentYearWhenYearIsNull() {
      LocalDate hireDate = LocalDate.of(2020, 1, 15);
      List<LeaveBalanceDto> expectedBalances = stubBalances(1L, today.getYear());
      when(employees.findById(1L)).thenReturn(Optional.of(employee(hireDate)));

      EmployeeBalanceDto result = service.balance(1L, null);
      System.out.println("balance result: " + result);

      assertThat(result.employeeId()).isEqualTo(1L);
      assertThat(result.year()).isEqualTo(today.getYear());
      assertThat(result.asOf()).isEqualTo(today);
      assertThat(result.hireDate()).isEqualTo(hireDate);
      assertThat(result.seniority()).isEqualTo(new SeniorityDto(6, 9));
      assertThat(result.balances()).containsExactlyElementsOf(expectedBalances);
    }

    @Test
    void balanceUsesRequestedYear() {
      LocalDate hireDate = LocalDate.of(2020, 1, 15);
      stubBalances(1L, 2025);
      when(employees.findById(1L)).thenReturn(Optional.of(employee(hireDate)));

      EmployeeBalanceDto result = service.balance(1L, 2025);
      System.out.println("balance requested year result: " + result);

      assertThat(result.year()).isEqualTo(2025);
      assertThat(result.asOf()).isEqualTo(today);
      verify(policy).balance(1L, LeaveType.ANNUAL, 2025);
      verify(policy).balance(1L, LeaveType.EXCEPTIONAL, 2025);
    }

    @Test
    void balanceComputesSeniorityWithoutPartialDays() {
      LocalDate hireDate = LocalDate.of(2020, 2, 13);
      stubBalances(1L, today.getYear());
      when(employees.findById(1L)).thenReturn(Optional.of(employee(hireDate)));

      EmployeeBalanceDto result = service.balance(1L, null);
      System.out.println("balance seniority result: " + result.seniority());

      assertThat(result.seniority()).isEqualTo(new SeniorityDto(6, 9));
    }

    @Test
    void balanceReturnsZeroSeniorityForFutureHire() {
      LocalDate hireDate = LocalDate.of(2027, 1, 1);
      stubBalances(1L, today.getYear());
      when(employees.findById(1L)).thenReturn(Optional.of(employee(hireDate)));

      EmployeeBalanceDto result = service.balance(1L, null);
      System.out.println("balance future hire result: " + result.seniority());

      assertThat(result.seniority()).isEqualTo(new SeniorityDto(0, 0));
    }

    @Test
    void balanceKeepsNegativeRemainingDays() {
      LeaveBalanceDto overdrawn = new LeaveBalanceDto(LeaveType.ANNUAL, 20, 22, -2);
      LeaveBalanceDto exceptional = new LeaveBalanceDto(LeaveType.EXCEPTIONAL, 5, 0, 5);
      when(policy.balance(1L, LeaveType.ANNUAL, today.getYear())).thenReturn(overdrawn);
      when(policy.balance(1L, LeaveType.EXCEPTIONAL, today.getYear())).thenReturn(exceptional);
      when(employees.findById(1L)).thenReturn(Optional.of(employee(LocalDate.of(2020, 1, 15))));

      EmployeeBalanceDto result = service.balance(1L, null);
      System.out.println("balance overdrawn result: " + result.balances());

      assertThat(result.balances()).containsExactly(overdrawn, exceptional);
    }

    @Test
    void balanceRejectsYearBelowSupportedRange() {
      assertThatThrownBy(() -> service.balance(1L, 999))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Year must be between 1000 and 9999");
    }

    @Test
    void balanceRejectsYearAboveSupportedRange() {
      assertThatThrownBy(() -> service.balance(1L, 10000))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Year must be between 1000 and 9999");
    }

    @Test
    void balanceRejectsMissingEmployee() {
      when(employees.findById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.balance(99L, null))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Employee not found: 99");
    }
  }
}
