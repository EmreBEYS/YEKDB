package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorDeadlockTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldRollbackDeadlockVictimAndReleaseItsLocks()
            throws Exception {

        try (QueryExecutor first = newExecutor();
             QueryExecutor second = newExecutor()) {

            createDatabaseAndTables(first);
            second.execute("USE DATABASE deadlock_db;");

            first.execute("BEGIN;");
            second.execute("BEGIN;");

            first.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Ada');"
            );

            second.execute(
                    "INSERT INTO orders (id, name) VALUES (1, 'Book');"
            );

            CompletableFuture<ExecuteResult> waitingInsert =
                    new CompletableFuture<>();

            Thread waitingThread =
                    new Thread(
                            () -> {
                                try {
                                    waitingInsert.complete(
                                            first.execute(
                                                    "INSERT INTO orders (id, name) "
                                                            + "VALUES (2, 'Pen');"
                                            )
                                    );
                                } catch (RuntimeException exception) {
                                    waitingInsert.completeExceptionally(
                                            exception
                                    );
                                }
                            }
                    );

            waitingThread.start();
            waitUntilThreadIsWaiting(waitingThread);

            QueryExecutionException deadlockException =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> second.execute(
                                    "INSERT INTO users (id, name) "
                                            + "VALUES (2, 'Ali');"
                            )
                    );

            assertEquals(
                    "Deadlock detected while waiting for table write lock: users",
                    deadlockException.getMessage()
            );

            QueryExecutionException commitException =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> second.execute("COMMIT;")
                    );

            assertEquals(
                    "No active transaction exists.",
                    commitException.getMessage()
            );

            assertTrue(
                    waitingInsert.get(
                            2,
                            TimeUnit.SECONDS
                    ).isSuccess()
            );

            waitingThread.join(
                    TimeUnit.SECONDS.toMillis(2)
            );

            first.execute("COMMIT;");

            assertEquals(
                    1,
                    second.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );

            assertEquals(
                    1,
                    second.execute(
                            "SELECT * FROM orders;"
                    ).getRowCount()
            );
        }
    }

    private QueryExecutor newExecutor() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        return new QueryExecutor(
                databaseManager,
                new StorageQueryDataSource(
                        databaseManager
                ),
                Duration.ofSeconds(5)
        );
    }

    private void createDatabaseAndTables(
            QueryExecutor executor
    ) {

        executor.execute(
                "CREATE DATABASE deadlock_db;"
        );

        executor.execute(
                "USE DATABASE deadlock_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );

        executor.execute(
                "CREATE TABLE orders (id INT, name STRING);"
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
