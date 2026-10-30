package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.atlasrh.dto.LeaveBalanceDto;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.impl.EmployeeBalanceServiceImpl;
import com.example.atlasrh.service.impl.LeaveBalancePolicy;

@ExtendWith(MockitoExtension.class)
class EmployeeBalanceServiceTests {
  @Mock
  private EmployeeRepository employees;
  @Mock
  private LeaveBalancePolicy policy;

  @Test
  void defaultsToCurrentUtcYearAndReturnsBalancesWithSeniority() {
    var today = LocalDate.of(2026, 10, 30);
    var hire = LocalDate.of(2020, 8, 15);
    when(employees.findById(1L)).thenReturn(Optional.of(Employee.builder().id(1L).hireDate(hire).build()));
    when(policy.balance(1L, LeaveType.ANNUAL, 2026)).thenReturn(new LeaveBalanceDto(LeaveType.ANNUAL, 22, 7, 15));
    when(policy.balance(1L, LeaveType.EXCEPTIONAL, 2026))
        .thenReturn(new LeaveBalanceDto(LeaveType.EXCEPTIONAL, 5, 0, 5));
    var result = service(today).balance(1L, null);
    assertThat(result.year()).isEqualTo(2026);
    assertThat(result.asOf()).isEqualTo(today);
    assertThat(result.hireDate()).isEqualTo(hire);
    assertThat(result.seniority().years()).isEqualTo(6);
    assertThat(result.seniority().months()).isEqualTo(2);
    assertThat(result.balances()).extracting(LeaveBalanceDto::remainingDays).containsExactly(15L, 5L);
  }

  @ParameterizedTest
  @CsvSource({
      "2026-10-30,2026-10-30,0,0",
      "2027-01-01,2026-10-30,0,0",
      "2024-02-29,2025-03-01,1,0",
      "2024-02-29,2025-02-28,0,11",
      "2026-01-31,2026-02-28,0,0",
      "2020-08-31,2026-10-30,6,1"
  })
  void seniorityUsesCompletedCalendarYearsAndMonths(String hire, String today, int years, int months) {
    when(employees.findById(1L))
        .thenReturn(Optional.of(Employee.builder().id(1L).hireDate(LocalDate.parse(hire)).build()));
    when(policy.balance(eq(1L), any(LeaveType.class), anyInt())).thenAnswer(invocation ->
        new LeaveBalanceDto(invocation.getArgument(1), 22, 0, 22));
    var result = service(LocalDate.parse(today)).balance(1L, 2020);
    assertThat(result.year()).isEqualTo(2020);
    assertThat(result.seniority().years()).isEqualTo(years);
    assertThat(result.seniority().months()).isEqualTo(months);
    verify(policy).balance(1L, LeaveType.ANNUAL, 2020);
  }

  @Test
  void missingEmployeeReturns404() {
    assertThatThrownBy(() -> service(LocalDate.of(2026, 10, 30)).balance(99L, null))
        .isInstanceOf(ResourceNotFoundException.class);
    verifyNoInteractions(policy);
  }

  @Test
  void validatesYearAtServiceBoundary() {
    var service = service(LocalDate.of(2026, 10, 30));
    assertThatThrownBy(() -> service.balance(1L, 999)).isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> service.balance(1L, 10000)).isInstanceOf(BadRequestException.class);
    verifyNoInteractions(employees, policy);
  }

  private EmployeeBalanceService service(LocalDate today) {
    return new EmployeeBalanceServiceImpl(employees, policy,
        Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
  }
}
