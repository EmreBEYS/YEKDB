package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import com.yekdb.transaction.TransactionDurabilityLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorTransactionDurabilityTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldPersistImplicitCommitDecisionToTransactionLog()
            throws IOException {

        try (QueryExecutor executor = newExecutor()) {

            createDatabaseAndTable(
                    executor
            );

            executor.execute(
                    "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
            );

            List<String> lines =
                    readTransactionLog();

            assertEquals(
                    3,
                    lines.size()
            );

            assertEquals(
                    "YEKDB_TRANSACTION_LOG_V1",
                    lines.get(0)
            );

            assertTrue(
                    lines.get(1).contains("txId=1|status=ACTIVE")
            );

            assertTrue(
                    lines.get(1).contains("completedAt=null")
            );

            assertTrue(
                    lines.get(2).contains("txId=1|status=COMMITTED")
            );

            assertTrue(
                    lines.get(2).contains("accessMode=READ_WRITE")
            );
        }
    }

    @Test
    void shouldPersistExplicitCommitAndRollbackDecisionsToTransactionLog()
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

            List<String> lines =
                    readTransactionLog();

            assertEquals(
                    5,
                    lines.size()
            );

            assertTrue(
                    lines.get(1).contains("txId=1|status=ACTIVE")
            );

            assertTrue(
                    lines.get(2).contains("txId=1|status=COMMITTED")
            );

            assertTrue(
                    lines.get(3).contains("txId=2|status=ACTIVE")
            );

            assertTrue(
                    lines.get(4).contains("txId=2|status=ROLLED_BACK")
            );
        }
    }

    @Test
    void shouldPersistCloseRollbackDecisionToTransactionLog()
            throws IOException {

        QueryExecutor executor =
                newExecutor();

        createDatabaseAndTable(
                executor
        );

        executor.execute(
                "BEGIN;"
        );

        executor.execute(
                "INSERT INTO users (id, name) VALUES (1, 'Yunus');"
        );

        executor.close();

        List<String> lines =
                readTransactionLog();

        assertEquals(
                3,
                lines.size()
        );

        assertTrue(
                lines.get(1).contains("txId=1|status=ACTIVE")
        );

        assertTrue(
                lines.get(2).contains("txId=1|status=ROLLED_BACK")
        );
    }

    private QueryExecutor newExecutor() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        dataDirectory
                );

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
                "CREATE DATABASE durability_db;"
        );

        executor.execute(
                "USE DATABASE durability_db;"
        );

        executor.execute(
                "CREATE TABLE users (id INT, name STRING);"
        );
    }

    private List<String> readTransactionLog()
            throws IOException {

        return Files.readAllLines(
                dataDirectory
                        .resolve("durability_db")
                        .resolve(TransactionDurabilityLog.FILE_NAME)
        );
    }
}

