package com.yekdb.query.statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * SQL GROUP BY ifadesini temsil eder.
 *
 * Örnek:
 *
 * GROUP BY department
 *
 * GROUP BY department, city
 *
 * Sprint 00-14
 */
public final class GroupByClause {

    private final List<String> columnNames;

    public GroupByClause(
            List<String> columnNames
    ) {

        Objects.requireNonNull(
                columnNames,
                "GROUP BY columns cannot be null."
        );

        if (columnNames.isEmpty()) {

            throw new IllegalArgumentException(
                    "GROUP BY must contain at least one column."
            );
        }

        List<String> normalizedColumns =
                new ArrayList<>();

        for (String columnName : columnNames) {

            if (columnName == null
                    || columnName.isBlank()) {

                throw new IllegalArgumentException(
                        "GROUP BY column cannot be null or blank."
                );
            }

            normalizedColumns.add(
                    columnName.trim()
            );
        }

        this.columnNames =
                List.copyOf(
                        normalizedColumns
                );
    }

    public GroupByClause(
            String columnName
    ) {

        this(
                List.of(
                        columnName
                )
        );
    }

    public List<String> getColumnNames() {

        return Collections.unmodifiableList(
                columnNames
        );
    }

    public int size() {

        return columnNames.size();
    }

    @Override
    public String toString() {

        return "GROUP BY "
                + String.join(
                ", ",
                columnNames
        );
    }
}