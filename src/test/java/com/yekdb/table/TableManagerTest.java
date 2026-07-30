package com.yekdb.table;

import com.yekdb.table.exception.TableAlreadyExistsException;
import com.yekdb.table.exception.TableNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableManagerTest {

    @TempDir
    Path tempDirectory;

    private Path databaseDirectory;
    private TableManager tableManager;
    private Table usersTable;

    @BeforeEach
    void setUp() {
        databaseDirectory = tempDirectory.resolve("company");

        tableManager = new TableManager(databaseDirectory);

        usersTable = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING),
                        new Column("age", DataType.INT)
                )
        );
    }

    @Test
    void shouldCreateTableSuccessfully() {
        TableMetadata metadata =
                tableManager.createTable(usersTable);

        assertNotNull(metadata);
        assertEquals("users", metadata.getTableName());
        assertEquals(3, metadata.getColumnCount());
        assertEquals("users.tbl", metadata.getFileName());
        assertEquals(1, metadata.getVersion());
    }

    @Test
    void shouldCreateDatabaseDirectoryAutomatically() {
        assertFalse(Files.exists(databaseDirectory));

        tableManager.createTable(usersTable);

        assertTrue(Files.exists(databaseDirectory));
        assertTrue(Files.isDirectory(databaseDirectory));
    }

    @Test
    void shouldCreatePhysicalTableFile() {
        tableManager.createTable(usersTable);

        Path tableFile =
                databaseDirectory.resolve("users.tbl");

        assertTrue(Files.exists(tableFile));
        assertTrue(Files.isRegularFile(tableFile));
    }

    @Test
    void shouldWriteTableHeaderAndSchemaToFile()
            throws IOException {

        tableManager.createTable(usersTable);

        Path tableFile =
                databaseDirectory.resolve("users.tbl");

        String content = Files.readString(
                tableFile,
                StandardCharsets.UTF_8
        );

        assertTrue(content.contains("YEKDB_TABLE"));
        assertTrue(content.contains("version=1"));
        assertTrue(content.contains("tableName=users"));
        assertTrue(content.contains("columnCount=3"));
        assertTrue(content.contains("columns="));

        assertTrue(content.contains("id:INT"));
        assertTrue(content.contains("name:STRING"));
        assertTrue(content.contains("age:INT"));
    }

    @Test
    void shouldRegisterCreatedTableInCatalog() {
        tableManager.createTable(usersTable);

        assertTrue(tableManager.exists("users"));
        assertEquals(1, tableManager.getTableCount());

        assertEquals(
                usersTable,
                tableManager.getTable("users")
        );
    }

    @Test
    void shouldRegisterMetadataInCatalog() {
        TableMetadata createdMetadata =
                tableManager.createTable(usersTable);

        TableMetadata catalogMetadata =
                tableManager.getMetadata("users");

        assertEquals(createdMetadata, catalogMetadata);
    }

    @Test
    void shouldCreateTableUsingNameAndColumns() {
        TableMetadata metadata =
                tableManager.createTable(
                        "products",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING),
                                new Column("price", DataType.DOUBLE)
                        )
                );

        assertEquals("products", metadata.getTableName());
        assertEquals(3, metadata.getColumnCount());

        assertTrue(
                Files.exists(
                        databaseDirectory.resolve("products.tbl")
                )
        );
    }

    @Test
    void shouldNormalizeTableName() {
        Table table = new Table(
                "  USERS  ",
                List.of(
                        new Column("id", DataType.INT)
                )
        );

        tableManager.createTable(table);

        assertTrue(tableManager.exists("users"));
        assertTrue(tableManager.exists("USERS"));
        assertTrue(tableManager.exists("  Users  "));

        assertTrue(
                Files.exists(
                        databaseDirectory.resolve("users.tbl")
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenCreatingNullTable() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> tableManager.createTable(
                                (Table) null
                        )
                );

        assertEquals(
                "Table cannot be null.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenTableAlreadyExists() {
        tableManager.createTable(usersTable);

        TableAlreadyExistsException exception =
                assertThrows(
                        TableAlreadyExistsException.class,
                        () -> tableManager.createTable(usersTable)
                );

        assertTrue(
                exception.getMessage().contains("users")
        );
    }

    @Test
    void shouldThrowExceptionWhenPhysicalFileAlreadyExists()
            throws IOException {

        Files.createDirectories(databaseDirectory);

        Files.createFile(
                databaseDirectory.resolve("users.tbl")
        );

        assertThrows(
                TableAlreadyExistsException.class,
                () -> tableManager.createTable(usersTable)
        );

        assertEquals(0, tableManager.getTableCount());
    }

    @Test
    void shouldReturnTrueWhenTableExists() {
        tableManager.createTable(usersTable);

        assertTrue(tableManager.exists("users"));
        assertTrue(tableManager.exists("USERS"));
    }

    @Test
    void shouldReturnFalseWhenTableDoesNotExist() {
        assertFalse(tableManager.exists("products"));
    }

    @Test
    void shouldReturnFalseForInvalidTableName() {
        assertFalse(tableManager.exists(null));
        assertFalse(tableManager.exists(""));
        assertFalse(tableManager.exists("   "));
    }

    @Test
    void shouldListCreatedTablesInOrder() {
        Table productsTable = new Table(
                "products",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("price", DataType.DOUBLE)
                )
        );

        tableManager.createTable(usersTable);
        tableManager.createTable(productsTable);

        assertEquals(
                List.of("users", "products"),
                tableManager.listTableNames()
        );

        assertEquals(
                List.of(usersTable, productsTable),
                tableManager.listTables()
        );

        assertEquals(2, tableManager.getTableCount());
    }

    @Test
    void shouldDropTableSuccessfully() {
        tableManager.createTable(usersTable);

        Table removedTable =
                tableManager.dropTable("users");

        assertEquals(usersTable, removedTable);
        assertFalse(tableManager.exists("users"));
        assertEquals(0, tableManager.getTableCount());
    }

    @Test
    void shouldDeletePhysicalFileWhenTableIsDropped() {
        tableManager.createTable(usersTable);

        Path tableFile =
                databaseDirectory.resolve("users.tbl");

        assertTrue(Files.exists(tableFile));

        tableManager.dropTable("users");

        assertFalse(Files.exists(tableFile));
    }

    @Test
    void shouldRemoveCatalogMetadataWhenTableIsDropped() {
        tableManager.createTable(usersTable);

        tableManager.dropTable("users");

        assertThrows(
                TableNotFoundException.class,
                () -> tableManager.getMetadata("users")
        );
    }

    @Test
    void shouldNormalizeTableNameWhenDropping() {
        tableManager.createTable(usersTable);

        Table removedTable =
                tableManager.dropTable("  USERS  ");

        assertEquals(usersTable, removedTable);
        assertFalse(tableManager.exists("users"));
    }

    @Test
    void shouldThrowExceptionWhenDroppingUnknownTable() {
        assertThrows(
                TableNotFoundException.class,
                () -> tableManager.dropTable("unknown")
        );
    }

    @Test
    void shouldReturnConfiguredDatabaseDirectory() {
        assertEquals(
                databaseDirectory
                        .toAbsolutePath()
                        .normalize(),
                tableManager.getDatabaseDirectory()
        );
    }

    @Test
    void shouldUseProvidedTableCatalog() {
        TableCatalog catalog = new TableCatalog();

        TableManager manager = new TableManager(
                databaseDirectory,
                catalog
        );

        manager.createTable(usersTable);

        assertSame(
                catalog,
                manager.getTableCatalog()
        );

        assertTrue(catalog.containsTable("users"));
    }

    @Test
    void shouldThrowExceptionWhenDatabaseDirectoryIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new TableManager(null)
                );

        assertEquals(
                "Database directory cannot be null.",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowExceptionWhenTableCatalogIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new TableManager(
                                databaseDirectory,
                                null
                        )
                );

        assertEquals(
                "Table catalog cannot be null.",
                exception.getMessage()
        );
    }

    @Test
    void shouldGenerateReadableToString() {
        tableManager.createTable(usersTable);

        String result = tableManager.toString();

        assertTrue(result.contains("TableManager"));
        assertTrue(result.contains("company"));
        assertTrue(result.contains("tableCount=1"));
    }
}