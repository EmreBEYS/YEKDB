package com.yekdb.constraint;

import java.util.List;
import java.util.Objects;

public final class ForeignKeyConstraint implements Constraint {

    private final String name;
    private final List<String> columns;
    private final String referencedTableName;
    private final List<String> referencedColumnNames;

    public ForeignKeyConstraint(
            List<String> columns,
            String referencedTableName,
            List<String> referencedColumnNames
    ) {
        this(
                null,
                columns,
                referencedTableName,
                referencedColumnNames
        );
    }

    public ForeignKeyConstraint(
            String column,
            String referencedTableName,
            String referencedColumn
    ) {
        this(
                null,
                List.of(column),
                referencedTableName,
                List.of(referencedColumn)
        );
    }

    public ForeignKeyConstraint(
            String name,
            List<String> columns,
            String referencedTableName,
            List<String> referencedColumnNames
    ) {
        Objects.requireNonNull(columns, "Columns cannot be null");
        Objects.requireNonNull(referencedColumnNames, "Referenced columns cannot be null");

        if (columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Foreign key must contain at least one local column"
            );
        }

        if (referencedColumnNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "Foreign key must reference at least one column"
            );
        }

        if (columns.size() != referencedColumnNames.size()) {
            throw new IllegalArgumentException(
                    "Foreign key column count must match referenced column count"
            );
        }

        if (referencedTableName == null || referencedTableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Referenced table name cannot be null or blank"
            );
        }

        this.name = ConstraintName.normalizeNullable(name);

        this.columns = columns.stream()
                .map(column -> requireText(column, "Column name"))
                .toList();

        this.referencedTableName = referencedTableName;

        this.referencedColumnNames = referencedColumnNames.stream()
                .map(column -> requireText(column, "Referenced column name"))
                .toList();
    }

    @Override
    public ConstraintType type() {
        return ConstraintType.FOREIGN_KEY;
    }

    @Override
    public List<String> columns() {
        return columns;
    }

    @Override
    public String name() {
        return name;
    }

    public String referencedTableName() {
        return referencedTableName;
    }

    public List<String> referencedColumnNames() {
        return referencedColumnNames;
    }

    public String referencedColumnName() {
        if (referencedColumnNames.size() != 1) {
            throw new IllegalStateException(
                    "Foreign key contains multiple referenced columns"
            );
        }

        return referencedColumnNames.getFirst();
    }

    public boolean isComposite() {
        return columns.size() > 1;
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be null or blank"
            );
        }

        return value;
    }

    @Override
    public String toString() {
        return "ForeignKeyConstraint{" +
                "name='" + name + '\'' +
                ", columns=" + columns +
                ", referencedTableName='" + referencedTableName + '\'' +
                ", referencedColumnNames=" + referencedColumnNames +
                '}';
    }
}
