package com.yekdb.storage.table;

import com.yekdb.storage.exception.InvalidColumnException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumericTypePhase10Test {

    @Test
    void shouldParseBareNumeric() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("NUMERIC");

        assertEquals(DataType.NUMERIC, type.dataType());
        assertNull(type.precision());
        assertNull(type.scale());
        assertEquals("NUMERIC", type.declaration());
    }

    @Test
    void shouldParseNumericPrecision() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("NUMERIC(8)");

        assertEquals(DataType.NUMERIC, type.dataType());
        assertEquals(8, type.precision());
        assertEquals(0, type.scale());
        assertEquals("NUMERIC(8,0)", type.declaration());
    }

    @Test
    void shouldParseNumericPrecisionAndScale() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("numeric(10, 2)");

        assertEquals(DataType.NUMERIC, type.dataType());
        assertEquals(10, type.precision());
        assertEquals(2, type.scale());
        assertEquals("NUMERIC(10,2)", type.declaration());
    }

    @Test
    void shouldSupportDecimalAlias() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("DECIMAL(12,4)");

        assertEquals(DataType.NUMERIC, type.dataType());
        assertEquals(12, type.precision());
        assertEquals(4, type.scale());
    }

    @Test
    void shouldParseFloatPrecision() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("FLOAT(24)");

        assertEquals(DataType.DOUBLE, type.dataType());
        assertEquals(24, type.precision());
        assertNull(type.scale());
        assertEquals("FLOAT(24)", type.declaration());
    }

    @Test
    void shouldRejectNumericScaleGreaterThanPrecision() {
        assertThrows(
                InvalidColumnException.class,
                () -> ColumnTypeDefinition.parse("NUMERIC(4,5)")
        );
    }

    @Test
    void shouldRejectInvalidFloatPrecision() {
        assertThrows(
                InvalidColumnException.class,
                () -> ColumnTypeDefinition.parse("FLOAT(54)")
        );
    }

    @Test
    void shouldPersistNumericMetadataOnColumn() {
        Column column = new Column(
                "price",
                DataType.NUMERIC,
                null,
                10,
                2
        );

        assertEquals(10, column.getPrecision());
        assertEquals(2, column.getScale());
        assertEquals("NUMERIC(10,2)", column.getTypeDeclaration());
    }

    @Test
    void shouldAcceptNumericValueWithinPrecisionAndScale() {
        Column column = new Column(
                "price",
                DataType.NUMERIC,
                null,
                5,
                2
        );

        assertTrue(column.acceptsNumericValue(999.99));
        assertTrue(column.acceptsNumericValue(12));
        assertTrue(column.acceptsNumericValue(-7.5));
    }

    @Test
    void shouldRejectNumericValueOutsidePrecisionOrScale() {
        Column column = new Column(
                "price",
                DataType.NUMERIC,
                null,
                5,
                2
        );

        assertFalse(column.acceptsNumericValue(1000));
        assertFalse(column.acceptsNumericValue(12.345));
    }
}
