package com.yekdb.query.command;

import java.util.Objects;

/**
 * SAVEPOINT komutunu execution katmanina tasir.
 */
public final class SavepointCommand implements Command {

    private final String savepointName;

    public SavepointCommand(
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
