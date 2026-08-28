package com.yekdb.query.command;

/**
 * ALTER TABLE ... DROP COLUMN ...
 */
public final class DropColumnAlterAction
        implements AlterTableAction {

    private final String columnName;

    public DropColumnAlterAction(String columnName) {
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

    @Override
    public String toString() {
        return "DropColumnAlterAction{" +
                "columnName='" + columnName + '\'' +
                '}';
    }
}