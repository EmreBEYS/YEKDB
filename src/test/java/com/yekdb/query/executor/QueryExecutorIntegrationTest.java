package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import com.yekdb.query.command.CreateDatabaseCommand;
import com.yekdb.query.command.CreateTableCommand;
import com.yekdb.query.command.DropDatabaseCommand;
import com.yekdb.query.command.DropTableCommand;
import com.yekdb.query.command.UseDatabaseCommand;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;

import java.util.List;

class QueryExecutorIntegrationTest {

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
    void fullQueryExecutionScenario_shouldWorkSuccessfully() {

        ExecuteResult result;

        /*
         * CREATE DATABASE
         */
        result = queryExecutor.execute(
                new CreateDatabaseCommand("testdb")
        );

        assertTrue(result.isSuccess());

        /*
         * USE DATABASE
         */
        result = queryExecutor.execute(
                new UseDatabaseCommand("testdb")
        );

        assertTrue(result.isSuccess());

        /*
         * CREATE TABLE
         */
        result = queryExecutor.execute(
                new CreateTableCommand(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING),
                                new Column("age", DataType.INT)
                        )
                )
        );

        assertTrue(result.isSuccess());

        /*
         * INSERT - SQL String
         */
        result = queryExecutor.execute(
                """
                INSERT INTO users
                VALUES (1, 'Emre', 21);
                """
        );

        assertTrue(result.isSuccess());
        assertEquals(
                1,
                result.getAffectedRows()
        );

        /*
         * SELECT - SQL String
         */
        result = queryExecutor.execute(
                """
                SELECT *
                FROM users;
                """
        );

        assertTrue(result.isSuccess());
        assertEquals(
                1,
                result.getRowCount()
        );

        assertEquals(
                1,
                result.getRows()
                        .getFirst()
                        .getValue(0)
        );

        assertEquals(
                "Emre",
                result.getRows()
                        .getFirst()
                        .getValue(1)
        );

        assertEquals(
                21,
                result.getRows()
                        .getFirst()
                        .getValue(2)
        );

        /*
         * DELETE - SQL String
         */
        result = queryExecutor.execute(
                """
                DELETE FROM users
                WHERE record_id = 0;
                """
        );

        assertTrue(result.isSuccess());
        assertEquals(
                1,
                result.getAffectedRows()
        );

        /*
         * SELECT AGAIN
         */
        result = queryExecutor.execute(
                """
                SELECT *
                FROM users;
                """
        );

        assertTrue(result.isSuccess());
        assertEquals(
                0,
                result.getRowCount()
        );

        /*
         * DROP TABLE
         */
        result = queryExecutor.execute(
                new DropTableCommand("users")
        );

        assertTrue(result.isSuccess());

        /*
         * DROP DATABASE
         */
        result = queryExecutor.execute(
                new DropDatabaseCommand("testdb")
        );

        assertTrue(result.isSuccess());
    }
}