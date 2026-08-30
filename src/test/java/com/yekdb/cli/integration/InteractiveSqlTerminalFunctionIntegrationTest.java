package com.yekdb.cli.integration;

import com.yekdb.cli.command.TerminalCommandParser;
import com.yekdb.cli.executor.SqlTerminalExecutor;
import com.yekdb.cli.metadata.TerminalMetadataService;
import com.yekdb.cli.output.QueryResultFormatter;
import com.yekdb.cli.output.TerminalErrorHandler;
import com.yekdb.cli.output.TerminalOutput;
import com.yekdb.cli.terminal.InteractiveSqlTerminal;
import com.yekdb.cli.terminal.TerminalConfig;
import com.yekdb.cli.terminal.TerminalSession;
import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import com.yekdb.query.executor.QueryExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 00-28 Phase 8.
 * Scalar SQL function desteğinin gerçek Interactive SQL Terminal
 * hattında uçtan uca çalışmasını doğrular.
 */
class InteractiveSqlTerminalFunctionIntegrationTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldExecuteScalarFunctionsInSelectProjection() {

        TerminalResult result = runTerminal(
                """
                CREATE DATABASE function_projection_db;
                USE DATABASE function_projection_db;
                CREATE TABLE users (
                    id INT,
                    name STRING,
                    city STRING,
                    balance INT
                );
                INSERT INTO users (id, name, city, balance)
                VALUES (1, 'YEKDB', 'Malatya', -250);
                SELECT LOWER(city) AS city_lower,
                       UPPER(name) AS name_upper,
                       ABS(balance) AS balance_abs
                FROM users;
                \\q
                """
        );

        assertTrue(result.output().contains("malatya"));
        assertTrue(result.output().contains("YEKDB"));
        assertTrue(result.output().contains("250"));
        assertTrue(result.error().isBlank());
        assertFalse(result.session().isRunning());
    }

    @Test
    void shouldExecuteScalarFunctionInsideWhereClause() {

        TerminalResult result = runTerminal(
                """
                CREATE DATABASE function_where_db;
                USE DATABASE function_where_db;
                CREATE TABLE users (
                    id INT,
                    name STRING,
                    city STRING
                );
                INSERT INTO users (id, name, city)
                VALUES (1, 'Emre', 'MALATYA');
                INSERT INTO users (id, name, city)
                VALUES (2, 'Other', 'ANKARA');
                SELECT name
                FROM users
                WHERE LOWER(city) = 'malatya';
                \\q
                """
        );

        assertTrue(result.output().contains("Emre"));
        assertFalse(result.output().contains("Other"));
        assertTrue(result.output().contains("1 row"));
        assertTrue(result.error().isBlank());
    }

    @Test
    void shouldExecuteNestedScalarFunctions() {

        TerminalResult result = runTerminal(
                """
                CREATE DATABASE nested_function_db;
                USE DATABASE nested_function_db;
                CREATE TABLE users (
                    id INT,
                    name STRING
                );
                INSERT INTO users (id, name)
                VALUES (1, '  YEKDB  ');
                SELECT LENGTH(TRIM(name)) AS clean_length
                FROM users;
                \\q
                """
        );

        assertTrue(result.output().contains("clean_length"));
        assertTrue(result.output().contains("5"));
        assertTrue(result.error().isBlank());
    }

    @Test
    void shouldRecoverAfterScalarFunctionExecutionError() {

        TerminalResult result = runTerminal(
                """
                CREATE DATABASE function_error_db;
                USE DATABASE function_error_db;
                CREATE TABLE users (
                    id INT,
                    name STRING
                );
                INSERT INTO users (id, name)
                VALUES (1, 'YEKDB');
                SELECT UNKNOWN_FUNC(name) FROM users;
                SELECT UPPER(name) AS name_upper FROM users;
                \\q
                """
        );

        assertTrue(
                result.error().contains(
                        "ERROR: Unknown scalar function in SELECT projection: UNKNOWN_FUNC"
                )
        );

        /*
         * Function hatası terminal REPL döngüsünü öldürmemeli.
         * Sonraki geçerli sorgu normal şekilde çalışmalı.
         */
        assertTrue(result.output().contains("name_upper"));
        assertTrue(result.output().contains("YEKDB"));
        assertFalse(result.session().isRunning());
    }

    private TerminalResult runTerminal(String input) {

        DatabaseManager databaseManager =
                new DatabaseManager(tempDirectory);

        StorageQueryDataSource queryDataSource =
                new StorageQueryDataSource(databaseManager);

        QueryExecutor queryExecutor =
                new QueryExecutor(
                        databaseManager,
                        queryDataSource
                );

        ByteArrayOutputStream outputBuffer =
                new ByteArrayOutputStream();

        ByteArrayOutputStream errorBuffer =
                new ByteArrayOutputStream();

        TerminalOutput output =
                new TerminalOutput(
                        new PrintStream(outputBuffer),
                        new PrintStream(errorBuffer)
                );

        TerminalSession session =
                new TerminalSession();

        TerminalErrorHandler errorHandler =
                new TerminalErrorHandler(
                        output,
                        false
                );

        InteractiveSqlTerminal terminal =
                new InteractiveSqlTerminal(
                        TerminalConfig.defaultConfig(),
                        session,
                        new TerminalCommandParser(),
                        new SqlTerminalExecutor(queryExecutor),
                        output,
                        new QueryResultFormatter(),
                        errorHandler,
                        new TerminalMetadataService(databaseManager),
                        new StringReader(input)
                );

        try {
            terminal.run();
        } finally {
            queryExecutor.close();
        }

        return new TerminalResult(
                outputBuffer.toString(),
                errorBuffer.toString(),
                session
        );
    }

    private record TerminalResult(
            String output,
            String error,
            TerminalSession session
    ) {
    }
}
