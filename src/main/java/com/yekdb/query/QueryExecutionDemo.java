package com.yekdb.query;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.command.CreateDatabaseCommand;
import com.yekdb.query.command.CreateTableCommand;
import com.yekdb.query.command.DropDatabaseCommand;
import com.yekdb.query.command.DropTableCommand;
import com.yekdb.query.command.UseDatabaseCommand;
import com.yekdb.query.executor.ExecuteResult;
import com.yekdb.query.executor.QueryExecutor;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;

import java.nio.file.Path;
import java.util.List;

/**
 * YEKDB Query Execution Pipeline demo uygulaması.
 *
 * <p>DDL işlemleri mevcut Command modelleri üzerinden,
 * DML işlemleri ise SQL metni üzerinden yürütülür.</p>
 */
public final class QueryExecutionDemo {

    private QueryExecutionDemo() {
    }

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println("      YEKDB Query Execution Demo");
        System.out.println("========================================");

        DatabaseManager databaseManager =
                new DatabaseManager(
                        Path.of("demo_database")
                );

        boolean completedSuccessfully = false;

        try (QueryExecutor executor =
                     new QueryExecutor(databaseManager)) {

            ExecuteResult result;

            /*
             * CREATE DATABASE
             */
            System.out.println("\n[1] CREATE DATABASE");

            result = executor.execute(
                    new CreateDatabaseCommand("company")
            );

            printResult(result);

            /*
             * USE DATABASE
             */
            System.out.println("\n[2] USE DATABASE");

            result = executor.execute(
                    new UseDatabaseCommand("company")
            );

            printResult(result);

            /*
             * CREATE TABLE
             */
            System.out.println("\n[3] CREATE TABLE");

            result = executor.execute(
                    new CreateTableCommand(
                            "employees",
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
                                    )
                            )
                    )
            );

            printResult(result);

            /*
             * INSERT RECORDS
             */
            System.out.println("\n[4] INSERT RECORDS");

            printResult(
                    executor.execute(
                            """
                            INSERT INTO employees
                            VALUES (1, 'Emre', 21);
                            """
                    )
            );

            printResult(
                    executor.execute(
                            """
                            INSERT INTO employees
                            VALUES (2, 'Ahmet', 24);
                            """
                    )
            );

            printResult(
                    executor.execute(
                            """
                            INSERT INTO employees
                            VALUES (3, 'Mehmet', 28);
                            """
                    )
            );

            /*
             * SELECT
             */
            System.out.println("\n[5] SELECT *");

            result = executor.execute(
                    """
                    SELECT *
                    FROM employees;
                    """
            );

            printResult(result);
            printRows(result);

            /*
             * DELETE
             *
             * Record ID değerleri 0'dan başladığı için
             * ikinci eklenen kayıt record_id = 1 olur.
             */
            System.out.println("\n[6] DELETE");

            result = executor.execute(
                    """
                    DELETE FROM employees
                    WHERE record_id = 1;
                    """
            );

            printResult(result);

            /*
             * SELECT AGAIN
             */
            System.out.println("\n[7] SELECT AFTER DELETE");

            result = executor.execute(
                    """
                    SELECT *
                    FROM employees;
                    """
            );

            printResult(result);
            printRows(result);

            /*
             * DROP TABLE
             */
            System.out.println("\n[8] DROP TABLE");

            result = executor.execute(
                    new DropTableCommand("employees")
            );

            printResult(result);

            /*
             * DROP DATABASE
             */
            System.out.println("\n[9] DROP DATABASE");

            result = executor.execute(
                    new DropDatabaseCommand("company")
            );

            printResult(result);

            completedSuccessfully = true;

        } catch (Exception exception) {

            System.err.println();
            System.err.println("========================================");
            System.err.println("             Demo Failed");
            System.err.println("========================================");

            exception.printStackTrace();
        }

        if (completedSuccessfully) {
            System.out.println();
            System.out.println("========================================");
            System.out.println("      Demo Completed Successfully");
            System.out.println("========================================");
        }
    }

    private static void printResult(
            ExecuteResult result
    ) {
        System.out.println("----------------------------------------");

        System.out.println(
                "Success       : " + result.isSuccess()
        );

        System.out.println(
                "Message       : " + result.getMessage()
        );

        System.out.println(
                "Affected Rows : " + result.getAffectedRows()
        );

        System.out.println("----------------------------------------");
    }

    private static void printRows(
            ExecuteResult result
    ) {
        if (!result.hasRows()) {
            System.out.println("No rows returned.");
            return;
        }

        System.out.println();
        System.out.println("Returned Rows");
        System.out.println("----------------------------------------");

        for (var row : result.getRows()) {
            System.out.println(row.getValues());
        }

        System.out.println("----------------------------------------");
    }
}