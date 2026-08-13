package com.yekdb.query.expression;

import java.util.Objects;

/**
 * Represents a column reference inside an SQL expression.
 *
 * Examples:
 *
 * name
 * employee_id
 * e.name
 * d.id
 */
public final class ColumnExpression implements Expression {

    private final String qualifier;
    private final String columnName;

    /**
     * Unqualified column.
     *
     * Example:
     *
     * name
     */
    public ColumnExpression(String columnName) {

        this(
                null,
                columnName
        );
    }

    /**
     * Qualified column.
     *
     * Example:
     *
     * e.name
     * department.id
     */
    public ColumnExpression(
            String qualifier,
            String columnName
    ) {

        this.qualifier =
                normalizeQualifier(
                        qualifier
                );

        this.columnName =
                normalizeColumnName(
                        columnName
                );
    }

    /**
     * Creates a ColumnExpression from raw SQL-like text.
     *
     * Examples:
     *
     * "name"
     * "e.name"
     * "department.id"
     */
    public static ColumnExpression parse(
            String expression
    ) {

        Objects.requireNonNull(
                expression,
                "column expression cannot be null"
        );

        String value =
                expression.trim();

        if (value.isEmpty()) {

            throw new IllegalArgumentException(
                    "column expression cannot be blank"
            );
        }

        int dotIndex =
                value.indexOf('.');

        /*
         * No qualifier:
         *
         * name
         */
        if (dotIndex < 0) {

            return new ColumnExpression(
                    value
            );
        }

        /*
         * Qualified:
         *
         * e.name
         */
        if (dotIndex == 0
                || dotIndex == value.length() - 1) {

            throw new IllegalArgumentException(
                    "Invalid qualified column: " + expression
            );
        }

        /*
         * Sprint 00-15 için yalnızca:
         *
         * qualifier.column
         *
         * formatını destekliyoruz.
         */
        if (value.indexOf('.', dotIndex + 1) >= 0) {

            throw new IllegalArgumentException(
                    "Invalid qualified column: " + expression
            );
        }

        String qualifier =
                value.substring(
                        0,
                        dotIndex
                );

        String columnName =
                value.substring(
                        dotIndex + 1
                );

        return new ColumnExpression(
                qualifier,
                columnName
        );
    }

    public String getQualifier() {

        return qualifier;
    }

    public String getColumnName() {

        return columnName;
    }

    public boolean isQualified() {

        return qualifier != null;
    }

    /**
     * Returns:
     *
     * name
     *
     * or:
     *
     * e.name
     */
    public String getQualifiedName() {

        if (!isQualified()) {

            return columnName;
        }

        return qualifier
                + "."
                + columnName;
    }

    /**
     * Case-insensitive qualifier match.
     */
    public boolean matchesQualifier(
            String value
    ) {

        if (!isQualified()
                || value == null) {

            return false;
        }

        return qualifier.equalsIgnoreCase(
                value
        );
    }

    /**
     * Case-insensitive column name match.
     */
    public boolean matchesColumn(
            String value
    ) {

        if (value == null) {

            return false;
        }

        return columnName.equalsIgnoreCase(
                value
        );
    }

    private static String normalizeQualifier(
            String qualifier
    ) {

        if (qualifier == null) {

            return null;
        }

        String normalized =
                qualifier.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    private static String normalizeColumnName(
            String columnName
    ) {

        Objects.requireNonNull(
                columnName,
                "column name cannot be null"
        );

        String normalized =
                columnName.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    "column name cannot be blank"
            );
        }

        if (normalized.contains(".")) {

            throw new IllegalArgumentException(
                    "column name cannot contain '.'"
            );
        }

        return normalized;
    }

    @Override
    public String toString() {

        return getQualifiedName();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {

            return true;
        }

        if (!(o instanceof ColumnExpression that)) {

            return false;
        }

        return Objects.equals(
                qualifier,
                that.qualifier
        ) && columnName.equals(
                that.columnName
        );
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                qualifier,
                columnName
        );
    }
}