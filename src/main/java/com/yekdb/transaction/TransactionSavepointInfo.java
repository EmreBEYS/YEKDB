package com.yekdb.transaction;

import java.util.Objects;

/**
 * Aktif transaction icindeki named savepoint gorunum bilgisidir.
 */
public record TransactionSavepointInfo(
        int ordinal,
        String name,
        int undoActionCount
) {

    public TransactionSavepointInfo {
        if (ordinal < 1) {
            throw new IllegalArgumentException(
                    "Savepoint ordinal must be positive."
            );
        }

        Objects.requireNonNull(
                name,
                "Savepoint name cannot be null."
        );

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Savepoint name cannot be blank."
            );
        }

        if (undoActionCount < 0) {
            throw new IllegalArgumentException(
                    "Undo action count cannot be negative."
            );
        }
    }
}
