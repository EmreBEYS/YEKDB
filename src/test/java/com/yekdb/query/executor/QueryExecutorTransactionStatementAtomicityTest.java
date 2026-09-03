package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryExecutorTransactionStatementAtomicityTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldRollbackMainInsertAndBeforeTriggerSideEffectsWhenAfterTriggerFails() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        try (QueryExecutor executor =
                     new QueryExecutor(
                             databaseManager,
                             new StorageQueryDataSource(
                                     databaseManager
                             )
                     )) {

            createDatabaseAndTables(
                    executor
            );

            executor.execute(
                    """
                    CREATE TRIGGER users_before_insert_audit
                    BEFORE INSERT ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (1, NEW.id, 'before')
                    END;
                    """
            );

            executor.execute(
                    """
                    CREATE TRIGGER users_after_insert_fail
                    AFTER INSERT ON users
                    BEGIN
                        INSERT INTO missing_table (id) VALUES (NEW.id)
                    END;
                    """
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> executor.execute(
                            "INSERT INTO users (id, name, age) VALUES (10, 'Emre', 21);"
                    )
            );

            assertEquals(
                    0,
                    executor.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );

            assertEquals(
                    0,
                    executor.execute(
                            "SELECT * FROM audit_log;"
                    ).getRowCount()
            );
        }
    }

    @Test
    void shouldKeepExplicitTransactionActiveAfterFailedStatementRollback() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        try (QueryExecutor executor =
                     new QueryExecutor(
                             databaseManager,
                             new StorageQueryDataSource(
                                     databaseManager
                             )
                     )) {

            createDatabaseAndTables(
                    executor
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "INSERT INTO users (id, name, age) VALUES (1, 'Yunus', 21);"
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> executor.execute(
                            "INSERT INTO missing_table (id) VALUES (2);"
                    )
            );

            executor.execute(
                    "INSERT INTO users (id, name, age) VALUES (3, 'Ali', 30);"
            );

            executor.execute(
                    "COMMIT;"
            );

            assertEquals(
                    2,
                    executor.execute(
                            "SELECT * FROM users;"
                    ).getRowCount()
            );
        }
    }

    private void createDatabaseAndTables(
            QueryExecutor executor
    ) {

        executor.execute(
                "CREATE DATABASE statement_atomicity_db;"
        );

        executor.execute(
                "USE DATABASE statement_atomicity_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING, age INT);"
        );

        executor.execute(
                "CREATE TABLE audit_log (id INT, user_id INT, message STRING);"
        );
    }
}
