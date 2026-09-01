package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorViewTriggerFinalRegressionTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void viewsAndTriggersShouldWorkTogetherInOneDatabaseSession() {
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

            createDatabaseAndTables(queryExecutor);

            queryExecutor.execute(
                    """
                    CREATE VIEW adult_users AS
                    SELECT id, name, age FROM users WHERE age >= 18;
                    """
            );

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_after_insert
                    AFTER INSERT ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (NEW.id, NEW.id, NEW.name)
                    END;
                    """
            );

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_after_update
                    AFTER UPDATE ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (20, OLD.id, NEW.name)
                    END;
                    """
            );

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_after_delete
                    AFTER DELETE ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (30, OLD.id, OLD.name)
                    END;
                    """
            );

            queryExecutor.execute(
                    "INSERT INTO users (id, name, age) VALUES (10, 'Emre', 21);"
            );
            queryExecutor.execute(
                    "INSERT INTO users (id, name, age) VALUES (11, 'Ali', 16);"
            );
            queryExecutor.execute(
                    "UPDATE users SET name = 'Yunus' WHERE id = 10;"
            );
            queryExecutor.execute(
                    "DELETE FROM users WHERE id = 11;"
            );

            ExecuteResult viewResult =
                    queryExecutor.execute(
                            "SELECT name FROM adult_users;"
                    );

            ExecuteResult showViewsResult =
                    queryExecutor.execute(
                            "SHOW VIEWS;"
                    );

            ExecuteResult showTriggersResult =
                    queryExecutor.execute(
                            "SHOW TRIGGERS FROM users;"
                    );

            ExecuteResult auditResult =
                    queryExecutor.execute(
                            "SELECT * FROM audit_log ORDER BY id ASC;"
                    );

            assertEquals(1, viewResult.getRowCount());
            assertEquals("Yunus", viewResult.getRows().get(0).getValue(0));

            assertEquals(1, showViewsResult.getRowCount());
            assertEquals("adult_users", showViewsResult.getRows().get(0).getValue(0));

            assertEquals(3, showTriggersResult.getRowCount());
            assertEquals("users_after_insert", showTriggersResult.getRows().get(0).getValue(0));
            assertEquals("users_after_update", showTriggersResult.getRows().get(1).getValue(0));
            assertEquals("users_after_delete", showTriggersResult.getRows().get(2).getValue(0));

            assertEquals(4, auditResult.getRowCount());
            assertEquals("Emre", auditResult.getRows().get(0).getValue(2));
            assertEquals("Ali", auditResult.getRows().get(1).getValue(2));
            assertEquals("Yunus", auditResult.getRows().get(2).getValue(2));
            assertEquals("Ali", auditResult.getRows().get(3).getValue(2));
        }
    }

    private void createDatabaseAndTables(
            QueryExecutor queryExecutor
    ) {
        queryExecutor.execute(
                "CREATE DATABASE view_trigger_final_db;"
        );

        queryExecutor.execute(
                "USE DATABASE view_trigger_final_db;"
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
