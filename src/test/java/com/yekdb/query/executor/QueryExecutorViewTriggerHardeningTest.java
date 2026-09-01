package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorViewTriggerHardeningTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createTriggerShouldFailWhenTargetTableDoesNotExist() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE missing_trigger_table_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE missing_trigger_table_db;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    """
                                    CREATE TRIGGER users_after_insert
                                    AFTER INSERT ON users
                                    BEGIN
                                        INSERT INTO audit_log (id) VALUES (1)
                                    END;
                                    """
                            )
                    );

            assertTrue(
                    exception.getMessage()
                            .contains("Table not found: users")
            );
            assertFalse(
                    databaseManager
                            .getCurrentDatabase()
                            .getTriggerCatalog()
                            .containsTrigger("users_after_insert")
            );
        }
    }

    @Test
    void createViewShouldFailWhenNameConflictsWithExistingTable() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE view_table_conflict_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE view_table_conflict_db;"
            );

            queryExecutor.execute(
                    """
                    CREATE TABLE users (
                        id INT,
                        name STRING
                    );
                    """
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "CREATE VIEW users AS SELECT id, name FROM users;"
                            )
                    );

            assertEquals(
                    "View name conflicts with existing table: users",
                    exception.getMessage()
            );
            assertFalse(
                    databaseManager
                            .getCurrentDatabase()
                            .getViewCatalog()
                            .containsView("users")
            );
        }
    }
}
