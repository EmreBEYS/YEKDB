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

import java.nio.file.Path;
import java.util.List;

/**
 * Sprint 00-11 Query Execution Engine demosu.
 *
 * Gösterilen akış:
 *
 * SelectCommand
 *      ↓
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
public final class QueryExecutionDemo {

    private QueryExecutionDemo() {
    }

    public static void main(String[] args) {

        /*
         * 1. Tablo şemasını oluştur.
         */
        Table usersTable = createUsersTable();

        /*
         * 2. Örnek satırları oluştur.
         */
        List<Row> users = createUsers();

        /*
         * 3. Bellek tabanlı sorgu veri kaynağını hazırla.
         */
        InMemoryQueryDataSource queryDataSource =
                new InMemoryQueryDataSource();

        queryDataSource.register(
                usersTable,
                users
        );

        /*
         * 4. DatabaseManager ve QueryExecutor oluştur.
         */
        DatabaseManager databaseManager =
                new DatabaseManager(
                        Path.of("data")
                );

        QueryExecutor queryExecutor =
                new QueryExecutor(
                        databaseManager,
                        queryDataSource
                );

        /*
         * 5. SELECT * sorgusunu çalıştır.
         */
        executeSelectAll(queryExecutor);

        /*
         * 6. WHERE koşullu SELECT sorgusunu çalıştır.
         */
        executeFilteredSelect(queryExecutor);

        /*
         * 7. OR operatörlü sorguyu çalıştır.
         */
        executeOrSelect(queryExecutor);

        /*
         * 8. NOT operatörlü sorguyu çalıştır.
         */
        executeNotSelect(queryExecutor);
    }

    /**
     * Kullanıcılar tablosunu oluşturur.
     */
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

    /**
     * Demo kullanıcı satırlarını oluşturur.
     */
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
                ),
                new Row(
                        List.of(
                                5,
                                "Zeynep",
                                19,
                                "Elazığ",
                                false
                        )
                )
        );
    }

    /**
     * SELECT * FROM users sorgusunu çalıştırır.
     */
    private static void executeSelectAll(
            QueryExecutor queryExecutor
    ) {
        SelectCommand command =
                SelectCommand.allFrom(
                        "users"
                );

        ExecuteResult result =
                queryExecutor.execute(
                        command
                );

        printResult(
                "SELECT * FROM users",
                result
        );
    }

    /**
     * age > 18 AND city = 'Malatya'
     * koşulunu çalıştırır.
     */
    private static void executeFilteredSelect(
            QueryExecutor queryExecutor
    ) {
        Expression ageExpression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        Expression cityExpression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Malatya"
                );

        Expression whereExpression =
                new LogicalExpression(
                        ageExpression,
                        LogicalOperator.AND,
                        cityExpression
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

        printResult(
                "SELECT * FROM users "
                        + "WHERE age > 18 "
                        + "AND city = 'Malatya'",
                result
        );
    }

    /**
     * city = 'Ankara' OR city = 'İstanbul'
     * koşulunu çalıştırır.
     */
    private static void executeOrSelect(
            QueryExecutor queryExecutor
    ) {
        Expression ankaraExpression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Ankara"
                );

        Expression istanbulExpression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "İstanbul"
                );

        Expression whereExpression =
                new LogicalExpression(
                        ankaraExpression,
                        LogicalOperator.OR,
                        istanbulExpression
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

        printResult(
                "SELECT * FROM users "
                        + "WHERE city = 'Ankara' "
                        + "OR city = 'İstanbul'",
                result
        );
    }

    /**
     * active != true koşulunu çalıştırır.
     */
    private static void executeNotSelect(
            QueryExecutor queryExecutor
    ) {
        Expression whereExpression =
                new ComparisonExpression(
                        "active",
                        ComparisonOperator.NOT_EQUALS,
                        true
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

        printResult(
                "SELECT * FROM users "
                        + "WHERE active != true",
                result
        );
    }

    /**
     * Sorgu sonucunu konsola düzenli biçimde yazdırır.
     */
    private static void printResult(
            String query,
            ExecuteResult result
    ) {
        System.out.println();
        System.out.println(
                "=================================================="
        );

        System.out.println(query);

        System.out.println(
                "=================================================="
        );

        if (!result.isSuccess()) {
            System.out.println(
                    "Sorgu başarısız: "
                            + result.getMessage()
            );

            return;
        }

        if (!result.hasRows()) {
            System.out.println(
                    "Sorgu herhangi bir satır döndürmedi."
            );
        } else {
            for (Row row : result.getRows()) {
                System.out.println(row);
            }
        }

        System.out.println();

        System.out.println(
                "Dönen satır sayısı: "
                        + result.getRowCount()
        );

        System.out.println(
                "Sonuç mesajı: "
                        + result.getMessage()
        );
    }
}