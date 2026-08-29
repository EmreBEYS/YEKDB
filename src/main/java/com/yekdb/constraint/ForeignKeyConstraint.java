package com.yekdb.constraint;

import java.util.List;
import java.util.Objects;

public final class ForeignKeyConstraint implements Constraint {

    private static final ReferentialAction DEFAULT_REFERENTIAL_ACTION =
            ReferentialAction.RESTRICT;

    private final String name;
    private final List<String> columns;
    private final String referencedTableName;
    private final List<String> referencedColumnNames;
    private final ReferentialAction onDelete;
    private final ReferentialAction onUpdate;

    public ForeignKeyConstraint(
            List<String> columns,
            String referencedTableName,
            List<String> referencedColumnNames
    ) {
        this(
                null,
                columns,
                referencedTableName,
                referencedColumnNames,
                DEFAULT_REFERENTIAL_ACTION,
                DEFAULT_REFERENTIAL_ACTION
        );
    }

    public ForeignKeyConstraint(
            List<String> columns,
            String referencedTableName,
            List<String> referencedColumnNames,
            ReferentialAction onDelete,
            ReferentialAction onUpdate
    ) {
        this(
                null,
                columns,
                referencedTableName,
                referencedColumnNames,
                onDelete,
                onUpdate
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
                List.of(referencedColumn),
                DEFAULT_REFERENTIAL_ACTION,
                DEFAULT_REFERENTIAL_ACTION
        );
    }

    public ForeignKeyConstraint(
            String column,
            String referencedTableName,
            String referencedColumn,
            ReferentialAction onDelete,
            ReferentialAction onUpdate
    ) {
        this(
                null,
                List.of(column),
                referencedTableName,
                List.of(referencedColumn),
                onDelete,
                onUpdate
        );
    }

    public ForeignKeyConstraint(
            String name,
            List<String> columns,
            String referencedTableName,
            List<String> referencedColumnNames
    ) {
        this(
                name,
                columns,
                referencedTableName,
                referencedColumnNames,
                DEFAULT_REFERENTIAL_ACTION,
                DEFAULT_REFERENTIAL_ACTION
        );
    }

    public ForeignKeyConstraint(
            String name,
            List<String> columns,
            String referencedTableName,
            List<String> referencedColumnNames,
            ReferentialAction onDelete,
            ReferentialAction onUpdate
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

        this.onDelete = normalizeAction(onDelete);
        this.onUpdate = normalizeAction(onUpdate);
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

    public ReferentialAction onDelete() {
        return onDelete;
    }

    public ReferentialAction onUpdate() {
        return onUpdate;
    }

    public boolean isComposite() {
        return columns.size() > 1;
    }

    private static ReferentialAction normalizeAction(
            ReferentialAction action
    ) {
        return action == null
                ? DEFAULT_REFERENTIAL_ACTION
                : action;
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
                ", onDelete=" + onDelete +
                ", onUpdate=" + onUpdate +
                '}';
    }
}
