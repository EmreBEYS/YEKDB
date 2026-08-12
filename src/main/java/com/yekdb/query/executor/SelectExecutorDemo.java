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

public class SelectExecutorDemo {

    public static void main(String[] args) {

        Table usersTable = new Table(
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
                        ),
                        new Column(
                                "city",
                                DataType.STRING
                        ),
                        new Column(
                                "active",
                                DataType.BOOLEAN
                        )
                )
        );

        List<Row> rows = List.of(

                new Row(
                        List.of(
                                1,
                                "Yunus Emre",
                                21,
                                "Malatya",
                                true
                        )
                ),

                new Row(
                        List.of(
                                2,
                                "Ali",
                                16,
                                "Ankara",
                                true
                        )
                ),

                new Row(
                        List.of(
                                3,
                                "Ayşe",
                                27,
                                "Malatya",
                                false
                        )
                ),

                new Row(
                        List.of(
                                4,
                                "Mehmet",
                                35,
                                "İstanbul",
                                true
                        )
                )
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

        SelectExecutor selectExecutor =
                new SelectExecutor();

        /*
         * WHERE içeren SELECT
         */
        QueryResult filteredResult =
                selectExecutor.execute(
                        usersTable,
                        rows,
                        whereExpression
                );

        /*
         * WHERE içermeyen SELECT
         *
         * Expression cast'i overload belirsizliğini
         * ortadan kaldırır.
         */
        QueryResult allRowsResult =
                selectExecutor.execute(
                        usersTable,
                        rows,
                        (Expression) null
                );

        System.out.println(
                "=== WHERE age > 18 AND city = 'Malatya' ==="
        );

        for (Row row : filteredResult.getRows()) {

            System.out.println(
                    row
            );
        }

        System.out.println();

        System.out.println(
                "Filtrelenen satır sayısı: "
                        + filteredResult
                        .getAffectedRowCount()
        );

        System.out.println(
                "Çalışma süresi: "
                        + filteredResult
                        .getExecutionTimeMillis()
                        + " ms"
        );

        System.out.println();

        System.out.println(
                "=== SELECT * FROM users ==="
        );

        for (Row row : allRowsResult.getRows()) {

            System.out.println(
                    row
            );
        }

        System.out.println();

        System.out.println(
                "Toplam satır sayısı: "
                        + allRowsResult
                        .getAffectedRowCount()
        );
    }
}