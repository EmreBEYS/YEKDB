package com.yekdb.transaction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionDurabilityLogRecoveryTest {

    @TempDir
    Path databasePath;

    @Test
    void shouldReadIncompleteActiveTransactionsFromLog()
            throws IOException {

        Path logFile =
                databasePath.resolve(
                        TransactionDurabilityLog.FILE_NAME
                );

        Files.write(
                logFile,
                List.of(
                        "YEKDB_TRANSACTION_LOG_V1",
                        "txId=1|status=ACTIVE|accessMode=READ_WRITE|isolationLevel=READ_COMMITTED|startedAt=2026-09-04T06:00:00Z|completedAt=null",
                        "txId=1|status=COMMITTED|accessMode=READ_WRITE|isolationLevel=READ_COMMITTED|startedAt=2026-09-04T06:00:00Z|completedAt=2026-09-04T06:00:01Z",
                        "txId=2|status=ACTIVE|accessMode=READ_ONLY|isolationLevel=SERIALIZABLE|startedAt=2026-09-04T06:00:02Z|completedAt=null",
                        "txId=3|status=ACTIVE|accessMode=READ_WRITE|isolationLevel=REPEATABLE_READ|startedAt=2026-09-04T06:00:03Z|completedAt=null",
                        "txId=3|status=ROLLED_BACK|accessMode=READ_WRITE|isolationLevel=REPEATABLE_READ|startedAt=2026-09-04T06:00:03Z|completedAt=2026-09-04T06:00:04Z"
                )
        );

        TransactionDurabilityLog log =
                new TransactionDurabilityLog();

        List<TransactionLogEntry> incompleteTransactions =
                log.readIncompleteTransactions(
                        databasePath
                );

        assertEquals(
                1,
                incompleteTransactions.size()
        );

        TransactionLogEntry entry =
                incompleteTransactions.get(0);

        assertEquals(
                2,
                entry.getTransactionId()
        );

        assertEquals(
                TransactionStatus.ACTIVE,
                entry.getStatus()
        );

        assertEquals(
                TransactionAccessMode.READ_ONLY,
                entry.getAccessMode()
        );

        assertEquals(
                TransactionIsolationLevel.SERIALIZABLE,
                entry.getIsolationLevel()
        );

        assertEquals(
                Instant.parse("2026-09-04T06:00:02Z"),
                entry.getStartedAt()
        );
    }

    @Test
    void shouldReturnEmptyIncompleteTransactionListWhenLogDoesNotExist() {

        TransactionDurabilityLog log =
                new TransactionDurabilityLog();

        assertTrue(
                log.readIncompleteTransactions(
                        databasePath
                ).isEmpty()
        );
    }
}
