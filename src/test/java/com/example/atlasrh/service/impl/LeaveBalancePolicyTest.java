package com.example.atlasrh.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.atlasrh.dto.LeaveBalanceDto;
import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.repository.LeaveTypeRepository;

@ExtendWith(MockitoExtension.class)
class LeaveBalancePolicyTest {
  private static final LocalDate YEAR_2026_START = LocalDate.of(2026, 1, 1);
  private static final LocalDate YEAR_2026_END = LocalDate.of(2026, 12, 31);

  @Mock
  private LeaveTypeRepository types;
  @Mock
  private LeaveRequestRepository requests;
  @InjectMocks
  private LeaveBalancePolicy policy;

  @BeforeEach
  void printTestMethodName(TestInfo testInfo) {
    System.out.println("\nRunning test: " + testInfo.getTestMethod().orElseThrow().getName());
  }

  private void allowance(LeaveType type, int daysPerYear) {
    when(types.findById(type.name()))
        .thenReturn(Optional.of(new com.example.atlasrh.entity.LeaveType(type.name(), daysPerYear)));
  }

  private LeaveRequest approved(String start, String end) {
    return LeaveRequest.builder()
        .employeeId(1L).leaveType(LeaveType.ANNUAL)
        .startDate(LocalDate.parse(start)).endDate(LocalDate.parse(end))
        .status(RequestStatus.APPROVED).build();
  }

  @Nested
  class BalanceComputation {
    @Test
    void balanceSubtractsApprovedWorkingDaysFromTheAllowance() {
      allowance(LeaveType.ANNUAL, 20);
      when(requests.findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END))
          .thenReturn(List.of(approved("2026-11-02", "2026-11-06")));

      LeaveBalanceDto result = policy.balance(1L, LeaveType.ANNUAL, 2026);
      System.out.println("balance result: " + result);

      assertThat(result).isEqualTo(new LeaveBalanceDto(LeaveType.ANNUAL, 20, 5, 15));
      verify(requests).findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END);
    }

    @Test
    void balanceIgnoresTheWeekendInsideAnApprovedRequest() {
      allowance(LeaveType.ANNUAL, 20);
      when(requests.findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END))
          .thenReturn(List.of(approved("2026-11-06", "2026-11-09")));

      LeaveBalanceDto result = policy.balance(1L, LeaveType.ANNUAL, 2026);
      System.out.println("balance weekend result: " + result);

      assertThat(result.approvedDays()).isEqualTo(2);
      assertThat(result.remainingDays()).isEqualTo(18);
    }

    @Test
    void balanceClampsApprovedRequestsToTheSelectedYear() {
      allowance(LeaveType.ANNUAL, 20);
      when(requests.findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END))
          .thenReturn(List.of(approved("2025-12-29", "2026-01-02")));

      LeaveBalanceDto result = policy.balance(1L, LeaveType.ANNUAL, 2026);
      System.out.println("balance clamped result: " + result);

      assertThat(result.approvedDays()).isEqualTo(2);
      assertThat(result.remainingDays()).isEqualTo(18);
    }

    @Test
    void balanceKeepsANegativeRemainderWhenLegacyUsageExceedsTheAllowance() {
      allowance(LeaveType.ANNUAL, 20);
      when(requests.findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END))
          .thenReturn(List.of(approved("2026-01-05", "2026-02-02")));

      LeaveBalanceDto result = policy.balance(1L, LeaveType.ANNUAL, 2026);
      System.out.println("balance overdrawn result: " + result);

      assertThat(result.approvedDays()).isEqualTo(21);
      assertThat(result.remainingDays()).isEqualTo(-1);
    }

    @Test
    void balanceRejectsAnUnconfiguredLeaveType() {
      when(types.findById(LeaveType.ANNUAL.name())).thenReturn(Optional.empty());

      assertThatThrownBy(() -> policy.balance(1L, LeaveType.ANNUAL, 2026))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Leave type allowance is not configured");
    }
  }

  @Nested
  class BalanceCheck {
    @Test
    void checkAcceptsARequestWithinTheRemainingBalance() {
      allowance(LeaveType.ANNUAL, 20);
      when(requests.findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END))
          .thenReturn(List.of());

      policy.check(1L, LeaveType.ANNUAL,
          LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6));

      verify(requests).findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END);
    }

    @Test
    void checkRejectsARequestExceedingTheRemainingBalance() {
      allowance(LeaveType.ANNUAL, 20);
      when(requests.findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END))
          .thenReturn(List.of(approved("2026-01-05", "2026-01-30")));

      assertThatThrownBy(() -> policy.check(1L, LeaveType.ANNUAL,
          LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6)))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("exceeds the remaining balance for 2026");
    }

    @Test
    void checkValidatesEachYearCoveredByTheRequest() {
      allowance(LeaveType.ANNUAL, 20);
      when(requests.findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END))
          .thenReturn(List.of());
      when(requests.findApprovedForYear(1L, LeaveType.ANNUAL,
          LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)))
          .thenReturn(List.of(approved("2027-01-04", "2027-01-29")));

      assertThatThrownBy(() -> policy.check(1L, LeaveType.ANNUAL,
          LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 1)))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("exceeds the remaining balance for 2027");
    }

    @Test
    void checkAcceptsAMultiYearRequestWhenEveryYearHasEnoughBalance() {
      allowance(LeaveType.ANNUAL, 20);
      when(requests.findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END))
          .thenReturn(List.of());
      when(requests.findApprovedForYear(1L, LeaveType.ANNUAL,
          LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31)))
          .thenReturn(List.of());

      policy.check(1L, LeaveType.ANNUAL,
          LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 1));

      verify(requests).findApprovedForYear(1L, LeaveType.ANNUAL, YEAR_2026_START, YEAR_2026_END);
      verify(requests).findApprovedForYear(1L, LeaveType.ANNUAL,
          LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31));
    }
  }
}
