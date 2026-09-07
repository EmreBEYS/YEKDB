package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryExecutorReadLockIsolationTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldAllowDirtyReadAtReadUncommitted() {

        try (QueryExecutor writer = newExecutor();
             QueryExecutor reader = newExecutor()) {

            createDatabaseAndTable(writer);
            useDatabase(reader);

            writer.execute("BEGIN;");
            writer.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Ada');"
            );

            reader.execute(
                    "START TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;"
            );

            assertEquals(
                    1,
                    reader.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );

            reader.execute("ROLLBACK;");
            writer.execute("ROLLBACK;");
        }
    }

    @Test
    void shouldRejectReadCommittedSelectWhileAnotherTransactionWrites() {

        try (QueryExecutor writer = newExecutor();
             QueryExecutor reader = newExecutor()) {

            createDatabaseAndTable(writer);
            useDatabase(reader);

            writer.execute("BEGIN;");
            writer.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Ada');"
            );

            reader.execute(
                    "START TRANSACTION ISOLATION LEVEL READ COMMITTED;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> reader.execute(
                                    "SELECT * FROM users;"
                            )
                    );

            assertEquals(
                    "Table is write-locked by another active transaction: users",
                    exception.getMessage()
            );

            reader.execute("ROLLBACK;");
            writer.execute("ROLLBACK;");
        }
    }

    @Test
    void shouldReleaseReadCommittedLockAfterStatement() {

        try (QueryExecutor reader = newExecutor();
             QueryExecutor writer = newExecutor()) {

            createDatabaseAndTable(reader);
            useDatabase(writer);

            reader.execute(
                    "START TRANSACTION ISOLATION LEVEL READ COMMITTED;"
            );

            reader.execute(
                    "SELECT * FROM users;"
            );

            writer.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Ada');"
            );

            reader.execute("ROLLBACK;");
        }
    }

    @Test
    void shouldHoldRepeatableReadLockUntilCommit() {

        try (QueryExecutor reader = newExecutor();
             QueryExecutor writer = newExecutor()) {

            createDatabaseAndTable(reader);
            useDatabase(writer);

            reader.execute(
                    "START TRANSACTION ISOLATION LEVEL REPEATABLE READ;"
            );

            reader.execute(
                    "SELECT * FROM users;"
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> writer.execute(
                            "INSERT INTO users (id, name) VALUES (1, 'Ada');"
                    )
            );

            reader.execute("COMMIT;");

            writer.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Ada');"
            );
        }
    }

    @Test
    void shouldHoldSerializableReadLockUntilRollback() {

        try (QueryExecutor reader = newExecutor();
             QueryExecutor writer = newExecutor()) {

            createDatabaseAndTable(reader);
            useDatabase(writer);

            reader.execute(
                    "START TRANSACTION ISOLATION LEVEL SERIALIZABLE;"
            );

            reader.execute(
                    "SELECT * FROM users;"
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> writer.execute(
                            "INSERT INTO users (id, name) VALUES (1, 'Ada');"
                    )
            );

            reader.execute("ROLLBACK;");

            writer.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Ada');"
            );
        }
    }

    @Test
    void shouldAllowConcurrentRepeatableReads() {

        try (QueryExecutor firstReader = newExecutor();
             QueryExecutor secondReader = newExecutor()) {

            createDatabaseAndTable(firstReader);
            useDatabase(secondReader);

            firstReader.execute(
                    "START TRANSACTION ISOLATION LEVEL REPEATABLE READ;"
            );

            secondReader.execute(
                    "START TRANSACTION ISOLATION LEVEL REPEATABLE READ;"
            );

            assertEquals(
                    0,
                    firstReader.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );

            assertEquals(
                    0,
                    secondReader.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );

            firstReader.execute("COMMIT;");
            secondReader.execute("COMMIT;");
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
                )
        );
    }

    private void createDatabaseAndTable(
            QueryExecutor executor
    ) {

        executor.execute(
                "CREATE DATABASE isolation_db;"
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
                "USE DATABASE isolation_db;"
        );
    }
}
