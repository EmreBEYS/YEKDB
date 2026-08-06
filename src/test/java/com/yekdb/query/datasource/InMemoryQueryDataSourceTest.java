package com.yekdb.query.datasource;

import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InMemoryQueryDataSource sınıfının birim testleri.
 */
class InMemoryQueryDataSourceTest {

    private InMemoryQueryDataSource dataSource;
    private Table usersTable;
    private List<Row> users;

    @BeforeEach
    void setUp() {
        dataSource = new InMemoryQueryDataSource();

        usersTable = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING),
                        new Column("age", DataType.INT)
                )
        );

        users = List.of(
                new Row(
                        List.of(
                                1,
                                "Yunus Emre",
                                21
                        )
                ),
                new Row(
                        List.of(
                                2,
                                "Ali",
                                16
                        )
                ),
                new Row(
                        List.of(
                                3,
                                "Ayşe",
                                27
                        )
                )
        );
    }

    @Test
    void register_shouldStoreTableAndRows() {
        dataSource.register(
                usersTable,
                users
        );

        Table storedTable =
                dataSource.getTable("users");

        List<Row> storedRows =
                dataSource.getRows("users");

        assertEquals(
                usersTable,
                storedTable
        );

        assertEquals(
                3,
                storedRows.size()
        );

        assertEquals(
                "Yunus Emre",
                storedRows
                        .get(0)
                        .getValue(1)
        );
    }

    @Test
    void getTable_shouldIgnoreTableNameCase() {
        dataSource.register(
                usersTable,
                users
        );

        Table storedTable =
                dataSource.getTable("USERS");

        assertEquals(
                usersTable,
                storedTable
        );
    }

    @Test
    void getRows_shouldIgnoreTableNameCase() {
        dataSource.register(
                usersTable,
                users
        );

        List<Row> storedRows =
                dataSource.getRows("UsErS");

        assertEquals(
                3,
                storedRows.size()
        );
    }

    @Test
    void getTable_shouldTrimTableName() {
        dataSource.register(
                usersTable,
                users
        );

        Table storedTable =
                dataSource.getTable("  users  ");

        assertEquals(
                usersTable,
                storedTable
        );
    }

    @Test
    void register_shouldSupportEmptyRowList() {
        dataSource.register(
                usersTable,
                List.of()
        );

        Table storedTable =
                dataSource.getTable("users");

        List<Row> storedRows =
                dataSource.getRows("users");

        assertNotNull(storedTable);
        assertTrue(storedRows.isEmpty());
    }

    @Test
    void register_shouldCreateImmutableRowCopy() {
        List<Row> mutableRows =
                new ArrayList<>(users);

        dataSource.register(
                usersTable,
                mutableRows
        );

        mutableRows.clear();

        List<Row> storedRows =
                dataSource.getRows("users");

        assertEquals(
                3,
                storedRows.size()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> storedRows.clear()
        );
    }

    @Test
    void register_shouldReplaceExistingTableData() {
        dataSource.register(
                usersTable,
                users
        );

        List<Row> newRows = List.of(
                new Row(
                        List.of(
                                10,
                                "Mehmet",
                                35
                        )
                )
        );

        dataSource.register(
                usersTable,
                newRows
        );

        List<Row> storedRows =
                dataSource.getRows("users");

        assertEquals(
                1,
                storedRows.size()
        );

        assertEquals(
                "Mehmet",
                storedRows
                        .get(0)
                        .getValue(1)
        );
    }

    @Test
    void register_shouldRejectNullTable() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> dataSource.register(
                                null,
                                users
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void register_shouldRejectNullRowList() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> dataSource.register(
                                usersTable,
                                null
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void register_shouldRejectNullRowInsideList() {
        List<Row> invalidRows =
                new ArrayList<>();

        invalidRows.add(users.get(0));
        invalidRows.add(null);
        invalidRows.add(users.get(1));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> dataSource.register(
                                usersTable,
                                invalidRows
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void register_shouldRejectRowWithMissingValues() {
        Row invalidRow = new Row(
                List.of(
                        1,
                        "Eksik Satır"
                )
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> dataSource.register(
                                usersTable,
                                List.of(invalidRow)
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void register_shouldRejectRowWithExtraValues() {
        Row invalidRow = new Row(
                List.of(
                        1,
                        "Fazla Satır",
                        25,
                        "fazladan"
                )
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> dataSource.register(
                                usersTable,
                                List.of(invalidRow)
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void getTable_shouldThrowExceptionForUnknownTable() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> dataSource.getTable(
                                "products"
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void getRows_shouldThrowExceptionForUnknownTable() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> dataSource.getRows(
                                "products"
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void getTable_shouldRejectBlankTableName() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> dataSource.getTable(
                                "   "
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void getRows_shouldRejectNullTableName() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> dataSource.getRows(
                                null
                        )
                );

        assertMessageExists(exception);
    }

    /**
     * Hata mesajının null veya boş olmadığını doğrular.
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