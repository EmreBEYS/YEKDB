package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import com.yekdb.storage.record.Row;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryExecutorTransactionRollbackTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldRollbackInsertedRows() {

        try (QueryExecutor executor = newExecutor()) {

            createUsersTable(
                    executor
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "INSERT INTO users (id, name, age) VALUES (1, 'Yunus', 21);"
            );

            assertEquals(
                    1,
                    executor.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );

            executor.execute(
                    "ROLLBACK;"
            );

            assertEquals(
                    0,
                    executor.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );
        }
    }

    @Test
    void shouldRollbackUpdatedRows() {

        try (QueryExecutor executor = newExecutor()) {

            createUsersTable(
                    executor
            );

            executor.execute(
                    "INSERT INTO users (id, name, age) VALUES (1, 'Yunus', 21);"
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "UPDATE users SET age = 22 WHERE id = 1;"
            );

            assertEquals(
                    22,
                    firstRow(
                            executor
                    ).getValue(
                            2
                    )
            );

            executor.execute(
                    "ROLLBACK;"
            );

            assertEquals(
                    21,
                    firstRow(
                            executor
                    ).getValue(
                            2
                    )
            );
        }
    }

    @Test
    void shouldRollbackDeletedRows() {

        try (QueryExecutor executor = newExecutor()) {

            createUsersTable(
                    executor
            );

            executor.execute(
                    "INSERT INTO users (id, name, age) VALUES (1, 'Yunus', 21);"
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "DELETE FROM users WHERE id = 1;"
            );

            assertEquals(
                    0,
                    executor.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );

            executor.execute(
                    "ROLLBACK;"
            );

            ExecuteResult result =
                    executor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(
                    1,
                    result.getRowCount()
            );

            assertEquals(
                    "Yunus",
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );
        }
    }

    @Test
    void shouldKeepCommittedRows() {

        try (QueryExecutor executor = newExecutor()) {

            createUsersTable(
                    executor
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "INSERT INTO users (id, name, age) VALUES (1, 'Yunus', 21);"
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

    private void createUsersTable(
            QueryExecutor executor
    ) {

        executor.execute(
                "CREATE DATABASE tx_db;"
        );

        executor.execute(
                "USE DATABASE tx_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING, age INT);"
        );
    }

    private Row firstRow(
            QueryExecutor executor
    ) {

        return executor.execute(
                        "SELECT * FROM users;"
                )
                .getRows()
                .get(0);
    }
}
