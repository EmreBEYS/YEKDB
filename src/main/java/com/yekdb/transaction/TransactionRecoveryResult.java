package com.yekdb.transaction;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Transaction recovery calismasinin ozet sonucunu tasir.
 */
public final class TransactionRecoveryResult {

    private final Path databasePath;
    private final List<TransactionLogEntry> recoveredTransactions;

    public TransactionRecoveryResult(
            Path databasePath,
            List<TransactionLogEntry> recoveredTransactions
    ) {

        this.databasePath =
                Objects.requireNonNull(
                        databasePath,
                        "DatabasePath cannot be null."
                ).normalize();

        this.recoveredTransactions =
                List.copyOf(
                        Objects.requireNonNull(
                                recoveredTransactions,
                                "RecoveredTransactions cannot be null."
                        )
                );
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public List<TransactionLogEntry> getRecoveredTransactions() {
        return recoveredTransactions;
    }

    public int getRecoveredTransactionCount() {
        return recoveredTransactions.size();
    }

    public boolean hasRecoveredTransactions() {
        return !recoveredTransactions.isEmpty();
    }
}
