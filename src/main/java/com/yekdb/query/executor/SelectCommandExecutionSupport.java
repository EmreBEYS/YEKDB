package com.yekdb.query.executor;

import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.datasource.QueryDataSource;
import com.yekdb.query.result.QueryResult;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SelectCommand için veri kaynağı hazırlama ve SelectExecutor çağrılarını
 * QueryExecutor'dan ayıran yardımcı yürütme sınıfıdır.
 */
final class SelectCommandExecutionSupport {

    private final SelectExecutor selectExecutor;

    SelectCommandExecutionSupport(SelectExecutor selectExecutor) {
        this.selectExecutor = Objects.requireNonNull(
                selectExecutor,
                "SelectExecutor cannot be null."
        );
    }

    ExecuteResult execute(
            SelectCommand command,
            QueryDataSource dataSource
    ) {
        Objects.requireNonNull(
                command,
                "SelectCommand cannot be null."
        );
        Objects.requireNonNull(
                dataSource,
                "QueryDataSource cannot be null."
        );

        Table leftTable = requireTable(
                dataSource,
                command.getTableName(),
                false
        );

        List<Row> leftRows = requireRows(
                dataSource,
                command.getTableName(),
                false
        );

        SelectStatement statement = command.getStatement();
        QueryResult queryResult;

        if (!statement.hasJoins()) {
            queryResult = selectExecutor.executeStatement(
                    leftTable,
                    leftRows,
                    statement
            );
        } else {
            queryResult = executeJoinSelect(
                    dataSource,
                    leftTable,
                    leftRows,
                    statement
            );
        }

        if (queryResult == null) {
            throw new QueryExecutionException(
                    "SelectExecutor returned null QueryResult."
            );
        }

        String message =
                "SELECT query executed successfully. "
                        + "Returned row count: "
                        + queryResult.getRows().size()
                        + ", execution time: "
                        + queryResult.getExecutionTimeMillis()
                        + " ms";

        return ExecuteResult.success(
                message,
                queryResult.getRows()
        );
    }

    private QueryResult executeJoinSelect(
            QueryDataSource dataSource,
            Table leftTable,
            List<Row> leftRows,
            SelectStatement statement
    ) {
        List<Table> rightTables = new ArrayList<>();
        List<List<Row>> rightTableRows = new ArrayList<>();

        for (JoinClause joinClause : statement.getJoins()) {
            String rightTableName = joinClause.getTableName();

            rightTables.add(
                    requireTable(
                            dataSource,
                            rightTableName,
                            true
                    )
            );

            rightTableRows.add(
                    requireRows(
                            dataSource,
                            rightTableName,
                            true
                    )
            );
        }

        if (statement.getJoinCount() == 1) {
            return selectExecutor.executeStatement(
                    leftTable,
                    leftRows,
                    rightTables.get(0),
                    rightTableRows.get(0),
                    statement
            );
        }

        return selectExecutor.executeStatement(
                leftTable,
                leftRows,
                rightTables,
                rightTableRows,
                statement
        );
    }

    private Table requireTable(
            QueryDataSource dataSource,
            String tableName,
            boolean joinTable
    ) {
        Table table = dataSource.getTable(tableName);

        if (table != null) {
            return table;
        }

        String prefix = joinTable
                ? "QueryDataSource returned null JOIN table for: "
                : "QueryDataSource returned null table for: ";

        throw new QueryExecutionException(
                prefix + tableName
        );
    }

    private List<Row> requireRows(
            QueryDataSource dataSource,
            String tableName,
            boolean joinTable
    ) {
        List<Row> rows = dataSource.getRows(tableName);

        if (rows != null) {
            return rows;
        }

        String prefix = joinTable
                ? "QueryDataSource returned null row list for JOIN table: "
                : "QueryDataSource returned null row list for table: ";

        throw new QueryExecutionException(
                prefix + tableName
        );
    }
}
