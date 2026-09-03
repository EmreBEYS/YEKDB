package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorTransactionLifecycleTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldExecuteLongTransactionSyntaxForms() {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndTable(
                    executor
            );

            ExecuteResult beginResult =
                    executor.execute(
                            "START TRANSACTION;"
                    );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            ExecuteResult commitResult =
                    executor.execute(
                            "COMMIT TRANSACTION;"
                    );

            assertTrue(
                    beginResult.isSuccess()
            );

            assertTrue(
                    commitResult.isSuccess()
            );

            assertEquals(
                    1,
                    executor.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );

            executor.execute(
                    "BEGIN TRANSACTION;"
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (2, 'Ali');"
            );

            executor.execute(
                    "ROLLBACK TRANSACTION;"
            );

            assertEquals(
                    1,
                    executor.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );
        }
    }

    @Test
    void shouldRollbackActiveTransactionWhenExecutorCloses() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        QueryExecutor firstExecutor =
                new QueryExecutor(
                        databaseManager,
                        new StorageQueryDataSource(
                                databaseManager
                        )
                );

        firstExecutor.execute(
                "CREATE DATABASE close_rollback_db;"
        );

        firstExecutor.execute(
                "USE DATABASE close_rollback_db;"
        );

        firstExecutor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );

        firstExecutor.execute(
                "BEGIN;"
        );

        firstExecutor.execute(
                "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
        );

        firstExecutor.close();

        try (QueryExecutor secondExecutor =
                     new QueryExecutor(
                             databaseManager,
                             new StorageQueryDataSource(
                                     databaseManager
                             )
                     )) {

            assertEquals(
                    0,
                    secondExecutor.execute(
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
                "CREATE DATABASE lifecycle_db;"
        );

        executor.execute(
                "USE DATABASE lifecycle_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );
    }
}
