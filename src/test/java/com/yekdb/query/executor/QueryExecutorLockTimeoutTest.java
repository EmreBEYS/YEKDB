package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryExecutorLockTimeoutTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldReportConfiguredWriteLockTimeout() {

        try (QueryExecutor lockOwner = newExecutor(Duration.ZERO);
             QueryExecutor waitingWriter = newExecutor(Duration.ofMillis(25))) {

            createDatabaseAndTable(lockOwner);
            useDatabase(waitingWriter);

            lockOwner.execute("BEGIN;");
            lockOwner.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Ada');"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> waitingWriter.execute(
                                    "INSERT INTO users (id, name) VALUES (2, 'Ali');"
                            )
                    );

            assertEquals(
                    "Timed out waiting for table write lock after 25 ms: users",
                    exception.getMessage()
            );

            lockOwner.execute("ROLLBACK;");
        }
    }

    @Test
    void shouldReportConfiguredReadLockTimeout() {

        try (QueryExecutor lockOwner = newExecutor(Duration.ZERO);
             QueryExecutor waitingReader = newExecutor(Duration.ofMillis(25))) {

            createDatabaseAndTable(lockOwner);
            useDatabase(waitingReader);

            lockOwner.execute("BEGIN;");
            lockOwner.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Ada');"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> waitingReader.execute(
                                    "SELECT * FROM users;"
                            )
                    );

            assertEquals(
                    "Timed out waiting for table read lock after 25 ms: users",
                    exception.getMessage()
            );

            lockOwner.execute("ROLLBACK;");
        }
    }

    private QueryExecutor newExecutor(
            Duration lockTimeout
    ) {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        return new QueryExecutor(
                databaseManager,
                new StorageQueryDataSource(
                        databaseManager
                ),
                lockTimeout
        );
    }

    private void createDatabaseAndTable(
            QueryExecutor executor
    ) {

        executor.execute(
                "CREATE DATABASE timeout_db;"
        );

        useDatabase(executor);

        executor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );
    }

    private void useDatabase(
            QueryExecutor executor
    ) {

        executor.execute(
                "USE DATABASE timeout_db;"
        );
    }
}
