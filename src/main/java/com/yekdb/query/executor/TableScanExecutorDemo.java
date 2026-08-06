package com.yekdb.query.executor;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;

import java.util.List;

public class TableScanExecutorDemo {

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

        QueryResult result =
                TableScanExecutor.execute(
                        usersTable,
                        rows,
                        whereExpression
                );

        System.out.println(
                "WHERE age > 18 AND city = 'Malatya'"
        );

        System.out.println();

        for (Row row : result.getRows()) {
            System.out.println(row);
        }

        System.out.println();
        System.out.println(
                "Bulunan satır sayısı: "
                        + result.getAffectedRowCount()
        );

        System.out.println(
                "Çalışma süresi: "
                        + result.getExecutionTimeMillis()
                        + " ms"
        );
    }
}