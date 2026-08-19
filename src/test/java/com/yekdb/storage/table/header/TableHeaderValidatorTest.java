package com.yekdb.storage.table.header;

import com.yekdb.storage.exception.InvalidTableHeaderException;

import com.yekdb.storage.table.header.TableHeader;
import com.yekdb.storage.table.header.TableHeaderValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TableHeaderValidatorTest {

    @Test
    void shouldAcceptValidHeader() {

        TableHeader header = new TableHeader(
                1L,
                "users",
                4,
                0L,
                -1L,
                -1L,
                512L,
                0
        );

        assertDoesNotThrow(
                () -> TableHeaderValidator.validate(header)
        );
    }

    @Test
    void shouldRejectNullHeader() {

        assertThrows(
                InvalidTableHeaderException.class,
                () -> TableHeaderValidator.validate(null)
        );
    }

    @Test
    void shouldRejectNegativeTableId() {

        TableHeader header = new TableHeader(
                -1L,
                "users",
                4,
                0L,
                -1L,
                -1L,
                512L,
                0
        );

        assertThrows(
                InvalidTableHeaderException.class,
                () -> TableHeaderValidator.validate(header)
        );
    }

    @Test
    void shouldRejectBlankTableName() {

        TableHeader header = new TableHeader(
                1L,
                " ",
                4,
                0L,
                -1L,
                -1L,
                512L,
                0
        );

        assertThrows(
                InvalidTableHeaderException.class,
                () -> TableHeaderValidator.validate(header)
        );
    }

    @Test
    void shouldRejectNegativeColumnCount() {

        TableHeader header = new TableHeader(
                1L,
                "users",
                -1,
                0L,
                -1L,
                -1L,
                512L,
                0
        );

        assertThrows(
                InvalidTableHeaderException.class,
                () -> TableHeaderValidator.validate(header)
        );
    }

    @Test
    void shouldRejectNegativeRowCount() {

        TableHeader header = new TableHeader(
                1L,
                "users",
                4,
                -1L,
                -1L,
                -1L,
                512L,
                0
        );

        assertThrows(
                InvalidTableHeaderException.class,
                () -> TableHeaderValidator.validate(header)
        );
    }

    @Test
    void shouldAllowMissingDataPages() {

        TableHeader header = new TableHeader(
                1L,
                "users",
                4,
                0L,
                -1L,
                -1L,
                512L,
                0
        );

        assertDoesNotThrow(
                () -> TableHeaderValidator.validate(header)
        );
    }

    @Test
    void shouldRejectInconsistentDataPages() {

        TableHeader header = new TableHeader(
                1L,
                "users",
                4,
                0L,
                5L,
                -1L,
                512L,
                0
        );

        assertThrows(
                InvalidTableHeaderException.class,
                () -> TableHeaderValidator.validate(header)
        );
    }

    @Test
    void shouldRejectNegativeSchemaOffset() {

        TableHeader header = new TableHeader(
                1L,
                "users",
                4,
                0L,
                -1L,
                -1L,
                -1L,
                0
        );

        assertThrows(
                InvalidTableHeaderException.class,
                () -> TableHeaderValidator.validate(header)
        );
    }

    @Test
    void shouldRejectNegativeFlags() {

        TableHeader header = new TableHeader(
                1L,
                "users",
                4,
                0L,
                -1L,
                -1L,
                512L,
                -1
        );

        assertThrows(
                InvalidTableHeaderException.class,
                () -> TableHeaderValidator.validate(header)
        );
    }
}