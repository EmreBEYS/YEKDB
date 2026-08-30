package com.yekdb.storage.table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates textual values for YEKDB logical UUID and temporal SQL types.
 *
 * Sprint 00-28 Phase 11 keeps these logical values as UTF-8 strings in the
 * V1 row format so the existing binary row format remains backward compatible.
 * The declared column type still enforces SQL-level validation on INSERT/UPDATE.
 */
public final class SqlTemporalValueValidator {

    private static final Pattern HUMAN_INTERVAL_PART = Pattern.compile(
            "([+-]?\\d+(?:\\.\\d+)?)\\s+" +
                    "(years?|months?|weeks?|days?|hours?|minutes?|seconds?)",
            Pattern.CASE_INSENSITIVE
    );

    private SqlTemporalValueValidator() {
        // Utility class.
    }

    public static boolean isValid(DataType dataType, String value) {
        if (value == null) {
            return true;
        }

        if (dataType == null) {
            return false;
        }

        String candidate = value.trim();
        if (candidate.isEmpty()) {
            return false;
        }

        return switch (dataType) {
            case UUID -> isUuid(candidate);
            case DATE -> isDate(candidate);
            case TIME -> isTime(candidate);
            case TIMESTAMP -> isTimestamp(candidate);
            case INTERVAL -> isInterval(candidate);
            default -> true;
        };
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean isDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private static boolean isTime(String value) {
        try {
            LocalTime.parse(value);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private static boolean isTimestamp(String value) {
        String normalized = value.replace(' ', 'T');

        try {
            OffsetDateTime.parse(normalized);
            return true;
        } catch (DateTimeParseException ignored) {
            // Try timestamp without timezone/offset next.
        }

        try {
            LocalDateTime.parse(normalized);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private static boolean isInterval(String value) {
        // ISO-8601 interval-like values are accepted as a V1 textual form.
        if (value.startsWith("P") || value.startsWith("-P") || value.startsWith("+P")) {
            return value.matches("[+-]?P(?=\\d|T)(?:\\d+Y)?(?:\\d+M)?(?:\\d+W)?(?:\\d+D)?(?:T(?=\\d)(?:\\d+H)?(?:\\d+M)?(?:\\d+(?:\\.\\d+)?S)?)?");
        }

        Matcher matcher = HUMAN_INTERVAL_PART.matcher(value);
        int cursor = 0;
        boolean found = false;

        while (matcher.find()) {
            String gap = value.substring(cursor, matcher.start());
            if (!gap.isBlank()) {
                return false;
            }

            found = true;
            cursor = matcher.end();
        }

        return found && value.substring(cursor).isBlank();
    }
}
