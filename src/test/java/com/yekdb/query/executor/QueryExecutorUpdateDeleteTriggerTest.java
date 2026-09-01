package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorUpdateDeleteTriggerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void afterUpdateTriggerShouldExecuteBodyWithOldAndNewValues() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        StorageQueryDataSource dataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             dataSource
                     )) {

            createDatabaseAndTables(queryExecutor, "after_update_trigger_db");

            queryExecutor.execute(
                    "INSERT INTO users (id, name, age) VALUES (1, 'Emre', 21);"
            );

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_after_update
                    AFTER UPDATE ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (1, OLD.id, NEW.name)
                    END;
                    """
            );

            queryExecutor.execute(
                    "UPDATE users SET name = 'Yunus' WHERE id = 1;"
            );

            ExecuteResult auditResult =
                    queryExecutor.execute(
                            "SELECT * FROM audit_log;"
                    );

            assertEquals(1, auditResult.getRowCount());
            assertEquals(1, auditResult.getRows().get(0).getValue(1));
            assertEquals("Yunus", auditResult.getRows().get(0).getValue(2));
        }
    }

    @Test
    void afterDeleteTriggerShouldExecuteBodyWithOldValues() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        StorageQueryDataSource dataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             dataSource
                     )) {

            createDatabaseAndTables(queryExecutor, "after_delete_trigger_db");

            queryExecutor.execute(
                    "INSERT INTO users (id, name, age) VALUES (2, 'Ayse', 30);"
            );

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_after_delete
                    AFTER DELETE ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (1, OLD.id, OLD.name)
                    END;
                    """
            );

            queryExecutor.execute(
                    "DELETE FROM users WHERE id = 2;"
            );

            ExecuteResult auditResult =
                    queryExecutor.execute(
                            "SELECT * FROM audit_log;"
                    );

            ExecuteResult usersResult =
                    queryExecutor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(1, auditResult.getRowCount());
            assertEquals(2, auditResult.getRows().get(0).getValue(1));
            assertEquals("Ayse", auditResult.getRows().get(0).getValue(2));
            assertEquals(0, usersResult.getRowCount());
        }
    }

    @Test
    void failingBeforeUpdateTriggerShouldStopMainUpdate() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        StorageQueryDataSource dataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             dataSource
                     )) {

            createDatabaseAndTables(queryExecutor, "failing_before_update_db");

            queryExecutor.execute(
                    "INSERT INTO users (id, name, age) VALUES (3, 'Ali', 24);"
            );

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_before_update
                    BEFORE UPDATE ON users
                    BEGIN
                        INSERT INTO missing_table (id) VALUES (OLD.id)
                    END;
                    """
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> queryExecutor.execute(
                            "UPDATE users SET name = 'Veli' WHERE id = 3;"
                    )
            );

            ExecuteResult usersResult =
                    queryExecutor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(1, usersResult.getRowCount());
            assertEquals("Ali", usersResult.getRows().get(0).getValue(1));
        }
    }

    private void createDatabaseAndTables(
            QueryExecutor queryExecutor,
            String databaseName
    ) {
        queryExecutor.execute(
                "CREATE DATABASE " + databaseName + ";"
        );

        queryExecutor.execute(
                "USE DATABASE " + databaseName + ";"
        );

        queryExecutor.execute(
                """
                CREATE TABLE users (
                    id INT,
                    name STRING,
                    age INT
                );
                """
        );

        queryExecutor.execute(
                """
                CREATE TABLE audit_log (
                    id INT,
                    user_id INT,
                    message STRING
                );
                """
        );
    }
}
