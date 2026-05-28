package com.skyfl.pfm.common.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

public final class DateUtils {

    private DateUtils() {
    }

    public static LocalDate monthStart(int year, int month) {
        return YearMonth.of(year, month).atDay(1);
    }

    public static LocalDate monthEnd(int year, int month) {
        return YearMonth.of(year, month).atEndOfMonth();
    }

    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    }

    public static LocalDate weekEnd(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
    }
}
