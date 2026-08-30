package com.yekdb.storage.table;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ColumnValueValidatorPhase15Test {

    @Test
    void shouldAcceptValidVarcharAndRejectOverflow() {
        Column column = new Column("name", DataType.VARCHAR, 5);

        assertDoesNotThrow(() -> ColumnValueValidator.validate(column, "YEKDB"));
        assertThrows(
                IllegalArgumentException.class,
                () -> ColumnValueValidator.validate(column, "YEKDATABASE")
        );
    }

    @Test
    void shouldEnforceNumericPrecisionAndScale() {
        Column column = new Column("price", DataType.NUMERIC, null, 5, 2);

        assertDoesNotThrow(() -> ColumnValueValidator.validate(column, 999.99));
        assertThrows(
                IllegalArgumentException.class,
                () -> ColumnValueValidator.validate(column, 1000.00)
        );
    }

    @Test
    void shouldRejectInvalidTemporalValue() {
        Column column = new Column("created_on", DataType.DATE);

        assertDoesNotThrow(() -> ColumnValueValidator.validate(column, "2026-08-30"));
        assertThrows(
                IllegalArgumentException.class,
                () -> ColumnValueValidator.validate(column, "2026-02-30")
        );
    }

    @Test
    void shouldRejectInvalidJsonValue() {
        Column column = new Column("metadata", DataType.JSON);

        assertDoesNotThrow(
                () -> ColumnValueValidator.validate(column, "{\"city\":\"Malatya\"}")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ColumnValueValidator.validate(column, "{\"city\":\"Malatya\",}")
        );
    }

    @Test
    void shouldRejectInvalidArrayElementType() {
        ColumnTypeDefinition elementType = ColumnTypeDefinition.parse("INTEGER");
        Column column = new Column(
                "scores",
                DataType.ARRAY,
                null,
                null,
                null,
                elementType
        );

        assertDoesNotThrow(() -> ColumnValueValidator.validate(column, "{1,2,3}"));
        assertThrows(
                IllegalArgumentException.class,
                () -> ColumnValueValidator.validate(column, "{1,two,3}")
        );
    }

    @Test
    void shouldRejectWrongRuntimeTypeAndAllowNull() {
        Column column = new Column("active", DataType.BOOLEAN);

        assertDoesNotThrow(() -> ColumnValueValidator.validate(column, null));
        assertDoesNotThrow(() -> ColumnValueValidator.validate(column, true));
        assertThrows(
                IllegalArgumentException.class,
                () -> ColumnValueValidator.validate(column, "true")
        );
    }
}
