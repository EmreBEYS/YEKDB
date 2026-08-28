package com.yekdb.query.command;

import com.yekdb.constraint.Constraint;

import java.util.Objects;

/**
 * ALTER TABLE ... ADD CONSTRAINT ...
 */
public final class AddConstraintAlterAction
        implements AlterTableAction {

    private final Constraint constraint;

    public AddConstraintAlterAction(
            Constraint constraint
    ) {
        this.constraint = Objects.requireNonNull(
                constraint,
                "Constraint cannot be null."
        );
    }

    public Constraint constraint() {
        return constraint;
    }
}