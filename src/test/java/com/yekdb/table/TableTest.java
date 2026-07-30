package com.yekdb.table;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableTest {

    @Test
    void shouldCreateTableSuccessfully() {

        Table table = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING)
                )
        );

        assertEquals("users", table.getTableName());
        assertEquals(2, table.getColumnCount());
    }

    @Test
    void shouldThrowExceptionWhenTableNameIsNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new Table(
                        null,
                        List.of(new Column("id", DataType.INT))
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenTableNameIsBlank() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new Table(
                        "   ",
                        List.of(new Column("id", DataType.INT))
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenColumnListIsEmpty() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new Table("users", List.of())
        );
    }

    @Test
    void shouldThrowExceptionWhenColumnListContainsNull() {

        List<Column> columns = new ArrayList<>();
        columns.add(new Column("id", DataType.INT));
        columns.add(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Table("users", columns)
        );

        assertEquals(
                "Column list cannot contain null values.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenDuplicateColumnsExist() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("ID", DataType.INT)
                        )
                )
        );
    }

    @Test
    void shouldFindExistingColumn() {

        Table table = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING)
                )
        );

        assertTrue(table.hasColumn("id"));
        assertTrue(table.hasColumn("ID"));
        assertTrue(table.hasColumn("Name"));
    }

    @Test
    void shouldReturnFalseForUnknownColumn() {

        Table table = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT)
                )
        );

        assertFalse(table.hasColumn("age"));
    }

    @Test
    void shouldReturnColumnByName() {

        Table table = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING)
                )
        );

        Column column = table.getColumn("name");

        assertEquals("name", column.getName());
        assertEquals(DataType.STRING, column.getDataType());
    }

    @Test
    void shouldThrowExceptionWhenColumnDoesNotExist() {

        Table table = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> table.getColumn("age")
        );
    }

    @Test
    void shouldReturnImmutableColumnList() {

        Table table = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT)
                )
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> table.getColumns().add(
                        new Column("age", DataType.INT)
                )
        );
    }

    @Test
    void shouldReturnEqualTables() {

        Table first = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT)
                )
        );

        Table second = new Table(
                "USERS",
                List.of(
                        new Column("ID", DataType.INT)
                )
        );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void shouldGenerateReadableToString() {

        Table table = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT)
                )
        );

        String result = table.toString();

        assertTrue(result.contains("users"));
        assertTrue(result.contains("id"));
    }
}