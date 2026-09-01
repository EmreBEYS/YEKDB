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

import static org.junit.jupiter.api.Assertions.*;

class ViewTriggerSqlTerminalIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void terminalShouldExecuteViewAndTriggerWorkflow() {
        TerminalResult result =
                runTerminal(
                        """
                        CREATE DATABASE terminal_view_trigger_db;
                        USE DATABASE terminal_view_trigger_db;
                        CREATE TABLE users (
                            id INT,
                            name STRING,
                            age INT
                        );
                        CREATE TABLE audit_log (
                            id INT,
                            user_id INT,
                            message STRING
                        );
                        CREATE VIEW adult_users AS
                        SELECT id, name FROM users WHERE age >= 18;
                        CREATE TRIGGER users_after_insert
                        AFTER INSERT ON users
                        BEGIN
                            INSERT INTO audit_log (id, user_id, message)
                            VALUES (NEW.id, NEW.id, NEW.name)
                        END;
                        INSERT INTO users (id, name, age) VALUES (1, 'Emre', 21);
                        SHOW VIEWS;
                        SHOW TRIGGERS FROM users;
                        SELECT name FROM adult_users;
                        SELECT * FROM audit_log;
                        \\q
                        """
                );

        assertTrue(result.error().isBlank());
        assertFalse(result.session().isRunning());

        assertTrue(result.output().contains("Database created successfully"));
        assertTrue(result.output().contains("View created successfully: adult_users"));
        assertTrue(result.output().contains("Trigger created successfully: users_after_insert"));
        assertTrue(result.output().contains("Views listed successfully."));
        assertTrue(result.output().contains("Triggers listed successfully."));
        assertTrue(result.output().contains("adult_users"));
        assertTrue(result.output().contains("users_after_insert"));
        assertTrue(result.output().contains("Emre"));
        assertTrue(result.output().contains("Bye."));
    }

    private TerminalResult runTerminal(
            String input
    ) {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        StorageQueryDataSource queryDataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

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

        try {
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
                            new SqlTerminalExecutor(
                                    queryExecutor
                            ),
                            output,
                            new QueryResultFormatter(),
                            errorHandler,
                            new TerminalMetadataService(
                                    databaseManager
                            ),
                            new StringReader(
                                    input
                            )
                    );

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
