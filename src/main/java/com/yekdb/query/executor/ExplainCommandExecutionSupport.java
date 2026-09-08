package com.yekdb.query.executor;

import com.yekdb.index.Index;
import com.yekdb.query.command.ExplainCommand;
import com.yekdb.query.datasource.QueryDataSource;
import com.yekdb.query.optimizer.OptimizationContext;
import com.yekdb.query.optimizer.OptimizedQuery;
import com.yekdb.query.optimizer.QueryOptimizer;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * EXPLAIN SELECT komutlari icin optimizer plan satirlarini uretir.
 */
final class ExplainCommandExecutionSupport {

    private final QueryOptimizer queryOptimizer;

    ExplainCommandExecutionSupport() {
        this(
                new QueryOptimizer()
        );
    }

    ExplainCommandExecutionSupport(
            QueryOptimizer queryOptimizer
    ) {

        this.queryOptimizer =
                Objects.requireNonNull(
                        queryOptimizer,
                        "QueryOptimizer cannot be null."
                );
    }

    ExecuteResult execute(
            ExplainCommand command,
            QueryDataSource dataSource,
            List<Index<?>> availableIndexes
    ) {

        Objects.requireNonNull(
                command,
                "ExplainCommand cannot be null."
        );

        Objects.requireNonNull(
                dataSource,
                "QueryDataSource cannot be null."
        );

        List<Index<?>> indexes =
                availableIndexes == null
                        ? List.of()
                        : List.copyOf(
                                availableIndexes
                        );

        Table table =
                dataSource.getTable(
                        command.getSelectStatement()
                                .getTableName()
                );

        if (table == null) {
            throw new QueryExecutionException(
                    "QueryDataSource returned null table for EXPLAIN: "
                            + command.getSelectStatement()
                            .getTableName()
            );
        }

        OptimizedQuery optimizedQuery =
                queryOptimizer.optimizeQuery(
                        new OptimizationContext(
                                table,
                                command.getSelectStatement()
                                        .getWhereExpression(),
                                indexes
                        )
                );

        List<Row> rows =
                optimizedQuery.getExplainLines()
                        .stream()
                        .map(line ->
                                new Row(
                                        List.of(
                                                line
                                        )
                                )
                        )
                        .toList();

        return ExecuteResult.selectSuccess(
                "EXPLAIN query executed successfully.",
                List.of(
                        new Column(
                                "plan_step",
                                DataType.STRING
                        )
                ),
                rows
        );
    }

    ExecuteResult attachAnalysis(
            ExecuteResult planResult,
            ExecuteResult queryResult,
            long executionTimeNanos
    ) {

        Objects.requireNonNull(
                planResult,
                "Plan result cannot be null."
        );

        Objects.requireNonNull(
                queryResult,
                "Query result cannot be null."
        );

        if (executionTimeNanos < 0) {
            throw new IllegalArgumentException(
                    "Execution time cannot be negative."
            );
        }

        List<Row> rows =
                new ArrayList<>(
                        planResult.getRows()
                );

        rows.add(
                new Row(
                        List.of(
                                "ANALYZE: TRUE"
                        )
                )
        );

        rows.add(
                new Row(
                        List.of(
                                "ACTUAL_ROWS: "
                                        + queryResult.getRowCount()
                        )
                )
        );

        rows.add(
                new Row(
                        List.of(
                                "EXECUTION_TIME_NANOS: "
                                        + executionTimeNanos
                        )
                )
        );

        return ExecuteResult.selectSuccess(
                "EXPLAIN ANALYZE query executed successfully.",
                planResult.getColumns(),
                rows
        );
    }
}
