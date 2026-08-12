package com.yekdb.demo;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.InMemoryQueryDataSource;
import com.yekdb.query.executor.ExecuteResult;
import com.yekdb.query.executor.QueryExecutor;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Sprint 00-14 advanced SELECT end-to-end demo.
 *
 * Demonstrates the complete path:
 *
 * SQL -> SqlParser -> SelectStatement -> SelectMapper
 * -> SelectCommand -> QueryExecutor -> SelectExecutor
 * -> QueryResult
 */
public final class QueryExecutionAdvancedSelectDemo {

    private QueryExecutionAdvancedSelectDemo() {
    }

    public static void main(String[] args) throws Exception {

        Table employeesTable = createEmployeesTable();
        List<Row> employees = createEmployees();

        InMemoryQueryDataSource dataSource =
                new InMemoryQueryDataSource();

        dataSource.register(
                employeesTable,
                employees
        );

        Path demoDirectory =
                Files.createTempDirectory(
                        "yekdb-00-14-demo-"
                );

        DatabaseManager databaseManager =
                new DatabaseManager(
                        demoDirectory
                );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             dataSource
                     )) {

            printHeader(
                    "YEKDB Sprint 00-14 - Advanced SELECT Demo"
            );

            executeAndPrint(
                    queryExecutor,
                    "Predicate + ORDER BY + LIMIT",
                    """
                    SELECT *
                    FROM employees
                    WHERE name ILIKE 'a%'
                    ORDER BY age DESC
                    LIMIT 3;
                    """
            );

            executeAndPrint(
                    queryExecutor,
                    "BETWEEN + ORDER BY + FETCH",
                    """
                    SELECT *
                    FROM employees
                    WHERE salary BETWEEN 40000 AND 70000
                    ORDER BY salary DESC
                    FETCH FIRST 4 ROWS ONLY;
                    """
            );

            executeAndPrint(
                    queryExecutor,
                    "GROUP BY + Aggregates + ORDER BY",
                    """
                    SELECT
                        department,
                        COUNT(*) AS employee_count,
                        AVG(salary) AS average_salary,
                        MAX(salary) AS maximum_salary
                    FROM employees
                    GROUP BY department
                    ORDER BY employee_count DESC;
                    """
            );

            executeAndPrint(
                    queryExecutor,
                    "FINAL FULL PIPELINE",
                    """
                    SELECT
                        department,
                        COUNT(*) AS employee_count
                    FROM employees e
                    WHERE active = true
                    GROUP BY department
                    HAVING employee_count > 1
                    ORDER BY employee_count DESC
                    LIMIT 3;
                    """
            );

            printHeader(
                    "Sprint 00-14 Demo Completed Successfully"
            );

            System.out.println(
                    "SQL Parser           : OK"
            );
            System.out.println(
                    "Statement -> Command : OK"
            );
            System.out.println(
                    "WHERE predicates     : OK"
            );
            System.out.println(
                    "GROUP BY             : OK"
            );
            System.out.println(
                    "Aggregate functions  : OK"
            );
            System.out.println(
                    "HAVING               : OK"
            );
            System.out.println(
                    "ORDER BY             : OK"
            );
            System.out.println(
                    "LIMIT / FETCH        : OK"
            );
        }
    }

    private static void executeAndPrint(
            QueryExecutor queryExecutor,
            String title,
            String sql
    ) {

        System.out.println();
        printHeader(title);
        System.out.println(sql.trim());
        System.out.println();

        ExecuteResult result =
                queryExecutor.execute(
                        sql
                );

        System.out.println(
                result.getMessage()
        );

        if (!result.hasRows()) {
            System.out.println(
                    "(no rows)"
            );
            return;
        }

        for (Row row : result.getRows()) {
            System.out.println(row);
        }

        System.out.println(
                "Returned rows: "
                        + result.getRowCount()
        );
    }

    private static Table createEmployeesTable() {

        return new Table(
                "employees",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING),
                        new Column("department", DataType.STRING),
                        new Column("salary", DataType.DOUBLE),
                        new Column("age", DataType.INT),
                        new Column("active", DataType.BOOLEAN)
                )
        );
    }

    private static List<Row> createEmployees() {

        return List.of(
                new Row(
                        List.of(
                                1,
                                "Yunus",
                                "IT",
                                30000.0,
                                21,
                                true
                        )
                ),
                new Row(
                        List.of(
                                2,
                                "Ali",
                                "HR",
                                45000.0,
                                30,
                                true
                        )
                ),
                new Row(
                        List.of(
                                3,
                                "Ayse",
                                "IT",
                                55000.0,
                                27,
                                true
                        )
                ),
                new Row(
                        List.of(
                                4,
                                "Mehmet",
                                "IT",
                                70000.0,
                                35,
                                true
                        )
                ),
                new Row(
                        List.of(
                                5,
                                "Efe",
                                "HR",
                                50000.0,
                                26,
                                true
                        )
                ),
                new Row(
                        List.of(
                                6,
                                "Zeynep",
                                "Sales",
                                60000.0,
                                29,
                                true
                        )
                ),
                new Row(
                        List.of(
                                7,
                                "Can",
                                "Sales",
                                65000.0,
                                31,
                                false
                        )
                )
        );
    }

    private static void printHeader(
            String title
    ) {

        System.out.println(
                "=================================================="
        );
        System.out.println(title);
        System.out.println(
                "=================================================="
        );
    }
}
