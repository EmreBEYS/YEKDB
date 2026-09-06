package com.yekdb.concurrency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLockManagerStressTest {

    @TempDir
    Path databaseDirectory;

    @Test
    @Timeout(20)
    void shouldSerializeConcurrentWritersWithoutLeakingLocks()
            throws Exception {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("accounts");

        int workerCount = 8;
        int iterationsPerWorker = 40;

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        workerCount
                );

        CountDownLatch start =
                new CountDownLatch(1);

        AtomicInteger insideCriticalSection =
                new AtomicInteger();

        AtomicInteger maximumConcurrentWriters =
                new AtomicInteger();

        AtomicInteger completedOperations =
                new AtomicInteger();

        List<Future<?>> futures =
                new ArrayList<>();

        try {

            for (int worker = 0;
                 worker < workerCount;
                 worker++) {

                String ownerId =
                        "writer-" + worker;

                futures.add(
                        executor.submit(() -> {

                            start.await();

                            for (int iteration = 0;
                                 iteration < iterationsPerWorker;
                                 iteration++) {

                                try (LockHandle ignored = lockManager.acquire(
                                        resource,
                                        LockMode.EXCLUSIVE,
                                        ownerId,
                                        Duration.ofSeconds(10)
                                )) {

                                    int concurrentWriters =
                                            insideCriticalSection
                                                    .incrementAndGet();

                                    maximumConcurrentWriters
                                            .accumulateAndGet(
                                                    concurrentWriters,
                                                    Math::max
                                            );

                                    try {
                                        completedOperations
                                                .incrementAndGet();
                                        Thread.yield();
                                    } finally {
                                        insideCriticalSection
                                                .decrementAndGet();
                                    }
                                }
                            }

                            return null;
                        })
                );
            }

            start.countDown();

            for (Future<?> future : futures) {
                future.get(
                        15,
                        TimeUnit.SECONDS
                );
            }

        } finally {
            executor.shutdownNow();
            assertTrue(
                    executor.awaitTermination(
                            5,
                            TimeUnit.SECONDS
                    )
            );
        }

        assertEquals(
                workerCount * iterationsPerWorker,
                completedOperations.get()
        );

        assertEquals(
                1,
                maximumConcurrentWriters.get()
        );

        assertEmptySnapshot(lockManager);
    }

    @Test
    @Timeout(20)
    void shouldAllowManyReadersAtTheSameTimeAndCleanAllHolds()
            throws Exception {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("catalog");

        int readerCount = 16;

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        readerCount
                );

        CountDownLatch start =
                new CountDownLatch(1);

        CountDownLatch allReadersAcquired =
                new CountDownLatch(readerCount);

        CountDownLatch releaseReaders =
                new CountDownLatch(1);

        List<Future<?>> futures =
                new ArrayList<>();

        try {

            for (int reader = 0;
                 reader < readerCount;
                 reader++) {

                String ownerId =
                        "reader-" + reader;

                futures.add(
                        executor.submit(() -> {

                            start.await();

                            try (LockHandle ignored = lockManager.acquire(
                                    resource,
                                    LockMode.SHARED,
                                    ownerId,
                                    Duration.ofSeconds(10)
                            )) {
                                allReadersAcquired.countDown();
                                releaseReaders.await();
                            }

                            return null;
                        })
                );
            }

            start.countDown();

            assertTrue(
                    allReadersAcquired.await(
                            10,
                            TimeUnit.SECONDS
                    )
            );

            LockManagerSnapshot activeSnapshot =
                    lockManager.snapshot();

            assertEquals(
                    readerCount,
                    activeSnapshot.resources()
                            .getFirst()
                            .owners()
                            .size()
            );

            releaseReaders.countDown();

            for (Future<?> future : futures) {
                future.get(
                        10,
                        TimeUnit.SECONDS
                );
            }

        } finally {
            releaseReaders.countDown();
            executor.shutdownNow();
            assertTrue(
                    executor.awaitTermination(
                            5,
                            TimeUnit.SECONDS
                    )
            );
        }

        assertEmptySnapshot(lockManager);
    }

    @Test
    @Timeout(20)
    void shouldCleanWaitingQueueAfterConcurrentTimeouts()
            throws Exception {

        InMemoryLockManager lockManager =
                new InMemoryLockManager();

        LockResource resource =
                tableResource("inventory");

        int waiterCount = 12;

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        waiterCount
                );

        CountDownLatch start =
                new CountDownLatch(1);

        AtomicInteger timedOut =
                new AtomicInteger();

        List<Future<?>> futures =
                new ArrayList<>();

        try (LockHandle ignored = lockManager.acquire(
                resource,
                LockMode.EXCLUSIVE,
                "blocking-owner"
        )) {

            try {

                for (int waiter = 0;
                     waiter < waiterCount;
                     waiter++) {

                    String ownerId =
                            "waiter-" + waiter;

                    futures.add(
                            executor.submit(() -> {

                                start.await();

                                try {
                                    lockManager.acquire(
                                            resource,
                                            LockMode.SHARED,
                                            ownerId,
                                            Duration.ofMillis(50)
                                    );
                                } catch (LockTimeoutException exception) {
                                    timedOut.incrementAndGet();
                                }

                                return null;
                            })
                    );
                }

                start.countDown();

                for (Future<?> future : futures) {
                    future.get(
                            5,
                            TimeUnit.SECONDS
                    );
                }

            } finally {
                executor.shutdownNow();
                assertTrue(
                        executor.awaitTermination(
                                5,
                                TimeUnit.SECONDS
                        )
                );
            }

            assertEquals(
                    waiterCount,
                    timedOut.get()
            );

            LockManagerSnapshot heldSnapshot =
                    lockManager.snapshot();

            assertEquals(
                    0,
                    heldSnapshot.getWaitingRequestCount()
            );

            assertEquals(
                    0,
                    heldSnapshot.getWaitingOwnerCount()
            );
        }

        assertEmptySnapshot(lockManager);
    }

    private void assertEmptySnapshot(
            InMemoryLockManager lockManager
    ) {

        LockManagerSnapshot snapshot =
                lockManager.snapshot();

        assertEquals(0, snapshot.getActiveResourceCount());
        assertEquals(0, snapshot.getWaitingRequestCount());
        assertEquals(0, snapshot.getWaitingOwnerCount());
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
