package com.yekdb.query.executor;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TableScanExecutor sınıfının birim testleri.
 */
class TableScanExecutorTest {

    private Table usersTable;
    private List<Row> users;

    @BeforeEach
    void setUp() {
        usersTable = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING),
                        new Column("age", DataType.INT),
                        new Column("city", DataType.STRING),
                        new Column("active", DataType.BOOLEAN)
                )
        );

        users = List.of(
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
    }

    @Test
    void executeWithoutWhere_shouldReturnAllRows() {
        QueryResult result =
                TableScanExecutor.execute(
                        usersTable,
                        users,
                        null
                );

        assertTrue(result.isSuccess());
        assertTrue(result.hasRows());

        assertEquals(
                4,
                result.getRows().size()
        );

        assertEquals(
                4,
                result.getAffectedRowCount()
        );

        assertEquals(
                "Yunus Emre",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );
    }

    @Test
    void executeWithComparison_shouldReturnMatchingRows() {
        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        QueryResult result =
                TableScanExecutor.execute(
                        usersTable,
                        users,
                        expression
                );

        assertTrue(result.isSuccess());

        assertEquals(
                3,
                result.getRows().size()
        );

        assertEquals(
                "Yunus Emre",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );

        assertEquals(
                "Ayşe",
                result.getRows()
                        .get(1)
                        .getValue(1)
        );

        assertEquals(
                "Mehmet",
                result.getRows()
                        .get(2)
                        .getValue(1)
        );
    }

    @Test
    void executeWithAndExpression_shouldReturnMatchingRows() {
        Expression expression =
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
                        users,
                        expression
                );

        assertEquals(
                2,
                result.getRows().size()
        );

        assertEquals(
                "Yunus Emre",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );

        assertEquals(
                "Ayşe",
                result.getRows()
                        .get(1)
                        .getValue(1)
        );
    }

    @Test
    void executeWithOrExpression_shouldReturnMatchingRows() {
        Expression expression =
                new LogicalExpression(
                        new ComparisonExpression(
                                "city",
                                ComparisonOperator.EQUALS,
                                "Ankara"
                        ),
                        LogicalOperator.OR,
                        new ComparisonExpression(
                                "city",
                                ComparisonOperator.EQUALS,
                                "İstanbul"
                        )
                );

        QueryResult result =
                TableScanExecutor.execute(
                        usersTable,
                        users,
                        expression
                );

        assertEquals(
                2,
                result.getRows().size()
        );

        assertEquals(
                "Ali",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );

        assertEquals(
                "Mehmet",
                result.getRows()
                        .get(1)
                        .getValue(1)
        );
    }

    @Test
    void executeWithNoMatches_shouldReturnEmptyResult() {
        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        100
                );

        QueryResult result =
                TableScanExecutor.execute(
                        usersTable,
                        users,
                        expression
                );

        assertTrue(result.isSuccess());
        assertFalse(result.hasRows());

        assertEquals(
                0,
                result.getRows().size()
        );

        assertEquals(
                0,
                result.getAffectedRowCount()
        );
    }

    @Test
    void executeWithEmptyRowList_shouldReturnEmptyResult() {
        QueryResult result =
                TableScanExecutor.execute(
                        usersTable,
                        List.of(),
                        null
                );

        assertTrue(result.isSuccess());
        assertFalse(result.hasRows());

        assertEquals(
                0,
                result.getRows().size()
        );
    }

    @Test
    void execute_shouldReturnTableColumns() {
        QueryResult result =
                TableScanExecutor.execute(
                        usersTable,
                        users,
                        null
                );

        assertTrue(result.hasColumns());

        assertEquals(
                5,
                result.getColumns().size()
        );

        assertEquals(
                "id",
                result.getColumns()
                        .get(0)
                        .getName()
        );

        assertEquals(
                "active",
                result.getColumns()
                        .get(4)
                        .getName()
        );
    }

    @Test
    void execute_shouldMeasureExecutionTime() {
        QueryResult result =
                TableScanExecutor.execute(
                        usersTable,
                        users,
                        null
                );

        assertTrue(
                result.getExecutionTimeNanos() >= 0
        );

        assertTrue(
                result.getExecutionTimeMillis() >= 0
        );
    }

    @Test
    void execute_shouldRejectNullTable() {
        assertThrows(
                NullPointerException.class,
                () -> TableScanExecutor.execute(
                        null,
                        users,
                        null
                )
        );
    }

    @Test
    void execute_shouldRejectNullRowList() {
        assertThrows(
                NullPointerException.class,
                () -> TableScanExecutor.execute(
                        usersTable,
                        null,
                        null
                )
        );
    }

    @Test
    void execute_shouldRejectNullRowInsideList() {
        List<Row> invalidRows =
                new ArrayList<>();

        invalidRows.add(users.get(0));
        invalidRows.add(null);
        invalidRows.add(users.get(1));

        assertThrows(
                IllegalArgumentException.class,
                () -> TableScanExecutor.execute(
                        usersTable,
                        invalidRows,
                        null
                )
        );
    }

    @Test
    void returnedRows_shouldBeImmutable() {
        QueryResult result =
                TableScanExecutor.execute(
                        usersTable,
                        users,
                        null
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.getRows().clear()
        );
    }
}