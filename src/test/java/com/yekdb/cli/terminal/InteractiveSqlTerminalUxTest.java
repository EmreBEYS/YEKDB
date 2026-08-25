package com.yekdb.cli.terminal;

import com.yekdb.cli.command.TerminalCommandParser;
import com.yekdb.cli.executor.SqlTerminalExecutor;
import com.yekdb.cli.metadata.TerminalMetadataService;
import com.yekdb.cli.output.QueryResultFormatter;
import com.yekdb.cli.output.TerminalErrorHandler;
import com.yekdb.cli.output.TerminalOutput;
import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import com.yekdb.query.executor.QueryExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractiveSqlTerminalUxTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldUseDefaultPromptWhenNoDatabaseIsSelected() {

        TerminalRunResult result =
                runTerminal(
                        "\\q\n",
                        false
                );

        assertTrue(
                result.output().contains(
                        "yekdb> "
                )
        );
    }

    @Test
    void shouldShowDatabaseNameInPromptWhenDatabaseIsSelected() {

        TerminalRunResult result =
                runTerminal(
                        "\\q\n",
                        true
                );

        assertTrue(
                result.output().contains(
                        "yekdb[ux_db]> "
                )
        );
    }

    @Test
    void shouldUseContinuationPromptForIncompleteSql() {

        TerminalRunResult result =
                runTerminal(
                        "SELECT\n",
                        false
                );

        assertTrue(
                result.output().contains(
                        "...> "
                )
        );
    }

    @Test
    void shouldShowHelpfulUnknownCommandError() {

        TerminalRunResult result =
                runTerminal(
                        "\\unknown\n\\q\n",
                        false
                );

        assertTrue(
                result.error().contains(
                        "ERROR: Unknown terminal command. Type \\help for help."
                )
        );
    }

    @Test
    void shouldShowEmptyTableMessageWithDatabaseName() {

        TerminalRunResult result =
                runTerminal(
                        "\\tables\n\\q\n",
                        true
                );

        assertTrue(
                result.output().contains(
                        "No tables found in database 'ux_db'."
                )
        );
    }

    private TerminalRunResult runTerminal(
            String input,
            boolean selectDatabase
    ) {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        if (selectDatabase) {

            databaseManager.createDatabase(
                    "ux_db"
            );

            databaseManager.useDatabase(
                    "ux_db"
            );
        }

        StorageQueryDataSource queryDataSource =
                new StorageQueryDataSource(
                        databaseManager
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

        QueryExecutor queryExecutor =
                new QueryExecutor(
                        databaseManager,
                        queryDataSource
                );

        try {

            TerminalErrorHandler errorHandler =
                    new TerminalErrorHandler(
                            output,
                            false
                    );

            InteractiveSqlTerminal terminal =
                    new InteractiveSqlTerminal(
                            TerminalConfig.defaultConfig(),
                            new TerminalSession(),
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

        return new TerminalRunResult(
                outputBuffer.toString(),
                errorBuffer.toString()
        );
    }

    private record TerminalRunResult(
            String output,
            String error
    ) {
    }
}
