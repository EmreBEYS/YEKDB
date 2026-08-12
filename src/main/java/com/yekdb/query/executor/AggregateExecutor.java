package com.yekdb.query.executor;

import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * SQL aggregate fonksiyonlarını yürütür.
 *
 * Desteklenen fonksiyonlar:
 *
 * COUNT(*)
 * COUNT(column)
 * SUM(column)
 * AVG(column)
 * MIN(column)
 * MAX(column)
 *
 * Sprint 00-14
 */
public final class AggregateExecutor {

    /**
     * Aggregate fonksiyon türü.
     */
    public enum AggregateFunction {
        COUNT,
        SUM,
        AVG,
        MIN,
        MAX
    }

    /**
     * Aggregate fonksiyonunu çalıştırır.
     *
     * COUNT(*) için columnName "*" olabilir.
     *
     * @param rows         kaynak satırlar
     * @param columns      tablo kolonları
     * @param function     aggregate fonksiyon
     * @param columnName   hedef kolon veya "*"
     * @return aggregate sonucu
     */
    public Object execute(
            List<Row> rows,
            List<Column> columns,
            AggregateFunction function,
            String columnName
    ) {

        Objects.requireNonNull(
                rows,
                "Rows cannot be null."
        );

        Objects.requireNonNull(
                columns,
                "Columns cannot be null."
        );

        Objects.requireNonNull(
                function,
                "Aggregate function cannot be null."
        );

        if (columnName == null
                || columnName.isBlank()) {

            throw new IllegalArgumentException(
                    "Aggregate column cannot be null or blank."
            );
        }

        return switch (function) {

            case COUNT ->
                    executeCount(
                            rows,
                            columns,
                            columnName
                    );

            case SUM ->
                    executeSum(
                            rows,
                            columns,
                            columnName
                    );

            case AVG ->
                    executeAverage(
                            rows,
                            columns,
                            columnName
                    );

            case MIN ->
                    executeMin(
                            rows,
                            columns,
                            columnName
                    );

            case MAX ->
                    executeMax(
                            rows,
                            columns,
                            columnName
                    );
        };
    }

    /**
     * COUNT
     *
     * COUNT(*) bütün satırları sayar.
     *
     * Row mevcut mimaride null değer kabul etmediği için
     * COUNT(column) da satır sayısına eşittir.
     */
    private long executeCount(
            List<Row> rows,
            List<Column> columns,
            String columnName
    ) {

        if ("*".equals(
                columnName.trim()
        )) {

            return rows.size();
        }

        /*
         * Kolon gerçekten mevcut mu kontrol et.
         */
        findColumnIndex(
                columns,
                columnName
        );

        return rows.size();
    }

    /**
     * SUM
     */
    private double executeSum(
            List<Row> rows,
            List<Column> columns,
            String columnName
    ) {

        int columnIndex =
                findColumnIndex(
                        columns,
                        columnName
                );

        double sum =
                0.0;

        for (Row row : rows) {

            Object value =
                    row.getValue(
                            columnIndex
                    );

            Number number =
                    requireNumber(
                            value,
                            columnName
                    );

            sum +=
                    number.doubleValue();
        }

        return sum;
    }

    /**
     * AVG
     */
    private double executeAverage(
            List<Row> rows,
            List<Column> columns,
            String columnName
    ) {

        if (rows.isEmpty()) {

            return 0.0;
        }

        double sum =
                executeSum(
                        rows,
                        columns,
                        columnName
                );

        return sum
                / rows.size();
    }

    /**
     * MIN
     */
    private Object executeMin(
            List<Row> rows,
            List<Column> columns,
            String columnName
    ) {

        if (rows.isEmpty()) {

            return null;
        }

        int columnIndex =
                findColumnIndex(
                        columns,
                        columnName
                );

        Object minimum =
                rows.getFirst()
                        .getValue(
                                columnIndex
                        );

        for (int i = 1;
             i < rows.size();
             i++) {

            Object currentValue =
                    rows.get(i)
                            .getValue(
                                    columnIndex
                            );

            if (compareValues(
                    currentValue,
                    minimum
            ) < 0) {

                minimum =
                        currentValue;
            }
        }

        return minimum;
    }

    /**
     * MAX
     */
    private Object executeMax(
            List<Row> rows,
            List<Column> columns,
            String columnName
    ) {

        if (rows.isEmpty()) {

            return null;
        }

        int columnIndex =
                findColumnIndex(
                        columns,
                        columnName
                );

        Object maximum =
                rows.getFirst()
                        .getValue(
                                columnIndex
                        );

        for (int i = 1;
             i < rows.size();
             i++) {

            Object currentValue =
                    rows.get(i)
                            .getValue(
                                    columnIndex
                            );

            if (compareValues(
                    currentValue,
                    maximum
            ) > 0) {

                maximum =
                        currentValue;
            }
        }

        return maximum;
    }

    /**
     * Kolon indeksini bulur.
     *
     * Qualified column desteği:
     *
     * users.salary
     *
     * -> salary
     */
    private int findColumnIndex(
            List<Column> columns,
            String columnName
    ) {

        String normalizedColumnName =
                normalizeColumnName(
                        columnName
                );

        for (int i = 0;
             i < columns.size();
             i++) {

            if (columns.get(i)
                    .getName()
                    .equalsIgnoreCase(
                            normalizedColumnName
                    )) {

                return i;
            }
        }

        throw new IllegalArgumentException(
                "Aggregate column not found: "
                        + columnName
        );
    }

    /**
     * Qualified kolon adını fiziksel kolon
     * adına dönüştürür.
     */
    private String normalizeColumnName(
            String columnName
    ) {

        String normalized =
                columnName
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        int dotIndex =
                normalized.lastIndexOf('.');

        if (dotIndex >= 0) {

            return normalized.substring(
                    dotIndex + 1
            );
        }

        return normalized;
    }

    /**
     * SUM / AVG için sayısal değer kontrolü.
     */
    private Number requireNumber(
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

    /**
     * MIN / MAX karşılaştırması.
     */
    private int compareValues(
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
                && left.getClass()
                .isInstance(
                        right
                )) {

            return compareComparable(
                    comparable,
                    right
            );
        }

        throw new IllegalArgumentException(
                "Values cannot be compared: "
                        + left
                        + " and "
                        + right
        );
    }

    @SuppressWarnings("unchecked")
    private int compareComparable(
            Comparable<?> left,
            Object right
    ) {

        Comparable<Object> comparable =
                (Comparable<Object>) left;

        return comparable.compareTo(
                right
        );
    }
}