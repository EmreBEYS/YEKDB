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
 * Sprint 00-26 final regression test.
 *
 * Terminalde elle doğrulanan ALTER TABLE senaryosunu tek bir uçtan uca
 * regression akışında sabitler. Amaç schema mutation işlemlerinin mevcut
 * veriyi kaybetmemesini ve named constraint yaşam döngüsünün korunmasını
 * garanti altına almaktır.
 */
class AlterTableFinalRegressionTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldCompleteAlterTableLifecycleWithoutDataLoss() {
        DatabaseManager databaseManager = new DatabaseManager(tempDirectory);

        try (QueryExecutor executor = new QueryExecutor(
                databaseManager,
                new StorageQueryDataSource(databaseManager)
        )) {
            executor.execute("CREATE DATABASE alter_final_db");
            executor.execute("USE DATABASE alter_final_db");

            executor.execute(
                    "CREATE TABLE users (id INT PRIMARY KEY, username STRING, email STRING)"
            );

            executor.execute(
                    "INSERT INTO users (id, username, email) "
                            + "VALUES (1, 'emre', 'emre@example.com')"
            );
            executor.execute(
                    "INSERT INTO users (id, username, email) "
                            + "VALUES (2, 'ahmet', 'ahmet@example.com')"
            );

            ExecuteResult initialRows = executor.execute("SELECT * FROM users");
            assertEquals(2, initialRows.getRowCount());

            QueryExecutionException addColumnException = assertThrows(
                    QueryExecutionException.class,
                    () -> executor.execute("ALTER TABLE users ADD COLUMN city STRING")
            );
            assertTrue(addColumnException.getMessage().contains("empty table"));
            assertEquals(2, executor.execute("SELECT * FROM users").getRowCount());

            executor.execute(
                    "ALTER TABLE users RENAME COLUMN username TO display_name"
            );
            assertEquals(2, executor.execute("SELECT * FROM users").getRowCount());

            executor.execute(
                    "ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email)"
            );

            QueryExecutionException uniqueException = assertThrows(
                    QueryExecutionException.class,
                    () -> executor.execute(
                            "INSERT INTO users (id, display_name, email) "
                                    + "VALUES (3, 'mehmet', 'emre@example.com')"
                    )
            );
            assertTrue(uniqueException.getMessage().contains("UNIQUE"));
            assertEquals(2, executor.execute("SELECT * FROM users").getRowCount());

            executor.execute(
                    "ALTER TABLE users DROP CONSTRAINT uq_users_email"
            );
            executor.execute(
                    "INSERT INTO users (id, display_name, email) "
                            + "VALUES (3, 'mehmet', 'emre@example.com')"
            );
            assertEquals(3, executor.execute("SELECT * FROM users").getRowCount());

            executor.execute("ALTER TABLE users RENAME TO customers");

            ExecuteResult renamedRows = executor.execute("SELECT * FROM customers");
            assertEquals(3, renamedRows.getRowCount());

            assertThrows(
                    QueryExecutionException.class,
                    () -> executor.execute("SELECT * FROM users")
            );
        }
    }
}
