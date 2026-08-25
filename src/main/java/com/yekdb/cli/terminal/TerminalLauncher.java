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

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * YEKDB Interactive SQL Terminal
 * başlangıç noktasıdır.
 */
public final class TerminalLauncher {

    /**
     * Varsayılan YEKDB veri dizini.
     *
     * JVM property ile değiştirilebilir:
     *
     * -Dyekdb.data.dir=C:/yekdb/data
     */
    private static final String DEFAULT_DATA_DIRECTORY =
            "data";

    private TerminalLauncher() {
    }

    public static void main(
            String[] args
    ) {

        /*
         * Terminal configuration.
         */
        TerminalConfig config =
                TerminalConfig.defaultConfig();

        /*
         * Terminal session.
         */
        TerminalSession session =
                new TerminalSession();

        /*
         * Meta-command parser.
         */
        TerminalCommandParser commandParser =
                new TerminalCommandParser();

        /*
         * Terminal output abstraction.
         */
        TerminalOutput output =
                new TerminalOutput();

        /*
         * SELECT ve DDL/DML sonuçlarını
         * formatlayan katman.
         */
        QueryResultFormatter resultFormatter =
                new QueryResultFormatter();

        /*
         * Merkezi terminal hata yönetimi.
         */
        TerminalErrorHandler errorHandler =
                new TerminalErrorHandler(
                        output,
                        config.isDebugEnabled()
                );

        /*
         * YEKDB ana veri dizini.
         */
        String configuredDataDirectory =
                System.getProperty(
                        "yekdb.data.dir",
                        DEFAULT_DATA_DIRECTORY
                );

        Path dataDirectory =
                Path.of(
                                configuredDataDirectory
                        )
                        .toAbsolutePath()
                        .normalize();

        /*
         * Database lifecycle manager.
         */
        DatabaseManager databaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        /*
         * Terminal metadata işlemleri.
         */
        TerminalMetadataService metadataService =
                new TerminalMetadataService(
                        databaseManager
                );

        /*
         * SELECT sorgularının gerçek persistent
         * storage veri kaynağı.
         */
        StorageQueryDataSource queryDataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

        /*
         * Gerçek YEKDB query engine.
         */
        QueryExecutor queryExecutor =
                new QueryExecutor(
                        databaseManager,
                        queryDataSource
                );

        /*
         * CLI ile QueryExecutor arasındaki adapter.
         */
        SqlTerminalExecutor sqlExecutor =
                new SqlTerminalExecutor(
                        queryExecutor
                );

        /*
         * UTF-8 terminal reader.
         */
        InputStreamReader reader =
                new InputStreamReader(
                        System.in,
                        StandardCharsets.UTF_8
                );

        /*
         * Interactive terminal.
         */
        InteractiveSqlTerminal terminal =
                new InteractiveSqlTerminal(
                        config,
                        session,
                        commandParser,
                        sqlExecutor,
                        output,
                        resultFormatter,
                        errorHandler,
                        metadataService,
                        reader
                );

        /*
         * QueryExecutor AutoCloseable olduğu için
         * terminal kapanırken temizlenir.
         */
        try {

            terminal.run();

        } finally {

            queryExecutor.close();
        }
    }
}