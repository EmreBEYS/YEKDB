package com.yekdb.storage.table;

import com.yekdb.storage.exception.TableNotFoundException;
import com.yekdb.storage.table.header.TableHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableManagerHeaderMutationTest {

    @TempDir
    Path tempDirectory;

    private TableManager tableManager;

    @BeforeEach
    void setUp() {

        tableManager =
                new TableManager(
                        tempDirectory
                );

        tableManager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        ),
                        new Column(
                                "name",
                                DataType.STRING
                        )
                )
        );
    }

    @Test
    void shouldReadTableHeaderThroughManager() {

        TableHeader header =
                tableManager.getTableHeader(
                        "users"
                );

        assertEquals(
                "users",
                header.getTableName()
        );

        assertEquals(
                2,
                header.getColumnCount()
        );

        assertEquals(
                0L,
                header.getRowCount()
        );

        assertEquals(
                -1L,
                header.getFirstDataPageId()
        );

        assertEquals(
                -1L,
                header.getLastDataPageId()
        );
    }

    @Test
    void shouldUpdateTableRowCount() {

        TableHeader updated =
                tableManager.updateTableRowCount(
                        "users",
                        25L
                );

        assertEquals(
                25L,
                updated.getRowCount()
        );

        assertEquals(
                25L,
                tableManager
                        .getTableHeader("users")
                        .getRowCount()
        );
    }

    @Test
    void shouldIncrementTableRowCount() {

        TableHeader updated =
                tableManager.incrementTableRowCount(
                        "users"
                );

        assertEquals(
                1L,
                updated.getRowCount()
        );

        assertEquals(
                1L,
                tableManager
                        .getTableHeader("users")
                        .getRowCount()
        );
    }

    @Test
    void shouldPersistMultipleRowCountIncrements() {

        tableManager.incrementTableRowCount(
                "users"
        );

        tableManager.incrementTableRowCount(
                "users"
        );

        tableManager.incrementTableRowCount(
                "users"
        );

        assertEquals(
                3L,
                tableManager
                        .getTableHeader("users")
                        .getRowCount()
        );
    }

    @Test
    void shouldDecrementTableRowCount() {

        tableManager.updateTableRowCount(
                "users",
                5L
        );

        TableHeader updated =
                tableManager.decrementTableRowCount(
                        "users"
                );

        assertEquals(
                4L,
                updated.getRowCount()
        );

        assertEquals(
                4L,
                tableManager
                        .getTableHeader("users")
                        .getRowCount()
        );
    }

    @Test
    void shouldUpdateTableDataPageRange() {

        TableHeader updated =
                tableManager.updateTableDataPageRange(
                        "users",
                        100L,
                        105L
                );

        assertEquals(
                100L,
                updated.getFirstDataPageId()
        );

        assertEquals(
                105L,
                updated.getLastDataPageId()
        );

        TableHeader persisted =
                tableManager.getTableHeader(
                        "users"
                );

        assertEquals(
                100L,
                persisted.getFirstDataPageId()
        );

        assertEquals(
                105L,
                persisted.getLastDataPageId()
        );
    }

    @Test
    void shouldClearTableDataPageRange() {

        tableManager.updateTableDataPageRange(
                "users",
                100L,
                105L
        );

        TableHeader cleared =
                tableManager.clearTableDataPageRange(
                        "users"
                );

        assertEquals(
                -1L,
                cleared.getFirstDataPageId()
        );

        assertEquals(
                -1L,
                cleared.getLastDataPageId()
        );

        TableHeader persisted =
                tableManager.getTableHeader(
                        "users"
                );

        assertEquals(
                -1L,
                persisted.getFirstDataPageId()
        );

        assertEquals(
                -1L,
                persisted.getLastDataPageId()
        );
    }

    @Test
    void shouldRecoverUpdatedHeaderAfterManagerRestart() {

        tableManager.updateTableRowCount(
                "users",
                42L
        );

        tableManager.updateTableDataPageRange(
                "users",
                10L,
                15L
        );

        TableManager restartedManager =
                new TableManager(
                        tempDirectory
                );

        restartedManager.loadCatalog();

        TableHeader recovered =
                restartedManager.getTableHeader(
                        "users"
                );

        assertEquals(
                42L,
                recovered.getRowCount()
        );

        assertEquals(
                10L,
                recovered.getFirstDataPageId()
        );

        assertEquals(
                15L,
                recovered.getLastDataPageId()
        );
    }

    @Test
    void shouldRejectRowCountUpdateForMissingTable() {

        assertThrows(
                TableNotFoundException.class,
                () -> tableManager
                        .incrementTableRowCount(
                                "missing"
                        )
        );
    }

    @Test
    void shouldRejectPageRangeUpdateForMissingTable() {

        assertThrows(
                TableNotFoundException.class,
                () -> tableManager
                        .updateTableDataPageRange(
                                "missing",
                                1L,
                                1L
                        )
        );
    }

    @Test
    void shouldNormalizeTableNameDuringHeaderMutation() {

        tableManager.updateTableRowCount(
                "  USERS  ",
                15L
        );

        assertEquals(
                15L,
                tableManager
                        .getTableHeader(
                                "users"
                        )
                        .getRowCount()
        );
    }
}