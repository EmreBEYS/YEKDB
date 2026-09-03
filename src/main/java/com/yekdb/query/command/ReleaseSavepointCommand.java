package com.yekdb.query.command;

import java.util.Objects;

/**
 * RELEASE SAVEPOINT komutunu execution katmanina tasir.
 */
public final class ReleaseSavepointCommand implements Command {

    private final String savepointName;

    public ReleaseSavepointCommand(
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
