package com.yekdb.transaction;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Tamamlanmadan kalmis transaction log kayitlarini guvenli rollback
 * karariyla kapatir.
 */
public final class TransactionRecoveryManager {

    private final TransactionDurabilityLog durabilityLog;

    public TransactionRecoveryManager() {
        this(
                new TransactionDurabilityLog()
        );
    }

    TransactionRecoveryManager(
            TransactionDurabilityLog durabilityLog
    ) {

        this.durabilityLog =
                Objects.requireNonNull(
                        durabilityLog,
                        "DurabilityLog cannot be null."
                );
    }

    public TransactionRecoveryResult recover(
            Path databasePath
    ) {

        Objects.requireNonNull(
                databasePath,
                "DatabasePath cannot be null."
        );

        List<TransactionLogEntry> incompleteTransactions =
                durabilityLog.readIncompleteTransactions(
                        databasePath
                );

        for (TransactionLogEntry transaction : incompleteTransactions) {
            durabilityLog.appendRecoveredRollback(
                    databasePath,
                    transaction,
                    Instant.now()
            );
        }

        return new TransactionRecoveryResult(
                databasePath,
                incompleteTransactions
        );
    }
}
