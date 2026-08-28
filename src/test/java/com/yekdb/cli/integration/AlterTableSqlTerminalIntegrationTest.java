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
 * Sprint 00-26 Phase 7.
 *
 * ALTER TABLE desteğinin gerçek Interactive SQL Terminal hattı üzerinden
 * uçtan uca çalıştığını doğrular.
 */
class AlterTableSqlTerminalIntegrationTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldExecuteColumnAlterFlowThroughInteractiveTerminal() {

        TerminalResult result = runTerminal(
                """
                CREATE DATABASE alter_column_db;
                USE DATABASE alter_column_db;
                CREATE TABLE users (
                    id INT,
                    name STRING
                );
                ALTER TABLE users ADD COLUMN email STRING;
                ALTER TABLE users RENAME COLUMN name TO full_name;
                ALTER TABLE users ALTER COLUMN email SET NOT NULL;
                ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);
                INSERT INTO users (id, full_name, email)
                VALUES (1, 'Emre', 'emre@example.com');
                SELECT * FROM users;
                \\describe users
                \\q
                """
        );

        assertTrue(result.output().contains("Column added successfully: email"));
        assertTrue(result.output().contains("Column renamed successfully: name -> full_name"));
        assertTrue(result.output().contains("NOT NULL added successfully: email"));
        assertTrue(result.output().contains("Constraint added successfully: uq_users_email"));
        assertTrue(result.output().contains("full_name"));
        assertTrue(result.output().contains("emre@example.com"));
        assertTrue(result.error().isBlank());
        assertFalse(result.session().isRunning());
    }

    @Test
    void shouldRenameTableThroughInteractiveTerminal() {

        TerminalResult result = runTerminal(
                """
                CREATE DATABASE alter_rename_db;
                USE DATABASE alter_rename_db;
                CREATE TABLE users (
                    id INT,
                    name STRING
                );
                ALTER TABLE users RENAME TO customers;
                \\tables
                \\describe customers
                \\q
                """
        );

        assertTrue(result.output().contains("Table renamed successfully: users -> customers"));
        assertTrue(result.output().contains("customers"));
        assertTrue(result.output().contains("Table: customers"));
        assertTrue(result.error().isBlank());
    }

    @Test
    void shouldEnforceUniqueAddedByAlterTableThroughTerminal() {

        TerminalResult result = runTerminal(
                """
                CREATE DATABASE alter_unique_db;
                USE DATABASE alter_unique_db;
                CREATE TABLE users (
                    id INT,
                    email STRING
                );
                ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);
                INSERT INTO users (id, email) VALUES (1, 'same@example.com');
                INSERT INTO users (id, email) VALUES (2, 'same@example.com');
                SELECT * FROM users;
                \\q
                """
        );

        assertTrue(result.output().contains("Constraint added successfully: uq_users_email"));
        assertTrue(result.error().contains("UNIQUE"));
        assertTrue(result.output().contains("same@example.com"));
        assertFalse(result.session().isRunning());
    }

    @Test
    void shouldEnforceForeignKeyAddedByAlterTableThroughTerminal() {

        TerminalResult result = runTerminal(
                """
                CREATE DATABASE alter_fk_db;
                USE DATABASE alter_fk_db;
                CREATE TABLE users (
                    id INT PRIMARY KEY
                );
                CREATE TABLE orders (
                    id INT PRIMARY KEY,
                    user_id INT
                );
                ALTER TABLE orders ADD CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id);
                INSERT INTO orders (id, user_id) VALUES (1, 999);
                INSERT INTO users (id) VALUES (999);
                INSERT INTO orders (id, user_id) VALUES (2, 999);
                SELECT * FROM orders;
                \\q
                """
        );

        assertTrue(result.output().contains("Constraint added successfully: fk_orders_user"));
        assertTrue(result.error().contains("FOREIGN KEY"));
        assertTrue(result.output().contains("999"));
        assertFalse(result.session().isRunning());
    }

    @Test
    void shouldDropNamedConstraintThroughInteractiveTerminal() {

        TerminalResult result = runTerminal(
                """
                CREATE DATABASE alter_drop_constraint_db;
                USE DATABASE alter_drop_constraint_db;
                CREATE TABLE users (
                    id INT,
                    email STRING
                );
                ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);
                ALTER TABLE users DROP CONSTRAINT uq_users_email;
                INSERT INTO users (id, email) VALUES (1, 'same@example.com');
                INSERT INTO users (id, email) VALUES (2, 'same@example.com');
                SELECT * FROM users;
                \\q
                """
        );

        assertTrue(result.output().contains("Constraint dropped successfully: uq_users_email"));
        assertTrue(result.output().contains("2 row(s)"));
        assertTrue(result.error().isBlank());
        assertFalse(result.session().isRunning());
    }

    private TerminalResult runTerminal(String input) {

        DatabaseManager databaseManager = new DatabaseManager(tempDirectory);
        StorageQueryDataSource queryDataSource = new StorageQueryDataSource(databaseManager);
        QueryExecutor queryExecutor = new QueryExecutor(databaseManager, queryDataSource);

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();

        TerminalOutput output = new TerminalOutput(
                new PrintStream(outputBuffer),
                new PrintStream(errorBuffer)
        );

        TerminalSession session = new TerminalSession();
        TerminalErrorHandler errorHandler = new TerminalErrorHandler(output, false);
        TerminalMetadataService metadataService = new TerminalMetadataService(databaseManager);

        InteractiveSqlTerminal terminal = new InteractiveSqlTerminal(
                TerminalConfig.defaultConfig(),
                session,
                new TerminalCommandParser(),
                new SqlTerminalExecutor(queryExecutor),
                output,
                new QueryResultFormatter(),
                errorHandler,
                metadataService,
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
