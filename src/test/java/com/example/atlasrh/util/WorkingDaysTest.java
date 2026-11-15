package com.example.atlasrh.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class WorkingDaysTest {

  @Test
  void shouldCountASingleWorkingDay() {
    LocalDate start = LocalDate.of(2026, 11, 2);

    long result = WorkingDays.between(start, start);

    assertThat(result).isEqualTo(1);
  }

  @Test
  void shouldCountAFullWorkingWeek() {
    LocalDate monday = LocalDate.of(2026, 11, 2);
    LocalDate friday = LocalDate.of(2026, 11, 6);

    long result = WorkingDays.between(monday, friday);

    assertThat(result).isEqualTo(5);
  }

  @Test
  void shouldIgnoreTheWeekendAtTheEndOfTheRange() {
    LocalDate monday = LocalDate.of(2026, 11, 2);
    LocalDate sunday = LocalDate.of(2026, 11, 8);

    long result = WorkingDays.between(monday, sunday);

    assertThat(result).isEqualTo(5);
  }

  @Test
  void shouldReturnZeroForAWeekendOnly() {
    LocalDate saturday = LocalDate.of(2026, 11, 7);
    LocalDate sunday = LocalDate.of(2026, 11, 8);

    long result = WorkingDays.between(saturday, sunday);

    assertThat(result).isZero();
  }

  @Test
  void shouldSkipTheWeekendInsideTheRange() {
    LocalDate friday = LocalDate.of(2026, 11, 6);
    LocalDate monday = LocalDate.of(2026, 11, 9);

    long result = WorkingDays.between(friday, monday);

    assertThat(result).isEqualTo(2);
  }

  @Test
  void shouldCountAcrossTwoWeeks() {
    LocalDate monday = LocalDate.of(2026, 11, 2);
    LocalDate friday = LocalDate.of(2026, 11, 13);

    long result = WorkingDays.between(monday, friday);

    assertThat(result).isEqualTo(10);
  }

  @Test
  void shouldCountACrossMonthRange() {
    LocalDate monday = LocalDate.of(2026, 11, 30);
    LocalDate friday = LocalDate.of(2026, 12, 4);

    long result = WorkingDays.between(monday, friday);

    assertThat(result).isEqualTo(5);
  }

  @Test
  void shouldCountACrossLeapYearFebruaryRange() {
    LocalDate monday = LocalDate.of(2028, 2, 28);
    LocalDate friday = LocalDate.of(2028, 3, 3);

    long result = WorkingDays.between(monday, friday);

    assertThat(result).isEqualTo(5);
  }

  @Test
  void shouldCountWorkingDaysOfAWholeYear() {
    LocalDate firstOfJanuary = LocalDate.of(2026, 1, 1);
    LocalDate lastOfDecember = LocalDate.of(2026, 12, 31);

    long result = WorkingDays.between(firstOfJanuary, lastOfDecember);

    assertThat(result).isEqualTo(261);
  }

  @Test
  void shouldReturnZeroWhenEndDateIsBeforeStartDate() {
    LocalDate start = LocalDate.of(2026, 11, 6);
    LocalDate end = LocalDate.of(2026, 11, 2);

    long result = WorkingDays.between(start, end);

    assertThat(result).isZero();
  }

  @Test
  void shouldReturnZeroWhenEndDateIsTheDayBeforeStartDate() {
    LocalDate start = LocalDate.of(2026, 11, 3);
    LocalDate end = LocalDate.of(2026, 11, 2);

    long result = WorkingDays.between(start, end);

    assertThat(result).isZero();
  }
}
