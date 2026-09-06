package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryExecutorTransactionIsolationTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldRejectConcurrentWritesToLockedTable() {

        DatabaseManager firstDatabaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        DatabaseManager secondDatabaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        try (QueryExecutor firstExecutor = newExecutor(firstDatabaseManager);
             QueryExecutor secondExecutor = newExecutor(secondDatabaseManager)) {

            createDatabaseAndTable(
                    firstExecutor
            );

            secondExecutor.execute(
                    "USE DATABASE isolation_db;"
            );

            firstExecutor.execute(
                    "BEGIN;"
            );

            firstExecutor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            secondExecutor.execute(
                    "BEGIN;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> secondExecutor.execute(
                                    "INSERT INTO users (id, name) VALUES (2, 'Ali');"
                            )
                    );

            assertEquals(
                    "Table is locked by another active transaction: users",
                    exception.getMessage()
            );

            secondExecutor.execute(
                    "ROLLBACK;"
            );

            firstExecutor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldReleaseWriteLockAfterCommit() {

        DatabaseManager firstDatabaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        DatabaseManager secondDatabaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        try (QueryExecutor firstExecutor = newExecutor(firstDatabaseManager);
             QueryExecutor secondExecutor = newExecutor(secondDatabaseManager)) {

            createDatabaseAndTable(
                    firstExecutor
            );

            secondExecutor.execute(
                    "USE DATABASE isolation_db;"
            );

            firstExecutor.execute(
                    "BEGIN;"
            );

            firstExecutor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            firstExecutor.execute(
                    "COMMIT;"
            );

            secondExecutor.execute(
                    "INSERT INTO users (id, name) VALUES (2, 'Ali');"
            );

            assertEquals(
                    2,
                    secondExecutor.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );
        }
    }

    @Test
    void shouldReleaseReentrantWriteLockAfterCommit() {

        DatabaseManager firstDatabaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        DatabaseManager secondDatabaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        try (QueryExecutor firstExecutor = newExecutor(firstDatabaseManager);
             QueryExecutor secondExecutor = newExecutor(secondDatabaseManager)) {

            createDatabaseAndTable(
                    firstExecutor
            );

            secondExecutor.execute(
                    "USE DATABASE isolation_db;"
            );

            firstExecutor.execute(
                    "BEGIN;"
            );

            firstExecutor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            firstExecutor.execute(
                    "INSERT INTO users (id, name) VALUES (2, 'Ada');"
            );

            firstExecutor.execute(
                    "COMMIT;"
            );

            secondExecutor.execute(
                    "INSERT INTO users (id, name) VALUES (3, 'Ali');"
            );

            assertEquals(
                    3,
                    secondExecutor.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );
        }
    }

    @Test
    void shouldReleaseWriteLockAfterRollback() {

        DatabaseManager firstDatabaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        DatabaseManager secondDatabaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        try (QueryExecutor firstExecutor = newExecutor(firstDatabaseManager);
             QueryExecutor secondExecutor = newExecutor(secondDatabaseManager)) {

            createDatabaseAndTable(
                    firstExecutor
            );

            secondExecutor.execute(
                    "USE DATABASE isolation_db;"
            );

            firstExecutor.execute(
                    "BEGIN;"
            );

            firstExecutor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            firstExecutor.execute(
                    "ROLLBACK;"
            );

            secondExecutor.execute(
                    "INSERT INTO users (id, name) VALUES (2, 'Ali');"
            );

            ExecuteResult result =
                    secondExecutor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(
                    1,
                    result.getRowCount()
            );

            assertEquals(
                    "Ali",
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );
        }
    }

    private QueryExecutor newExecutor(
            DatabaseManager databaseManager
    ) {

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

        executor.execute(
                "USE DATABASE isolation_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );
    }
}
