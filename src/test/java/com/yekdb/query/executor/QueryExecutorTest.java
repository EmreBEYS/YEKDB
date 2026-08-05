package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.command.CreateDatabaseCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import com.yekdb.query.command.UseDatabaseCommand;

import com.yekdb.table.Column;
import com.yekdb.table.TableManager;
import com.yekdb.table.DataType;
import com.yekdb.query.command.CreateTableCommand;

import java.util.List;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.command.DropTableCommand;
import com.yekdb.query.command.DropDatabaseCommand;
import com.yekdb.storage.record.Row;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QueryExecutor birim testleri.
 */
class QueryExecutorTest {

    @TempDir
    Path tempDirectory;

    private DatabaseManager databaseManager;
    private QueryExecutor queryExecutor;

    @BeforeEach
    void setUp() {
        databaseManager = new DatabaseManager(
                tempDirectory.resolve("data")
        );

        queryExecutor = new QueryExecutor(
                databaseManager
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        queryExecutor.close();
    }

    @Test
    void createDatabase_shouldCreateDatabaseSuccessfully() {
        CreateDatabaseCommand command =
                new CreateDatabaseCommand("testdb");

        ExecuteResult result =
                queryExecutor.execute(command);

        assertTrue(result.isSuccess());

        assertEquals(
                "Database created successfully: testdb",
                result.getMessage()
        );

        assertEquals(
                0,
                result.getAffectedRows()
        );

        assertTrue(
                databaseManager.exists("testdb")
        );

        assertTrue(
                databaseManager.listDatabases()
                        .contains("testdb")
        );

        assertNotNull(result);
    }
    @Test
    void useDatabase_shouldSelectDatabaseSuccessfully() {

        queryExecutor.execute(
                new CreateDatabaseCommand("testdb")
        );

        ExecuteResult result =
                queryExecutor.execute(
                        new UseDatabaseCommand("testdb")
                );

        assertTrue(result.isSuccess());

        assertEquals(
                "Database selected successfully: testdb",
                result.getMessage()
        );

        assertNotNull(
                databaseManager.getCurrentDatabase()
        );

        assertEquals(
                "testdb",
                databaseManager
                        .getCurrentDatabase()
                        .getName()
        );
    }
    @Test
    void createTable_shouldCreateTableSuccessfully() {

        queryExecutor.execute(
                new CreateDatabaseCommand("testdb")
        );

        queryExecutor.execute(
                new UseDatabaseCommand("testdb")
        );

        List<Column> columns = List.of(
                new Column("id", DataType.INT),
                new Column("name", DataType.STRING),
                new Column("age", DataType.INT)
        );

        CreateTableCommand command =
                new CreateTableCommand(
                        "users",
                        columns
                );

        ExecuteResult result =
                queryExecutor.execute(command);

        assertTrue(result.isSuccess());

        assertEquals(
                "Table created successfully: users",
                result.getMessage()
        );

        assertTrue(
                databaseManager
                        .getCurrentDatabase() != null
        );

        TableManager tableManager =
                new TableManager(
                        databaseManager
                                .getCurrentDatabase()
                                .getDatabasePath()
                );

        assertTrue(
                tableManager.exists("users")
        );
    }
    @Test
    void insert_shouldInsertRecordSuccessfully() {

        queryExecutor.execute(
                new CreateDatabaseCommand("testdb")
        );

        queryExecutor.execute(
                new UseDatabaseCommand("testdb")
        );

        List<Column> columns = List.of(
                new Column("id", DataType.INT),
                new Column("name", DataType.STRING),
                new Column("age", DataType.INT)
        );

        queryExecutor.execute(
                new CreateTableCommand(
                        "users",
                        columns
                )
        );

        InsertCommand command =
                new InsertCommand(
                        "users",
                        List.of(
                                1,
                                "Emre",
                                21
                        )
                );

        ExecuteResult result =
                queryExecutor.execute(command);

        assertTrue(result.isSuccess());

        assertEquals(
                1,
                result.getAffectedRows()
        );

        assertTrue(
                result.getMessage()
                        .contains("Record inserted successfully")
        );
    }
    @Test
    void select_shouldReturnInsertedRows() {

        queryExecutor.execute(
                new CreateDatabaseCommand("testdb")
        );

        queryExecutor.execute(
                new UseDatabaseCommand("testdb")
        );

        List<Column> columns = List.of(
                new Column("id", DataType.INT),
                new Column("name", DataType.STRING),
                new Column("age", DataType.INT)
        );

        queryExecutor.execute(
                new CreateTableCommand(
                        "users",
                        columns
                )
        );

        queryExecutor.execute(
                new InsertCommand(
                        "users",
                        List.of(
                                1,
                                "Emre",
                                21
                        )
                )
        );

        ExecuteResult result =
                queryExecutor.execute(
                        SelectCommand.allFrom("users")
                );

        assertTrue(result.isSuccess());

        assertNotNull(
                result.getRows()
        );

        assertEquals(
                1,
                result.getRows().size()
        );

        Row row = result.getRows().getFirst();

        assertEquals(
                1,
                row.getValue(0)
        );

        assertEquals(
                "Emre",
                row.getValue(1)
        );

        assertEquals(
                21,
                row.getValue(2)
        );
    }
    @Test
    void delete_shouldDeleteRecordSuccessfully() {

        queryExecutor.execute(
                new CreateDatabaseCommand("testdb")
        );

        queryExecutor.execute(
                new UseDatabaseCommand("testdb")
        );

        List<Column> columns = List.of(
                new Column("id", DataType.INT),
                new Column("name", DataType.STRING),
                new Column("age", DataType.INT)
        );

        queryExecutor.execute(
                new CreateTableCommand(
                        "users",
                        columns
                )
        );

        queryExecutor.execute(
                new InsertCommand(
                        "users",
                        List.of(
                                1,
                                "Emre",
                                21
                        )
                )
        );

        ExecuteResult deleteResult =
                queryExecutor.execute(
                        new DeleteCommand(
                                "users",
                                "record_id = 0"
                        )
                );

        assertTrue(deleteResult.isSuccess());

        assertEquals(
                1,
                deleteResult.getAffectedRows()
        );

        ExecuteResult selectResult =
                queryExecutor.execute(
                        SelectCommand.allFrom("users")
                );

        assertTrue(selectResult.isSuccess());

        assertEquals(
                0,
                selectResult.getRows().size()
        );
    }
    @Test
    void dropTable_shouldDropTableSuccessfully() {

        queryExecutor.execute(
                new CreateDatabaseCommand("testdb")
        );

        queryExecutor.execute(
                new UseDatabaseCommand("testdb")
        );

        List<Column> columns = List.of(
                new Column("id", DataType.INT),
                new Column("name", DataType.STRING)
        );

        queryExecutor.execute(
                new CreateTableCommand(
                        "users",
                        columns
                )
        );

        ExecuteResult result =
                queryExecutor.execute(
                        new DropTableCommand("users")
                );

        assertTrue(result.isSuccess());

        assertEquals(
                "Table dropped successfully: users",
                result.getMessage()
        );

        Path tableFile =
                databaseManager
                        .getCurrentDatabase()
                        .getDatabasePath()
                        .resolve("users.tbl");

        Path dataFile =
                databaseManager
                        .getCurrentDatabase()
                        .getDatabasePath()
                        .resolve("users.data");

        assertFalse(
                java.nio.file.Files.exists(tableFile)
        );

        assertFalse(
                java.nio.file.Files.exists(dataFile)
        );
    }
    @Test
    void dropDatabase_shouldDropDatabaseSuccessfully() {

        queryExecutor.execute(
                new CreateDatabaseCommand("testdb")
        );

        queryExecutor.execute(
                new UseDatabaseCommand("testdb")
        );

        ExecuteResult result =
                queryExecutor.execute(
                        new DropDatabaseCommand("testdb")
                );

        assertTrue(result.isSuccess());

        assertEquals(
                "Database dropped successfully: testdb",
                result.getMessage()
        );

        assertFalse(
                databaseManager.exists("testdb")
        );

        assertNull(
                databaseManager.getCurrentDatabase()
        );
    }
}