package com.yekdb.query.statement;

import com.yekdb.transaction.TransactionAccessMode;
import com.yekdb.transaction.TransactionIsolationLevel;

import java.util.Objects;

/**
 * BEGIN transaction statement modelidir.
 */
public final class BeginTransactionStatement implements Statement {

    private final TransactionAccessMode accessMode;
    private final TransactionIsolationLevel isolationLevel;

    public BeginTransactionStatement() {

        this(
                TransactionAccessMode.READ_WRITE,
                TransactionIsolationLevel.READ_COMMITTED
        );
    }

    public BeginTransactionStatement(
            TransactionAccessMode accessMode
    ) {

        this(
                accessMode,
                TransactionIsolationLevel.READ_COMMITTED
        );
    }

    public BeginTransactionStatement(
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

    @Override
    public StatementType getType() {
        return StatementType.BEGIN_TRANSACTION;
    }
}
