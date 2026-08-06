package com.yekdb.query.executor;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.optimizer.QueryOptimizer;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SelectExecutor sınıfının birim testleri.
 */
class SelectExecutorTest {

    private SelectExecutor selectExecutor;
    private Table usersTable;
    private List<Row> users;

    @BeforeEach
    void setUp() {
        selectExecutor = new SelectExecutor();

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
        QueryResult result = selectExecutor.execute(
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
    }

    @Test
    void executeWithComparison_shouldReturnMatchingRows() {
        Expression whereExpression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        QueryResult result = selectExecutor.execute(
                usersTable,
                users,
                whereExpression
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

        QueryResult result = selectExecutor.execute(
                usersTable,
                users,
                whereExpression
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
        Expression whereExpression =
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

        QueryResult result = selectExecutor.execute(
                usersTable,
                users,
                whereExpression
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
        Expression whereExpression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        100
                );

        QueryResult result = selectExecutor.execute(
                usersTable,
                users,
                whereExpression
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
    void executeWithEmptyRows_shouldReturnEmptyResult() {
        QueryResult result = selectExecutor.execute(
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
        QueryResult result = selectExecutor.execute(
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
        QueryResult result = selectExecutor.execute(
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
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> selectExecutor.execute(
                                null,
                                users,
                                null
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void execute_shouldRejectNullRows() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> selectExecutor.execute(
                                usersTable,
                                null,
                                null
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void execute_shouldRejectNullRowInsideList() {
        List<Row> invalidRows = new ArrayList<>();

        invalidRows.add(users.get(0));
        invalidRows.add(null);
        invalidRows.add(users.get(1));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> selectExecutor.execute(
                                usersTable,
                                invalidRows,
                                null
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void constructor_shouldRejectNullQueryOptimizer() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new SelectExecutor(null)
                );

        assertMessageExists(exception);
    }

    @Test
    void constructorWithOptimizer_shouldCreateExecutorSuccessfully() {
        SelectExecutor executor =
                new SelectExecutor(
                        new QueryOptimizer()
                );

        assertNotNull(executor);

        QueryResult result = executor.execute(
                usersTable,
                users,
                null
        );

        assertEquals(
                4,
                result.getRows().size()
        );
    }

    @Test
    void returnedRows_shouldBeImmutable() {
        QueryResult result = selectExecutor.execute(
                usersTable,
                users,
                null
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result.getRows().clear()
        );
    }

    private static void assertMessageExists(
            Throwable exception
    ) {
        assertNotNull(exception);
        assertNotNull(exception.getMessage());

        assertFalse(
                exception.getMessage().isBlank()
        );
    }
}