package com.yekdb.query.statement;

import java.util.Objects;

/**
 * RELEASE SAVEPOINT statement modelidir.
 */
public final class ReleaseSavepointStatement implements Statement {

    private final String savepointName;

    public ReleaseSavepointStatement(
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
        return StatementType.RELEASE_SAVEPOINT;
    }
}
