package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.datasource.InMemoryQueryDataSource;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QueryExecutor bileşenlerinin birlikte çalışmasını doğrular.
 *
 * Test edilen zincir:
 *
 * QueryExecutor
 *      ↓
 * QueryDataSource
 *      ↓
 * SelectExecutor
 *      ↓
 * QueryOptimizer
 *      ↓
 * TableScanExecutor
 *      ↓
 * WhereEvaluator
 *      ↓
 * ExecuteResult
 */
class QueryExecutorIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void fullQueryExecutionScenario_shouldWorkSuccessfully() {

        /*
         * 1. SELECT veri kaynağında kullanılacak tablo şeması.
         */
        Table usersTable = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING),
                        new Column("age", DataType.INT),
                        new Column("city", DataType.STRING),
                        new Column("active", DataType.BOOLEAN)
                )
        );

        /*
         * 2. Demo kayıtları.
         *
         * INSERT yürütme bağlantısı henüz tamamlanmadığı için
         * satırlar InMemoryQueryDataSource içerisine hazırlanır.
         */
        List<Row> users = List.of(
                new Row(
                        List.of(
                                1,
                                "Emre",
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
                )
        );

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

            /*
             * 3. Veritabanı oluşturma.
             */
            ExecuteResult createDatabaseResult =
                    queryExecutor.execute(
                            "CREATE DATABASE integration_db;"
                    );

            assertTrue(createDatabaseResult.isSuccess());

            /*
             * 4. Veritabanı seçme.
             */
            ExecuteResult useDatabaseResult =
                    queryExecutor.execute(
                            "USE DATABASE integration_db;"
                    );

            assertTrue(useDatabaseResult.isSuccess());

            /*
             * 5. Fiziksel tablo metadata ve .tbl dosyası oluşturma.
             */
            ExecuteResult createTableResult =
                    queryExecutor.execute(
                            """
                            CREATE TABLE users (
                                id INT,
                                name STRING,
                                age INT,
                                city STRING,
                                active BOOLEAN
                            );
                            """
                    );

            assertTrue(createTableResult.isSuccess());

            /*
             * 6. SELECT * FROM users.
             */
            ExecuteResult selectAllResult =
                    queryExecutor.execute(
                            SelectCommand.allFrom("users")
                    );

            assertTrue(selectAllResult.isSuccess());
            assertTrue(selectAllResult.hasRows());
            assertEquals(
                    3,
                    selectAllResult.getRowCount()
            );

            /*
             * 7. WHERE age > 18 AND city = 'Malatya'
             */
            Expression whereExpression =
                    new LogicalExpression(
                            new ComparisonExpression(
                                    "age",
                                    ComparisonOperator.GREATER_THAN,
                                    18
                            ),
                            LogicalOperator.AND,
                            new ComparisonExpression(
                                    "city",
                                    ComparisonOperator.EQUALS,
                                    "Malatya"
                            )
                    );

            SelectCommand filteredSelectCommand =
                    SelectCommand.allFromWhere(
                            "users",
                            whereExpression
                    );

            ExecuteResult filteredSelectResult =
                    queryExecutor.execute(
                            filteredSelectCommand
                    );

            assertTrue(filteredSelectResult.isSuccess());
            assertTrue(filteredSelectResult.hasRows());
            assertEquals(
                    2,
                    filteredSelectResult.getRowCount()
            );

            assertEquals(
                    "Emre",
                    filteredSelectResult
                            .getRows()
                            .get(0)
                            .getValue(1)
            );

            assertEquals(
                    "Ayşe",
                    filteredSelectResult
                            .getRows()
                            .get(1)
                            .getValue(1)
            );

            /*
             * 8. Eşleşmeyen WHERE koşulu.
             */
            Expression unmatchedExpression =
                    new ComparisonExpression(
                            "age",
                            ComparisonOperator.GREATER_THAN,
                            100
                    );

            ExecuteResult emptyResult =
                    queryExecutor.execute(
                            SelectCommand.allFromWhere(
                                    "users",
                                    unmatchedExpression
                            )
                    );

            assertTrue(emptyResult.isSuccess());
            assertFalse(emptyResult.hasRows());
            assertEquals(
                    0,
                    emptyResult.getRowCount()
            );

            /*
             * 9. Tabloyu silme.
             */
            ExecuteResult dropTableResult =
                    queryExecutor.execute(
                            "DROP TABLE users;"
                    );

            assertTrue(dropTableResult.isSuccess());

            /*
             * 10. Veritabanını silme.
             */
            ExecuteResult dropDatabaseResult =
                    queryExecutor.execute(
                            "DROP DATABASE integration_db;"
                    );

            assertTrue(dropDatabaseResult.isSuccess());
        }
    }
}