package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.command.CreateViewCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorCreateViewTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createViewSqlShouldRegisterViewInCurrentDatabaseCatalog() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE view_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE view_db;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "CREATE VIEW adult_users AS " +
                                    "SELECT id, name FROM users WHERE age >= 18;"
                    );

            assertTrue(result.isSuccess());
            assertTrue(
                    result.getMessage()
                            .contains("adult_users")
            );
            assertTrue(
                    databaseManager
                            .getCurrentDatabase()
                            .getViewCatalog()
                            .containsView("adult_users")
            );
            assertEquals(
                    "SELECT id, name FROM users WHERE age >= 18",
                    databaseManager
                            .getCurrentDatabase()
                            .getViewCatalog()
                            .getView("adult_users")
                            .getSourceSelect()
            );
        }
    }

    @Test
    void createViewCommandShouldRegisterViewInCurrentDatabaseCatalog() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE view_command_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE view_command_db;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            new CreateViewCommand(
                                    "active_users",
                                    "SELECT id FROM users WHERE active = true"
                            )
                    );

            assertTrue(result.isSuccess());
            assertTrue(
                    databaseManager
                            .getCurrentDatabase()
                            .getViewCatalog()
                            .containsView("active_users")
            );
        }
    }

    @Test
    void createViewWithoutSelectedDatabaseShouldFail() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "CREATE VIEW adults AS SELECT * FROM users;"
                            )
                    );

            assertEquals(
                    "No database selected. Execute USE DATABASE first.",
                    exception.getMessage()
            );
        }
    }

    @Test
    void duplicateCreateViewShouldFail() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE duplicate_view_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE duplicate_view_db;"
            );

            queryExecutor.execute(
                    "CREATE VIEW adults AS SELECT * FROM users;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "CREATE VIEW adults AS SELECT id FROM users;"
                            )
                    );

            assertTrue(
                    exception.getMessage()
                            .contains("View already exists: adults")
            );
        }
    }
}
