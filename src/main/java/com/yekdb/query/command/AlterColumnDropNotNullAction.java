package com.yekdb.query.command;

/**
 * ALTER TABLE ... ALTER COLUMN ... DROP NOT NULL
 */
public final class AlterColumnDropNotNullAction
        implements AlterTableAction {

    private final String columnName;

    public AlterColumnDropNotNullAction(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException(
                    "Column name cannot be null or blank."
            );
        }

        this.columnName = columnName.trim();
    }

    public String columnName() {
        return columnName;
    }
}