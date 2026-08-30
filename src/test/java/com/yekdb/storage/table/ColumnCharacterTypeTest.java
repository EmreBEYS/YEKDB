package com.yekdb.storage.table;

import com.yekdb.storage.exception.InvalidColumnException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnCharacterTypeTest {

    @Test
    void shouldParseVarcharWithLength() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("VARCHAR(100)");
        assertEquals(DataType.VARCHAR, type.dataType());
        assertEquals(100, type.length());
        assertEquals("VARCHAR(100)", type.declaration());
    }

    @Test
    void shouldParseCharWithLengthCaseInsensitively() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("char(8)");
        assertEquals(DataType.CHAR, type.dataType());
        assertEquals(8, type.length());
    }

    @Test
    void shouldParseText() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("TEXT");
        assertEquals(DataType.TEXT, type.dataType());
        assertNull(type.length());
    }

    @Test
    void shouldParseBooleanAliases() {
        assertEquals(DataType.BOOLEAN, ColumnTypeDefinition.parse("BOOLEAN").dataType());
        assertEquals(DataType.BOOLEAN, ColumnTypeDefinition.parse("BOOL").dataType());
    }

    @Test
    void shouldRejectZeroCharacterLength() {
        assertThrows(InvalidColumnException.class,
                () -> ColumnTypeDefinition.parse("VARCHAR(0)"));
    }

    @Test
    void shouldRejectMalformedCharacterLength() {
        assertThrows(InvalidColumnException.class,
                () -> ColumnTypeDefinition.parse("VARCHAR(abc)"));
    }

    @Test
    void shouldPersistLengthOnColumn() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("VARCHAR(50)");

        Column column = new Column(
                "username",
                type.dataType(),
                type.length()
        );

        assertEquals(DataType.VARCHAR, column.getDataType());
        assertEquals(50, column.getLength());
        assertEquals("VARCHAR(50)", column.getTypeDeclaration());
    }

    @Test
    void shouldValidateVarcharLength() {
        Column column = new Column("code", DataType.VARCHAR, 5);
        assertTrue(column.acceptsStringLength("YEKDB"));
        assertFalse(column.acceptsStringLength("YEKDBMS"));
    }
}
