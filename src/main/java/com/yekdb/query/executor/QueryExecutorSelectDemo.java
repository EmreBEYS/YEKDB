package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.datasource.InMemoryQueryDataSource;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;

import java.nio.file.Path;
import java.util.List;

public class QueryExecutorSelectDemo {

    public static void main(String[] args) {

        Table usersTable = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING),
                        new Column("age", DataType.INT),
                        new Column("city", DataType.STRING),
                        new Column("active", DataType.BOOLEAN)
                )
        );

        List<Row> rows = List.of(
                new Row(List.of(
                        1,
                        "Yunus Emre",
                        21,
                        "Malatya",
                        true
                )),
                new Row(List.of(
                        2,
                        "Ali",
                        16,
                        "Ankara",
                        true
                )),
                new Row(List.of(
                        3,
                        "Ayşe",
                        27,
                        "Malatya",
                        false
                )),
                new Row(List.of(
                        4,
                        "Mehmet",
                        35,
                        "İstanbul",
                        true
                ))
        );

        InMemoryQueryDataSource dataSource =
                new InMemoryQueryDataSource();

        dataSource.register(
                usersTable,
                rows
        );

        DatabaseManager databaseManager =
                new DatabaseManager(
                        Path.of("data")
                );

        QueryExecutor queryExecutor =
                new QueryExecutor(
                        databaseManager,
                        dataSource
                );

        Expression whereExpression =
                new LogicalExpression(
                        new ComparisonExpression(
                                "age",
                                ComparisonOperator.GREATER_THAN,
                                18
                        ),
                        LogicalOperator.AND,
                        new ComparisonExpression(
                                "city",
                                ComparisonOperator.EQUALS,
                                "Malatya"
                        )
                );

        SelectCommand filteredCommand =
                SelectCommand.allFromWhere(
                        "users",
                        whereExpression
                );

        ExecuteResult filteredResult =
                queryExecutor.execute(
                        filteredCommand
                );

        System.out.println(
                "=== QueryExecutor: Filtreli SELECT ==="
        );

        for (Row row : filteredResult.getRows()) {
            System.out.println(row);
        }

        System.out.println();
        System.out.println(filteredResult.getMessage());

        SelectCommand allRowsCommand =
                SelectCommand.allFrom("users");

        ExecuteResult allRowsResult =
                queryExecutor.execute(
                        allRowsCommand
                );

        System.out.println();
        System.out.println(
                "=== QueryExecutor: SELECT * ==="
        );

        for (Row row : allRowsResult.getRows()) {
            System.out.println(row);
        }

        System.out.println();
        System.out.println(allRowsResult.getMessage());
    }
}