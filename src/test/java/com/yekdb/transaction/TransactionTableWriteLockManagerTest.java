package com.yekdb.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionTableWriteLockManagerTest {

    @TempDir
    Path databaseDirectory;

    @Test
    void shouldPreserveConcurrentWriteConflictContract() {

        TransactionTableWriteLockManager firstManager =
                new TransactionTableWriteLockManager();

        TransactionTableWriteLockManager secondManager =
                new TransactionTableWriteLockManager();

        try (TransactionTableWriteLock ignored = firstManager.acquire(
                databaseDirectory,
                "users",
                "session-a"
        )) {

            TransactionLockException exception =
                    assertThrows(
                            TransactionLockException.class,
                            () -> secondManager.acquire(
                                    databaseDirectory,
                                    "users",
                                    "session-b"
                            )
                    );

            assertEquals(
                    "Table is locked by another active transaction: users",
                    exception.getMessage()
            );
        }
    }

    @Test
    void shouldReleaseDelegatedLockWhenHandleCloses() {

        TransactionTableWriteLockManager firstManager =
                new TransactionTableWriteLockManager();

        TransactionTableWriteLockManager secondManager =
                new TransactionTableWriteLockManager();

        TransactionTableWriteLock firstLock =
                firstManager.acquire(
                        databaseDirectory,
                        "users",
                        "session-a"
                );

        firstLock.close();

        assertDoesNotThrow(
                () -> {
                    try (TransactionTableWriteLock ignored =
                                 secondManager.acquire(
                                         databaseDirectory,
                                         "users",
                                         "session-b"
                                 )) {
                    }
                }
        );
    }
}
