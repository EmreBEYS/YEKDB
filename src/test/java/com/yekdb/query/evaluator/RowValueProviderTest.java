package com.yekdb.query.evaluator;

import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RowValueProvider sınıfının birim testleri.
 */
class RowValueProviderTest {

    private Table usersTable;
    private Row userRow;

    @BeforeEach
    void setUp() {
        usersTable = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING),
                        new Column("age", DataType.INT),
                        new Column("city", DataType.STRING),
                        new Column("active", DataType.BOOLEAN)
                )
        );

        userRow = new Row(
                List.of(
                        1,
                        "Yunus Emre",
                        21,
                        "Malatya",
                        true
                )
        );
    }

    @Test
    void apply_shouldReturnValueByColumnName() {
        RowValueProvider provider =
                new RowValueProvider(
                        userRow,
                        usersTable
                );

        assertEquals(
                1,
                provider.apply("id")
        );

        assertEquals(
                "Yunus Emre",
                provider.apply("name")
        );

        assertEquals(
                21,
                provider.apply("age")
        );

        assertEquals(
                "Malatya",
                provider.apply("city")
        );

        assertEquals(
                true,
                provider.apply("active")
        );
    }

    @Test
    void apply_shouldIgnoreColumnNameCase() {
        RowValueProvider provider =
                new RowValueProvider(
                        userRow,
                        usersTable
                );

        assertEquals(
                "Malatya",
                provider.apply("CITY")
        );

        assertEquals(
                21,
                provider.apply("AgE")
        );
    }

    @Test
    void apply_shouldTrimColumnName() {
        RowValueProvider provider =
                new RowValueProvider(
                        userRow,
                        usersTable
                );

        assertEquals(
                "Yunus Emre",
                provider.apply("  name  ")
        );
    }

    @Test
    void apply_shouldThrowExceptionForUnknownColumn() {
        RowValueProvider provider =
                new RowValueProvider(
                        userRow,
                        usersTable
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> provider.apply("salary")
                );

        assertMessageExists(exception);
    }

    @Test
    void apply_shouldThrowExceptionForNullColumnName() {
        RowValueProvider provider =
                new RowValueProvider(
                        userRow,
                        usersTable
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> provider.apply(null)
                );

        assertMessageExists(exception);
    }

    @Test
    void apply_shouldThrowExceptionForBlankColumnName() {
        RowValueProvider provider =
                new RowValueProvider(
                        userRow,
                        usersTable
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> provider.apply("   ")
                );

        assertMessageExists(exception);
    }

    @Test
    void constructor_shouldThrowExceptionWhenRowIsNull() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new RowValueProvider(
                                null,
                                usersTable
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void constructor_shouldThrowExceptionWhenTableIsNull() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> new RowValueProvider(
                                userRow,
                                null
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void constructor_shouldRejectRowWithMissingValues() {
        Row incompleteRow = new Row(
                List.of(
                        1,
                        "Eksik Satır"
                )
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new RowValueProvider(
                                incompleteRow,
                                usersTable
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void constructor_shouldRejectRowWithExtraValues() {
        Row oversizedRow = new Row(
                List.of(
                        1,
                        "Yunus Emre",
                        21,
                        "Malatya",
                        true,
                        "fazladan"
                )
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new RowValueProvider(
                                oversizedRow,
                                usersTable
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void row_shouldRejectNullStoredValue() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new Row(
                                java.util.Arrays.asList(
                                        1,
                                        "Yunus Emre",
                                        null,
                                        "Malatya",
                                        true
                                )
                        )
                );

        assertMessageExists(exception);
    }

    /**
     * Exception mesajının mevcut olduğunu doğrular.
     */
    private static void assertMessageExists(
            Throwable exception
    ) {
        assertNotNull(exception);
        assertNotNull(exception.getMessage());

        assertFalse(
                exception.getMessage().isBlank()
        );
    }
}