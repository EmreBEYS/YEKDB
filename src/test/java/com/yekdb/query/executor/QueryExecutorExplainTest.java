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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorExplainTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldExecuteExplainSelectAsPlanRows() {

        InMemoryQueryDataSource dataSource =
                createUsersDataSource();

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             new DatabaseManager(
                                     temporaryDirectory
                             ),
                             dataSource
                     )) {

            ExecuteResult result =
                    queryExecutor.execute(
                            "EXPLAIN SELECT id FROM users WHERE age > 18;"
                    );

            assertTrue(
                    result.isSuccess()
            );

            assertEquals(
                    1,
                    result.getColumnCount()
            );

            assertEquals(
                    "plan_step",
                    result.getColumns()
                            .get(0)
                            .getName()
            );

            assertTrue(
                    containsPlanLine(
                            result,
                            "PLAN: FULL_TABLE_SCAN"
                    )
            );
        }
    }

    @Test
    void shouldExecuteExplainSelectWithIndexPlan() {

        InMemoryQueryDataSource dataSource =
                createUsersDataSource();

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             new DatabaseManager(
                                     temporaryDirectory
                             ),
                             dataSource
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE explain_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE explain_db;"
            );

            queryExecutor.execute(
                    "CREATE TABLE users (id INT, name STRING, age INT);"
            );

            queryExecutor.execute(
                    "CREATE UNIQUE INDEX idx_users_id ON users (id);"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "EXPLAIN SELECT id FROM users WHERE id = 10 AND age > 18;"
                    );

            assertTrue(
                    containsPlanLine(
                            result,
                            "PLAN: INDEX_SCAN"
                    )
            );

            assertTrue(
                    containsPlanLine(
                            result,
                            "INDEX: idx_users_id"
                    )
            );

            assertTrue(
                    containsPlanLine(
                            result,
                            "RESIDUAL_PREDICATE: age GREATER_THAN 18"
                    )
            );
        }
    }


    @Test
    void shouldExplainSelectWithoutWhereAsFullTableScan() {

        InMemoryQueryDataSource dataSource =
                createUsersDataSource();

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             new DatabaseManager(
                                     temporaryDirectory
                             ),
                             dataSource
                     )) {

            ExecuteResult result =
                    queryExecutor.execute(
                            "EXPLAIN SELECT id FROM users;"
                    );

            assertTrue(result.isSuccess());
            assertTrue(
                    containsPlanLine(
                            result,
                            "PLAN: FULL_TABLE_SCAN"
                    )
            );
            assertTrue(
                    containsPlanLine(
                            result,
                            "ORIGINAL_WHERE: NONE"
                    )
            );
            assertTrue(
                    containsPlanLine(
                            result,
                            "OPTIMIZED_WHERE: NONE"
                    )
            );
            assertTrue(
                    containsPlanLine(
                            result,
                            "ACCESS_PREDICATE: NONE"
                    )
            );
            assertTrue(
                    containsPlanLine(
                            result,
                            "RESIDUAL_PREDICATE: NONE"
                    )
            );
        }
    }

    @Test
    void shouldKeepFullTableScanWhenAvailableIndexDoesNotMatchWhereColumn() {

        InMemoryQueryDataSource dataSource =
                createUsersDataSource();

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             new DatabaseManager(
                                     temporaryDirectory
                             ),
                             dataSource
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE explain_non_matching_index_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE explain_non_matching_index_db;"
            );

            queryExecutor.execute(
                    "CREATE TABLE users (id INT, name STRING, age INT);"
            );

            queryExecutor.execute(
                    "CREATE UNIQUE INDEX idx_users_id ON users (id);"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "EXPLAIN SELECT id FROM users WHERE age > 18;"
                    );

            assertTrue(result.isSuccess());
            assertTrue(
                    containsPlanLine(
                            result,
                            "PLAN: FULL_TABLE_SCAN"
                    )
            );
            assertTrue(
                    containsPlanLine(
                            result,
                            "INDEX: NONE"
                    )
            );
            assertTrue(
                    containsPlanLine(
                            result,
                            "ACCESS_PREDICATE: NONE"
                    )
            );
            assertTrue(
                    containsPlanLine(
                            result,
                            "RESIDUAL_PREDICATE: age GREATER_THAN 18"
                    )
            );
        }
    }

    @Test
    void explainShouldNotChangeNormalSelectExecution() {

        InMemoryQueryDataSource dataSource =
                createUsersDataSource();

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             new DatabaseManager(
                                     temporaryDirectory
                             ),
                             dataSource
                     )) {

            ExecuteResult beforeExplain =
                    queryExecutor.execute(
                            "SELECT id FROM users WHERE age > 18;"
                    );

            ExecuteResult explainResult =
                    queryExecutor.execute(
                            "EXPLAIN SELECT id FROM users WHERE age > 18;"
                    );

            ExecuteResult afterExplain =
                    queryExecutor.execute(
                            "SELECT id FROM users WHERE age > 18;"
                    );

            assertTrue(explainResult.isSuccess());
            assertEquals(
                    beforeExplain.getRows(),
                    afterExplain.getRows()
            );
            assertEquals(
                    1,
                    afterExplain.getRows().size()
            );
            assertEquals(
                    20,
                    afterExplain.getRows().get(0).getValue(0)
            );
        }
    }

    private InMemoryQueryDataSource createUsersDataSource() {

        InMemoryQueryDataSource dataSource =
                new InMemoryQueryDataSource();

        dataSource.register(
                new Table(
                        "users",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INT
                                ),
                                new Column(
                                        "name",
                                        DataType.STRING
                                ),
                                new Column(
                                        "age",
                                        DataType.INT
                                )
                        )
                ),
                List.of(
                        new Row(
                                List.of(
                                        10,
                                        "Ali",
                                        18
                                )
                        ),
                        new Row(
                                List.of(
                                        20,
                                        "Ayse",
                                        22
                                )
                        )
                )
        );

        return dataSource;
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
