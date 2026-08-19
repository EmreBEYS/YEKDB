package com.yekdb.query.executor;

import com.yekdb.query.statement.OrderByItem;
import com.yekdb.query.statement.SortDirection;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderByExecutorTest {

    private OrderByExecutor executor;
    private List<Column> columns;
    private List<Row> rows;

    @BeforeEach
    void setUp() {

        executor =
                new OrderByExecutor();

        columns =
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING),
                        new Column("age", DataType.INT),
                        new Column("department", DataType.STRING)
                );

        rows =
                List.of(
                        new Row(
                                List.of(
                                        1,
                                        "Yunus",
                                        21,
                                        "IT"
                                )
                        ),
                        new Row(
                                List.of(
                                        2,
                                        "Ali",
                                        30,
                                        "HR"
                                )
                        ),
                        new Row(
                                List.of(
                                        3,
                                        "Mehmet",
                                        25,
                                        "IT"
                                )
                        ),
                        new Row(
                                List.of(
                                        4,
                                        "Ayşe",
                                        19,
                                        "Finance"
                                )
                        )
                );
    }

    @Test
    void shouldSortAscending() {

        List<Row> result =
                executor.execute(
                        rows,
                        columns,
                        List.of(
                                new OrderByItem(
                                        "age",
                                        SortDirection.ASC
                                )
                        )
                );

        assertEquals(
                19,
                result.get(0)
                        .getValue(2)
        );

        assertEquals(
                21,
                result.get(1)
                        .getValue(2)
        );

        assertEquals(
                25,
                result.get(2)
                        .getValue(2)
        );

        assertEquals(
                30,
                result.get(3)
                        .getValue(2)
        );
    }

    @Test
    void shouldSortDescending() {

        List<Row> result =
                executor.execute(
                        rows,
                        columns,
                        List.of(
                                new OrderByItem(
                                        "age",
                                        SortDirection.DESC
                                )
                        )
                );

        assertEquals(
                30,
                result.get(0)
                        .getValue(2)
        );

        assertEquals(
                25,
                result.get(1)
                        .getValue(2)
        );

        assertEquals(
                21,
                result.get(2)
                        .getValue(2)
        );

        assertEquals(
                19,
                result.get(3)
                        .getValue(2)
        );
    }

    @Test
    void shouldSortStringsAscending() {

        List<Row> result =
                executor.execute(
                        rows,
                        columns,
                        List.of(
                                new OrderByItem(
                                        "name",
                                        SortDirection.ASC
                                )
                        )
                );

        assertEquals(
                "Ali",
                result.get(0)
                        .getValue(1)
        );
    }

    @Test
    void shouldSortByMultipleColumns() {

        List<Row> result =
                executor.execute(
                        rows,
                        columns,
                        List.of(
                                new OrderByItem(
                                        "department",
                                        SortDirection.ASC
                                ),
                                new OrderByItem(
                                        "age",
                                        SortDirection.DESC
                                )
                        )
                );

        assertEquals(
                "Finance",
                result.get(0)
                        .getValue(3)
        );

        assertEquals(
                "HR",
                result.get(1)
                        .getValue(3)
        );

        assertEquals(
                "IT",
                result.get(2)
                        .getValue(3)
        );

        assertEquals(
                25,
                result.get(2)
                        .getValue(2)
        );

        assertEquals(
                21,
                result.get(3)
                        .getValue(2)
        );
    }

    @Test
    void shouldReturnCopyWhenOrderByIsEmpty() {

        List<Row> result =
                executor.execute(
                        rows,
                        columns,
                        List.of()
                );

        assertEquals(
                rows,
                result
        );

        assertNotSame(
                rows,
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
                        List.of(
                                new OrderByItem(
                                        "salary",
                                        SortDirection.ASC
                                )
                        )
                )
        );
    }

    @Test
    void shouldNotModifyOriginalRows() {

        List<Row> original =
                List.copyOf(
                        rows
                );

        executor.execute(
                rows,
                columns,
                List.of(
                        new OrderByItem(
                                "age",
                                SortDirection.ASC
                        )
                )
        );

        assertEquals(
                original,
                rows
        );
    }
}