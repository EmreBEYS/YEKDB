package com.yekdb.query.executor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JOIN sonucu oluşan Map tabanlı satırlarda aggregate fonksiyonlarını yürütür.
 */
final class JoinedAggregateExecutor {

    Object execute(
            List<Map<String, Object>> rows,
            AggregateExecutor.AggregateFunction function,
            String columnName
    ) {
        Objects.requireNonNull(rows, "JOIN satırları null olamaz.");
        Objects.requireNonNull(function, "Aggregate fonksiyon null olamaz.");

        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException(
                    "Aggregate kolon adı null veya boş olamaz."
            );
        }

        return switch (function) {
            case COUNT -> executeCount(rows, columnName);
            case SUM -> executeSum(rows, columnName);
            case AVG -> executeAverage(rows, columnName);
            case MIN -> executeMin(rows, columnName);
            case MAX -> executeMax(rows, columnName);
        };
    }

    private long executeCount(
            List<Map<String, Object>> rows,
            String columnName
    ) {
        if ("*".equals(columnName.trim())) {
            return rows.size();
        }

        long count = 0;

        for (Map<String, Object> row : rows) {
            if (resolveJoinedValue(row, columnName) != null) {
                count++;
            }
        }

        return count;
    }

    private double executeSum(
            List<Map<String, Object>> rows,
            String columnName
    ) {
        double sum = 0.0;

        for (Map<String, Object> row : rows) {
            Object value = resolveJoinedValue(row, columnName);

            if (value == null) {
                continue;
            }

            sum += AggregateValueSupport
                    .requireNumber(value, columnName)
                    .doubleValue();
        }

        return sum;
    }

    private double executeAverage(
            List<Map<String, Object>> rows,
            String columnName
    ) {
        double sum = 0.0;
        long count = 0;

        for (Map<String, Object> row : rows) {
            Object value = resolveJoinedValue(row, columnName);

            if (value == null) {
                continue;
            }

            sum += AggregateValueSupport
                    .requireNumber(value, columnName)
                    .doubleValue();
            count++;
        }

        return count == 0 ? 0.0 : sum / count;
    }

    private Object executeMin(
            List<Map<String, Object>> rows,
            String columnName
    ) {
        Object minimum = null;

        for (Map<String, Object> row : rows) {
            Object value = resolveJoinedValue(row, columnName);

            if (value == null) {
                continue;
            }

            if (minimum == null
                    || AggregateValueSupport.compareValues(value, minimum) < 0) {
                minimum = value;
            }
        }

        return minimum;
    }

    private Object executeMax(
            List<Map<String, Object>> rows,
            String columnName
    ) {
        Object maximum = null;

        for (Map<String, Object> row : rows) {
            Object value = resolveJoinedValue(row, columnName);

            if (value == null) {
                continue;
            }

            if (maximum == null
                    || AggregateValueSupport.compareValues(value, maximum) > 0) {
                maximum = value;
            }
        }

        return maximum;
    }

    private Object resolveJoinedValue(
            Map<String, Object> row,
            String columnName
    ) {
        String normalized = columnName.trim();

        if (!normalized.contains(".")) {
            throw new IllegalArgumentException(
                    "JOIN aggregate kolonları qualified olmalıdır: "
                            + columnName
            );
        }

        if (!row.containsKey(normalized)) {
            throw new IllegalArgumentException(
                    "Aggregate kolonu JOIN sonucunda bulunamadı: "
                            + columnName
            );
        }

        return row.get(normalized);
    }
}
