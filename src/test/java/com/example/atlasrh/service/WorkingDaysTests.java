package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.atlasrh.util.WorkingDays;

class WorkingDaysTests {
  @ParameterizedTest
  @CsvSource({
      "2026-11-02,2026-11-02,1",
      "2026-11-07,2026-11-08,0",
      "2026-11-06,2026-11-09,2",
      "2026-11-02,2026-11-08,5",
      "2026-11-02,2026-11-15,10",
      "2026-11-07,2026-11-13,5",
      "2026-12-31,2027-01-04,3",
      "2024-02-28,2024-03-01,3",
      "2026-11-03,2026-11-02,0"
  })
  void countsInclusiveWeekdays(String start, String end, long expected) {
    assertThat(WorkingDays.between(LocalDate.parse(start), LocalDate.parse(end))).isEqualTo(expected);
  }
}
