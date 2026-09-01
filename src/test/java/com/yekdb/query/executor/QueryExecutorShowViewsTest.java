package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorShowViewsTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void showViewsShouldReturnViewMetadataRows() {
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

            createDatabaseAndTable(queryExecutor, "show_views_db");

            queryExecutor.execute(
                    """
                    CREATE VIEW adult_users AS
                    SELECT id, name FROM users WHERE age >= 18;
                    """
            );

            queryExecutor.execute(
                    """
                    CREATE VIEW young_users AS
                    SELECT id, name FROM users WHERE age < 18;
                    """
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "SHOW VIEWS;"
                    );

            assertTrue(result.isSuccess());
            assertEquals(4, result.getColumnCount());
            assertEquals(2, result.getRowCount());

            assertEquals("view_name", result.getColumns().get(0).getName());
            assertEquals("source_select", result.getColumns().get(1).getName());
            assertEquals("adult_users", result.getRows().get(0).getValue(0));
            assertEquals(
                    "SELECT id, name FROM users WHERE age >= 18",
                    result.getRows().get(0).getValue(1)
            );
            assertEquals(1, result.getRows().get(0).getValue(2));
            assertEquals("young_users", result.getRows().get(1).getValue(0));
        }
    }

    @Test
    void showViewsShouldReturnEmptyRowsWhenNoViewsExist() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE empty_views_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE empty_views_db;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "SHOW VIEWS;"
                    );

            assertTrue(result.isSuccess());
            assertEquals(4, result.getColumnCount());
            assertEquals(0, result.getRowCount());
        }
    }

    @Test
    void showViewsShouldRequireCurrentDatabase() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            assertThrows(
                    QueryExecutionException.class,
                    () -> queryExecutor.execute(
                            "SHOW VIEWS;"
                    )
            );
        }
    }

    private void createDatabaseAndTable(
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
    }
}
