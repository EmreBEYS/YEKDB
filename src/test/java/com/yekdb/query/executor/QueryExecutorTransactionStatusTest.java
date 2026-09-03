package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QueryExecutorTransactionStatusTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldShowInactiveTransactionStatus() {

        try (QueryExecutor executor = newExecutor()) {

            ExecuteResult result =
                    executor.execute(
                            "SHOW TRANSACTION;"
                    );

            assertEquals(
                    "Transaction status",
                    result.getMessage()
            );

            assertEquals(
                    1,
                    result.getRowCount()
            );

            assertEquals(
                    "INACTIVE",
                    result.getRows()
                            .get(0)
                            .getValue(0)
            );

            assertNull(
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );
        }
    }

    @Test
    void shouldShowActiveTransactionCounters() {

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

            ExecuteResult result =
                    executor.execute(
                            "SHOW TRANSACTION;"
                    );

            assertEquals(
                    "ACTIVE",
                    result.getRows()
                            .get(0)
                            .getValue(0)
            );

            assertEquals(
                    1L,
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );

            assertEquals(
                    1,
                    result.getRows()
                            .get(0)
                            .getValue(3)
            );

            assertEquals(
                    1,
                    result.getRows()
                            .get(0)
                            .getValue(4)
            );

            assertEquals(
                    "READ_WRITE",
                    result.getRows()
                            .get(0)
                            .getValue(5)
            );

            assertEquals(
                    "READ_COMMITTED",
                    result.getRows()
                            .get(0)
                            .getValue(6)
            );

            executor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldShowInactiveAfterCommit() {

        try (QueryExecutor executor = newExecutor()) {

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "COMMIT;"
            );

            ExecuteResult result =
                    executor.execute(
                            "SHOW TRANSACTION;"
                    );

            assertEquals(
                    "INACTIVE",
                    result.getRows()
                            .get(0)
                            .getValue(0)
            );
        }
    }

    @Test
    void shouldShowReadOnlyAccessMode() {

        try (QueryExecutor executor = newExecutor()) {

            executor.execute(
                    "START TRANSACTION READ ONLY;"
            );

            ExecuteResult result =
                    executor.execute(
                            "SHOW TRANSACTION;"
                    );

            assertEquals(
                    "READ_ONLY",
                    result.getRows()
                            .get(0)
                            .getValue(5)
            );

            executor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldShowIsolationLevel() {

        try (QueryExecutor executor = newExecutor()) {

            executor.execute(
                    "START TRANSACTION ISOLATION LEVEL SERIALIZABLE;"
            );

            ExecuteResult result =
                    executor.execute(
                            "SHOW TRANSACTION;"
                    );

            assertEquals(
                    "SERIALIZABLE",
                    result.getRows()
                            .get(0)
                            .getValue(6)
            );

            executor.execute(
                    "ROLLBACK;"
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
                "CREATE DATABASE tx_status_db;"
        );

        executor.execute(
                "USE DATABASE tx_status_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );
    }
}
