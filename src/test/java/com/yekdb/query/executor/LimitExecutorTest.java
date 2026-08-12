package com.yekdb.query.executor;

import com.yekdb.query.statement.FetchClause;
import com.yekdb.query.statement.LimitClause;
import com.yekdb.storage.record.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LimitExecutorTest {

    private LimitExecutor executor;
    private List<Row> rows;

    @BeforeEach
    void setUp() {

        executor =
                new LimitExecutor();

        rows =
                List.of(
                        new Row(List.of(1, "A")),
                        new Row(List.of(2, "B")),
                        new Row(List.of(3, "C")),
                        new Row(List.of(4, "D")),
                        new Row(List.of(5, "E"))
                );
    }

    @Test
    void shouldApplyLimit() {

        List<Row> result =
                executor.execute(
                        rows,
                        new LimitClause(3)
                );

        assertEquals(
                3,
                result.size()
        );

        assertEquals(
                1,
                result.get(0)
                        .getValue(0)
        );

        assertEquals(
                3,
                result.get(2)
                        .getValue(0)
        );
    }

    @Test
    void shouldApplyFetchFirst() {

        List<Row> result =
                executor.execute(
                        rows,
                        new FetchClause(
                                FetchClause.Mode.FIRST,
                                2
                        )
                );

        assertEquals(
                2,
                result.size()
        );

        assertEquals(
                1,
                result.get(0)
                        .getValue(0)
        );

        assertEquals(
                2,
                result.get(1)
                        .getValue(0)
        );
    }

    @Test
    void shouldApplyFetchNext() {

        List<Row> result =
                executor.execute(
                        rows,
                        new FetchClause(
                                FetchClause.Mode.NEXT,
                                3
                        )
                );

        assertEquals(
                3,
                result.size()
        );
    }

    @Test
    void shouldReturnEmptyListForLimitZero() {

        List<Row> result =
                executor.execute(
                        rows,
                        new LimitClause(0)
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldReturnEmptyListForFetchZero() {

        List<Row> result =
                executor.execute(
                        rows,
                        new FetchClause(
                                FetchClause.Mode.FIRST,
                                0
                        )
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void shouldReturnAllRowsWhenLimitIsGreaterThanRowCount() {

        List<Row> result =
                executor.execute(
                        rows,
                        new LimitClause(100)
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
    void shouldReturnAllRowsWhenFetchIsGreaterThanRowCount() {

        List<Row> result =
                executor.execute(
                        rows,
                        new FetchClause(
                                FetchClause.Mode.FIRST,
                                100
                        )
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
    void shouldNotModifyOriginalRows() {

        List<Row> original =
                List.copyOf(
                        rows
                );

        executor.execute(
                rows,
                new LimitClause(2)
        );

        assertEquals(
                original,
                rows
        );
    }

    @Test
    void shouldRejectNullRows() {

        assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        null,
                        new LimitClause(2)
                )
        );
    }

    @Test
    void shouldRejectNullLimitClause() {

        assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        rows,
                        (LimitClause) null
                )
        );
    }

    @Test
    void shouldRejectNullFetchClause() {

        assertThrows(
                NullPointerException.class,
                () -> executor.execute(
                        rows,
                        (FetchClause) null
                )
        );
    }
}