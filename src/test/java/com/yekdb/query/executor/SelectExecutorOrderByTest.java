package com.yekdb.query.executor;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.result.QueryResult;
import com.yekdb.query.statement.OrderByItem;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.SortDirection;
import com.yekdb.query.statement.TableReference;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelectExecutorOrderByTest {

    @Test
    void shouldApplyWhereBeforeOrderBy() {

        Table table =
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING),
                                new Column("age", DataType.INT),
                                new Column("city", DataType.STRING)
                        )
                );

        List<Row> rows =
                List.of(
                        new Row(
                                List.of(
                                        1,
                                        "Yunus",
                                        21,
                                        "Malatya"
                                )
                        ),
                        new Row(
                                List.of(
                                        2,
                                        "Ali",
                                        16,
                                        "Ankara"
                                )
                        ),
                        new Row(
                                List.of(
                                        3,
                                        "Ayşe",
                                        27,
                                        "Malatya"
                                )
                        ),
                        new Row(
                                List.of(
                                        4,
                                        "Mehmet",
                                        35,
                                        "İstanbul"
                                )
                        ),
                        new Row(
                                List.of(
                                        5,
                                        "Efe",
                                        24,
                                        "Malatya"
                                )
                        )
                );

        ComparisonExpression whereExpression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Malatya"
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference("users"),
                        List.of(
                                new SelectItem("*")
                        ),
                        whereExpression,
                        List.of(
                                new OrderByItem(
                                        "age",
                                        SortDirection.DESC
                                )
                        )
                );

        SelectExecutor executor =
                new SelectExecutor();

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                3,
                result.getRows().size()
        );

        assertEquals(
                27,
                result.getRows()
                        .get(0)
                        .getValue(2)
        );

        assertEquals(
                24,
                result.getRows()
                        .get(1)
                        .getValue(2)
        );

        assertEquals(
                21,
                result.getRows()
                        .get(2)
                        .getValue(2)
        );
    }

    @Test
    void shouldApplyOrderByWithoutWhere() {

        Table table =
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING),
                                new Column("age", DataType.INT)
                        )
                );

        List<Row> rows =
                List.of(
                        new Row(
                                List.of(
                                        1,
                                        "Yunus",
                                        21
                                )
                        ),
                        new Row(
                                List.of(
                                        2,
                                        "Ali",
                                        30
                                )
                        ),
                        new Row(
                                List.of(
                                        3,
                                        "Ayşe",
                                        19
                                )
                        )
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference("users"),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        List.of(
                                new OrderByItem(
                                        "age",
                                        SortDirection.ASC
                                )
                        )
                );

        SelectExecutor executor =
                new SelectExecutor();

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                19,
                result.getRows()
                        .get(0)
                        .getValue(2)
        );

        assertEquals(
                21,
                result.getRows()
                        .get(1)
                        .getValue(2)
        );

        assertEquals(
                30,
                result.getRows()
                        .get(2)
                        .getValue(2)
        );
    }

    @Test
    void shouldReturnFilteredRowsWithoutOrderBy() {

        Table table =
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("age", DataType.INT)
                        )
                );

        List<Row> rows =
                List.of(
                        new Row(
                                List.of(
                                        1,
                                        18
                                )
                        ),
                        new Row(
                                List.of(
                                        2,
                                        25
                                )
                        ),
                        new Row(
                                List.of(
                                        3,
                                        30
                                )
                        )
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference("users"),
                        List.of(
                                new SelectItem("*")
                        ),
                        new ComparisonExpression(
                                "age",
                                ComparisonOperator.GREATER_THAN,
                                20
                        ),
                        List.of()
                );

        SelectExecutor executor =
                new SelectExecutor();

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                2,
                result.getRows().size()
        );
    }
}