package com.yekdb.storage.table;

import com.yekdb.storage.exception.InvalidColumnException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HstoreTypePhase13Test {

    @Test
    void shouldParseHstoreType() {
        ColumnTypeDefinition type = ColumnTypeDefinition.parse("HSTORE");
        assertEquals(DataType.HSTORE, type.dataType());
        assertEquals("HSTORE", type.declaration());
    }

    @Test
    void shouldCreateHstoreColumn() {
        Column column = Column.fromTypeDefinition(
                "attributes",
                ColumnTypeDefinition.parse("HSTORE")
        );
        assertEquals(DataType.HSTORE, column.getDataType());
        assertEquals("HSTORE", column.getTypeDeclaration());
    }

    @Test
    void shouldValidateQuotedHstorePairs() {
        Column column = new Column("attributes", DataType.HSTORE);
        assertTrue(column.acceptsTypedStringValue(
                "\"brand\"=>\"ASUS\", \"model\"=>\"TUF\""
        ));
    }

    @Test
    void shouldValidateUnquotedHstorePairs() {
        Column column = new Column("attributes", DataType.HSTORE);
        assertTrue(column.acceptsTypedStringValue(
                "brand=>ASUS, model=>TUF, active=>true"
        ));
    }

    @Test
    void shouldAllowNullHstoreValue() {
        Column column = new Column("attributes", DataType.HSTORE);
        assertTrue(column.acceptsTypedStringValue(
                "brand=>ASUS, note=>NULL"
        ));
    }

    @Test
    void shouldRejectMissingArrow() {
        Column column = new Column("attributes", DataType.HSTORE);
        assertFalse(column.acceptsTypedStringValue(
                "brand=ASUS"
        ));
    }

    @Test
    void shouldRejectBlankKey() {
        Column column = new Column("attributes", DataType.HSTORE);
        assertFalse(column.acceptsTypedStringValue(
                "\"\"=>\"value\""
        ));
    }

    @Test
    void shouldRejectMalformedQuotedValue() {
        Column column = new Column("attributes", DataType.HSTORE);
        assertFalse(column.acceptsTypedStringValue(
                "brand=>\"ASUS"
        ));
    }

    @Test
    void shouldRejectDuplicateKeysInV1() {
        Column column = new Column("attributes", DataType.HSTORE);
        assertFalse(column.acceptsTypedStringValue(
                "brand=>ASUS, brand=>TUF"
        ));
    }

    @Test
    void shouldRejectHstoreArrayInV1() {
        assertThrows(
                InvalidColumnException.class,
                () -> ColumnTypeDefinition.parse("HSTORE[]")
        );
    }
}
