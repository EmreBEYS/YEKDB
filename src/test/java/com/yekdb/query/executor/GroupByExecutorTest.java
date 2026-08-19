package com.yekdb.query.executor;

import com.yekdb.query.statement.GroupByClause;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GroupByExecutorTest {

    private GroupByExecutor executor;
    private List<Column> columns;
    private List<Row> rows;

    @BeforeEach
    void setUp() {

        executor =
                new GroupByExecutor();

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
                                "department",
                                DataType.STRING
                        ),
                        new Column(
                                "city",
                                DataType.STRING
                        )
                );

        rows =
                List.of(
                        new Row(
                                List.of(
                                        1,
                                        "Yunus",
                                        "IT",
                                        "Malatya"
                                )
                        ),
                        new Row(
                                List.of(
                                        2,
                                        "Ali",
                                        "HR",
                                        "Ankara"
                                )
                        ),
                        new Row(
                                List.of(
                                        3,
                                        "Mehmet",
                                        "IT",
                                        "Malatya"
                                )
                        ),
                        new Row(
                                List.of(
                                        4,
                                        "Ayşe",
                                        "IT",
                                        "Ankara"
                                )
                        ),
                        new Row(
                                List.of(
                                        5,
                                        "Efe",
                                        "HR",
                                        "Ankara"
                                )
                        )
                );
    }

    @Test
    void shouldGroupBySingleColumn() {

        Map<List<Object>, List<Row>> groups =
                executor.execute(
                        rows,
                        columns,
                        new GroupByClause(
                                "department"
                        )
                );

        assertEquals(
                2,
                groups.size()
        );

        assertEquals(
                3,
                groups.get(
                        List.of(
                                "IT"
                        )
                ).size()
        );

        assertEquals(
                2,
                groups.get(
                        List.of(
                                "HR"
                        )
                ).size()
        );
    }

    @Test
    void shouldGroupByMultipleColumns() {

        Map<List<Object>, List<Row>> groups =
                executor.execute(
                        rows,
                        columns,
                        new GroupByClause(
                                List.of(
                                        "department",
                                        "city"
                                )
                        )
                );

        assertEquals(
                3,
                groups.size()
        );

        assertEquals(
                2,
                groups.get(
                        List.of(
                                "IT",
                                "Malatya"
                        )
                ).size()
        );

        assertEquals(
                1,
                groups.get(
                        List.of(
                                "IT",
                                "Ankara"
                        )
                ).size()
        );

        assertEquals(
                2,
                groups.get(
                        List.of(
                                "HR",
                                "Ankara"
                        )
                ).size()
        );
    }

    @Test
    void shouldSupportQualifiedColumn() {

        Map<List<Object>, List<Row>> groups =
                executor.execute(
                        rows,
                        columns,
                        new GroupByClause(
                                "users.department"
                        )
                );

        assertEquals(
                2,
                groups.size()
        );
    }

    @Test
    void shouldRejectUnknownColumn() {

        assertThrows(
                IllegalArgumentException.class,
                () -> executor.execute(
                        rows,
                        columns,
                        new GroupByClause(
                                "salary"
                        )
                )
        );
    }

    @Test
    void shouldRejectNullRows() {

        assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        null,
                        columns,
                        new GroupByClause(
                                "department"
                        )
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
                        new GroupByClause(
                                "department"
                        )
                )
        );
    }

    @Test
    void shouldRejectNullGroupByClause() {

        assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        rows,
                        columns,
                        null
                )
        );
    }
}