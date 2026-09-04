package com.yekdb.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRecoveryManagerTest {

    @TempDir
    Path databasePath;

    @Test
    void shouldAppendRollbackDecisionForIncompleteTransactions()
            throws IOException {

        writeLog(
                "YEKDB_TRANSACTION_LOG_V1",
                "txId=1|status=ACTIVE|accessMode=READ_WRITE|isolationLevel=READ_COMMITTED|startedAt=2026-09-04T06:00:00Z|completedAt=null",
                "txId=1|status=COMMITTED|accessMode=READ_WRITE|isolationLevel=READ_COMMITTED|startedAt=2026-09-04T06:00:00Z|completedAt=2026-09-04T06:00:01Z",
                "txId=2|status=ACTIVE|accessMode=READ_WRITE|isolationLevel=SERIALIZABLE|startedAt=2026-09-04T06:00:02Z|completedAt=null"
        );

        TransactionRecoveryResult result =
                new TransactionRecoveryManager()
                        .recover(
                                databasePath
                        );

        assertTrue(
                result.hasRecoveredTransactions()
        );

        assertEquals(
                1,
                result.getRecoveredTransactionCount()
        );

        assertEquals(
                2,
                result.getRecoveredTransactions()
                        .get(0)
                        .getTransactionId()
        );

        List<String> lines =
                readLog();

        assertEquals(
                5,
                lines.size()
        );

        assertTrue(
                lines.get(4).contains("txId=2|status=ROLLED_BACK")
        );

        assertTrue(
                lines.get(4).contains("isolationLevel=SERIALIZABLE")
        );
    }

    @Test
    void shouldBeIdempotentAfterRecoveredTransactionsAreClosed()
            throws IOException {

        writeLog(
                "YEKDB_TRANSACTION_LOG_V1",
                "txId=7|status=ACTIVE|accessMode=READ_WRITE|isolationLevel=READ_COMMITTED|startedAt=2026-09-04T06:00:00Z|completedAt=null"
        );

        TransactionRecoveryManager recoveryManager =
                new TransactionRecoveryManager();

        TransactionRecoveryResult firstResult =
                recoveryManager.recover(
                        databasePath
                );

        TransactionRecoveryResult secondResult =
                recoveryManager.recover(
                        databasePath
                );

        assertEquals(
                1,
                firstResult.getRecoveredTransactionCount()
        );

        assertFalse(
                secondResult.hasRecoveredTransactions()
        );

        assertEquals(
                3,
                readLog().size()
        );
    }

    private void writeLog(
            String... lines
    ) throws IOException {

        Files.write(
                databasePath.resolve(
                        TransactionDurabilityLog.FILE_NAME
                ),
                List.of(lines)
        );
    }

    private List<String> readLog()
            throws IOException {

        return Files.readAllLines(
                databasePath.resolve(
                        TransactionDurabilityLog.FILE_NAME
                )
        );
    }
}
