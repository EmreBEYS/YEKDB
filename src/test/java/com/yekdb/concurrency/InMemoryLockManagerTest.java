package com.yekdb.concurrency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLockManagerTest {

    @TempDir
    Path databaseDirectory;

    @Test
    void shouldAllowSharedLocksForDifferentOwners() {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        try (LockHandle first = lockManager.acquire(
                resource,
                LockMode.SHARED,
                "session-a"
        ); LockHandle second = lockManager.acquire(
                resource,
                LockMode.SHARED,
                "session-b"
        )) {

            assertEquals(
                    1,
                    lockManager.getActiveResourceCount()
            );

            assertFalse(first.isReleased());
            assertFalse(second.isReleased());
        }

        assertEquals(
                0,
                lockManager.getActiveResourceCount()
        );
    }

    @Test
    void shouldRejectExclusiveLockWhenAnotherOwnerHoldsSharedLock() {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        try (LockHandle ignored = lockManager.acquire(
                resource,
                LockMode.SHARED,
                "session-a"
        )) {

            LockConflictException exception =
                    assertThrows(
                            LockConflictException.class,
                            () -> lockManager.acquire(
                                    resource,
                                    LockMode.EXCLUSIVE,
                                    "session-b"
                            )
                    );

            assertEquals(
                    resource,
                    exception.getResource()
            );

            assertEquals(
                    LockMode.EXCLUSIVE,
                    exception.getRequestedMode()
            );
        }
    }

    @Test
    void shouldRejectSharedLockWhenAnotherOwnerHoldsExclusiveLock() {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        try (LockHandle ignored = lockManager.acquire(
                resource,
                LockMode.EXCLUSIVE,
                "session-a"
        )) {

            assertThrows(
                    LockConflictException.class,
                    () -> lockManager.acquire(
                            resource,
                            LockMode.SHARED,
                            "session-b"
                    )
            );
        }
    }

    @Test
    void shouldAllowReentrantLockAndSoleOwnerUpgrade() {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        LockHandle shared =
                lockManager.acquire(
                        resource,
                        LockMode.SHARED,
                        "session-a"
                );

        LockHandle exclusive =
                lockManager.acquire(
                        resource,
                        LockMode.EXCLUSIVE,
                        "session-a"
                );

        assertEquals(
                2,
                lockManager.getHoldCount(
                        resource,
                        "session-a"
                )
        );

        exclusive.close();

        assertEquals(
                1,
                lockManager.getHoldCount(
                        resource,
                        "session-a"
                )
        );

        shared.close();
    }

    @Test
    void shouldReleaseHandleOnlyOnce() {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        LockHandle handle =
                lockManager.acquire(
                        resource,
                        LockMode.EXCLUSIVE,
                        "session-a"
                );

        handle.close();
        handle.close();

        assertTrue(handle.isReleased());

        assertEquals(
                0,
                lockManager.getActiveResourceCount()
        );
    }

    @Test
    void shouldNormalizeTableResourceIdentity() {

        LockResource first =
                LockResource.table(
                        databaseDirectory.resolve("db"),
                        " Users "
                );

        LockResource second =
                LockResource.table(
                        databaseDirectory.resolve("db").resolve("."),
                        "users"
                );

        assertEquals(
                first,
                second
        );
    }

    @Test
    void shouldAcquireWaitingLockAfterConflictingOwnerReleases()
            throws Exception {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        LockHandle first =
                lockManager.acquire(
                        resource,
                        LockMode.EXCLUSIVE,
                        "session-a"
                );

        CompletableFuture<LockHandle> waitingLock =
                new CompletableFuture<>();

        Thread waitingThread =
                new Thread(
                        () -> {
                            try {
                                waitingLock.complete(
                                        lockManager.acquire(
                                                resource,
                                                LockMode.EXCLUSIVE,
                                                "session-b",
                                                Duration.ofSeconds(2)
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

        assertEquals(
                Thread.State.TIMED_WAITING,
                waitingThread.getState()
        );

        first.close();

        try (LockHandle acquired = waitingLock.get(
                2,
                TimeUnit.SECONDS
        )) {

            assertEquals(
                    "session-b",
                    acquired.getOwnerId()
            );
        }

        waitingThread.join(
                TimeUnit.SECONDS.toMillis(2)
        );
    }

    @Test
    void shouldThrowTimeoutWhenConflictIsNotReleased() {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        try (LockHandle ignored = lockManager.acquire(
                resource,
                LockMode.EXCLUSIVE,
                "session-a"
        )) {

            LockTimeoutException exception =
                    assertThrows(
                            LockTimeoutException.class,
                            () -> lockManager.acquire(
                                    resource,
                                    LockMode.SHARED,
                                    "session-b",
                                    Duration.ofMillis(25)
                            )
                    );

            assertEquals(
                    Duration.ofMillis(25),
                    exception.getTimeout()
            );

            assertEquals(
                    resource,
                    exception.getResource()
            );
        }
    }

    @Test
    void shouldPreserveInterruptStatusWhenWaitingThreadIsInterrupted()
            throws Exception {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        try (LockHandle ignored = lockManager.acquire(
                resource,
                LockMode.EXCLUSIVE,
                "session-a"
        )) {

            CompletableFuture<Boolean> interrupted =
                    new CompletableFuture<>();

            Thread waitingThread =
                    new Thread(
                            () -> {
                                try {
                                    lockManager.acquire(
                                            resource,
                                            LockMode.SHARED,
                                            "session-b",
                                            Duration.ofSeconds(5)
                                    );
                                    interrupted.complete(false);
                                } catch (LockAcquisitionInterruptedException exception) {
                                    interrupted.complete(
                                            Thread.currentThread()
                                                    .isInterrupted()
                                    );
                                }
                            }
                    );

            waitingThread.start();

            waitUntilThreadIsWaiting(waitingThread);
            waitingThread.interrupt();

            assertTrue(
                    interrupted.get(
                            2,
                            TimeUnit.SECONDS
                    )
            );

            waitingThread.join(
                    TimeUnit.SECONDS.toMillis(2)
            );
        }
    }

    @Test
    void shouldDetectDeadlockAndCleanWaitingDependencies()
            throws Exception {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource users =
                tableResource("users");

        LockResource orders =
                tableResource("orders");

        LockHandle usersByFirstOwner =
                lockManager.acquire(
                        users,
                        LockMode.EXCLUSIVE,
                        "session-a"
                );

        LockHandle ordersBySecondOwner =
                lockManager.acquire(
                        orders,
                        LockMode.EXCLUSIVE,
                        "session-b"
                );

        CompletableFuture<LockHandle> firstOwnerWaiting =
                new CompletableFuture<>();

        Thread waitingThread =
                new Thread(
                        () -> {
                            try {
                                firstOwnerWaiting.complete(
                                        lockManager.acquire(
                                                orders,
                                                LockMode.EXCLUSIVE,
                                                "session-a",
                                                Duration.ofSeconds(5)
                                        )
                                );
                            } catch (RuntimeException exception) {
                                firstOwnerWaiting.completeExceptionally(
                                        exception
                                );
                            }
                        }
                );

        waitingThread.start();
        waitUntilThreadIsWaiting(waitingThread);

        DeadlockDetectedException exception =
                assertThrows(
                        DeadlockDetectedException.class,
                        () -> lockManager.acquire(
                                users,
                                LockMode.EXCLUSIVE,
                                "session-b",
                                Duration.ofSeconds(5)
                        )
                );

        assertEquals(
                "session-b",
                exception.getOwnerId()
        );

        assertEquals(
                "session-b",
                exception.getCycleOwners().getFirst()
        );

        assertEquals(
                "session-b",
                exception.getCycleOwners().getLast()
        );

        ordersBySecondOwner.close();

        try (LockHandle ignored = firstOwnerWaiting.get(
                2,
                TimeUnit.SECONDS
        )) {
            assertEquals(
                    0,
                    lockManager.getWaitingOwnerCount()
            );
        } finally {
            usersByFirstOwner.close();
        }

        waitingThread.join(
                TimeUnit.SECONDS.toMillis(2)
        );
    }

    @Test
    void shouldPreventLateReaderFromBypassingWaitingWriter()
            throws Exception {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        LockHandle firstReader =
                lockManager.acquire(
                        resource,
                        LockMode.SHARED,
                        "session-a"
                );

        CompletableFuture<LockHandle> waitingWriter =
                acquireInBackground(
                        lockManager,
                        resource,
                        LockMode.EXCLUSIVE,
                        "session-b"
                );

        waitUntilOwnerCount(
                lockManager,
                1
        );

        assertThrows(
                LockConflictException.class,
                () -> lockManager.acquire(
                        resource,
                        LockMode.SHARED,
                        "session-c"
                )
        );

        firstReader.close();

        try (LockHandle writer = waitingWriter.get(
                2,
                TimeUnit.SECONDS
        )) {
            assertEquals(
                    "session-b",
                    writer.getOwnerId()
            );
        }

        try (LockHandle reader = lockManager.acquire(
                resource,
                LockMode.SHARED,
                "session-c"
        )) {
            assertEquals(
                    "session-c",
                    reader.getOwnerId()
            );
        }
    }

    @Test
    void shouldGrantExclusiveWaitersInArrivalOrder()
            throws Exception {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("orders");

        LockHandle currentOwner =
                lockManager.acquire(
                        resource,
                        LockMode.EXCLUSIVE,
                        "session-a"
                );

        CompletableFuture<LockHandle> firstWaiter =
                acquireInBackground(
                        lockManager,
                        resource,
                        LockMode.EXCLUSIVE,
                        "session-b"
                );

        waitUntilOwnerCount(
                lockManager,
                1
        );

        CompletableFuture<LockHandle> secondWaiter =
                acquireInBackground(
                        lockManager,
                        resource,
                        LockMode.EXCLUSIVE,
                        "session-c"
                );

        waitUntilOwnerCount(
                lockManager,
                2
        );

        currentOwner.close();

        LockHandle firstAcquired =
                firstWaiter.get(
                        2,
                        TimeUnit.SECONDS
                );

        assertEquals(
                "session-b",
                firstAcquired.getOwnerId()
        );

        assertFalse(secondWaiter.isDone());

        firstAcquired.close();

        try (LockHandle secondAcquired = secondWaiter.get(
                2,
                TimeUnit.SECONDS
        )) {
            assertEquals(
                    "session-c",
                    secondAcquired.getOwnerId()
            );
        }
    }

    @Test
    void shouldUpgradeSoleSharedLockAndClosePreviousHandle() {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        LockHandle shared =
                lockManager.acquire(
                        resource,
                        LockMode.SHARED,
                        "session-a"
                );

        try (LockHandle exclusive = lockManager.convert(
                shared,
                LockMode.EXCLUSIVE
        )) {

            assertTrue(shared.isReleased());

            assertEquals(
                    LockMode.EXCLUSIVE,
                    exclusive.getMode()
            );

            assertEquals(
                    1,
                    lockManager.getHoldCount(
                            resource,
                            "session-a"
                    )
            );

            assertThrows(
                    LockConflictException.class,
                    () -> lockManager.acquire(
                            resource,
                            LockMode.SHARED,
                            "session-b"
                    )
            );
        }
    }

    @Test
    void shouldKeepSharedLockWhenImmediateUpgradeFails() {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        try (LockHandle first = lockManager.acquire(
                resource,
                LockMode.SHARED,
                "session-a"
        ); LockHandle second = lockManager.acquire(
                resource,
                LockMode.SHARED,
                "session-b"
        )) {

            assertThrows(
                    LockConflictException.class,
                    () -> lockManager.convert(
                            first,
                            LockMode.EXCLUSIVE
                    )
            );

            assertFalse(first.isReleased());

            assertEquals(
                    1,
                    lockManager.getHoldCount(
                            resource,
                            "session-a"
                    )
            );
        }
    }

    @Test
    void shouldDowngradeExclusiveLockAndAllowConcurrentReader() {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("orders");

        LockHandle exclusive =
                lockManager.acquire(
                        resource,
                        LockMode.EXCLUSIVE,
                        "session-a"
                );

        try (LockHandle downgraded = lockManager.convert(
                exclusive,
                LockMode.SHARED
        ); LockHandle otherReader = lockManager.acquire(
                resource,
                LockMode.SHARED,
                "session-b"
        )) {

            assertTrue(exclusive.isReleased());

            assertEquals(
                    LockMode.SHARED,
                    downgraded.getMode()
            );

            assertEquals(
                    LockMode.SHARED,
                    otherReader.getMode()
            );
        }
    }

    @Test
    void shouldDetectConcurrentUpgradeDeadlockAndPreserveVictimLock()
            throws Exception {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("accounts");

        LockHandle firstShared =
                lockManager.acquire(
                        resource,
                        LockMode.SHARED,
                        "session-a"
                );

        LockHandle secondShared =
                lockManager.acquire(
                        resource,
                        LockMode.SHARED,
                        "session-b"
                );

        CompletableFuture<LockHandle> firstUpgrade =
                new CompletableFuture<>();

        Thread waitingThread =
                new Thread(
                        () -> {
                            try {
                                firstUpgrade.complete(
                                        lockManager.convert(
                                                firstShared,
                                                LockMode.EXCLUSIVE,
                                                Duration.ofSeconds(5)
                                        )
                                );
                            } catch (RuntimeException exception) {
                                firstUpgrade.completeExceptionally(
                                        exception
                                );
                            }
                        }
                );

        waitingThread.start();

        waitUntilOwnerCount(
                lockManager,
                1
        );

        DeadlockDetectedException exception =
                assertThrows(
                        DeadlockDetectedException.class,
                        () -> lockManager.convert(
                                secondShared,
                                LockMode.EXCLUSIVE,
                                Duration.ofSeconds(5)
                        )
                );

        assertEquals(
                "session-b",
                exception.getOwnerId()
        );

        assertFalse(secondShared.isReleased());
        secondShared.close();

        try (LockHandle upgraded = firstUpgrade.get(
                2,
                TimeUnit.SECONDS
        )) {
            assertEquals(
                    LockMode.EXCLUSIVE,
                    upgraded.getMode()
            );

            assertTrue(firstShared.isReleased());
        }

        waitingThread.join(
                TimeUnit.SECONDS.toMillis(2)
        );
    }

    @Test
    void shouldExposeDetachedImmutableOwnerSnapshot() {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("users");

        LockHandle first =
                lockManager.acquire(
                        resource,
                        LockMode.SHARED,
                        "session-a"
                );

        LockHandle second =
                lockManager.acquire(
                        resource,
                        LockMode.SHARED,
                        "session-a"
                );

        LockManagerSnapshot snapshot =
                lockManager.snapshot();

        first.close();
        second.close();

        assertEquals(
                1,
                snapshot.getActiveResourceCount()
        );

        LockManagerSnapshot.OwnerState owner =
                snapshot.resources()
                        .getFirst()
                        .owners()
                        .getFirst();

        assertEquals("session-a", owner.ownerId());
        assertEquals(2, owner.sharedHoldCount());
        assertEquals(0, owner.exclusiveHoldCount());
        assertEquals(2, owner.getHoldCount());
        assertEquals(
                LockMode.SHARED,
                owner.getEffectiveMode()
        );

        assertEquals(
                0,
                lockManager.snapshot()
                        .getActiveResourceCount()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.resources()
                        .clear()
        );
    }

    @Test
    void shouldExposeWaitingQueueAndWaitForDependencies()
            throws Exception {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("orders");

        LockHandle owner =
                lockManager.acquire(
                        resource,
                        LockMode.EXCLUSIVE,
                        "session-a"
                );

        CompletableFuture<LockHandle> waitingLock =
                acquireInBackground(
                        lockManager,
                        resource,
                        LockMode.SHARED,
                        "session-b"
                );

        waitUntilOwnerCount(
                lockManager,
                1
        );

        LockManagerSnapshot snapshot =
                lockManager.snapshot();

        assertEquals(1, snapshot.getWaitingRequestCount());
        assertEquals(1, snapshot.getWaitingOwnerCount());

        LockManagerSnapshot.WaitingState waiter =
                snapshot.resources()
                        .getFirst()
                        .waitingRequests()
                        .getFirst();

        assertEquals(1, waiter.queuePosition());
        assertEquals("session-b", waiter.ownerId());
        assertEquals(
                LockMode.SHARED,
                waiter.requestedMode()
        );

        assertEquals(
                List.of("session-a"),
                snapshot.waitForDependencies()
                        .get("session-b")
        );

        owner.close();

        try (LockHandle ignored = waitingLock.get(
                2,
                TimeUnit.SECONDS
        )) {
        }
    }

    private CompletableFuture<LockHandle> acquireInBackground(
            InMemoryLockManager lockManager,
            LockResource resource,
            LockMode mode,
            String ownerId
    ) {

        CompletableFuture<LockHandle> result =
                new CompletableFuture<>();

        Thread thread =
                new Thread(
                        () -> {
                            try {
                                result.complete(
                                        lockManager.acquire(
                                                resource,
                                                mode,
                                                ownerId,
                                                Duration.ofSeconds(5)
                                        )
                                );
                            } catch (RuntimeException exception) {
                                result.completeExceptionally(
                                        exception
                                );
                            }
                        }
                );

        thread.start();
        return result;
    }

    private void waitUntilOwnerCount(
            InMemoryLockManager lockManager,
            int expectedCount
    ) {

        long deadline =
                System.nanoTime()
                        + TimeUnit.SECONDS.toNanos(1);

        while (lockManager.getWaitingOwnerCount()
                != expectedCount
                && System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }

        assertEquals(
                expectedCount,
                lockManager.getWaitingOwnerCount()
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
    }

    private LockResource tableResource(
            String tableName
    ) {

        return LockResource.table(
                databaseDirectory.resolve("concurrency_db"),
                tableName
        );
    }
}
