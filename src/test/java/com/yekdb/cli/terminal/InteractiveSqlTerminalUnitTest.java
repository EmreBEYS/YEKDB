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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractiveSqlTerminalUnitTest {

    @TempDir
    Path tempDirectory;

    /**
     * \q komutu terminal session'ını
     * kontrollü biçimde sonlandırmalıdır.
     */
    @Test
    void shouldStopTerminalWhenQuitCommandIsExecuted() {

        TerminalRunResult result =
                runTerminal(
                        "\\q\n"
                );

        assertFalse(
                result.session().isRunning()
        );

        assertTrue(
                result.output().contains(
                        "Bye."
                )
        );
    }

    /**
     * \history komutunun kendisi
     * command history içerisine eklenmemelidir.
     */
    @Test
    void shouldNotAddHistoryCommandToHistory() {

        TerminalRunResult result =
                runTerminal(
                        "\\history\n"
                                + "\\q\n"
                );

        assertTrue(
                result.output().contains(
                        "History is empty."
                )
        );

        assertFalse(
                result.session()
                        .getHistory()
                        .contains(
                                "\\history"
                        )
        );
    }

    /**
     * Bilinmeyen terminal komutu
     * REPL session'ını sonlandırmamalıdır.
     *
     * Sonraki \help komutunun çalışması
     * terminalin devam ettiğini doğrular.
     */
    @Test
    void shouldContinueAfterUnknownCommand() {

        TerminalRunResult result =
                runTerminal(
                        "\\unknown\n"
                                + "\\help\n"
                                + "\\q\n"
                );

        assertTrue(
                result.error().contains(
                        "ERROR: Unknown terminal command."
                )
        );

        assertTrue(
                result.output().contains(
                        "YEKDB Terminal Commands"
                )
        );

        assertTrue(
                result.output().contains(
                        "Bye."
                )
        );
    }

    /**
     * SQL execution sırasında hata oluşması
     * terminal session'ını sonlandırmamalıdır.
     *
     * Hatalı SQL sonrasında \help çalışabiliyorsa
     * REPL devam etmiş demektir.
     */
    @Test
    void shouldContinueAfterSqlExecutionError() {

        TerminalRunResult result =
                runTerminal(
                        "THIS IS NOT SQL;\n"
                                + "\\help\n"
                                + "\\q\n"
                );

        assertTrue(
                result.error().contains(
                        "ERROR:"
                )
        );

        assertTrue(
                result.output().contains(
                        "YEKDB Terminal Commands"
                )
        );

        assertTrue(
                result.output().contains(
                        "Bye."
                )
        );
    }

    /**
     * Birden fazla satırdan oluşan SQL statement
     * history içerisinde tek kayıt olarak
     * tutulmalıdır.
     */
    @Test
    void shouldStoreMultiLineSqlAsSingleHistoryEntry() {

        TerminalRunResult result =
                runTerminal(
                        "SELECT\n"
                                + "FROM;\n"
                                + "\\history\n"
                                + "\\q\n"
                );

        /*
         * SQL statement + \q.
         *
         * \history kendisini history'ye eklemez.
         */
        assertTrue(
                result.session()
                        .getHistorySize() == 2
        );

        String firstHistoryEntry =
                result.session()
                        .getHistory()
                        .get(0);

        assertTrue(
                firstHistoryEntry.contains(
                        "SELECT"
                )
        );

        assertTrue(
                firstHistoryEntry.contains(
                        "FROM;"
                )
        );

        /*
         * History ekranında multi-line SQL
         * tek satır olarak gösterilmelidir.
         */
        assertTrue(
                result.output().contains(
                        "1  SELECT FROM;"
                )
        );
    }

    /**
     * Input stream EOF durumuna geldiğinde
     * terminal exception üretmeden
     * kontrollü şekilde kapanmalıdır.
     */
    @Test
    void shouldStopGracefullyOnEndOfFile() {

        TerminalRunResult result =
                runTerminal(
                        ""
                );

        assertFalse(
                result.session().isRunning()
        );

        assertTrue(
                result.output().contains(
                        "Bye."
                )
        );
    }

    /**
     * Test için gerçek terminal bağımlılıklarını
     * geçici filesystem üzerinde oluşturur.
     */
    private TerminalRunResult runTerminal(
            String input
    ) {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
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
                        new PrintStream(
                                outputBuffer
                        ),
                        new PrintStream(
                                errorBuffer
                        )
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

        return new TerminalRunResult(
                outputBuffer.toString(),
                errorBuffer.toString(),
                session
        );
    }

    private record TerminalRunResult(
            String output,
            String error,
            TerminalSession session
    ) {
    }
}