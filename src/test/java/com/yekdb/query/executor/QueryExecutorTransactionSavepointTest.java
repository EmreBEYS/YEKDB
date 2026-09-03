package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryExecutorTransactionSavepointTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldRollbackToNamedSavepointAndKeepTransactionOpen() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndTable(
                    executor
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            executor.execute(
                    "SAVEPOINT after_first_insert;"
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (2, 'Ali');"
            );

            executor.execute(
                    "ROLLBACK TO SAVEPOINT after_first_insert;"
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (3, 'Ayse');"
            );

            executor.execute(
                    "COMMIT;"
            );

            ExecuteResult result =
                    executor.execute(
                            "SELECT * FROM users ORDER BY id ASC;"
                    );

            assertEquals(
                    2,
                    result.getRowCount()
            );

            assertEquals(
                    1,
                    result.getRows()
                            .get(0)
                            .getValue(0)
            );

            assertEquals(
                    3,
                    result.getRows()
                            .get(1)
                            .getValue(0)
            );
        }
    }

    @Test
    void shouldRejectReleasedSavepointThroughExecutor() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndTable(
                    executor
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "SAVEPOINT sp1;"
            );

            executor.execute(
                    "RELEASE SAVEPOINT sp1;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> executor.execute(
                                    "ROLLBACK TO SAVEPOINT sp1;"
                            )
                    );

            assertEquals(
                    "Savepoint not found: sp1",
                    exception.getMessage()
            );

            executor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldShowSavepointsThroughExecutor() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndTable(
                    executor
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "SAVEPOINT before_insert;"
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            executor.execute(
                    "SAVEPOINT after_insert;"
            );

            ExecuteResult result =
                    executor.execute(
                            "SHOW SAVEPOINTS;"
                    );

            assertEquals(
                    2,
                    result.getRowCount()
            );

            assertEquals(
                    1,
                    result.getRows()
                            .get(0)
                            .getValue(0)
            );

            assertEquals(
                    "before_insert",
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );

            assertEquals(
                    0,
                    result.getRows()
                            .get(0)
                            .getValue(2)
            );

            assertEquals(
                    2,
                    result.getRows()
                            .get(1)
                            .getValue(0)
            );

            assertEquals(
                    "after_insert",
                    result.getRows()
                            .get(1)
                            .getValue(1)
            );

            assertEquals(
                    1,
                    result.getRows()
                            .get(1)
                            .getValue(2)
            );

            executor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldShowRemainingSavepointsAfterRollbackToSavepoint() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndTable(
                    executor
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "SAVEPOINT sp1;"
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            executor.execute(
                    "SAVEPOINT sp2;"
            );

            executor.execute(
                    "ROLLBACK TO SAVEPOINT sp1;"
            );

            ExecuteResult result =
                    executor.execute(
                            "SHOW SAVEPOINTS;"
                    );

            assertEquals(
                    1,
                    result.getRowCount()
            );

            assertEquals(
                    "sp1",
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );

            executor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldShowEmptySavepointsWhenNoTransactionIsActive() {

        try (QueryExecutor executor = newExecutor()) {

            ExecuteResult result =
                    executor.execute(
                            "SHOW SAVEPOINTS;"
                    );

            assertEquals(
                    0,
                    result.getRowCount()
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
                )
        );
    }

    private void createDatabaseAndTable(
            QueryExecutor executor
    ) {

        executor.execute(
                "CREATE DATABASE savepoint_db;"
        );

        executor.execute(
                "USE DATABASE savepoint_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );
    }
}
