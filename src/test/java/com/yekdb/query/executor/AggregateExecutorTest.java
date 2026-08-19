package com.yekdb.query.executor;

import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AggregateExecutorTest {

    private AggregateExecutor executor;
    private List<Column> columns;
    private List<Row> rows;

    @BeforeEach
    void setUp() {

        executor =
                new AggregateExecutor();

        columns =
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
                                "salary",
                                DataType.DOUBLE
                        ),
                        new Column(
                                "age",
                                DataType.INT
                        )
                );

        rows =
                List.of(
                        new Row(
                                List.of(
                                        1,
                                        "Yunus",
                                        30000.0,
                                        21
                                )
                        ),
                        new Row(
                                List.of(
                                        2,
                                        "Ali",
                                        45000.0,
                                        30
                                )
                        ),
                        new Row(
                                List.of(
                                        3,
                                        "Ayşe",
                                        55000.0,
                                        27
                                )
                        ),
                        new Row(
                                List.of(
                                        4,
                                        "Mehmet",
                                        70000.0,
                                        35
                                )
                        )
                );
    }

    @Test
    void shouldCountAllRows() {

        Object result =
                executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.COUNT,
                        "*"
                );

        assertEquals(
                4L,
                result
        );
    }

    @Test
    void shouldCountColumn() {

        Object result =
                executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.COUNT,
                        "name"
                );

        assertEquals(
                4L,
                result
        );
    }

    @Test
    void shouldCalculateSum() {

        Object result =
                executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.SUM,
                        "salary"
                );

        assertEquals(
                200000.0,
                (Double) result,
                0.0001
        );
    }

    @Test
    void shouldCalculateAverage() {

        Object result =
                executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.AVG,
                        "salary"
                );

        assertEquals(
                50000.0,
                (Double) result,
                0.0001
        );
    }

    @Test
    void shouldCalculateMinimumNumber() {

        Object result =
                executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.MIN,
                        "age"
                );

        assertEquals(
                21,
                result
        );
    }

    @Test
    void shouldCalculateMaximumNumber() {

        Object result =
                executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.MAX,
                        "age"
                );

        assertEquals(
                35,
                result
        );
    }

    @Test
    void shouldCalculateMinimumString() {

        Object result =
                executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.MIN,
                        "name"
                );

        assertEquals(
                "Ali",
                result
        );
    }

    @Test
    void shouldCalculateMaximumString() {

        Object result =
                executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.MAX,
                        "name"
                );

        assertEquals(
                "Yunus",
                result
        );
    }

    @Test
    void shouldSupportQualifiedColumnName() {

        Object result =
                executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.MAX,
                        "users.age"
                );

        assertEquals(
                35,
                result
        );
    }

    @Test
    void shouldRejectUnknownColumn() {

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.SUM,
                        "unknown"
                )
        );
    }

    @Test
    void shouldRejectSumOnNonNumericColumn() {

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.SUM,
                        "name"
                )
        );
    }

    @Test
    void shouldRejectAverageOnNonNumericColumn() {

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.AVG,
                        "name"
                )
        );
    }

    @Test
    void shouldReturnZeroForAverageOfEmptyRows() {

        Object result =
                executor.execute(
                        List.of(),
                        columns,
                        AggregateExecutor.AggregateFunction.AVG,
                        "salary"
                );

        assertEquals(
                0.0,
                result
        );
    }

    @Test
    void shouldReturnNullForMinimumOfEmptyRows() {

        Object result =
                executor.execute(
                        List.of(),
                        columns,
                        AggregateExecutor.AggregateFunction.MIN,
                        "salary"
                );

        assertNull(
                result
        );
    }

    @Test
    void shouldReturnNullForMaximumOfEmptyRows() {

        Object result =
                executor.execute(
                        List.of(),
                        columns,
                        AggregateExecutor.AggregateFunction.MAX,
                        "salary"
                );

        assertNull(
                result
        );
    }

    @Test
    void shouldReturnZeroForSumOfEmptyRows() {

        Object result =
                executor.execute(
                        List.of(),
                        columns,
                        AggregateExecutor.AggregateFunction.SUM,
                        "salary"
                );

        assertEquals(
                0.0,
                result
        );
    }

    @Test
    void shouldCountEmptyRowsAsZero() {

        Object result =
                executor.execute(
                        List.of(),
                        columns,
                        AggregateExecutor.AggregateFunction.COUNT,
                        "*"
                );

        assertEquals(
                0L,
                result
        );
    }

    @Test
    void shouldRejectNullRows() {

        assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        null,
                        columns,
                        AggregateExecutor.AggregateFunction.COUNT,
                        "*"
                )
        );
    }

    @Test
    void shouldRejectNullColumns() {

        assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        rows,
                        null,
                        AggregateExecutor.AggregateFunction.COUNT,
                        "*"
                )
        );
    }

    @Test
    void shouldRejectNullFunction() {

        assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        rows,
                        columns,
                        null,
                        "age"
                )
        );
    }

    @Test
    void shouldRejectBlankColumnName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        rows,
                        columns,
                        AggregateExecutor.AggregateFunction.SUM,
                        " "
                )
        );
    }
}