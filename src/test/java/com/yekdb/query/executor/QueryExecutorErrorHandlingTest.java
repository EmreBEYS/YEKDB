package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.StorageQueryDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueryExecutorErrorHandlingTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldPreserveTableNotFoundMessageForSelect() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        databaseManager.createDatabase(
                "test_db"
        );

        databaseManager.useDatabase(
                "test_db"
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

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "SELECT * FROM olmayan_tablo;"
                            )
                    );

            assertEquals(
                    "Table not found: olmayan_tablo",
                    exception.getMessage()
            );
        }
    }

    @Test
    void shouldPreserveTableNotFoundMessageForInsert() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        databaseManager.createDatabase(
                "test_db"
        );

        databaseManager.useDatabase(
                "test_db"
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

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "INSERT INTO olmayan_tablo(id) VALUES(1);"
                            )
                    );

            assertEquals(
                    "Table not found: olmayan_tablo",
                    exception.getMessage()
            );
        }
    }

    @Test
    void shouldPreserveTableNotFoundMessageForUpdate() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        databaseManager.createDatabase(
                "test_db"
        );

        databaseManager.useDatabase(
                "test_db"
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

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "UPDATE olmayan_tablo "
                                            + "SET name='Test';"
                            )
                    );

            assertEquals(
                    "Table not found: olmayan_tablo",
                    exception.getMessage()
            );
        }
    }

    @Test
    void shouldPreserveTableNotFoundMessageForDelete() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        tempDirectory
                );

        databaseManager.createDatabase(
                "test_db"
        );

        databaseManager.useDatabase(
                "test_db"
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

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "DELETE FROM olmayan_tablo "
                                            + "WHERE id=1;"
                            )
                    );

            assertEquals(
                    "Table not found: olmayan_tablo",
                    exception.getMessage()
            );
        }
    }
}