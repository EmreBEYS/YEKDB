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

class InteractiveSqlTerminalIntegrationTest {

    @TempDir
    Path tempDirectory;

    /**
     * Database oluşturma, database seçme,
     * tablo oluşturma, INSERT ve SELECT
     * işlemlerinin terminal üzerinden
     * uçtan uca çalışmasını doğrular.
     */
    @Test
    void shouldExecuteCompleteCrudBootstrapFlow() {

        TerminalResult result =
                runTerminal(
                        """
                        CREATE DATABASE integration_db;
                        USE DATABASE integration_db;
                        CREATE TABLE users (
                            id INT,
                            name STRING
                        );
                        INSERT INTO users (id, name)
                        VALUES (1, 'Emre');
                        SELECT * FROM users;
                        \\q
                        """
                );

        assertTrue(
                result.output().contains(
                        "Database created successfully: integration_db"
                )
        );

        assertTrue(
                result.output().contains(
                        "Database selected successfully: integration_db"
                )
        );

        assertTrue(
                result.output().contains(
                        "Table created successfully: users"
                )
        );

        assertTrue(
                result.output().contains(
                        "Emre"
                )
        );

        assertTrue(
                result.output().contains(
                        "1 row"
                )
        );

        assertTrue(
                result.error().isBlank()
        );

        assertFalse(
                result.session().isRunning()
        );
    }

    /**
     * Terminal metadata komutlarının gerçek
     * persistent katalog ile çalıştığını doğrular.
     */
    @Test
    void shouldListAndDescribePersistedTable() {

        TerminalResult result =
                runTerminal(
                        """
                        CREATE DATABASE metadata_db;
                        USE DATABASE metadata_db;
                        CREATE TABLE products (
                            id INT,
                            name STRING
                        );
                        \\tables
                        \\describe products
                        \\q
                        """
                );

        assertTrue(
                result.output().contains(
                        "products"
                )
        );

        assertTrue(
                result.output().contains(
                        "Table: products"
                )
        );

        assertTrue(
                result.output().contains(
                        "id"
                )
        );

        assertTrue(
                result.output().contains(
                        "INT"
                )
        );

        assertTrue(
                result.output().contains(
                        "name"
                )
        );

        assertTrue(
                result.output().contains(
                        "STRING"
                )
        );

        assertTrue(
                result.error().isBlank()
        );
    }

    /**
     * Gerçek storage üzerinde INSERT sonrası
     * SELECT projection sonucunu doğrular.
     */
    @Test
    void shouldExecuteProjectionAgainstPersistentStorage() {

        TerminalResult result =
                runTerminal(
                        """
                        CREATE DATABASE projection_db;
                        USE DATABASE projection_db;
                        CREATE TABLE users (
                            id INT,
                            name STRING,
                            age INT
                        );
                        INSERT INTO users (id, name, age)
                        VALUES (1, 'Emre', 21);
                        SELECT name, age FROM users;
                        \\q
                        """
                );

        assertTrue(
                result.output().contains(
                        "name"
                )
        );

        assertTrue(
                result.output().contains(
                        "age"
                )
        );

        assertTrue(
                result.output().contains(
                        "Emre"
                )
        );

        assertTrue(
                result.output().contains(
                        "21"
                )
        );

        assertTrue(
                result.error().isBlank()
        );
    }

    /**
     * UPDATE ve DELETE işlemlerinin gerçek
     * persistent storage üzerinde çalışmasını
     * doğrular.
     */
    @Test
    void shouldExecuteUpdateAndDeleteAgainstStorage() {

        TerminalResult result =
                runTerminal(
                        """
                        CREATE DATABASE mutation_db;
                        USE DATABASE mutation_db;
                        CREATE TABLE users (
                            id INT,
                            name STRING
                        );
                        INSERT INTO users (id, name)
                        VALUES (1, 'OldName');
                        UPDATE users
                        SET name = 'NewName'
                        WHERE id = 1;
                        SELECT * FROM users;
                        DELETE FROM users
                        WHERE id = 1;
                        SELECT * FROM users;
                        \\q
                        """
                );

        assertTrue(
                result.output().contains(
                        "NewName"
                )
        );

        assertTrue(
                result.output().contains(
                        "UPDATE 1"
                )
        );

        assertTrue(
                result.output().contains(
                        "DELETE 1"
                )
        );

        assertTrue(
                result.output().contains(
                        "0 rows"
                )
                        || result.output().contains(
                        "0 row"
                )
        );

        assertTrue(
                result.error().isBlank()
        );
    }

    /**
     * SQL hatasının terminali öldürmemesini
     * gerçek QueryExecutor hattı üzerinden
     * doğrular.
     */
    @Test
    void shouldRecoverFromRealQueryError() {

        TerminalResult result =
                runTerminal(
                        """
                        CREATE DATABASE error_db;
                        USE DATABASE error_db;
                        SELECT * FROM missing_table;
                        CREATE TABLE users (
                            id INT,
                            name STRING
                        );
                        \\tables
                        \\q
                        """
                );

        assertTrue(
                result.error().contains(
                        "ERROR: Table not found: missing_table"
                )
        );

        /*
         * Hatalı SELECT sonrasında terminal
         * çalışmaya devam edip CREATE TABLE
         * ve \\tables işlemlerini gerçekleştirmeli.
         */
        assertTrue(
                result.output().contains(
                        "Table created successfully: users"
                )
        );

        assertTrue(
                result.output().contains(
                        "users"
                )
        );

        assertFalse(
                result.session().isRunning()
        );
    }

    private TerminalResult runTerminal(
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

        TerminalErrorHandler errorHandler =
                new TerminalErrorHandler(
                        output,
                        false
                );

        TerminalMetadataService metadataService =
                new TerminalMetadataService(
                        databaseManager
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
                        metadataService,
                        new StringReader(
                                input
                        )
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