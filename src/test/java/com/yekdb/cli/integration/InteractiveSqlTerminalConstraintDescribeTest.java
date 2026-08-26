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
import com.yekdb.query.executor.QueryExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractiveSqlTerminalConstraintDescribeTest {

    @TempDir
    Path tempDirectory;

    @Test
    void describeShouldDisplayColumnConstraints() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        QueryExecutor queryExecutor =
                new QueryExecutor(
                        databaseManager
                );

        queryExecutor.execute(
                "CREATE DATABASE phase7db;"
        );

        queryExecutor.execute(
                "USE DATABASE phase7db;"
        );

        queryExecutor.execute(
                "CREATE TABLE users (" +
                        "id INT PRIMARY KEY, " +
                        "username STRING UNIQUE, " +
                        "email STRING NOT NULL" +
                        ");"
        );

        ByteArrayOutputStream outputBytes =
                new ByteArrayOutputStream();

        ByteArrayOutputStream errorBytes =
                new ByteArrayOutputStream();

        TerminalOutput output =
                new TerminalOutput(
                        new PrintStream(outputBytes),
                        new PrintStream(errorBytes)
                );

        InteractiveSqlTerminal terminal =
                new InteractiveSqlTerminal(
                        TerminalConfig.defaultConfig(),
                        new TerminalSession(),
                        new TerminalCommandParser(),
                        new SqlTerminalExecutor(queryExecutor),
                        output,
                        new QueryResultFormatter(),
                        new TerminalErrorHandler(output, false),
                        new TerminalMetadataService(databaseManager),
                        new StringReader(
                                "\\d users\n\\q\n"
                        )
                );

        terminal.run();

        String terminalOutput =
                outputBytes.toString(
                        StandardCharsets.UTF_8
                );

        assertTrue(
                terminalOutput.contains("Constraints")
        );

        assertTrue(
                terminalOutput.contains("PRIMARY KEY")
        );

        assertTrue(
                terminalOutput.contains("UNIQUE")
        );

        assertTrue(
                terminalOutput.contains("NOT NULL")
        );
    }
}
