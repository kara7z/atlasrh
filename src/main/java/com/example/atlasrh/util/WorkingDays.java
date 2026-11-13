package com.example.atlasrh.util;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class WorkingDays {

  public static long between(LocalDate start, LocalDate end) {
    if (end.isBefore(start)) {
      return 0;
    }
    long days = ChronoUnit.DAYS.between(start, end) + 1;
    long result = (days / 7) * 5;
    for (int i = 0; i < days % 7; i++) {
      if (start.plusDays(i).getDayOfWeek().getValue() <= 5) {
        result++;
      }
    }
    return result;
  }
}
