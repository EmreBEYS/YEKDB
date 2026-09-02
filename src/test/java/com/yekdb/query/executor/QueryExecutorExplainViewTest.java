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

import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorExplainViewTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void explainSelectFromViewShouldShowViewAndSourcePlan() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        InMemoryQueryDataSource dataSource =
                new InMemoryQueryDataSource();

        dataSource.register(
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING),
                                new Column("age", DataType.INT)
                        )
                ),
                List.of(
                        new Row(
                                List.of(1, "Emre", 21)
                        )
                )
        );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             dataSource
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE explain_view_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE explain_view_db;"
            );

            queryExecutor.execute(
                    "CREATE VIEW adult_users AS "
                            + "SELECT id, name FROM users WHERE age >= 18;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "EXPLAIN SELECT name FROM adult_users;"
                    );

            assertTrue(result.isSuccess());
            assertTrue(containsPlanLine(result, "VIEW: adult_users"));
            assertTrue(
                    containsPlanLine(
                            result,
                            "VIEW_SOURCE: SELECT id, name FROM users WHERE age >= 18"
                    )
            );
            assertTrue(
                    containsPlanLine(
                            result,
                            "PLAN: FULL_TABLE_SCAN"
                    )
            );
        }
    }

    private boolean containsPlanLine(
            ExecuteResult result,
            String expected
    ) {

        return result.getRows()
                .stream()
                .anyMatch(row ->
                        expected.equals(
                                row.getValue(0)
                        )
                );
    }
}
