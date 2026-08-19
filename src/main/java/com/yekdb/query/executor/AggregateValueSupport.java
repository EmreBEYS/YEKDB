package com.yekdb.query.executor;

import com.yekdb.storage.table.Column;

import java.util.List;
import java.util.Locale;

/**
 * Aggregate yürütücülerinin ortak düşük seviyeli değer işlemlerini toplar.
 */
final class AggregateValueSupport {

    private AggregateValueSupport() {
        // Utility sınıfı.
    }

    static int findColumnIndex(
            List<Column> columns,
            String columnName
    ) {
        String normalizedColumnName = normalizeColumnName(columnName);

        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i)
                    .getName()
                    .equalsIgnoreCase(normalizedColumnName)) {
                return i;
            }
        }

        throw new IllegalArgumentException(
                "Aggregate column not found: " + columnName
        );
    }

    static Number requireNumber(
            Object value,
            String columnName
    ) {
        if (value instanceof Number number) {
            return number;
        }

        throw new IllegalArgumentException(
                "Aggregate function requires numeric column: "
                        + columnName
                        + ". Value: "
                        + value
        );
    }

    static int compareValues(
            Object left,
            Object right
    ) {
        if (left instanceof Number leftNumber
                && right instanceof Number rightNumber) {
            return Double.compare(
                    leftNumber.doubleValue(),
                    rightNumber.doubleValue()
            );
        }

        if (left instanceof Comparable<?> comparable
                && left.getClass().isInstance(right)) {
            return compareComparable(comparable, right);
        }

        throw new IllegalArgumentException(
                "Values cannot be compared: "
                        + left
                        + " and "
                        + right
        );
    }

    private static String normalizeColumnName(String columnName) {
        String normalized = columnName
                .trim()
                .toLowerCase(Locale.ROOT);

        int dotIndex = normalized.lastIndexOf('.');

        if (dotIndex >= 0) {
            return normalized.substring(dotIndex + 1);
        }

        return normalized;
    }

    @SuppressWarnings("unchecked")
    private static int compareComparable(
            Comparable<?> left,
            Object right
    ) {
        Comparable<Object> comparable =
                (Comparable<Object>) left;

        return comparable.compareTo(right);
    }
}
