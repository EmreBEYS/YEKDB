package com.yekdb.table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Column sınıfı için birim testleri.
 */
class ColumnTest {

    @Test
    void shouldCreateColumnSuccessfully() {
        Column column = new Column("id", DataType.INT);

        assertEquals("id", column.getName());
        assertEquals(DataType.INT, column.getDataType());
    }

    @Test
    void shouldNormalizeColumnName() {
        Column column = new Column("  USER_NAME  ", DataType.STRING);

        assertEquals("user_name", column.getName());
    }

    @Test
    void shouldThrowExceptionWhenColumnNameIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Column(null, DataType.INT)
        );

        assertEquals(
                "Column name cannot be null or blank.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenColumnNameIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Column("   ", DataType.INT)
        );

        assertEquals(
                "Column name cannot be null or blank.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenDataTypeIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Column("id", null)
        );

        assertEquals(
                "Data type cannot be null.",
                exception.getMessage()
        );
    }

    @Test
    void shouldReturnTrueForEqualColumns() {
        Column firstColumn = new Column("id", DataType.INT);
        Column secondColumn = new Column("ID", DataType.INT);

        assertEquals(firstColumn, secondColumn);
        assertEquals(firstColumn.hashCode(), secondColumn.hashCode());
    }

    @Test
    void shouldReturnFalseForDifferentColumnNames() {
        Column firstColumn = new Column("id", DataType.INT);
        Column secondColumn = new Column("age", DataType.INT);

        assertNotEquals(firstColumn, secondColumn);
    }

    @Test
    void shouldReturnFalseForDifferentDataTypes() {
        Column firstColumn = new Column("id", DataType.INT);
        Column secondColumn = new Column("id", DataType.LONG);

        assertNotEquals(firstColumn, secondColumn);
    }

    @Test
    void shouldGenerateReadableToString() {
        Column column = new Column("name", DataType.STRING);

        String result = column.toString();

        assertTrue(result.contains("name"));
        assertTrue(result.contains("STRING"));
    }
}