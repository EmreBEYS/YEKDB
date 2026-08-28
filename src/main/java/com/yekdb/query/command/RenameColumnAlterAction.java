package com.yekdb.query.command;

/**
 * ALTER TABLE ... RENAME COLUMN ... TO ...
 */
public final class RenameColumnAlterAction
        implements AlterTableAction {

    private final String oldColumnName;
    private final String newColumnName;

    public RenameColumnAlterAction(
            String oldColumnName,
            String newColumnName
    ) {
        if (oldColumnName == null || oldColumnName.isBlank()) {
            throw new IllegalArgumentException(
                    "Old column name cannot be null or blank."
            );
        }

        if (newColumnName == null || newColumnName.isBlank()) {
            throw new IllegalArgumentException(
                    "New column name cannot be null or blank."
            );
        }

        this.oldColumnName = oldColumnName.trim();
        this.newColumnName = newColumnName.trim();
    }

    public String oldColumnName() {
        return oldColumnName;
    }

    public String newColumnName() {
        return newColumnName;
    }
}