package com.yekdb.query.command;

/**
 * ALTER TABLE ... RENAME TO ...
 */
public final class RenameTableAlterAction
        implements AlterTableAction {

    private final String newTableName;

    public RenameTableAlterAction(String newTableName) {
        if (newTableName == null || newTableName.isBlank()) {
            throw new IllegalArgumentException(
                    "New table name cannot be null or blank."
            );
        }

        this.newTableName = newTableName.trim();
    }

    public String newTableName() {
        return newTableName;
    }
}