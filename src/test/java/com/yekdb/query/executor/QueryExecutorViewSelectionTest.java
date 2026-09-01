package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.InMemoryQueryDataSource;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorViewSelectionTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void selectAllFromViewShouldExecuteUnderlyingSelect() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        InMemoryQueryDataSource dataSource =
                createUsersDataSource();

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             dataSource
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE view_select_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE view_select_db;"
            );

            queryExecutor.execute(
                    "CREATE VIEW adults AS " +
                            "SELECT id, name, age FROM users WHERE age >= 18;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "SELECT * FROM adults;"
                    );

            assertTrue(result.isSuccess());
            assertEquals(2, result.getRowCount());
            assertEquals("Emre", result.getRows().get(0).getValue(1));
            assertEquals("Ayse", result.getRows().get(1).getValue(1));
        }
    }

    @Test
    void selectColumnsFromViewShouldApplyOuterProjection() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        InMemoryQueryDataSource dataSource =
                createUsersDataSource();

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             dataSource
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE view_projection_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE view_projection_db;"
            );

            queryExecutor.execute(
                    "CREATE VIEW adults AS " +
                            "SELECT id, name, age FROM users WHERE age >= 18;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "SELECT name FROM adults WHERE age > 25;"
                    );

            assertTrue(result.isSuccess());
            assertEquals(1, result.getRowCount());
            assertEquals(1, result.getColumnCount());
            assertEquals("Ayse", result.getRows().get(0).getValue(0));
        }
    }

    @Test
    void dropViewShouldRemoveViewFromCatalog() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE drop_view_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE drop_view_db;"
            );

            queryExecutor.execute(
                    "CREATE VIEW adults AS SELECT * FROM users;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "DROP VIEW adults;"
                    );

            assertTrue(result.isSuccess());
            assertFalse(
                    databaseManager
                            .getCurrentDatabase()
                            .getViewCatalog()
                            .containsView("adults")
            );
        }
    }

    @Test
    void selectRecursiveViewShouldFail() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        InMemoryQueryDataSource dataSource =
                createUsersDataSource();

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             dataSource
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE recursive_view_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE recursive_view_db;"
            );

            queryExecutor.execute(
                    "CREATE VIEW loop_view AS SELECT * FROM loop_view;"
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "SELECT * FROM loop_view;"
                            )
                    );

            assertTrue(
                    exception.getMessage()
                            .contains("Recursive view reference detected")
            );
        }
    }

    private InMemoryQueryDataSource createUsersDataSource() {
        InMemoryQueryDataSource dataSource =
                new InMemoryQueryDataSource();

        Table users =
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING),
                                new Column("age", DataType.INT)
                        )
                );

        dataSource.register(
                users,
                List.of(
                        new Row(List.of(1, "Emre", 21)),
                        new Row(List.of(2, "Ali", 17)),
                        new Row(List.of(3, "Ayse", 30))
                )
        );

        return dataSource;
    }
}
