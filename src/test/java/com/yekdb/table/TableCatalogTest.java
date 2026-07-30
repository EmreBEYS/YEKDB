package com.yekdb.table;

import com.yekdb.table.exception.TableAlreadyExistsException;
import com.yekdb.table.exception.TableNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableCatalogTest {

    private TableCatalog catalog;
    private Table usersTable;
    private TableMetadata usersMetadata;

    @BeforeEach
    void setUp() {
        catalog = new TableCatalog();

        usersTable = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING)
                )
        );

        usersMetadata = new TableMetadata(
                "users",
                usersTable.getColumnCount()
        );
    }

    @Test
    void shouldCreateEmptyCatalog() {
        assertTrue(catalog.isEmpty());
        assertEquals(0, catalog.size());
        assertTrue(catalog.listTables().isEmpty());
        assertTrue(catalog.listTableNames().isEmpty());
        assertTrue(catalog.listMetadata().isEmpty());
    }

    @Test
    void shouldRegisterTableSuccessfully() {
        catalog.registerTable(usersTable, usersMetadata);

        assertFalse(catalog.isEmpty());
        assertEquals(1, catalog.size());
        assertTrue(catalog.containsTable("users"));
    }

    @Test
    void shouldNormalizeTableNameWhenSearching() {
        catalog.registerTable(usersTable, usersMetadata);

        assertTrue(catalog.containsTable("USERS"));
        assertTrue(catalog.containsTable("  Users  "));
    }

    @Test
    void shouldThrowExceptionWhenRegisteringNullTable() {
        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.registerTable(null, usersMetadata)
        );
    }

    @Test
    void shouldThrowExceptionWhenRegisteringNullMetadata() {
        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.registerTable(usersTable, null)
        );
    }

    @Test
    void shouldThrowExceptionWhenTableAndMetadataNamesDoNotMatch() {
        TableMetadata incorrectMetadata =
                new TableMetadata("products", 2);

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.registerTable(
                        usersTable,
                        incorrectMetadata
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenColumnCountsDoNotMatch() {
        TableMetadata incorrectMetadata =
                new TableMetadata("users", 5);

        assertThrows(
                IllegalArgumentException.class,
                () -> catalog.registerTable(
                        usersTable,
                        incorrectMetadata
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenTableAlreadyExists() {
        catalog.registerTable(usersTable, usersMetadata);

        Table duplicateTable = new Table(
                "USERS",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING)
                )
        );

        TableMetadata duplicateMetadata =
                new TableMetadata("USERS", 2);

        assertThrows(
                TableAlreadyExistsException.class,
                () -> catalog.registerTable(
                        duplicateTable,
                        duplicateMetadata
                )
        );
    }

    @Test
    void shouldReturnRegisteredTable() {
        catalog.registerTable(usersTable, usersMetadata);

        Table result = catalog.getTable("USERS");

        assertEquals(usersTable, result);
    }

    @Test
    void shouldReturnRegisteredMetadata() {
        catalog.registerTable(usersTable, usersMetadata);

        TableMetadata result = catalog.getMetadata("users");

        assertEquals(usersMetadata, result);
    }

    @Test
    void shouldThrowExceptionWhenTableDoesNotExist() {
        assertThrows(
                TableNotFoundException.class,
                () -> catalog.getTable("products")
        );
    }

    @Test
    void shouldThrowExceptionWhenMetadataDoesNotExist() {
        assertThrows(
                TableNotFoundException.class,
                () -> catalog.getMetadata("products")
        );
    }

    @Test
    void shouldUnregisterTableSuccessfully() {
        catalog.registerTable(usersTable, usersMetadata);

        Table removedTable = catalog.unregisterTable("USERS");

        assertEquals(usersTable, removedTable);
        assertFalse(catalog.containsTable("users"));
        assertTrue(catalog.isEmpty());
    }

    @Test
    void shouldRemoveMetadataWhenTableIsUnregistered() {
        catalog.registerTable(usersTable, usersMetadata);
        catalog.unregisterTable("users");

        assertThrows(
                TableNotFoundException.class,
                () -> catalog.getMetadata("users")
        );
    }

    @Test
    void shouldThrowExceptionWhenUnregisteringUnknownTable() {
        assertThrows(
                TableNotFoundException.class,
                () -> catalog.unregisterTable("unknown")
        );
    }

    @Test
    void shouldListTablesInRegistrationOrder() {
        Table productsTable = new Table(
                "products",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("price", DataType.DOUBLE)
                )
        );

        TableMetadata productsMetadata =
                new TableMetadata("products", 2);

        catalog.registerTable(usersTable, usersMetadata);
        catalog.registerTable(productsTable, productsMetadata);

        assertEquals(
                List.of(usersTable, productsTable),
                catalog.listTables()
        );

        assertEquals(
                List.of("users", "products"),
                catalog.listTableNames()
        );
    }

    @Test
    void shouldReturnImmutableTableList() {
        catalog.registerTable(usersTable, usersMetadata);

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listTables().add(usersTable)
        );
    }

    @Test
    void shouldReturnImmutableTableNameList() {
        catalog.registerTable(usersTable, usersMetadata);

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listTableNames().add("products")
        );
    }

    @Test
    void shouldReturnImmutableMetadataList() {
        catalog.registerTable(usersTable, usersMetadata);

        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.listMetadata().add(usersMetadata)
        );
    }

    @Test
    void shouldReturnFalseForInvalidTableName() {
        assertFalse(catalog.containsTable(null));
        assertFalse(catalog.containsTable(""));
        assertFalse(catalog.containsTable("   "));
    }

    @Test
    void shouldClearCatalog() {
        catalog.registerTable(usersTable, usersMetadata);

        catalog.clear();

        assertTrue(catalog.isEmpty());
        assertEquals(0, catalog.size());
        assertTrue(catalog.listTables().isEmpty());
        assertTrue(catalog.listMetadata().isEmpty());
    }
}