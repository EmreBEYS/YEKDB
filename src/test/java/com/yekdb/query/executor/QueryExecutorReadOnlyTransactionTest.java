package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryExecutorReadOnlyTransactionTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldRejectInsertInsideReadOnlyTransaction() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndTable(
                    executor
            );

            executor.execute(
                    "START TRANSACTION READ ONLY;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> executor.execute(
                                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
                            )
                    );

            assertEquals(
                    "DML cannot run inside a READ ONLY transaction.",
                    exception.getMessage()
            );

            executor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldAllowSelectInsideReadOnlyTransaction() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndTable(
                    executor
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            executor.execute(
                    "BEGIN TRANSACTION READ ONLY;"
            );

            ExecuteResult result =
                    executor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(
                    1,
                    result.getRowCount()
            );

            executor.execute(
                    "COMMIT;"
            );
        }
    }

    @Test
    void shouldAllowInsertInsideReadWriteTransaction() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndTable(
                    executor
            );

            executor.execute(
                    "BEGIN TRANSACTION READ WRITE;"
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            executor.execute(
                    "COMMIT;"
            );

            assertEquals(
                    1,
                    executor.execute(
                            "SELECT * FROM users;"
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
                )
        );
    }

    private void createDatabaseAndTable(
            QueryExecutor executor
    ) {

        executor.execute(
                "CREATE DATABASE read_only_tx_db;"
        );

        executor.execute(
                "USE DATABASE read_only_tx_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );
    }
}
