package com.yekdb.query.executor;

import com.yekdb.query.statement.GroupByClause;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * SQL GROUP BY işlemini yürütür.
 *
 * Aynı GROUP BY anahtarına sahip satırlar
 * aynı grup altında toplanır.
 *
 * Sprint 00-14
 */
public final class GroupByExecutor {

    /**
     * Satırları GROUP BY kolonlarına göre gruplar.
     *
     * @return
     * key   -> GROUP BY değerleri
     * value -> o gruba ait satırlar
     */
    public Map<List<Object>, List<Row>> execute(
            List<Row> rows,
            List<Column> columns,
            GroupByClause groupByClause
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
                groupByClause,
                "GroupByClause cannot be null."
        );

        List<Integer> groupColumnIndexes =
                resolveColumnIndexes(
                        columns,
                        groupByClause.getColumnNames()
                );

        Map<List<Object>, List<Row>> groups =
                new LinkedHashMap<>();

        for (Row row : rows) {

            Objects.requireNonNull(
                    row,
                    "Row cannot be null."
            );

            List<Object> groupKey =
                    createGroupKey(
                            row,
                            groupColumnIndexes
                    );

            groups.computeIfAbsent(
                    groupKey,
                    ignored ->
                            new ArrayList<>()
            ).add(
                    row
            );
        }

        return groups;
    }

    /**
     * GROUP BY kolonlarının Row içindeki
     * fiziksel indekslerini çözer.
     */
    private List<Integer> resolveColumnIndexes(
            List<Column> columns,
            List<String> groupByColumns
    ) {

        List<Integer> indexes =
                new ArrayList<>();

        for (String columnName : groupByColumns) {

            indexes.add(
                    findColumnIndex(
                            columns,
                            columnName
                    )
            );
        }

        return indexes;
    }

    /**
     * Tek bir kolonun indeksini bulur.
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

            Column column =
                    columns.get(i);

            if (column.getName()
                    .equalsIgnoreCase(
                            normalizedColumnName
                    )) {

                return i;
            }
        }

        throw new IllegalArgumentException(
                "GROUP BY column not found: "
                        + columnName
        );
    }

    /**
     * Qualified kolon desteği:
     *
     * users.department
     *
     * -> department
     *
     * Execution seviyesinde tablo kolonunun
     * fiziksel adı kullanılır.
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
     * Bir satır için GROUP BY anahtarını oluşturur.
     *
     * Örnek:
     *
     * GROUP BY department, city
     *
     * ["IT", "Malatya"]
     */
    private List<Object> createGroupKey(
            Row row,
            List<Integer> columnIndexes
    ) {

        List<Object> key =
                new ArrayList<>();

        for (Integer columnIndex : columnIndexes) {

            key.add(
                    row.getValue(
                            columnIndex
                    )
            );
        }

        return List.copyOf(
                key
        );
    }
}