package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorTransactionBoundaryTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldRejectCreateTableInsideActiveTransaction() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndUsersTable(
                    executor
            );

            executor.execute(
                    "BEGIN;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> executor.execute(
                                    "CREATE TABLE orders (id INT);"
                            )
                    );

            assertEquals(
                    "CREATE TABLE cannot run inside an active transaction.",
                    exception.getMessage()
            );

            executor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldRejectUseDatabaseInsideActiveTransaction() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndUsersTable(
                    executor
            );

            executor.execute(
                    "CREATE DATABASE other_db;"
            );

            executor.execute(
                    "BEGIN;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> executor.execute(
                                    "USE DATABASE other_db;"
                            )
                    );

            assertEquals(
                    "USE DATABASE cannot run inside an active transaction.",
                    exception.getMessage()
            );

            executor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldRejectDropIndexInsideActiveTransaction() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndUsersTable(
                    executor
            );

            executor.execute(
                    "CREATE INDEX idx_users_id ON users (id);"
            );

            executor.execute(
                    "BEGIN;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> executor.execute(
                                    "DROP INDEX idx_users_id;"
                            )
                    );

            assertEquals(
                    "DROP INDEX cannot run inside an active transaction.",
                    exception.getMessage()
            );

            executor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldAllowSelectAndShowCommandsInsideActiveTransaction() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndUsersTable(
                    executor
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            executor.execute(
                    "BEGIN;"
            );

            ExecuteResult selectResult =
                    executor.execute(
                            "SELECT * FROM users;"
                    );

            ExecuteResult showViewsResult =
                    executor.execute(
                            "SHOW VIEWS;"
                    );

            assertEquals(
                    1,
                    selectResult.getRowCount()
            );

            assertTrue(
                    showViewsResult.isSuccess()
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

    private void createDatabaseAndUsersTable(
            QueryExecutor executor
    ) {

        executor.execute(
                "CREATE DATABASE tx_boundary_db;"
        );

        executor.execute(
                "USE DATABASE tx_boundary_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );
    }
}
