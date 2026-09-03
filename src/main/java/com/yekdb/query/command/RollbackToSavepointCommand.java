package com.yekdb.query.command;

import java.util.Objects;

/**
 * ROLLBACK TO SAVEPOINT komutunu execution katmanina tasir.
 */
public final class RollbackToSavepointCommand implements Command {

    private final String savepointName;

    public RollbackToSavepointCommand(
            String savepointName
    ) {

        this.savepointName =
                Objects.requireNonNull(
                        savepointName,
                        "SavepointName cannot be null."
                );
    }

    public String getSavepointName() {
        return savepointName;
    }
}
