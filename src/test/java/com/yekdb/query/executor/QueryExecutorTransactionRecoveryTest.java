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

class QueryExecutorTransactionRecoveryTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldRecoverIncompleteTransactionsWhenDatabaseIsSelected()
            throws IOException {

        try (QueryExecutor executor = newExecutor()) {
            executor.execute(
                    "CREATE DATABASE recovery_db;"
            );
        }

        writeRecoveryLog(
                "YEKDB_TRANSACTION_LOG_V1",
                "txId=1|status=ACTIVE|accessMode=READ_WRITE|isolationLevel=READ_COMMITTED|startedAt=2026-09-04T06:00:00Z|completedAt=null"
        );

        try (QueryExecutor executor = newExecutor()) {
            executor.execute(
                    "USE DATABASE recovery_db;"
            );
        }

        List<String> lines =
                readRecoveryLog();

        assertEquals(
                3,
                lines.size()
        );

        assertTrue(
                lines.get(2).contains("txId=1|status=ROLLED_BACK")
        );
    }

    @Test
    void shouldNotRecoverTheSameTransactionTwiceWhenDatabaseIsSelectedAgain()
            throws IOException {

        try (QueryExecutor executor = newExecutor()) {
            executor.execute(
                    "CREATE DATABASE recovery_db;"
            );
        }

        writeRecoveryLog(
                "YEKDB_TRANSACTION_LOG_V1",
                "txId=9|status=ACTIVE|accessMode=READ_WRITE|isolationLevel=SERIALIZABLE|startedAt=2026-09-04T06:00:00Z|completedAt=null"
        );

        try (QueryExecutor executor = newExecutor()) {
            executor.execute(
                    "USE DATABASE recovery_db;"
            );

            executor.execute(
                    "USE DATABASE recovery_db;"
            );
        }

        List<String> lines =
                readRecoveryLog();

        assertEquals(
                3,
                lines.size()
        );

        assertTrue(
                lines.get(2).contains("txId=9|status=ROLLED_BACK")
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

    private void writeRecoveryLog(
            String... lines
    ) throws IOException {

        Files.write(
                recoveryLogPath(),
                List.of(lines)
        );
    }

    private List<String> readRecoveryLog()
            throws IOException {

        return Files.readAllLines(
                recoveryLogPath()
        );
    }

    private Path recoveryLogPath() {

        return dataDirectory
                .resolve("recovery_db")
                .resolve(TransactionDurabilityLog.FILE_NAME);
    }
}
