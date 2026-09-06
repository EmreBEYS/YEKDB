package com.yekdb.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionTableReadLockManagerTest {

    @TempDir
    Path databaseDirectory;

    @Test
    void shouldAllowSharedReadLocksForDifferentOwners() {

        TransactionTableReadLockManager firstManager =
                new TransactionTableReadLockManager();

        TransactionTableReadLockManager secondManager =
                new TransactionTableReadLockManager();

        try (TransactionTableReadLock firstLock = firstManager.acquire(
                databaseDirectory,
                "users",
                "session-a"
        ); TransactionTableReadLock secondLock = secondManager.acquire(
                databaseDirectory,
                "users",
                "session-b"
        )) {

            assertEquals(
                    firstLock.getLockKey(),
                    secondLock.getLockKey()
            );
        }
    }

    @Test
    void shouldRejectReadLockWhenAnotherOwnerHoldsWriteLock() {

        TransactionTableWriteLockManager writeLockManager =
                new TransactionTableWriteLockManager();

        TransactionTableReadLockManager readLockManager =
                new TransactionTableReadLockManager();

        try (TransactionTableWriteLock ignored =
                     writeLockManager.acquire(
                             databaseDirectory,
                             "users",
                             "session-a"
                     )) {

            TransactionLockException exception =
                    assertThrows(
                            TransactionLockException.class,
                            () -> readLockManager.acquire(
                                    databaseDirectory,
                                    "users",
                                    "session-b"
                            )
                    );

            assertEquals(
                    "Table is write-locked by another active transaction: users",
                    exception.getMessage()
            );
        }
    }

    @Test
    void shouldRejectWriteLockUntilReadLockIsReleased() {

        TransactionTableReadLockManager readLockManager =
                new TransactionTableReadLockManager();

        TransactionTableWriteLockManager writeLockManager =
                new TransactionTableWriteLockManager();

        TransactionTableReadLock readLock =
                readLockManager.acquire(
                        databaseDirectory,
                        "users",
                        "session-a"
                );

        assertThrows(
                TransactionLockException.class,
                () -> writeLockManager.acquire(
                        databaseDirectory,
                        "users",
                        "session-b"
                )
        );

        readLock.close();

        assertDoesNotThrow(
                () -> {
                    try (TransactionTableWriteLock ignored =
                                 writeLockManager.acquire(
                                         databaseDirectory,
                                         "users",
                                         "session-b"
                                 )) {
                    }
                }
        );
    }
}
