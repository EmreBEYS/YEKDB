package com.yekdb.storage.table;

import com.yekdb.storage.exception.InvalidColumnException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrayAndJsonTypePhase12Test {

    @Test
    void shouldParseJsonType() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("JSON");
        assertEquals(DataType.JSON, type.dataType());
        assertEquals("JSON", type.declaration());
    }

    @Test
    void shouldParseIntegerArrayType() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("INTEGER[]");
        assertEquals(DataType.ARRAY, type.dataType());
        assertNotNull(type.arrayElementType());
        assertEquals(DataType.INT, type.arrayElementType().dataType());
        assertEquals("INT[]", type.declaration());
    }

    @Test
    void shouldParseSizedVarcharArrayType() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("VARCHAR(50)[]");
        assertEquals(DataType.ARRAY, type.dataType());
        assertEquals(DataType.VARCHAR, type.arrayElementType().dataType());
        assertEquals(50, type.arrayElementType().length());
        assertEquals("VARCHAR(50)[]", type.declaration());
    }

    @Test
    void shouldRejectMultiDimensionalArrayInV1() {
        assertThrows(
                InvalidColumnException.class,
                () -> ColumnTypeDefinition.parse("INTEGER[][]")
        );
    }

    @Test
    void shouldValidateJsonObjectAndArray() {
        Column column = new Column("metadata", DataType.JSON);
        assertTrue(column.acceptsTypedStringValue("{\"city\":\"Malatya\",\"active\":true}"));
        assertTrue(column.acceptsTypedStringValue("[1,2,{\"ok\":false},null]"));
    }

    @Test
    void shouldRejectMalformedJson() {
        Column column = new Column("metadata", DataType.JSON);
        assertFalse(column.acceptsTypedStringValue("{\"city\":\"Malatya\",}"));
        assertFalse(column.acceptsTypedStringValue("[1,2,]"));
    }

    @Test
    void shouldValidateIntegerArrayElements() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("INTEGER[]");
        Column column = Column.fromTypeDefinition("scores", type);
        assertTrue(column.acceptsTypedStringValue("{1,2,-3,NULL}"));
        assertFalse(column.acceptsTypedStringValue("{1,two,3}"));
    }

    @Test
    void shouldValidateVarcharArrayElementLength() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("VARCHAR(5)[]");
        Column column = Column.fromTypeDefinition("tags", type);
        assertTrue(column.acceptsTypedStringValue("{Emre,YEKDB}"));
        assertFalse(column.acceptsTypedStringValue("{Database,YEKDB}"));
    }

    @Test
    void shouldValidateNumericArrayPrecisionAndScale() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("NUMERIC(5,2)[]");
        Column column = Column.fromTypeDefinition("prices", type);
        assertTrue(column.acceptsTypedStringValue("{999.99,12.5,-7}"));
        assertFalse(column.acceptsTypedStringValue("{1000,12.5}"));
        assertFalse(column.acceptsTypedStringValue("{12.345}"));
    }

    @Test
    void shouldPreserveArrayDeclarationOnColumn() {
        Column column = Column.fromTypeDefinition(
                "names",
                ColumnTypeDefinition.parse("TEXT[]")
        );
        assertEquals(DataType.ARRAY, column.getDataType());
        assertEquals("TEXT[]", column.getTypeDeclaration());
        assertEquals(DataType.TEXT, column.getArrayElementType().dataType());
    }
}
