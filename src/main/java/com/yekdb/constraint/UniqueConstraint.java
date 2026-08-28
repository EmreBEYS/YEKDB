package com.yekdb.constraint;

import java.util.List;
import java.util.Objects;

public final class UniqueConstraint implements Constraint {

    private final String name;
    private final List<String> columns;

    public UniqueConstraint(List<String> columns) {
        this(null, columns);
    }

    public UniqueConstraint(String column) {
        this(null, List.of(column));
    }

    public UniqueConstraint(
            String name,
            List<String> columns
    ) {
        this.name = ConstraintName.normalizeNullable(name);
        this.columns = validateColumns(columns);
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.UNIQUE;
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
                    "UNIQUE constraint must contain at least one column"
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
                    "UNIQUE constraint cannot contain duplicate columns"
            );
        }

        return List.copyOf(normalized);
    }

    @Override
    public String toString() {
        return "UniqueConstraint{" +
                "name='" + name + '\'' +
                ", columns=" + columns +
                '}';
    }
}
