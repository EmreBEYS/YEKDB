package com.yekdb.query.executor;

import com.yekdb.query.statement.OrderByItem;
import com.yekdb.query.statement.SortDirection;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * ORDER BY işlemlerini uygular.
 *
 * <p>Örnek:</p>
 *
 * <pre>
 * ORDER BY salary ASC
 * ORDER BY salary DESC
 * ORDER BY department ASC, salary DESC
 * </pre>
 *
 * Sprint 00-14
 */
public final class OrderByExecutor {

    /**
     * Satırları verilen ORDER BY kurallarına göre sıralar.
     *
     * @param rows         sıralanacak satırlar
     * @param columns      tablo kolonları
     * @param orderByItems sıralama kuralları
     * @return sıralanmış yeni liste
     */
    public List<Row> execute(
            List<Row> rows,
            List<Column> columns,
            List<OrderByItem> orderByItems
    ) {

        Objects.requireNonNull(
                rows,
                "rows cannot be null"
        );

        Objects.requireNonNull(
                columns,
                "columns cannot be null"
        );

        Objects.requireNonNull(
                orderByItems,
                "orderByItems cannot be null"
        );

        if (orderByItems.isEmpty()) {
            return new ArrayList<>(rows);
        }

        Comparator<Row> comparator =
                buildComparator(
                        columns,
                        orderByItems
                );

        List<Row> result =
                new ArrayList<>(
                        rows
                );

        result.sort(
                comparator
        );

        return result;
    }

    /**
     * Çoklu ORDER BY ifadelerini tek comparator altında birleştirir.
     */
    private Comparator<Row> buildComparator(
            List<Column> columns,
            List<OrderByItem> orderByItems
    ) {

        Comparator<Row> comparator =
                createComparator(
                        columns,
                        orderByItems.getFirst()
                );

        for (int i = 1;
             i < orderByItems.size();
             i++) {

            comparator =
                    comparator.thenComparing(
                            createComparator(
                                    columns,
                                    orderByItems.get(i)
                            )
                    );
        }

        return comparator;
    }

    /**
     * Tek bir ORDER BY item için comparator oluşturur.
     */
    private Comparator<Row> createComparator(
            List<Column> columns,
            OrderByItem item
    ) {

        int columnIndex =
                findColumnIndex(
                        columns,
                        item.getColumnName()
                );

        return (leftRow, rightRow) -> {

            Objects.requireNonNull(
                    leftRow,
                    "left row cannot be null"
            );

            Objects.requireNonNull(
                    rightRow,
                    "right row cannot be null"
            );

            Object leftValue =
                    leftRow.getValue(
                            columnIndex
                    );

            Object rightValue =
                    rightRow.getValue(
                            columnIndex
                    );

            int result =
                    compareValues(
                            leftValue,
                            rightValue
                    );

            return item.getDirection()
                    == SortDirection.DESC
                    ? -result
                    : result;
        };
    }

    /**
     * Kolon adına göre fiziksel Row indeksini bulur.
     */
    private int findColumnIndex(
            List<Column> columns,
            String columnName
    ) {

        Objects.requireNonNull(
                columnName,
                "columnName cannot be null"
        );

        String normalizedColumnName =
                columnName
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        for (int i = 0;
             i < columns.size();
             i++) {

            Column column =
                    columns.get(i);

            if (column.getName()
                    .equals(
                            normalizedColumnName
                    )) {

                return i;
            }
        }

        throw new IllegalArgumentException(
                "ORDER BY column not found: "
                        + columnName
        );
    }

    /**
     * ORDER BY değerlerini karşılaştırır.
     */
    private int compareValues(
            Object leftValue,
            Object rightValue
    ) {

        /*
         * Row şu anda null değer kabul etmiyor.
         * Ancak ileride NULL desteği geldiğinde
         * executor hazır olsun.
         */
        if (leftValue == null
                && rightValue == null) {

            return 0;
        }

        if (leftValue == null) {
            return 1;
        }

        if (rightValue == null) {
            return -1;
        }

        /*
         * Integer / Long / Double gibi
         * farklı Number türlerini destekler.
         */
        if (leftValue instanceof Number leftNumber
                && rightValue instanceof Number rightNumber) {

            return Double.compare(
                    leftNumber.doubleValue(),
                    rightNumber.doubleValue()
            );
        }

        /*
         * Aynı tür Comparable değerler.
         */
        if (leftValue instanceof Comparable<?> comparable
                && leftValue.getClass()
                .isInstance(rightValue)) {

            return compareComparable(
                    comparable,
                    rightValue
            );
        }

        throw new IllegalArgumentException(
                "Values cannot be compared: "
                        + leftValue
                        + " and "
                        + rightValue
        );
    }

    @SuppressWarnings("unchecked")
    private int compareComparable(
            Comparable<?> leftValue,
            Object rightValue
    ) {

        Comparable<Object> comparable =
                (Comparable<Object>) leftValue;

        return comparable.compareTo(
                rightValue
        );
    }
}