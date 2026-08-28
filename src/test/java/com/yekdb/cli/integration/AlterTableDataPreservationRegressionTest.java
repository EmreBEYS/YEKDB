package com.yekdb.cli.integration;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import com.yekdb.query.executor.ExecuteResult;
import com.yekdb.query.executor.QueryExecutionException;
import com.yekdb.query.executor.QueryExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 00-26 Phase 7.1 regression tests.
 *
 * Gerçek row storage .data dosyasında tutulduğu için ALTER TABLE
 * işlemlerinin mevcut kayıtları görünmez hale getirmemesi gerekir.
 */
class AlterTableDataPreservationRegressionTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldRejectAddColumnOnNonEmptyTableAndPreserveRows() {
        try (QueryExecutor executor = createExecutor()) {
            prepareDatabase(executor, "alter_add_guard_db");
            executor.execute("CREATE TABLE users (id INT, name STRING)");
            executor.execute("INSERT INTO users (id, name) VALUES (1, 'Emre')");

            QueryExecutionException exception = assertThrows(
                    QueryExecutionException.class,
                    () -> executor.execute("ALTER TABLE users ADD COLUMN city STRING")
            );

            assertTrue(exception.getMessage().contains("empty table"));

            ExecuteResult result = executor.execute("SELECT * FROM users");
            assertEquals(1, result.getRowCount());
        }
    }

    @Test
    void shouldRejectDropColumnOnNonEmptyTableAndPreserveRows() {
        try (QueryExecutor executor = createExecutor()) {
            prepareDatabase(executor, "alter_drop_guard_db");
            executor.execute("CREATE TABLE users (id INT, name STRING)");
            executor.execute("INSERT INTO users (id, name) VALUES (1, 'Emre')");

            QueryExecutionException exception = assertThrows(
                    QueryExecutionException.class,
                    () -> executor.execute("ALTER TABLE users DROP COLUMN name")
            );

            assertTrue(exception.getMessage().contains("empty table"));

            ExecuteResult result = executor.execute("SELECT * FROM users");
            assertEquals(1, result.getRowCount());
        }
    }

    @Test
    void shouldRejectSetNotNullOnNonEmptyTableUntilExistingRowValidationExists() {
        try (QueryExecutor executor = createExecutor()) {
            prepareDatabase(executor, "alter_not_null_guard_db");
            executor.execute("CREATE TABLE users (id INT, email STRING)");
            executor.execute("INSERT INTO users (id, email) VALUES (1, 'emre@example.com')");

            QueryExecutionException exception = assertThrows(
                    QueryExecutionException.class,
                    () -> executor.execute("ALTER TABLE users ALTER COLUMN email SET NOT NULL")
            );

            assertTrue(exception.getMessage().contains("empty table"));

            ExecuteResult result = executor.execute("SELECT * FROM users");
            assertEquals(1, result.getRowCount());
        }
    }

    @Test
    void shouldRenameTableAndKeepExistingRowsAccessibleUnderNewName() {
        try (QueryExecutor executor = createExecutor()) {
            prepareDatabase(executor, "alter_rename_data_db");
            executor.execute("CREATE TABLE users (id INT, name STRING)");
            executor.execute("INSERT INTO users (id, name) VALUES (1, 'Emre')");
            executor.execute("INSERT INTO users (id, name) VALUES (2, 'Ahmet')");

            executor.execute("ALTER TABLE users RENAME TO customers");

            ExecuteResult result = executor.execute("SELECT * FROM customers");
            assertEquals(2, result.getRowCount());
        }
    }

    @Test
    void shouldRenameColumnWithoutLosingExistingRows() {
        try (QueryExecutor executor = createExecutor()) {
            prepareDatabase(executor, "alter_rename_column_data_db");
            executor.execute("CREATE TABLE users (id INT, name STRING)");
            executor.execute("INSERT INTO users (id, name) VALUES (1, 'Emre')");

            executor.execute("ALTER TABLE users RENAME COLUMN name TO display_name");

            ExecuteResult result = executor.execute("SELECT * FROM users");
            assertEquals(1, result.getRowCount());
        }
    }

    private QueryExecutor createExecutor() {
        DatabaseManager databaseManager = new DatabaseManager(tempDirectory);
        return new QueryExecutor(
                databaseManager,
                new StorageQueryDataSource(databaseManager)
        );
    }

    private void prepareDatabase(QueryExecutor executor, String databaseName) {
        executor.execute("CREATE DATABASE " + databaseName);
        executor.execute("USE DATABASE " + databaseName);
    }
}
