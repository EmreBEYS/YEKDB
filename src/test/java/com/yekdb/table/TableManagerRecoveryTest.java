package com.yekdb.table;

import com.yekdb.table.exception.TableAlreadyExistsException;
import com.yekdb.table.exception.TableNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableManagerRecoveryTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldRecoverCreatedTableAfterRestart() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        firstManager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        ),
                        new Column(
                                "username",
                                DataType.STRING
                        ),
                        new Column(
                                "active",
                                DataType.BOOLEAN
                        )
                )
        );

        /*
         * Yeni TableManager nesnesi uygulamanın
         * yeniden başlatılmasını simüle eder.
         */
        TableManager secondManager =
                new TableManager(tempDirectory);

        assertEquals(
                0,
                secondManager.getTableCount()
        );

        secondManager.loadCatalog();

        assertEquals(
                1,
                secondManager.getTableCount()
        );

        assertTrue(
                secondManager.exists("users")
        );

        Table recoveredTable =
                secondManager.getTable("users");

        assertEquals(
                "users",
                recoveredTable.getTableName()
        );

        assertEquals(
                3,
                recoveredTable.getColumnCount()
        );
    }

    @Test
    void shouldRecoverColumnDefinitionsAfterRestart() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        firstManager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        ),
                        new Column(
                                "name",
                                DataType.STRING
                        ),
                        new Column(
                                "balance",
                                DataType.DOUBLE
                        )
                )
        );

        TableManager secondManager =
                new TableManager(tempDirectory);

        secondManager.loadCatalog();

        Table recoveredTable =
                secondManager.getTable("users");

        assertEquals(
                DataType.INT,
                recoveredTable
                        .getColumn("id")
                        .getDataType()
        );

        assertEquals(
                DataType.STRING,
                recoveredTable
                        .getColumn("name")
                        .getDataType()
        );

        assertEquals(
                DataType.DOUBLE,
                recoveredTable
                        .getColumn("balance")
                        .getDataType()
        );
    }

    @Test
    void shouldRecoverMetadataAfterRestart() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        TableMetadata originalMetadata =
                firstManager.createTable(
                        "products",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.LONG
                                ),
                                new Column(
                                        "name",
                                        DataType.STRING
                                )
                        )
                );

        TableManager secondManager =
                new TableManager(tempDirectory);

        secondManager.loadCatalog();

        TableMetadata recoveredMetadata =
                secondManager.getMetadata(
                        "products"
                );

        assertEquals(
                originalMetadata,
                recoveredMetadata
        );
    }

    @Test
    void shouldRecoverMultipleTables() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        firstManager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        firstManager.createTable(
                "orders",
                List.of(
                        new Column(
                                "id",
                                DataType.LONG
                        )
                )
        );

        firstManager.createTable(
                "products",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        TableManager secondManager =
                new TableManager(tempDirectory);

        secondManager.loadCatalog();

        assertEquals(
                3,
                secondManager.getTableCount()
        );

        assertTrue(
                secondManager.exists("users")
        );

        assertTrue(
                secondManager.exists("orders")
        );

        assertTrue(
                secondManager.exists("products")
        );
    }

    @Test
    void shouldLoadEmptyCatalogFromEmptyDirectory() {

        TableManager manager =
                new TableManager(tempDirectory);

        manager.loadCatalog();

        assertEquals(
                0,
                manager.getTableCount()
        );

        assertTrue(
                manager.listTables().isEmpty()
        );

        assertTrue(
                manager.listTableNames().isEmpty()
        );
    }

    @Test
    void shouldAllowCatalogReload() {

        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        manager.loadCatalog();

        assertEquals(
                1,
                manager.getTableCount()
        );

        /*
         * Aynı manager üzerinde tekrar recovery
         * yapılabilmeli ve duplicate oluşmamalıdır.
         */
        manager.loadCatalog();

        assertEquals(
                1,
                manager.getTableCount()
        );

        assertTrue(
                manager.exists("users")
        );
    }
    @Test
    void shouldKeepExistingCatalogWhenRecoveryFails()
            throws IOException {

        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        Path corruptedTable =
                tempDirectory.resolve(
                        "products.tbl"
                );

        Files.writeString(
                corruptedTable,
                """
                INVALID_TABLE_FILE
                """,
                StandardCharsets.UTF_8
        );

        assertThrows(
                RuntimeException.class,
                manager::loadCatalog
        );

        /*
         * Recovery başarısız olsa bile önceki
         * katalog durumu korunmalıdır.
         */
        assertEquals(
                1,
                manager.getTableCount()
        );

        assertTrue(
                manager.getTableCatalog()
                        .containsTable("users")
        );
    }

    @Test
    void shouldDropRecoveredTable() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        firstManager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        TableManager secondManager =
                new TableManager(tempDirectory);

        secondManager.loadCatalog();

        assertTrue(
                secondManager.exists("users")
        );

        secondManager.dropTable("users");

        assertFalse(
                secondManager.exists("users")
        );

        assertEquals(
                0,
                secondManager.getTableCount()
        );

        assertFalse(
                Files.exists(
                        tempDirectory.resolve(
                                "users.tbl"
                        )
                )
        );
    }
    @Test
    void shouldNotRecoverDroppedTable() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        firstManager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        TableManager secondManager =
                new TableManager(tempDirectory);

        secondManager.loadCatalog();

        secondManager.dropTable("users");

        TableManager thirdManager =
                new TableManager(tempDirectory);

        thirdManager.loadCatalog();

        assertFalse(
                thirdManager.exists("users")
        );

        assertEquals(
                0,
                thirdManager.getTableCount()
        );
    }
    @Test
    void shouldIgnoreNonTableFiles()
            throws IOException {

        Files.writeString(
                tempDirectory.resolve("readme.txt"),
                "NOT_A_TABLE"
        );

        Files.writeString(
                tempDirectory.resolve("config.json"),
                "{}"
        );

        TableManager manager =
                new TableManager(tempDirectory);

        manager.loadCatalog();

        assertEquals(
                0,
                manager.getTableCount()
        );
    }
    @Test
    void shouldIgnoreDirectoriesEndingWithTbl()
            throws IOException {

        Files.createDirectory(
                tempDirectory.resolve(
                        "fake.tbl"
                )
        );

        TableManager manager =
                new TableManager(tempDirectory);

        manager.loadCatalog();

        assertEquals(
                0,
                manager.getTableCount()
        );
    }
    @Test
    void shouldCreateNewTableAfterRecovery() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        firstManager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        TableManager secondManager =
                new TableManager(tempDirectory);

        secondManager.loadCatalog();

        secondManager.createTable(
                "orders",
                List.of(
                        new Column(
                                "id",
                                DataType.LONG
                        )
                )
        );

        assertEquals(
                2,
                secondManager.getTableCount()
        );

        assertTrue(
                secondManager.exists("users")
        );

        assertTrue(
                secondManager.exists("orders")
        );
    }

    @Test
    void shouldPreserveTableOrderAfterRecovery() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        firstManager.createTable(
                "alpha",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        firstManager.createTable(
                "beta",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        TableManager secondManager =
                new TableManager(tempDirectory);

        secondManager.loadCatalog();

        assertEquals(
                List.of(
                        "alpha",
                        "beta"
                ),
                secondManager.listTableNames()
        );
    }
    @Test
    void shouldRejectDuplicateTableAfterRecovery() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        firstManager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        TableManager secondManager =
                new TableManager(tempDirectory);

        secondManager.loadCatalog();

        assertThrows(
                TableAlreadyExistsException.class,
                () -> secondManager.createTable(
                        "users",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INT
                                )
                        )
                )
        );
    }
    @Test
    void shouldRemoveRecoveredMetadataWhenTableIsDropped() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        firstManager.createTable(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        TableManager secondManager =
                new TableManager(tempDirectory);

        secondManager.loadCatalog();

        assertNotNull(
                secondManager.getMetadata("users")
        );

        secondManager.dropTable("users");

        assertThrows(
                TableNotFoundException.class,
                () -> secondManager.getMetadata(
                        "users"
                )
        );
    }


}