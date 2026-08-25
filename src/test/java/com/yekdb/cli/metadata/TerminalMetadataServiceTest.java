package com.yekdb.cli.metadata;

import com.yekdb.database.DatabaseManager;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import com.yekdb.storage.table.TableManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalMetadataServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldRejectListTablesWhenNoDatabaseSelected() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        TerminalMetadataService metadataService =
                new TerminalMetadataService(
                        databaseManager
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        metadataService::listTables
                );

        assertEquals(
                "No database selected. Execute USE DATABASE first.",
                exception.getMessage()
        );
    }

    @Test
    void shouldReturnCurrentDatabaseName() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        databaseManager.createDatabase(
                "metadata_db"
        );

        databaseManager.useDatabase(
                "metadata_db"
        );

        TerminalMetadataService metadataService =
                new TerminalMetadataService(
                        databaseManager
                );

        assertEquals(
                "metadata_db",
                metadataService.getCurrentDatabaseName()
        );
    }

    @Test
    void shouldListTablesFromPersistentCatalog() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        databaseManager.createDatabase(
                "metadata_db"
        );

        databaseManager.useDatabase(
                "metadata_db"
        );

        TableManager tableManager =
                new TableManager(
                        databaseManager
                                .getCurrentDatabase()
                                .getDatabasePath()
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

        tableManager.createTable(
                "orders",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        )
                )
        );

        TerminalMetadataService metadataService =
                new TerminalMetadataService(
                        databaseManager
                );

        List<String> tableNames =
                metadataService.listTables();

        assertEquals(
                2,
                tableNames.size()
        );

        assertTrue(
                tableNames.contains(
                        "users"
                )
        );

        assertTrue(
                tableNames.contains(
                        "orders"
                )
        );
    }

    @Test
    void shouldDescribeExistingTable() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        databaseManager.createDatabase(
                "metadata_db"
        );

        databaseManager.useDatabase(
                "metadata_db"
        );

        TableManager tableManager =
                new TableManager(
                        databaseManager
                                .getCurrentDatabase()
                                .getDatabasePath()
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
                        ),
                        new Column(
                                "age",
                                DataType.INT
                        )
                )
        );

        TerminalMetadataService metadataService =
                new TerminalMetadataService(
                        databaseManager
                );

        Table table =
                metadataService.describeTable(
                        "users"
                );

        assertEquals(
                "users",
                table.getTableName()
        );

        assertEquals(
                3,
                table.getColumns().size()
        );

        assertEquals(
                "id",
                table.getColumns()
                        .get(0)
                        .getName()
        );

        assertEquals(
                DataType.INT,
                table.getColumns()
                        .get(0)
                        .getDataType()
        );

        assertEquals(
                "name",
                table.getColumns()
                        .get(1)
                        .getName()
        );

        assertEquals(
                DataType.STRING,
                table.getColumns()
                        .get(1)
                        .getDataType()
        );
    }

    @Test
    void shouldReturnDescribeColumns() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        databaseManager.createDatabase(
                "metadata_db"
        );

        databaseManager.useDatabase(
                "metadata_db"
        );

        TableManager tableManager =
                new TableManager(
                        databaseManager
                                .getCurrentDatabase()
                                .getDatabasePath()
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

        TerminalMetadataService metadataService =
                new TerminalMetadataService(
                        databaseManager
                );

        List<Column> columns =
                metadataService.describeColumns(
                        "users"
                );

        assertEquals(
                2,
                columns.size()
        );

        assertEquals(
                "id",
                columns.get(0).getName()
        );

        assertEquals(
                "name",
                columns.get(1).getName()
        );
    }

    @Test
    void shouldRejectBlankTableName() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        databaseManager.createDatabase(
                "metadata_db"
        );

        databaseManager.useDatabase(
                "metadata_db"
        );

        TerminalMetadataService metadataService =
                new TerminalMetadataService(
                        databaseManager
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> metadataService.describeTable(
                                "   "
                        )
                );

        assertEquals(
                "Table name cannot be null or blank.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectUnknownTable() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        databaseManager.createDatabase(
                "metadata_db"
        );

        databaseManager.useDatabase(
                "metadata_db"
        );

        TerminalMetadataService metadataService =
                new TerminalMetadataService(
                        databaseManager
                );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> metadataService.describeTable(
                                "missing_table"
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Table not found"
                        )
        );
    }
}