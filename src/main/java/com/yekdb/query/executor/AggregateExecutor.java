package com.yekdb.query.executor;

import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SQL aggregate fonksiyonlarını yürütür.
 *
 * <p>Desteklenen fonksiyonlar:</p>
 * <ul>
 *     <li>COUNT(*)</li>
 *     <li>COUNT(column)</li>
 *     <li>SUM(column)</li>
 *     <li>AVG(column)</li>
 *     <li>MIN(column)</li>
 *     <li>MAX(column)</li>
 * </ul>
 *
 * <p>Normal tablo satırları bu sınıfta, JOIN sonucu oluşan Map tabanlı
 * satırlar ise {@link JoinedAggregateExecutor} üzerinden yürütülür.</p>
 *
 * Sprint 00-17 refactor.
 */
public final class AggregateExecutor {

    public enum AggregateFunction {
        COUNT,
        SUM,
        AVG,
        MIN,
        MAX
    }

    private final JoinedAggregateExecutor joinedAggregateExecutor;

    public AggregateExecutor() {
        this.joinedAggregateExecutor = new JoinedAggregateExecutor();
    }

    /**
     * Normal tablo satırları üzerinde aggregate fonksiyonu çalıştırır.
     */
    public Object execute(
            List<Row> rows,
            List<Column> columns,
            AggregateFunction function,
            String columnName
    ) {
        Objects.requireNonNull(rows, "Rows cannot be null.");
        Objects.requireNonNull(columns, "Columns cannot be null.");
        Objects.requireNonNull(function, "Aggregate function cannot be null.");

        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException(
                    "Aggregate column cannot be null or blank."
            );
        }

        return switch (function) {
            case COUNT -> executeCount(rows, columns, columnName);
            case SUM -> executeSum(rows, columns, columnName);
            case AVG -> executeAverage(rows, columns, columnName);
            case MIN -> executeMin(rows, columns, columnName);
            case MAX -> executeMax(rows, columns, columnName);
        };
    }

    /**
     * JOIN sonucu oluşan Map tabanlı satırlar üzerinde aggregate çalıştırır.
     */
    public Object executeJoinedRows(
            List<Map<String, Object>> rows,
            AggregateFunction function,
            String columnName
    ) {
        return joinedAggregateExecutor.execute(
                rows,
                function,
                columnName
        );
    }

    private long executeCount(
            List<Row> rows,
            List<Column> columns,
            String columnName
    ) {
        if ("*".equals(columnName.trim())) {
            return rows.size();
        }

        AggregateValueSupport.findColumnIndex(
                columns,
                columnName
        );

        /*
         * Row mevcut formatta null değer kabul etmediği için
         * COUNT(column) satır sayısına eşittir.
         */
        return rows.size();
    }

    private double executeSum(
            List<Row> rows,
            List<Column> columns,
            String columnName
    ) {
        int columnIndex = AggregateValueSupport.findColumnIndex(
                columns,
                columnName
        );

        double sum = 0.0;

        for (Row row : rows) {
            Object value = row.getValue(columnIndex);

            sum += AggregateValueSupport
                    .requireNumber(value, columnName)
                    .doubleValue();
        }

        return sum;
    }

    private double executeAverage(
            List<Row> rows,
            List<Column> columns,
            String columnName
    ) {
        if (rows.isEmpty()) {
            return 0.0;
        }

        return executeSum(rows, columns, columnName)
                / rows.size();
    }

    private Object executeMin(
            List<Row> rows,
            List<Column> columns,
            String columnName
    ) {
        if (rows.isEmpty()) {
            return null;
        }

        int columnIndex = AggregateValueSupport.findColumnIndex(
                columns,
                columnName
        );

        Object minimum = rows.getFirst().getValue(columnIndex);

        for (int i = 1; i < rows.size(); i++) {
            Object currentValue = rows.get(i).getValue(columnIndex);

            if (AggregateValueSupport.compareValues(
                    currentValue,
                    minimum
            ) < 0) {
                minimum = currentValue;
            }
        }

        return minimum;
    }

    private Object executeMax(
            List<Row> rows,
            List<Column> columns,
            String columnName
    ) {
        if (rows.isEmpty()) {
            return null;
        }

        int columnIndex = AggregateValueSupport.findColumnIndex(
                columns,
                columnName
        );

        Object maximum = rows.getFirst().getValue(columnIndex);

        for (int i = 1; i < rows.size(); i++) {
            Object currentValue = rows.get(i).getValue(columnIndex);

            if (AggregateValueSupport.compareValues(
                    currentValue,
                    maximum
            ) > 0) {
                maximum = currentValue;
            }
        }

        return maximum;
    }
}
