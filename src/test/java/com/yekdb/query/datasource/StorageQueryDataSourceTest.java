package com.yekdb.query.datasource;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.executor.QueryExecutor;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Table;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StorageQueryDataSourceTest {

    @TempDir
    Path temporaryDirectory;

    /**
     * Aktif database seçilmeden tablo erişimi
     * yapılmaya çalışılırsa hata verilmelidir.
     */
    @Test
    void getTable_withoutSelectedDatabase_shouldThrowException() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        StorageQueryDataSource dataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> dataSource.getTable(
                                "users"
                        )
                );

        assertEquals(
                "No database selected. "
                        + "Execute USE DATABASE first.",
                exception.getMessage()
        );
    }

    /**
     * Disk üzerinde oluşturulmuş tablonun
     * şeması datasource üzerinden okunabilmelidir.
     */
    @Test
    void getTable_shouldLoadTableFromPersistentCatalog() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE test_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE test_db;"
            );

            queryExecutor.execute(
                    """
                    CREATE TABLE users (
                        id INT,
                        name STRING,
                        age INT
                    );
                    """
            );
        }

        StorageQueryDataSource dataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

        Table table =
                dataSource.getTable(
                        "users"
                );

        assertNotNull(
                table
        );

        assertEquals(
                "users",
                table.getTableName()
        );

        assertEquals(
                3,
                table.getColumnCount()
        );
    }

    /**
     * INSERT edilen fiziksel kayıt datasource
     * üzerinden tekrar Row olarak okunabilmelidir.
     */
    @Test
    void getRows_shouldReadActiveRowsFromStorage() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE test_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE test_db;"
            );

            queryExecutor.execute(
                    """
                    CREATE TABLE users (
                        id INT,
                        name STRING,
                        age INT
                    );
                    """
            );

            queryExecutor.execute(
                    """
                    INSERT INTO users
                    (id, name, age)
                    VALUES
                    (1, 'Emre', 21);
                    """
            );

            queryExecutor.execute(
                    """
                    INSERT INTO users
                    (id, name, age)
                    VALUES
                    (2, 'Ali', 24);
                    """
            );
        }

        StorageQueryDataSource dataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

        List<Row> rows =
                dataSource.getRows(
                        "users"
                );

        assertEquals(
                2,
                rows.size()
        );

        assertEquals(
                1,
                rows.get(0)
                        .getValue(0)
        );

        assertEquals(
                "Emre",
                rows.get(0)
                        .getValue(1)
        );

        assertEquals(
                21,
                rows.get(0)
                        .getValue(2)
        );

        assertEquals(
                2,
                rows.get(1)
                        .getValue(0)
        );

        assertEquals(
                "Ali",
                rows.get(1)
                        .getValue(1)
        );

        assertEquals(
                24,
                rows.get(1)
                        .getValue(2)
        );
    }

    /**
     * Logical DELETE uygulanmış satırlar
     * datasource sonucuna dahil edilmemelidir.
     */
    @Test
    void getRows_shouldExcludeDeletedRows() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE test_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE test_db;"
            );

            queryExecutor.execute(
                    """
                    CREATE TABLE users (
                        id INT,
                        name STRING,
                        age INT
                    );
                    """
            );

            queryExecutor.execute(
                    """
                    INSERT INTO users
                    (id, name, age)
                    VALUES
                    (1, 'Emre', 21);
                    """
            );

            queryExecutor.execute(
                    """
                    INSERT INTO users
                    (id, name, age)
                    VALUES
                    (2, 'Ali', 24);
                    """
            );

            queryExecutor.execute(
                    """
                    DELETE FROM users
                    WHERE id = 1;
                    """
            );
        }

        StorageQueryDataSource dataSource =
                new StorageQueryDataSource(
                        databaseManager
                );

        List<Row> rows =
                dataSource.getRows(
                        "users"
                );

        assertEquals(
                1,
                rows.size()
        );

        assertEquals(
                2,
                rows.get(0)
                        .getValue(0)
        );

        assertEquals(
                "Ali",
                rows.get(0)
                        .getValue(1)
        );

        assertEquals(
                24,
                rows.get(0)
                        .getValue(2)
        );
    }
}