package com.yekdb.query.result;

import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QueryResult sınıfının birim testleri.
 */
class QueryResultTest {

    @Test
    void selectSuccess_shouldCreateSuccessfulResult() {
        List<Column> columns = createColumns();

        List<Row> rows = List.of(
                new Row(List.of(1, "Yunus Emre", 21)),
                new Row(List.of(2, "Ayşe", 27))
        );

        QueryResult result = QueryResult.selectSuccess(
                columns,
                rows,
                2_500_000L
        );

        assertTrue(result.isSuccess());
        assertMessageExists(result.getMessage());

        assertEquals(
                3,
                result.getColumns().size()
        );

        assertEquals(
                2,
                result.getRows().size()
        );

        assertEquals(
                2,
                result.getAffectedRowCount()
        );

        assertEquals(
                2_500_000L,
                result.getExecutionTimeNanos()
        );

        assertEquals(
                2.5,
                result.getExecutionTimeMillis(),
                0.0001
        );

        assertTrue(result.hasRows());
        assertTrue(result.hasColumns());
    }

    @Test
    void selectSuccess_shouldSupportEmptyRowResult() {
        QueryResult result = QueryResult.selectSuccess(
                createColumns(),
                List.of(),
                100L
        );

        assertTrue(result.isSuccess());
        assertFalse(result.hasRows());
        assertTrue(result.hasColumns());

        assertEquals(
                0,
                result.getAffectedRowCount()
        );
    }

    @Test
    void modificationSuccess_shouldCreateSuccessfulResult() {
        QueryResult result =
                QueryResult.modificationSuccess(
                        "3 satır silindi.",
                        3,
                        1_000_000L
                );

        assertTrue(result.isSuccess());

        assertEquals(
                "3 satır silindi.",
                result.getMessage()
        );

        assertEquals(
                3,
                result.getAffectedRowCount()
        );

        assertFalse(result.hasRows());
        assertFalse(result.hasColumns());

        assertEquals(
                1.0,
                result.getExecutionTimeMillis(),
                0.0001
        );
    }

    @Test
    void modificationSuccess_shouldNormalizeBlankMessage() {
        QueryResult result =
                QueryResult.modificationSuccess(
                        "   ",
                        1,
                        0L
                );

        assertMessageExists(result.getMessage());

        assertFalse(
                result.getMessage().isBlank()
        );
    }

    @Test
    void failure_shouldCreateFailedResult() {
        QueryResult result =
                QueryResult.failure(
                        "Tablo bulunamadı.",
                        500L
                );

        assertFalse(result.isSuccess());

        assertEquals(
                "Tablo bulunamadı.",
                result.getMessage()
        );

        assertEquals(
                0,
                result.getAffectedRowCount()
        );

        assertFalse(result.hasRows());
        assertFalse(result.hasColumns());
    }

    @Test
    void failure_shouldNormalizeNullMessage() {
        QueryResult result =
                QueryResult.failure(
                        null,
                        0L
                );

        assertMessageExists(result.getMessage());

        assertFalse(
                result.getMessage().isBlank()
        );
    }

    @Test
    void resultLists_shouldBeImmutableCopies() {
        List<Column> columns =
                new ArrayList<>(
                        createColumns()
                );

        List<Row> rows =
                new ArrayList<>(
                        List.of(
                                new Row(
                                        List.of(
                                                1,
                                                "Yunus Emre",
                                                21
                                        )
                                )
                        )
                );

        QueryResult result =
                QueryResult.selectSuccess(
                        columns,
                        rows,
                        0L
                );

        columns.clear();
        rows.clear();

        assertEquals(
                3,
                result.getColumns().size()
        );

        assertEquals(
                1,
                result.getRows().size()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result
                        .getRows()
                        .add(
                                new Row(
                                        List.of(
                                                2,
                                                "Ali",
                                                16
                                        )
                                )
                        )
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result
                        .getColumns()
                        .clear()
        );
    }

    @Test
    void selectSuccess_shouldRejectNullColumns() {
        assertThrows(
                NullPointerException.class,
                () -> QueryResult.selectSuccess(
                        null,
                        List.of(),
                        0L
                )
        );
    }

    @Test
    void selectSuccess_shouldRejectNullRows() {
        assertThrows(
                NullPointerException.class,
                () -> QueryResult.selectSuccess(
                        createColumns(),
                        null,
                        0L
                )
        );
    }

    @Test
    void modificationSuccess_shouldRejectNegativeAffectedRowCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> QueryResult.modificationSuccess(
                        "Hatalı değer",
                        -1,
                        0L
                )
        );
    }

    @Test
    void selectSuccess_shouldRejectNegativeExecutionTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> QueryResult.selectSuccess(
                        createColumns(),
                        List.of(),
                        -1L
                )
        );
    }

    @Test
    void failure_shouldRejectNegativeExecutionTime() {
        assertThrows(
                IllegalArgumentException.class,
                () -> QueryResult.failure(
                        "Hata",
                        -1L
                )
        );
    }

    @Test
    void toString_shouldContainSummaryInformation() {
        QueryResult result =
                QueryResult.selectSuccess(
                        createColumns(),
                        List.of(
                                new Row(
                                        List.of(
                                                1,
                                                "Yunus Emre",
                                                21
                                        )
                                )
                        ),
                        1_000_000L
                );

        String value = result.toString();

        assertTrue(
                value.contains("success=true")
        );

        assertTrue(
                value.contains("columnCount=3")
        );

        assertTrue(
                value.contains("rowCount=1")
        );

        assertTrue(
                value.contains("affectedRowCount=1")
        );
    }

    private static List<Column> createColumns() {
        return List.of(
                new Column("id", DataType.INT),
                new Column("name", DataType.STRING),
                new Column("age", DataType.INT)
        );
    }

    /**
     * Sonuç mesajının null veya boş olmadığını doğrular.
     */
    private static void assertMessageExists(
            String message
    ) {
        assertNotNull(message);
        assertFalse(message.isBlank());
    }
}