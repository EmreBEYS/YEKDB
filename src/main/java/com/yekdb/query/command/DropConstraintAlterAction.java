package com.yekdb.query.command;

/**
 * ALTER TABLE ... DROP CONSTRAINT ...
 */
public final class DropConstraintAlterAction
        implements AlterTableAction {

    private final String constraintName;

    public DropConstraintAlterAction(
            String constraintName
    ) {
        if (constraintName == null ||
                constraintName.isBlank()) {

            throw new IllegalArgumentException(
                    "Constraint name cannot be null or blank."
            );
        }

        this.constraintName = constraintName.trim();
    }

    public String constraintName() {
        return constraintName;
    }
}