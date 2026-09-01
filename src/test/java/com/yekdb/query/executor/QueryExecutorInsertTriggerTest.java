package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorInsertTriggerTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createTriggerShouldRegisterTriggerInCurrentDatabaseCatalog() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE trigger_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE trigger_db;"
            );

            createUsersAndAuditTables(queryExecutor);

            ExecuteResult result =
                    queryExecutor.execute(
                            """
                            CREATE TRIGGER users_insert_log
                            AFTER INSERT ON users
                            BEGIN
                                INSERT INTO audit_log (id, user_id, message)
                                VALUES (1, NEW.id, NEW.name)
                            END;
                            """
                    );

            assertTrue(result.isSuccess());
            assertTrue(
                    databaseManager
                            .getCurrentDatabase()
                            .getTriggerCatalog()
                            .containsTrigger("users_insert_log")
            );
        }
    }

    @Test
    void dropTriggerShouldRemoveTriggerFromCurrentDatabaseCatalog() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE drop_trigger_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE drop_trigger_db;"
            );

            createUsersAndAuditTables(queryExecutor);

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_insert_log
                    AFTER INSERT ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (1, NEW.id, NEW.name)
                    END;
                    """
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "DROP TRIGGER users_insert_log;"
                    );

            assertTrue(result.isSuccess());
            assertFalse(
                    databaseManager
                            .getCurrentDatabase()
                            .getTriggerCatalog()
                            .containsTrigger("users_insert_log")
            );
        }
    }

    @Test
    void afterInsertTriggerShouldExecuteBodyWithNewValues() {
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

            createDatabaseAndTables(queryExecutor, "after_insert_trigger_db");

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
                    "INSERT INTO users (id, name, age) VALUES (10, 'Emre', 21);"
            );

            ExecuteResult auditResult =
                    queryExecutor.execute(
                            "SELECT * FROM audit_log;"
                    );

            assertEquals(1, auditResult.getRowCount());
            assertEquals(10, auditResult.getRows().get(0).getValue(1));
            assertEquals("Emre", auditResult.getRows().get(0).getValue(2));
        }
    }

    @Test
    void beforeInsertTriggerShouldExecuteBeforeMainInsert() {
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

            createDatabaseAndTables(queryExecutor, "before_insert_trigger_db");

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_before_insert
                    BEFORE INSERT ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (1, NEW.id, 'before')
                    END;
                    """
            );

            queryExecutor.execute(
                    "INSERT INTO users (id, name, age) VALUES (20, 'Ayse', 30);"
            );

            ExecuteResult auditResult =
                    queryExecutor.execute(
                            "SELECT * FROM audit_log;"
                    );

            assertEquals(1, auditResult.getRowCount());
            assertEquals(20, auditResult.getRows().get(0).getValue(1));
            assertEquals("before", auditResult.getRows().get(0).getValue(2));
        }
    }

    @Test
    void multipleInsertTriggersShouldExecuteInRegistrationOrder() {
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

            createDatabaseAndTables(queryExecutor, "multiple_trigger_db");

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_after_insert_first
                    AFTER INSERT ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (1, NEW.id, 'first')
                    END;
                    """
            );

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_after_insert_second
                    AFTER INSERT ON users
                    BEGIN
                        INSERT INTO audit_log (id, user_id, message)
                        VALUES (2, NEW.id, 'second')
                    END;
                    """
            );

            queryExecutor.execute(
                    "INSERT INTO users (id, name, age) VALUES (30, 'Ali', 24);"
            );

            ExecuteResult auditResult =
                    queryExecutor.execute(
                            "SELECT * FROM audit_log ORDER BY id ASC;"
                    );

            assertEquals(2, auditResult.getRowCount());
            assertEquals("first", auditResult.getRows().get(0).getValue(2));
            assertEquals("second", auditResult.getRows().get(1).getValue(2));
        }
    }

    @Test
    void failingBeforeInsertTriggerShouldStopMainInsert() {
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

            createDatabaseAndTables(queryExecutor, "failing_before_trigger_db");

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_before_insert
                    BEFORE INSERT ON users
                    BEGIN
                        INSERT INTO missing_table (id) VALUES (NEW.id)
                    END;
                    """
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> queryExecutor.execute(
                            "INSERT INTO users (id, name, age) VALUES (40, 'Veli', 19);"
                    )
            );

            ExecuteResult usersResult =
                    queryExecutor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(0, usersResult.getRowCount());
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

    private void createUsersAndAuditTables(
            QueryExecutor queryExecutor
    ) {
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
