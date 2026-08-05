package com.yekdb.query.executor;

import com.yekdb.storage.record.Row;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecuteResultTest {

    @Test
    void success_shouldCreateSuccessfulResultWithoutRows() {
        ExecuteResult result =
                ExecuteResult.success(
                        "Operation completed."
                );

        assertTrue(result.isSuccess());
        assertEquals(
                "Operation completed.",
                result.getMessage()
        );
        assertEquals(
                0,
                result.getAffectedRows()
        );
        assertEquals(
                0,
                result.getRowCount()
        );
        assertFalse(result.hasRows());
    }

    @Test
    void successWithAffectedRows_shouldStoreAffectedRowCount() {
        ExecuteResult result =
                ExecuteResult.success(
                        "1 row inserted.",
                        1
                );

        assertTrue(result.isSuccess());
        assertEquals(
                "1 row inserted.",
                result.getMessage()
        );
        assertEquals(
                1,
                result.getAffectedRows()
        );
        assertFalse(result.hasRows());
    }

    @Test
    void successWithRows_shouldStoreReturnedRows() {
        Row firstRow = new Row(
                List.of(
                        1,
                        "Emre",
                        21
                )
        );

        Row secondRow = new Row(
                List.of(
                        2,
                        "Ahmet",
                        24
                )
        );

        ExecuteResult result =
                ExecuteResult.success(
                        "2 rows selected.",
                        List.of(
                                firstRow,
                                secondRow
                        )
                );

        assertTrue(result.isSuccess());
        assertEquals(
                "2 rows selected.",
                result.getMessage()
        );
        assertEquals(
                2,
                result.getRowCount()
        );
        assertEquals(
                2,
                result.getAffectedRows()
        );
        assertTrue(result.hasRows());
        assertEquals(
                firstRow,
                result.getRows().get(0)
        );
        assertEquals(
                secondRow,
                result.getRows().get(1)
        );
    }

    @Test
    void failure_shouldCreateFailedResult() {
        ExecuteResult result =
                ExecuteResult.failure(
                        "Operation failed."
                );

        assertFalse(result.isSuccess());
        assertEquals(
                "Operation failed.",
                result.getMessage()
        );
        assertEquals(
                0,
                result.getAffectedRows()
        );
        assertEquals(
                0,
                result.getRowCount()
        );
        assertFalse(result.hasRows());
    }

    @Test
    void successWithNullRows_shouldUseEmptyList() {
        ExecuteResult result =
                ExecuteResult.success(
                        "No rows.",
                        (List<Row>) null
                );

        assertTrue(result.isSuccess());
        assertEquals(
                0,
                result.getRowCount()
        );
        assertEquals(
                0,
                result.getAffectedRows()
        );
        assertFalse(result.hasRows());
    }

    @Test
    void toString_shouldContainResultSummary() {
        ExecuteResult result =
                ExecuteResult.success(
                        "1 row inserted.",
                        1
                );

        String text = result.toString();

        assertTrue(
                text.contains("success=true")
        );

        assertTrue(
                text.contains("affectedRows=1")
        );

        assertTrue(
                text.contains("rowCount=0")
        );
    }
}