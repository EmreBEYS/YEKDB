package com.yekdb.query.statement;

import java.util.Objects;

/**
 * SAVEPOINT statement modelidir.
 */
public final class SavepointStatement implements Statement {

    private final String savepointName;

    public SavepointStatement(
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
        return StatementType.SAVEPOINT;
    }
}
