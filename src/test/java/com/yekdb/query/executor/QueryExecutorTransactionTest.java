package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorTransactionTest {

    @TempDir
    Path dataDirectory;

    @Test
    void shouldExecuteBeginAndCommit() {

        QueryExecutor executor =
                newExecutor();

        ExecuteResult beginResult =
                executor.execute(
                        "BEGIN;"
                );

        assertTrue(
                beginResult.isSuccess()
        );

        assertEquals(
                "Transaction started successfully: 1",
                beginResult.getMessage()
        );

        ExecuteResult commitResult =
                executor.execute(
                        "COMMIT;"
                );

        assertTrue(
                commitResult.isSuccess()
        );

        assertEquals(
                "Transaction committed successfully: 1",
                commitResult.getMessage()
        );
    }

    @Test
    void shouldExecuteBeginAndRollback() {

        QueryExecutor executor =
                newExecutor();

        executor.execute(
                "BEGIN;"
        );

        ExecuteResult rollbackResult =
                executor.execute(
                        "ROLLBACK;"
                );

        assertTrue(
                rollbackResult.isSuccess()
        );

        assertEquals(
                "Transaction rolled back successfully: 1",
                rollbackResult.getMessage()
        );
    }

    @Test
    void shouldRejectNestedBeginThroughExecutor() {

        QueryExecutor executor =
                newExecutor();

        executor.execute(
                "BEGIN;"
        );

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () -> executor.execute(
                                "BEGIN;"
                        )
                );

        assertEquals(
                "A transaction is already active.",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectCommitWithoutActiveTransactionThroughExecutor() {

        QueryExecutor executor =
                newExecutor();

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () -> executor.execute(
                                "COMMIT;"
                        )
                );

        assertEquals(
                "No active transaction exists.",
                exception.getMessage()
        );
    }

    private QueryExecutor newExecutor() {

        return new QueryExecutor(
                new DatabaseManager(
                        dataDirectory
                )
        );
    }
}
