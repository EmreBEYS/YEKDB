package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorTriggerRecursionGuardTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void recursiveInsertTriggerShouldFailWithoutStackOverflow() {
        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        StorageQueryDataSource dataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             dataSource
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE recursive_trigger_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE recursive_trigger_db;"
            );

            queryExecutor.execute(
                    """
                    CREATE TABLE users (
                        id INT,
                        name STRING
                    );
                    """
            );

            queryExecutor.execute(
                    """
                    CREATE TRIGGER users_after_insert
                    AFTER INSERT ON users
                    BEGIN
                        INSERT INTO users (id, name)
                        VALUES (2, NEW.name)
                    END;
                    """
            );

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "INSERT INTO users (id, name) VALUES (1, 'Emre');"
                            )
                    );

            assertTrue(
                    containsMessage(
                            exception,
                            "Recursive trigger execution detected"
                    )
            );
        }
    }

    private boolean containsMessage(
            Throwable throwable,
            String expectedMessage
    ) {
        Throwable current =
                throwable;

        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage()
                    .contains(expectedMessage)) {
                return true;
            }

            current =
                    current.getCause();
        }

        return false;
    }
}
