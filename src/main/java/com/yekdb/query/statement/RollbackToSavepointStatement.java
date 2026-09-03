package com.yekdb.query.statement;

import java.util.Objects;

/**
 * ROLLBACK TO SAVEPOINT statement modelidir.
 */
public final class RollbackToSavepointStatement implements Statement {

    private final String savepointName;

    public RollbackToSavepointStatement(
            String savepointName
    ) {

        this.savepointName =
                Objects.requireNonNull(
                        savepointName,
                        "SavepointName cannot be null."
                );

        if (savepointName.isBlank()) {
            throw new IllegalArgumentException(
                    "Savepoint name cannot be blank."
            );
        }
    }

    public String getSavepointName() {
        return savepointName;
    }

    @Override
    public StatementType getType() {
        return StatementType.ROLLBACK_TO_SAVEPOINT;
    }
}
