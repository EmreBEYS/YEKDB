package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorProcedureTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldCreateShowCallAndDropProcedure() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_lifecycle_db"
            );

            ExecuteResult createResult =
                    queryExecutor.execute(
                            """
                            CREATE PROCEDURE create_user(user_id INT, user_name STRING)
                            BEGIN
                                INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                            END;
                            """
                    );

            assertTrue(createResult.isSuccess());

            ExecuteResult showResult =
                    queryExecutor.execute(
                            "SHOW PROCEDURES;"
                    );

            assertEquals(1, showResult.getRowCount());
            assertEquals(
                    "create_user",
                    showResult.getRows().get(0).getValue(0)
            );
            assertEquals(
                    "create_user(user_id INT, user_name STRING)",
                    showResult.getRows().get(0).getValue(1)
            );
            assertEquals(2, showResult.getRows().get(0).getValue(2));

            ExecuteResult callResult =
                    queryExecutor.execute(
                            "CALL create_user(1, 'Ada');"
                    );

            assertTrue(callResult.isSuccess());
            assertEquals(1, callResult.getAffectedRows());

            ExecuteResult selectResult =
                    queryExecutor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(1, selectResult.getRowCount());
            assertEquals(1, selectResult.getRows().get(0).getValue(0));
            assertEquals("Ada", selectResult.getRows().get(0).getValue(1));

            ExecuteResult dropResult =
                    queryExecutor.execute(
                            "DROP PROCEDURE create_user;"
                    );

            assertTrue(dropResult.isSuccess());

            assertEquals(
                    0,
                    queryExecutor.execute("SHOW PROCEDURES;")
                            .getRowCount()
            );
        }
    }

    @Test
    void shouldCallProcedureWithNamedArguments() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_named_call_db"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE create_user(user_id INT, user_name STRING)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                    END;
                    """
            );

            ExecuteResult callResult =
                    queryExecutor.execute(
                            "CALL create_user(user_name => 'Ada', user_id => 1);"
                    );

            assertTrue(callResult.isSuccess());
            assertEquals(1, callResult.getAffectedRows());

            ExecuteResult selectResult =
                    queryExecutor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(1, selectResult.getRowCount());
            assertEquals(1, selectResult.getRows().get(0).getValue(0));
            assertEquals("Ada", selectResult.getRows().get(0).getValue(1));
        }
    }

    @Test
    void shouldRejectUnknownNamedProcedureArgument() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_unknown_named_call_db"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE create_user(user_id INT, user_name STRING)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                    END;
                    """
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> queryExecutor.execute(
                            "CALL create_user(user_id => 1, bad_name => 'Ada');"
                    )
            );
        }
    }

    @Test
    void nestedProcedureCallShouldExecuteInnerProcedure() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_nested_call_db"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE insert_user(user_id INT, user_name STRING)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                    END;
                    """
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE seed_user()
                    BEGIN
                        CALL insert_user(user_name => 'Ada', user_id => 1)
                    END;
                    """
            );

            ExecuteResult callResult =
                    queryExecutor.execute(
                            "CALL seed_user();"
                    );

            assertTrue(callResult.isSuccess());
            assertEquals(1, callResult.getAffectedRows());

            ExecuteResult selectResult =
                    queryExecutor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(1, selectResult.getRowCount());
            assertEquals(1, selectResult.getRows().get(0).getValue(0));
            assertEquals("Ada", selectResult.getRows().get(0).getValue(1));
        }
    }

    @Test
    void procedureDdlShouldRespectActiveTransactionBoundary() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE procedure_transaction_boundary_db;"
            );
            queryExecutor.execute(
                    "USE DATABASE procedure_transaction_boundary_db;"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE create_audit_table()
                    BEGIN
                        CREATE TABLE audit_log (id INT)
                    END;
                    """
            );

            queryExecutor.execute(
                    "BEGIN;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "CALL create_audit_table();"
                            )
                    );

            assertTrue(
                    exception.getMessage()
                            .contains("Procedure execution failed")
            );
            assertNotNull(exception.getCause());
            assertTrue(
                    exception.getCause()
                            .getMessage()
                            .contains(
                                    "CREATE TABLE cannot run inside an active transaction."
                            )
            );

            queryExecutor.execute(
                    "ROLLBACK;"
            );
        }
    }

    @Test
    void shouldReturnLastSelectResultFromProcedure() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_select_db"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE seed_and_list()
                    BEGIN
                        INSERT INTO users (id, name) VALUES (1, 'Ada');
                        SELECT * FROM users
                    END;
                    """
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "CALL seed_and_list();"
                    );

            assertTrue(result.isSuccess());
            assertEquals(2, result.getColumnCount());
            assertEquals(1, result.getRowCount());
            assertEquals("Ada", result.getRows().get(0).getValue(1));
        }
    }

    @Test
    void dropProcedureIfExistsShouldSucceedWhenProcedureIsMissing() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE procedure_drop_if_exists_db;"
            );
            queryExecutor.execute(
                    "USE DATABASE procedure_drop_if_exists_db;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "DROP PROCEDURE IF EXISTS missing_proc;"
                    );

            assertTrue(result.isSuccess());
            assertEquals(0, result.getAffectedRows());
        }
    }

    @Test
    void dropProcedureShouldStillFailWhenProcedureIsMissing() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE procedure_drop_missing_db;"
            );
            queryExecutor.execute(
                    "USE DATABASE procedure_drop_missing_db;"
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> queryExecutor.execute(
                            "DROP PROCEDURE missing_proc;"
                    )
            );
        }
    }

    @Test
    void showProcedureShouldReturnProcedureBody() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_detail_db"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE create_user(user_id INT, user_name STRING)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                    END;
                    """
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "SHOW PROCEDURE create_user;"
                    );

            assertTrue(result.isSuccess());
            assertEquals(6, result.getColumnCount());
            assertEquals(1, result.getRowCount());
            assertEquals(
                    "create_user",
                    result.getRows().get(0).getValue(0)
            );
            assertEquals(
                    "create_user(user_id INT, user_name STRING)",
                    result.getRows().get(0).getValue(1)
            );
            assertEquals(2, result.getRows().get(0).getValue(2));
            assertEquals(
                    "INSERT INTO users (id, name) VALUES (:user_id, :user_name)",
                    result.getRows().get(0).getValue(3)
            );
        }
    }

    @Test
    void showProceduresLikeShouldFilterByProcedureName() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_like_db"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE create_user(user_id INT, user_name STRING)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                    END;
                    """
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE archive_user(user_id INT)
                    BEGIN
                        SELECT * FROM users
                    END;
                    """
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "SHOW PROCEDURES LIKE 'create_%';"
                    );

            assertTrue(result.isSuccess());
            assertEquals(1, result.getRowCount());
            assertEquals(
                    "create_user",
                    result.getRows().get(0).getValue(0)
            );
        }
    }

    @Test
    void createOrReplaceProcedureShouldUpdateBodyAndVersion() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_replace_db"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE create_user(user_id INT, user_name STRING)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                    END;
                    """
            );

            ExecuteResult replaceResult =
                    queryExecutor.execute(
                            """
                            CREATE OR REPLACE PROCEDURE create_user(user_name STRING)
                            BEGIN
                                INSERT INTO users (id, name) VALUES (2, :user_name)
                            END;
                            """
                    );

            assertTrue(replaceResult.isSuccess());

            ExecuteResult detailResult =
                    queryExecutor.execute(
                            "SHOW PROCEDURE create_user;"
                    );

            assertEquals(
                    "create_user(user_name STRING)",
                    detailResult.getRows().get(0).getValue(1)
            );
            assertEquals(
                    1,
                    detailResult.getRows().get(0).getValue(2)
            );
            assertEquals(
                    "INSERT INTO users (id, name) VALUES (2, :user_name)",
                    detailResult.getRows().get(0).getValue(3)
            );
            assertEquals(
                    2,
                    detailResult.getRows().get(0).getValue(4)
            );

            ExecuteResult callResult =
                    queryExecutor.execute(
                            "CALL create_user('Grace');"
                    );

            assertEquals(1, callResult.getAffectedRows());
            assertEquals(
                    "Grace",
                    queryExecutor.execute("SELECT * FROM users;")
                            .getRows()
                            .get(0)
                            .getValue(1)
            );
        }
    }

    @Test
    void createProcedureIfNotExistsShouldKeepExistingProcedure() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_if_not_exists_db"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE create_user(user_id INT, user_name STRING)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                    END;
                    """
            );

            ExecuteResult secondCreate =
                    queryExecutor.execute(
                            """
                            CREATE PROCEDURE IF NOT EXISTS create_user(user_name STRING)
                            BEGIN
                                INSERT INTO users (id, name) VALUES (2, :user_name)
                            END;
                            """
                    );

            assertTrue(secondCreate.isSuccess());

            ExecuteResult detailResult =
                    queryExecutor.execute(
                            "SHOW PROCEDURE create_user;"
                    );

            assertEquals(
                    "create_user(user_id INT, user_name STRING)",
                    detailResult.getRows().get(0).getValue(1)
            );
            assertEquals(
                    "INSERT INTO users (id, name) VALUES (:user_id, :user_name)",
                    detailResult.getRows().get(0).getValue(3)
            );
            assertEquals(
                    1,
                    detailResult.getRows().get(0).getValue(4)
            );
        }
    }

    @Test
    void shouldRejectWrongArgumentCountAndType() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_validation_db"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE create_user(user_id INT, user_name STRING)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                    END;
                    """
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> queryExecutor.execute(
                            "CALL create_user(1);"
                    )
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> queryExecutor.execute(
                            "CALL create_user('bad-id', 'Ada');"
                    )
            );
        }
    }

    @Test
    void shouldRejectRecursiveProcedureCall() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE procedure_recursion_db;"
            );
            queryExecutor.execute(
                    "USE DATABASE procedure_recursion_db;"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE loop_proc()
                    BEGIN
                        CALL loop_proc()
                    END;
                    """
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> queryExecutor.execute(
                            "CALL loop_proc();"
                    )
            );
        }
    }

    @Test
    void storedProcedureFinalRegressionShouldCoverLifecycleVariants() {
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

            createDatabaseAndUsersTable(
                    queryExecutor,
                    "procedure_final_regression_db"
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE IF NOT EXISTS create_user(user_id INT, user_name STRING)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                    END;
                    """
            );

            queryExecutor.execute(
                    """
                    CREATE PROCEDURE IF NOT EXISTS create_user(user_name STRING)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (99, :user_name)
                    END;
                    """
            );

            ExecuteResult initialDetail =
                    queryExecutor.execute(
                            "SHOW PROCEDURE create_user;"
                    );

            assertEquals(
                    "create_user(user_id INT, user_name STRING)",
                    initialDetail.getRows().get(0).getValue(1)
            );
            assertEquals(
                    1,
                    initialDetail.getRows().get(0).getValue(4)
            );

            queryExecutor.execute(
                    "CALL create_user(1, 'Ada');"
            );

            queryExecutor.execute(
                    """
                    CREATE OR REPLACE PROCEDURE create_user(user_name STRING, user_id INT)
                    BEGIN
                        INSERT INTO users (id, name) VALUES (:user_id, :user_name)
                    END;
                    """
            );

            ExecuteResult replacedDetail =
                    queryExecutor.execute(
                            "SHOW PROCEDURE create_user;"
                    );

            assertEquals(
                    "create_user(user_name STRING, user_id INT)",
                    replacedDetail.getRows().get(0).getValue(1)
            );
            assertEquals(
                    2,
                    replacedDetail.getRows().get(0).getValue(4)
            );

            queryExecutor.execute(
                    "CALL create_user(user_id => 2, user_name => 'Grace');"
            );

            ExecuteResult showLike =
                    queryExecutor.execute(
                            "SHOW PROCEDURES LIKE 'create_%';"
                    );

            assertEquals(1, showLike.getRowCount());
            assertEquals(
                    "create_user",
                    showLike.getRows().get(0).getValue(0)
            );

            ExecuteResult users =
                    queryExecutor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(2, users.getRowCount());
            assertEquals("Ada", users.getRows().get(0).getValue(1));
            assertEquals("Grace", users.getRows().get(1).getValue(1));

            queryExecutor.execute(
                    "DROP PROCEDURE create_user;"
            );

            ExecuteResult missingDrop =
                    queryExecutor.execute(
                            "DROP PROCEDURE IF EXISTS create_user;"
                    );

            assertTrue(missingDrop.isSuccess());
            assertEquals(
                    0,
                    queryExecutor.execute("SHOW PROCEDURES;")
                            .getRowCount()
            );
        }
    }

    private void createDatabaseAndUsersTable(
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
    }
}
