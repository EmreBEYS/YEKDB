package com.yekdb.query.command;

import com.yekdb.transaction.TransactionAccessMode;
import com.yekdb.transaction.TransactionIsolationLevel;

import java.util.Objects;

/**
 * BEGIN komutunu execution katmanina tasir.
 */
public final class BeginTransactionCommand implements Command {

    private final TransactionAccessMode accessMode;
    private final TransactionIsolationLevel isolationLevel;

    public BeginTransactionCommand() {

        this(
                TransactionAccessMode.READ_WRITE,
                TransactionIsolationLevel.READ_COMMITTED
        );
    }

    public BeginTransactionCommand(
            TransactionAccessMode accessMode
    ) {

        this(
                accessMode,
                TransactionIsolationLevel.READ_COMMITTED
        );
    }

    public BeginTransactionCommand(
            TransactionAccessMode accessMode,
            TransactionIsolationLevel isolationLevel
    ) {

        this.accessMode =
                Objects.requireNonNull(
                        accessMode,
                        "AccessMode cannot be null."
                );

        this.isolationLevel =
                Objects.requireNonNull(
                        isolationLevel,
                        "IsolationLevel cannot be null."
                );
    }

    public TransactionAccessMode getAccessMode() {
        return accessMode;
    }

    public TransactionIsolationLevel getIsolationLevel() {
        return isolationLevel;
    }
}
