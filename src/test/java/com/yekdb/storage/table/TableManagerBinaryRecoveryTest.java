package com.yekdb.storage.table;

import com.yekdb.storage.table.header.TableHeader;
import com.yekdb.storage.table.header.TableHeaderIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableManagerBinaryRecoveryTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldContinueTableIdsAfterRestart()
            throws IOException {

        /*
         * İlk YEKDB instance.
         */
        TableManager firstManager =
                new TableManager(tempDirectory);

        firstManager.createTable(
                new Table(
                        "users",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INT
                                )
                        )
                )
        );

        firstManager.createTable(
                new Table(
                        "products",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INT
                                )
                        )
                )
        );

        TableHeader usersHeader =
                TableHeaderIO.read(
                        tempDirectory.resolve(
                                "users.tbl"
                        )
                );

        TableHeader productsHeader =
                TableHeaderIO.read(
                        tempDirectory.resolve(
                                "products.tbl"
                        )
                );

        assertEquals(
                1L,
                usersHeader.getTableId()
        );

        assertEquals(
                2L,
                productsHeader.getTableId()
        );

        /*
         * YEKDB restart simülasyonu.
         */
        TableManager restartedManager =
                new TableManager(tempDirectory);

        restartedManager.loadCatalog();

        restartedManager.createTable(
                new Table(
                        "orders",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INT
                                )
                        )
                )
        );

        TableHeader ordersHeader =
                TableHeaderIO.read(
                        tempDirectory.resolve(
                                "orders.tbl"
                        )
                );

        assertEquals(
                3L,
                ordersHeader.getTableId()
        );
    }

    @Test
    void shouldRecoverTablesAfterRestart() {

        TableManager firstManager =
                new TableManager(tempDirectory);

        Table users =
                new Table(
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

        firstManager.createTable(users);

        TableManager restartedManager =
                new TableManager(tempDirectory);

        restartedManager.loadCatalog();

        assertEquals(
                1,
                restartedManager.getTableCount()
        );

        assertTrue(
                restartedManager.exists("users")
        );

        assertEquals(
                users,
                restartedManager.getTable("users")
        );
    }
    @Test
    void shouldRejectHeaderSchemaColumnCountMismatch()
            throws IOException {

        TableManager manager =
                new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "users",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INT
                                )
                        )
                )
        );

        Path tableFile =
                tempDirectory.resolve("users.tbl");

        TableHeader original =
                TableHeaderIO.read(tableFile);

        TableHeader corrupted =
                new TableHeader(
                        original.getTableId(),
                        original.getTableName(),

                        // schema 1 column diyor,
                        // binary header artık 99 diyecek.
                        99,

                        original.getRowCount(),
                        original.getFirstDataPageId(),
                        original.getLastDataPageId(),
                        original.getSchemaOffset(),
                        original.getFlags()
                );

        TableHeaderIO.write(
                tableFile,
                corrupted
        );

        TableManager restartedManager =
                new TableManager(tempDirectory);

        assertThrows(
                RuntimeException.class,
                restartedManager::loadCatalog
        );
    }
}