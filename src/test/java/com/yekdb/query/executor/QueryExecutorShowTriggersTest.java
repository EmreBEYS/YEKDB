package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorShowTriggersTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void showTriggersShouldReturnTriggerMetadataRows() {
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

            createDatabaseAndTables(queryExecutor, "show_triggers_db");

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_after_insert
                    AFTER INSERT ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (1, NEW.id, NEW.name)
                    END;
                    """
            );

            queryExecutor.execute(
                    """
                    CREATE TRIGGER orders_before_delete
                    BEFORE DELETE ON orders
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (2, OLD.id, OLD.description)
                    END;
                    """
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "SHOW TRIGGERS;"
                    );

            assertTrue(result.isSuccess());
            assertEquals(6, result.getColumnCount());
            assertEquals(2, result.getRowCount());

            assertEquals("users_after_insert", result.getRows().get(0).getValue(0));
            assertEquals("users", result.getRows().get(0).getValue(1));
            assertEquals("AFTER", result.getRows().get(0).getValue(2));
            assertEquals("INSERT", result.getRows().get(0).getValue(3));
            assertEquals(1, result.getRows().get(0).getValue(4));

            assertEquals("orders_before_delete", result.getRows().get(1).getValue(0));
            assertEquals("orders", result.getRows().get(1).getValue(1));
            assertEquals("BEFORE", result.getRows().get(1).getValue(2));
            assertEquals("DELETE", result.getRows().get(1).getValue(3));
        }
    }

    @Test
    void showTriggersFromTableShouldFilterByTableName() {
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

            createDatabaseAndTables(queryExecutor, "show_triggers_filter_db");

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
                    """
                    CREATE TRIGGER orders_after_insert
                    AFTER INSERT ON orders
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (2, NEW.id, NEW.description)
                    END;
                    """
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "SHOW TRIGGERS FROM users;"
                    );

            assertEquals(1, result.getRowCount());
            assertEquals("users_after_update", result.getRows().get(0).getValue(0));
            assertEquals("users", result.getRows().get(0).getValue(1));
        }
    }

    @Test
    void showTriggersShouldRequireCurrentDatabase() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            assertThrows(
                    QueryExecutionException.class,
                    () -> queryExecutor.execute(
                            "SHOW TRIGGERS;"
                    )
            );
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
                    name STRING
                );
                """
        );

        queryExecutor.execute(
                """
                CREATE TABLE orders (
                    id INT,
                    description STRING
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
