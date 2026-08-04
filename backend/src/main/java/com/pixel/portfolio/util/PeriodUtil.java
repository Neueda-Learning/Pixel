package com.pixel.portfolio.util;

import com.pixel.portfolio.exception.BadRequestException;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

/** Shared parsing for the 1M/3M/6M/1Y/ALL period query params used across chart endpoints. */
public final class PeriodUtil {

    public static final Set<String> VALID_PERIODS = Set.of("1M", "3M", "6M", "1Y", "ALL");

    private PeriodUtil() {}

    /** Returns the inclusive start date for the given period, or null for "ALL" (no lower bound). */
    public static LocalDate startDateFor(String period, LocalDate today) {
        String p = period == null ? "ALL" : period.toUpperCase(Locale.ROOT);
        return switch (p) {
            case "1M" -> today.minusMonths(1);
            case "3M" -> today.minusMonths(3);
            case "6M" -> today.minusMonths(6);
            case "1Y" -> today.minusYears(1);
            case "ALL" -> null;
            default -> throw new BadRequestException(
                    "Invalid period '" + period + "'. Expected one of " + VALID_PERIODS);
        };
    }
}
