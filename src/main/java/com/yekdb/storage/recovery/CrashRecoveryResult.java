package com.yekdb.storage.recovery;

import java.util.Set;

/**
 * Analysis, redo ve undo asamalarinin degistirilemez ozetidir.
 */
public record CrashRecoveryResult(
        int analyzedRecords,
        Set<Long> committedTransactions,
        Set<Long> incompleteTransactions,
        int redonePageUpdates,
        int undonePageUpdates,
        int idempotentPageSkips,
        boolean tornTailRepaired
) {

    public CrashRecoveryResult {
        committedTransactions = Set.copyOf(committedTransactions);
        incompleteTransactions = Set.copyOf(incompleteTransactions);
    }

    public static CrashRecoveryResult empty() {
        return new CrashRecoveryResult(
                0,
                Set.of(),
                Set.of(),
                0,
                0,
                0,
                false
        );
    }

    public boolean changedDataFile() {
        return redonePageUpdates > 0
                || undonePageUpdates > 0;
    }
}
