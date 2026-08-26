package com.yekdb.constraint;

import java.util.List;
import java.util.Objects;

public final class NotNullConstraint implements Constraint {

    private final String column;

    public NotNullConstraint(String column) {
        this.column = requireColumn(column);
    }

    public String column() {
        return column;
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.NOT_NULL;
    }

    @Override
    public List<String> columns() {
        return List.of(column);
    }

    private static String requireColumn(String column) {
        Objects.requireNonNull(column, "column cannot be null");

        String normalized = column.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("column cannot be blank");
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "NotNullConstraint{" +
                "column='" + column + '\'' +
                '}';
    }
}