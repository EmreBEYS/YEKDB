package com.yekdb.demo;

import com.yekdb.query.evaluator.*;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;

import java.util.List;

public class RowWhereEvaluatorDemo {

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

        Row yunusRow = new Row(
                List.of(
                        1,
                        "Yunus Emre",
                        21,
                        "Malatya",
                        true
                )
        );

        Row aliRow = new Row(
                List.of(
                        2,
                        "Ali",
                        16,
                        "Ankara",
                        true
                )
        );

        Expression ageExpression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        Expression cityExpression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Malatya"
                );

        Expression whereExpression =
                new LogicalExpression(
                        ageExpression,
                        LogicalOperator.AND,
                        cityExpression
                );

        boolean yunusResult = WhereEvaluator.evaluate(
                whereExpression,
                yunusRow,
                usersTable
        );

        boolean aliResult = WhereEvaluator.evaluate(
                whereExpression,
                aliRow,
                usersTable
        );

        System.out.println(
                "WHERE age > 18 AND city = 'Malatya'"
        );

        System.out.println();

        System.out.println(
                "Yunus Emre satırı eşleşti: "
                        + yunusResult
        );

        System.out.println(
                "Ali satırı eşleşti: "
                        + aliResult
        );
    }
}