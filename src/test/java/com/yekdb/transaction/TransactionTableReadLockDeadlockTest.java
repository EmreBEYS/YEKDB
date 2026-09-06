package com.yekdb.transaction;

import com.yekdb.concurrency.LockMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTableReadLockDeadlockTest {

    @TempDir
    Path databaseDirectory;

    @Test
    void shouldTranslateReadLockDeadlockToTransactionException()
            throws Exception {

        TransactionTableWriteLockManager writeLockManager =
                new TransactionTableWriteLockManager();

        TransactionTableReadLockManager readLockManager =
                new TransactionTableReadLockManager();

        TransactionTableWriteLock usersByFirstOwner =
                writeLockManager.acquire(
                        databaseDirectory,
                        "users",
                        "session-a"
                );

        TransactionTableWriteLock ordersBySecondOwner =
                writeLockManager.acquire(
                        databaseDirectory,
                        "orders",
                        "session-b"
                );

        CompletableFuture<TransactionTableWriteLock> waitingLock =
                new CompletableFuture<>();

        Thread waitingThread =
                new Thread(
                        () -> {
                            try {
                                waitingLock.complete(
                                        writeLockManager.acquire(
                                                databaseDirectory,
                                                "orders",
                                                "session-a",
                                                Duration.ofSeconds(5)
                                        )
                                );
                            } catch (RuntimeException exception) {
                                waitingLock.completeExceptionally(
                                        exception
                                );
                            }
                        }
                );

        waitingThread.start();
        waitUntilThreadIsWaiting(waitingThread);

        TransactionDeadlockException exception =
                assertThrows(
                        TransactionDeadlockException.class,
                        () -> readLockManager.acquire(
                                databaseDirectory,
                                "users",
                                "session-b",
                                Duration.ofSeconds(5)
                        )
                );

        assertEquals(
                LockMode.SHARED,
                exception.getLockMode()
        );

        assertEquals(
                "users",
                exception.getTableName()
        );

        assertEquals(
                "session-b",
                exception.getOwnerId()
        );

        assertTrue(
                exception.getCycleOwners()
                        .contains("session-a")
        );

        ordersBySecondOwner.close();

        try (TransactionTableWriteLock ignored = waitingLock.get(
                2,
                TimeUnit.SECONDS
        )) {
        } finally {
            usersByFirstOwner.close();
        }

        waitingThread.join(
                TimeUnit.SECONDS.toMillis(2)
        );
    }

    private void waitUntilThreadIsWaiting(
            Thread thread
    ) {

        long deadline =
                System.nanoTime()
                        + TimeUnit.SECONDS.toNanos(1);

        while (thread.getState() != Thread.State.TIMED_WAITING
                && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }

        assertEquals(
                Thread.State.TIMED_WAITING,
                thread.getState()
        );
    }
}
