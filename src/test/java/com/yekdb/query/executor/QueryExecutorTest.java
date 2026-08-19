package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.command.CreateDatabaseCommand;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.datasource.InMemoryQueryDataSource;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;


import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QueryExecutor sınıfının birim testleri.
 *
 * Test edilen özellikler:
 *
 * - CREATE DATABASE
 * - USE DATABASE
 * - CREATE TABLE
 * - DROP TABLE
 * - DROP DATABASE
 * - SELECT *
 * - SELECT + WHERE
 * - INSERT
 * - Hatalı komut kontrolleri
 */
class QueryExecutorTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void createDatabaseCommand_shouldCreateDatabaseSuccessfully() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            ExecuteResult result =
                    queryExecutor.execute(
                            new CreateDatabaseCommand(
                                    "school_db"
                            )
                    );

            assertTrue(result.isSuccess());

            assertTrue(
                    result.getMessage()
                            .contains("school_db")
            );

            assertTrue(
                    databaseManager.exists(
                            "school_db"
                    )
            );
        }
    }

    @Test
    void createDatabaseSql_shouldCreateDatabaseSuccessfully() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            ExecuteResult result =
                    queryExecutor.execute(
                            "CREATE DATABASE company_db;"
                    );

            assertTrue(result.isSuccess());

            assertTrue(
                    databaseManager.exists(
                            "company_db"
                    )
            );
        }
    }

    @Test
    void useDatabase_shouldSelectDatabaseSuccessfully() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE application_db;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "USE DATABASE application_db;"
                    );

            assertTrue(result.isSuccess());

            assertEquals(
                    "application_db",
                    databaseManager
                            .getCurrentDatabase()
                            .getName()
            );
        }
    }

    @Test
    void createTable_shouldCreateTableSuccessfully() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE table_test_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE table_test_db;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            """
                            CREATE TABLE users (
                                id INT,
                                name STRING,
                                age INT,
                                active BOOLEAN
                            );
                            """
                    );

            assertTrue(result.isSuccess());

            assertTrue(
                    result.getMessage()
                            .contains("users")
            );

            Path tableFile = temporaryDirectory
                    .resolve("table_test_db")
                    .resolve("users.tbl");

            assertTrue(
                    tableFile.toFile().exists()
            );
        }
    }

    @Test
    void dropTable_shouldDeleteTableSuccessfully() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE drop_table_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE drop_table_db;"
            );

            queryExecutor.execute(
                    """
                    CREATE TABLE products (
                        id INT,
                        name STRING
                    );
                    """
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "DROP TABLE products;"
                    );

            assertTrue(result.isSuccess());

            Path tableFile = temporaryDirectory
                    .resolve("drop_table_db")
                    .resolve("products.tbl");

            assertFalse(
                    tableFile.toFile().exists()
            );
        }
    }

    @Test
    void dropDatabase_shouldDeleteDatabaseSuccessfully() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE removable_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE removable_db;"
            );

            ExecuteResult result =
                    queryExecutor.execute(
                            "DROP DATABASE removable_db;"
                    );

            assertTrue(result.isSuccess());

            assertFalse(
                    databaseManager.exists(
                            "removable_db"
                    )
            );
        }
    }

    @Test
    void selectAll_shouldReturnEveryRow() {

        Table usersTable =
                createUsersTable();

        List<Row> users =
                createUsers();

        InMemoryQueryDataSource queryDataSource =
                new InMemoryQueryDataSource();

        queryDataSource.register(
                usersTable,
                users
        );

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             queryDataSource
                     )) {

            ExecuteResult result =
                    queryExecutor.execute(
                            SelectCommand.allFrom(
                                    "users"
                            )
                    );

            assertTrue(result.isSuccess());
            assertTrue(result.hasRows());

            assertEquals(
                    4,
                    result.getRowCount()
            );

            assertEquals(
                    "Yunus Emre",
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );
        }
    }

    @Test
    void selectWithWhere_shouldReturnOnlyMatchingRows() {

        Table usersTable =
                createUsersTable();

        List<Row> users =
                createUsers();

        InMemoryQueryDataSource queryDataSource =
                new InMemoryQueryDataSource();

        queryDataSource.register(
                usersTable,
                users
        );

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             queryDataSource
                     )) {

            ComparisonExpression whereExpression =
                    new ComparisonExpression(
                            "age",
                            ComparisonOperator.GREATER_THAN,
                            18
                    );

            SelectCommand command =
                    SelectCommand.allFromWhere(
                            "users",
                            whereExpression
                    );

            ExecuteResult result =
                    queryExecutor.execute(
                            command
                    );

            assertTrue(result.isSuccess());

            assertEquals(
                    3,
                    result.getRowCount()
            );

            assertEquals(
                    "Yunus Emre",
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );

            assertEquals(
                    "Ayşe",
                    result.getRows()
                            .get(1)
                            .getValue(1)
            );

            assertEquals(
                    "Mehmet",
                    result.getRows()
                            .get(2)
                            .getValue(1)
            );
        }
    }

    @Test
    void selectWithoutQueryDataSource_shouldThrowException() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    SelectCommand.allFrom(
                                            "users"
                                    )
                            )
                    );

            assertEquals(
                    "SELECT execution requires a QueryDataSource.",
                    exception.getMessage()
            );
        }
    }

    @Test
    void insertCommand_shouldInsertRowSuccessfully() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            queryExecutor.execute(
                    "CREATE DATABASE insert_test_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE insert_test_db;"
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

            InsertCommand command =
                    new InsertCommand(
                            "users",
                            List.of(
                                    "id",
                                    "name",
                                    "age"
                            ),
                            List.of(
                                    1,
                                    "Emre",
                                    21
                            )
                    );

            ExecuteResult result =
                    queryExecutor.execute(
                            command
                    );

            assertTrue(
                    result.isSuccess()
            );

            assertTrue(
                    result.getMessage()
                            .contains(
                                    "Row inserted successfully"
                            )
            );

            assertTrue(
                    result.getMessage()
                            .contains(
                                    "users"
                            )
            );

            Path dataFile =
                    temporaryDirectory
                            .resolve(
                                    "insert_test_db"
                            )
                            .resolve(
                                    "users.data"
                            );

            assertTrue(
                    dataFile.toFile()
                            .exists()
            );

            assertTrue(
                    dataFile.toFile()
                            .length() > 0
            );
        }
    }

    @Test
    void nullCommand_shouldThrowException() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    (com.yekdb.query.command.Command) null
                            )
                    );

            assertEquals(
                    "Command cannot be null.",
                    exception.getMessage()
            );
        }
    }

    @Test
    void blankSqlStatement_shouldThrowException() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(databaseManager)) {

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "   "
                            )
                    );

            assertEquals(
                    "SQL statement cannot be null or blank.",
                    exception.getMessage()
            );
        }
    }

    @Test
    void unsupportedSqlStatement_shouldThrowException() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager
                     )) {

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () -> queryExecutor.execute(
                                    "ALTER TABLE users ADD COLUMN city STRING;"
                            )
                    );

            assertTrue(
                    exception.getMessage()
                            .contains(
                                    "Unsupported SQL statement"
                            )
            );
        }
    }

    @Test
    void close_shouldCompleteWithoutException() {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        QueryExecutor queryExecutor =
                new QueryExecutor(
                        databaseManager
                );

        assertDoesNotThrow(
                queryExecutor::close
        );
    }

    private static Table createUsersTable() {
        return new Table(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        ),
                        new Column(
                                "name",
                                DataType.STRING
                        ),
                        new Column(
                                "age",
                                DataType.INT
                        ),
                        new Column(
                                "city",
                                DataType.STRING
                        ),
                        new Column(
                                "active",
                                DataType.BOOLEAN
                        )
                )
        );
    }

    private static List<Row> createUsers() {
        return List.of(
                new Row(
                        List.of(
                                1,
                                "Yunus Emre",
                                21,
                                "Malatya",
                                true
                        )
                ),
                new Row(
                        List.of(
                                2,
                                "Ali",
                                16,
                                "Ankara",
                                true
                        )
                ),
                new Row(
                        List.of(
                                3,
                                "Ayşe",
                                27,
                                "Malatya",
                                false
                        )
                ),
                new Row(
                        List.of(
                                4,
                                "Mehmet",
                                35,
                                "İstanbul",
                                true
                        )
                )
        );
    }
    @Test
    void updateSql_shouldUpdatePersistedRowSuccessfully()
            throws Exception {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        /*
         * 1. Veritabanını, tabloyu ve ilk kaydı oluştur.
         * 2. UPDATE sorgusunu gerçek SQL metni üzerinden çalıştır.
         */
        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE update_test_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE update_test_db;"
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

            ExecuteResult insertResult =
                    queryExecutor.execute(
                            """
                            INSERT INTO users
                            (id, name, age)
                            VALUES (1, 'Emre', 21);
                            """
                    );

            assertTrue(
                    insertResult.isSuccess()
            );

            ExecuteResult updateResult =
                    queryExecutor.execute(
                            """
                            UPDATE users
                            SET age = 22
                            WHERE id = 1;
                            """
                    );

            assertTrue(
                    updateResult.isSuccess()
            );

            assertTrue(
                    updateResult.getMessage()
                            .contains(
                                    "Updated row count: 1"
                            )
            );
        }

        /*
         * QueryExecutor kapandıktan sonra fiziksel .data
         * dosyasını yeniden açıyoruz.
         *
         * Böylece yalnızca RAM'deki değeri değil,
         * diske gerçekten yazılmış UPDATE sonucunu
         * doğrulamış oluyoruz.
         */
        Path dataFile =
                temporaryDirectory
                        .resolve(
                                "update_test_db"
                        )
                        .resolve(
                                "users.data"
                        );

        assertTrue(
                dataFile.toFile().exists()
        );

        StorageEngine storageEngine =
                new StorageEngine(
                        dataFile
                );

        try {

            storageEngine.initialize();

            RecordManager recordManager =
                    new RecordManager(
                            storageEngine.getPageManager(),
                            PageType.DATA
                    );

            List<Record> records =
                    recordManager.getActiveRecords();

            assertEquals(
                    1,
                    records.size()
            );

            Record record =
                    records.get(0);

            Row persistedRow =
                    recordManager.getRow(
                            record.getRecordId()
                    );

            /*
             * users kolon sırası:
             *
             * 0 -> id
             * 1 -> name
             * 2 -> age
             */
            assertEquals(
                    1,
                    persistedRow.getValue(0)
            );

            assertEquals(
                    "Emre",
                    persistedRow.getValue(1)
            );

            assertEquals(
                    22,
                    persistedRow.getValue(2)
            );

        } finally {

            if (storageEngine.isInitialized()) {
                storageEngine.shutdown();
            }
        }
    }
    @Test
    void deleteSqlWithWhere_shouldDeletePersistedRowSuccessfully()
            throws Exception {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE delete_test_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE delete_test_db;"
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

            ExecuteResult insertResult =
                    queryExecutor.execute(
                            """
                            INSERT INTO users
                            (id, name, age)
                            VALUES (1, 'Emre', 21);
                            """
                    );

            assertTrue(
                    insertResult.isSuccess()
            );

            ExecuteResult deleteResult =
                    queryExecutor.execute(
                            """
                            DELETE FROM users
                            WHERE id = 1;
                            """
                    );

            assertTrue(
                    deleteResult.isSuccess()
            );

            assertTrue(
                    deleteResult.getMessage()
                            .contains(
                                    "Deleted row count: 1"
                            )
            );
        }

        Path dataFile =
                temporaryDirectory
                        .resolve(
                                "delete_test_db"
                        )
                        .resolve(
                                "users.data"
                        );

        assertTrue(
                dataFile.toFile().exists()
        );

        StorageEngine storageEngine =
                new StorageEngine(
                        dataFile
                );

        try {

            storageEngine.initialize();

            RecordManager recordManager =
                    new RecordManager(
                            storageEngine.getPageManager(),
                            PageType.DATA
                    );

            /*
             * Logical delete sonrası kayıt fiziksel dosyada
             * bulunabilir fakat ACTIVE kayıt listesinde
             * kesinlikle görünmemelidir.
             */
            assertEquals(
                    0,
                    recordManager
                            .getActiveRecords()
                            .size()
            );

        } finally {

            if (storageEngine.isInitialized()) {
                storageEngine.shutdown();
            }
        }
    }
    @Test
    void deleteSqlWithNonMatchingWhere_shouldDeleteZeroRows()
            throws Exception {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE delete_no_match_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE delete_no_match_db;"
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
                    VALUES (1, 'Emre', 21);
                    """
            );

            ExecuteResult deleteResult =
                    queryExecutor.execute(
                            """
                            DELETE FROM users
                            WHERE id = 999;
                            """
                    );

            assertTrue(
                    deleteResult.isSuccess()
            );

            assertTrue(
                    deleteResult.getMessage()
                            .contains(
                                    "Deleted row count: 0"
                            )
            );
        }

        Path dataFile =
                temporaryDirectory
                        .resolve(
                                "delete_no_match_db"
                        )
                        .resolve(
                                "users.data"
                        );

        StorageEngine storageEngine =
                new StorageEngine(
                        dataFile
                );

        try {

            storageEngine.initialize();

            RecordManager recordManager =
                    new RecordManager(
                            storageEngine.getPageManager(),
                            PageType.DATA
                    );

            /*
             * id = 999 bulunamadığı için
             * id = 1 kaydı hâlâ aktif olmalıdır.
             */
            assertEquals(
                    1,
                    recordManager
                            .getActiveRecords()
                            .size()
            );

        } finally {

            if (storageEngine.isInitialized()) {
                storageEngine.shutdown();
            }
        }
    }
    @Test
    void deleteSqlWithoutWhere_shouldDeleteAllActiveRows()
            throws Exception {

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager
                     )) {

            queryExecutor.execute(
                    "CREATE DATABASE delete_all_db;"
            );

            queryExecutor.execute(
                    "USE DATABASE delete_all_db;"
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
                    VALUES (1, 'Emre', 21);
                    """
            );

            queryExecutor.execute(
                    """
                    INSERT INTO users
                    (id, name, age)
                    VALUES (2, 'Ali', 24);
                    """
            );

            queryExecutor.execute(
                    """
                    INSERT INTO users
                    (id, name, age)
                    VALUES (3, 'Ayse', 26);
                    """
            );

            ExecuteResult deleteResult =
                    queryExecutor.execute(
                            "DELETE FROM users;"
                    );

            assertTrue(
                    deleteResult.isSuccess()
            );

            assertTrue(
                    deleteResult.getMessage()
                            .contains(
                                    "Deleted row count: 3"
                            )
            );
        }

        Path dataFile =
                temporaryDirectory
                        .resolve(
                                "delete_all_db"
                        )
                        .resolve(
                                "users.data"
                        );

        StorageEngine storageEngine =
                new StorageEngine(
                        dataFile
                );

        try {

            storageEngine.initialize();

            RecordManager recordManager =
                    new RecordManager(
                            storageEngine.getPageManager(),
                            PageType.DATA
                    );

            assertEquals(
                    0,
                    recordManager
                            .getActiveRecords()
                            .size()
            );

        } finally {

            if (storageEngine.isInitialized()) {
                storageEngine.shutdown();
            }
        }
    }
    @Test
    void multilineSelectSql_shouldUseParserPipeline() {

        Table employees =
                new Table(
                        "employees",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INT
                                ),
                                new Column(
                                        "department",
                                        DataType.STRING
                                )
                        )
                );

        List<Row> rows =
                List.of(
                        new Row(
                                List.of(
                                        1,
                                        "IT"
                                )
                        ),
                        new Row(
                                List.of(
                                        2,
                                        "IT"
                                )
                        ),
                        new Row(
                                List.of(
                                        3,
                                        "HR"
                                )
                        )
                );

        InMemoryQueryDataSource dataSource =
                new InMemoryQueryDataSource();

        dataSource.register(
                employees,
                rows
        );

        DatabaseManager databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        try (QueryExecutor executor =
                     new QueryExecutor(
                             databaseManager,
                             dataSource
                     )) {

            ExecuteResult result =
                    executor.execute(
                            """
                            SELECT
                                department,
                                COUNT(*) AS employee_count
                            FROM employees
                            GROUP BY department
                            ORDER BY employee_count DESC;
                            """
                    );

            assertTrue(
                    result.isSuccess()
            );

            assertEquals(
                    2,
                    result.getRowCount()
            );

            assertEquals(
                    "IT",
                    result.getRows()
                            .get(0)
                            .getValue(0)
            );

            assertEquals(
                    2L,
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );
        }
    }
}