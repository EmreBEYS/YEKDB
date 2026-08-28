package com.yekdb.constraint;

import java.util.List;
import java.util.Objects;

public final class PrimaryKeyConstraint implements Constraint {

    private final String name;
    private final List<String> columns;

    public PrimaryKeyConstraint(List<String> columns) {
        this(null, columns);
    }

    public PrimaryKeyConstraint(String column) {
        this(null, List.of(column));
    }

    public PrimaryKeyConstraint(
            String name,
            List<String> columns
    ) {
        this.name = ConstraintName.normalizeNullable(name);
        this.columns = validateColumns(columns);
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.PRIMARY_KEY;
    }

    @Override
    public List<String> columns() {
        return columns;
    }

    @Override
    public String name() {
        return name;
    }

    private static List<String> validateColumns(List<String> columns) {
        Objects.requireNonNull(columns, "columns cannot be null");

        if (columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "PRIMARY KEY must contain at least one column"
            );
        }

        List<String> normalized = columns.stream()
                .map(column -> {
                    Objects.requireNonNull(
                            column,
                            "column cannot be null"
                    );

                    String value = column.trim();

                    if (value.isEmpty()) {
                        throw new IllegalArgumentException(
                                "column cannot be blank"
                        );
                    }

                    return value;
                })
                .toList();

        if (normalized.stream().distinct().count() != normalized.size()) {
            throw new IllegalArgumentException(
                    "PRIMARY KEY cannot contain duplicate columns"
            );
        }

        return List.copyOf(normalized);
    }

    @Override
    public String toString() {
        return "PrimaryKeyConstraint{" +
                "name='" + name + '\'' +
                ", columns=" + columns +
                '}';
    }
}
