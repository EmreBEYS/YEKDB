package com.yekdb.transaction;

import java.time.Instant;
import java.util.Objects;

/**
 * Aktif transaction oturum bilgisini tasir.
 *
 * Sprint 00-32 Phase 1 kapsaminda transaction context
 * session-scope tutulur. Fiziksel undo/redo ve WAL entegrasyonu
 * sonraki phase'lerde bu model uzerine eklenecektir.
 */
public final class TransactionContext {

    private final long transactionId;
    private final Instant startedAt;
    private final TransactionAccessMode accessMode;
    private final TransactionIsolationLevel isolationLevel;

    private TransactionStatus status;
    private Instant completedAt;

    public TransactionContext(
            long transactionId,
            Instant startedAt
    ) {

        this(
                transactionId,
                startedAt,
                TransactionAccessMode.READ_WRITE,
                TransactionIsolationLevel.READ_COMMITTED
        );
    }

    public TransactionContext(
            long transactionId,
            Instant startedAt,
            TransactionAccessMode accessMode
    ) {

        this(
                transactionId,
                startedAt,
                accessMode,
                TransactionIsolationLevel.READ_COMMITTED
        );
    }

    public TransactionContext(
            long transactionId,
            Instant startedAt,
            TransactionAccessMode accessMode,
            TransactionIsolationLevel isolationLevel
    ) {

        if (transactionId <= 0) {
            throw new IllegalArgumentException(
                    "Transaction id must be positive."
            );
        }

        this.transactionId =
                transactionId;

        this.startedAt =
                Objects.requireNonNull(
                        startedAt,
                        "StartedAt cannot be null."
                );

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

        this.status =
                TransactionStatus.ACTIVE;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public TransactionAccessMode getAccessMode() {
        return accessMode;
    }

    public TransactionIsolationLevel getIsolationLevel() {
        return isolationLevel;
    }

    public boolean isReadOnly() {
        return accessMode == TransactionAccessMode.READ_ONLY;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean isActive() {
        return status == TransactionStatus.ACTIVE;
    }

    void markCommitted(
            Instant completedAt
    ) {

        complete(
                TransactionStatus.COMMITTED,
                completedAt
        );
    }

    void markRolledBack(
            Instant completedAt
    ) {

        complete(
                TransactionStatus.ROLLED_BACK,
                completedAt
        );
    }

    private void complete(
            TransactionStatus finalStatus,
            Instant completedAt
    ) {

        if (!isActive()) {
            throw new IllegalStateException(
                    "Transaction is already completed."
            );
        }

        this.status =
                Objects.requireNonNull(
                        finalStatus,
                        "FinalStatus cannot be null."
                );

        this.completedAt =
                Objects.requireNonNull(
                        completedAt,
                        "CompletedAt cannot be null."
                );
    }
}
