package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import com.yekdb.transaction.TransactionDurabilityLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorAcidIntegrationTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldKeepCommittedDataRollbackUncommittedDataAndRecoverIncompleteLog()
            throws IOException {

        try (QueryExecutor executor = newExecutor()) {
            createDatabaseAndTable(
                    executor
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            executor.execute(
                    "COMMIT;"
            );

            executor.execute(
                    "BEGIN;"
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (2, 'Ali');"
            );

            executor.execute(
                    "ROLLBACK;"
            );

            ExecuteResult result =
                    executor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(
                    1,
                    result.getRowCount()
            );

            assertEquals(
                    "Yunus",
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );
        }

        appendIncompleteTransactionLog(
                99
        );

        try (QueryExecutor executor = newExecutor()) {
            executor.execute(
                    "USE DATABASE acid_db;"
            );

            ExecuteResult result =
                    executor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(
                    1,
                    result.getRowCount()
            );

            assertEquals(
                    "Yunus",
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );
        }

        assertTrue(
                readTransactionLog()
                        .stream()
                        .anyMatch(line ->
                                line.contains(
                                        "txId=99|status=ROLLED_BACK"
                                )
                        )
        );
    }

    @Test
    void shouldReleaseWriteLockAfterRollbackAndAllowAnotherExecutorToCommit() {

        DatabaseManager firstDatabaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        DatabaseManager secondDatabaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        try (QueryExecutor firstExecutor = newExecutor(firstDatabaseManager);
             QueryExecutor secondExecutor = newExecutor(secondDatabaseManager)) {

            createDatabaseAndTable(
                    firstExecutor
            );

            secondExecutor.execute(
                    "USE DATABASE acid_db;"
            );

            firstExecutor.execute(
                    "BEGIN;"
            );

            firstExecutor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            secondExecutor.execute(
                    "BEGIN;"
            );

            assertThrows(
                    QueryExecutionException.class,
                    () -> secondExecutor.execute(
                            "INSERT INTO users (id, name) VALUES (2, 'Ali');"
                    )
            );

            secondExecutor.execute(
                    "ROLLBACK;"
            );

            firstExecutor.execute(
                    "ROLLBACK;"
            );

            secondExecutor.execute(
                    "INSERT INTO users (id, name) VALUES (3, 'Zeynep');"
            );

            ExecuteResult result =
                    secondExecutor.execute(
                            "SELECT * FROM users;"
                    );

            assertEquals(
                    1,
                    result.getRowCount()
            );

            assertEquals(
                    "Zeynep",
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );
        }
    }

    private QueryExecutor newExecutor() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        dataDirectory
                );

        return newExecutor(
                databaseManager
        );
    }

    private QueryExecutor newExecutor(
            DatabaseManager databaseManager
    ) {

        return new QueryExecutor(
                databaseManager,
                new StorageQueryDataSource(
                        databaseManager
                )
        );
    }

    private void createDatabaseAndTable(
            QueryExecutor executor
    ) {

        executor.execute(
                "CREATE DATABASE acid_db;"
        );

        executor.execute(
                "USE DATABASE acid_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );
    }

    private void appendIncompleteTransactionLog(
            long transactionId
    ) throws IOException {

        Files.writeString(
                transactionLogPath(),
                "txId="
                        + transactionId
                        + "|status=ACTIVE|accessMode=READ_WRITE"
                        + "|isolationLevel=READ_COMMITTED"
                        + "|startedAt=2026-09-04T06:00:00Z"
                        + "|completedAt=null"
                        + System.lineSeparator(),
                StandardOpenOption.APPEND
        );
    }

    private List<String> readTransactionLog()
            throws IOException {

        return Files.readAllLines(
                transactionLogPath()
        );
    }

    private Path transactionLogPath() {

        return dataDirectory
                .resolve("acid_db")
                .resolve(TransactionDurabilityLog.FILE_NAME);
    }
}
