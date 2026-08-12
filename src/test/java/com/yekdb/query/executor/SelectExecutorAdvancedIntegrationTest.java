package com.yekdb.query.executor;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.result.QueryResult;
import com.yekdb.query.statement.FetchClause;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.HavingClause;
import com.yekdb.query.statement.LimitClause;
import com.yekdb.query.statement.OrderByItem;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.SortDirection;
import com.yekdb.query.statement.TableReference;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelectExecutorAdvancedIntegrationTest {

    private SelectExecutor executor;

    private Table table;

    private List<Row> rows;

    @BeforeEach
    void setUp() {

        executor =
                new SelectExecutor();

        table =
                new Table(
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
                                        "department",
                                        DataType.STRING
                                ),
                                new Column(
                                        "salary",
                                        DataType.DOUBLE
                                ),
                                new Column(
                                        "age",
                                        DataType.INT
                                ),
                                new Column(
                                        "active",
                                        DataType.BOOLEAN
                                )
                        )
                );

        /*
         * Department dağılımı:
         *
         * IT:
         *  Yunus   true
         *  Ayse    true
         *  Mehmet  true
         *
         * HR:
         *  Ali     true
         *  Efe     true
         *
         * Sales:
         *  Zeynep  true
         *  Can     false
         *
         * Active employee counts:
         *
         * IT    -> 3
         * HR    -> 2
         * Sales -> 1
         */
        rows =
                List.of(
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

    // ==================================================
    // WHERE + ORDER BY
    // ==================================================

    @Test
    void shouldExecuteWhereAndOrderBy() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        new ComparisonExpression(
                                "active",
                                ComparisonOperator.EQUALS,
                                true
                        ),
                        null,
                        null,
                        List.of(
                                new OrderByItem(
                                        "age",
                                        SortDirection.DESC
                                )
                        ),
                        null,
                        null
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                6,
                result.getRows().size()
        );

        /*
         * En yaşlı active çalışan Mehmet -> 35
         */
        assertEquals(
                35,
                result.getRows()
                        .get(0)
                        .getValue(4)
        );

        /*
         * En genç active çalışan Yunus -> 21
         */
        assertEquals(
                21,
                result.getRows()
                        .get(5)
                        .getValue(4)
        );
    }

    // ==================================================
    // LIMIT
    // ==================================================

    @Test
    void shouldExecuteLimit() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        null,
                        null,
                        List.of(),
                        new LimitClause(3),
                        null
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                3,
                result.getRows().size()
        );

        assertEquals(
                1,
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                3,
                result.getRows()
                        .get(2)
                        .getValue(0)
        );
    }

    // ==================================================
    // FETCH FIRST
    // ==================================================

    @Test
    void shouldExecuteFetchFirst() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        new FetchClause(
                                FetchClause.Mode.FIRST,
                                2
                        )
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                2,
                result.getRows().size()
        );
    }

    // ==================================================
    // FETCH NEXT
    // ==================================================

    @Test
    void shouldExecuteFetchNext() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem("*")
                        ),
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        new FetchClause(
                                FetchClause.Mode.NEXT,
                                4
                        )
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        /*
         * OFFSET desteğimiz henüz olmadığı için
         * FETCH NEXT execution açısından ilk N satırı
         * sınırlar.
         */
        assertEquals(
                4,
                result.getRows().size()
        );
    }

    // ==================================================
    // COUNT(*)
    // ==================================================

    @Test
    void shouldExecuteGlobalCount() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem(
                                        "COUNT(*)",
                                        "employee_count"
                                )
                        ),
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                1,
                result.getRows().size()
        );

        assertEquals(
                1,
                result.getColumns().size()
        );

        assertEquals(
                "employee_count",
                result.getColumns()
                        .get(0)
                        .getName()
        );

        assertEquals(
                DataType.LONG,
                result.getColumns()
                        .get(0)
                        .getDataType()
        );

        assertEquals(
                7L,
                result.getRows()
                        .get(0)
                        .getValue(0)
        );
    }

    // ==================================================
    // SUM
    // ==================================================

    @Test
    void shouldExecuteGlobalSum() {

        SelectStatement statement =
                aggregateStatement(
                        "SUM(salary)",
                        "total_salary"
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                375000.0,
                (Double) result
                        .getRows()
                        .get(0)
                        .getValue(0),
                0.0001
        );

        assertEquals(
                DataType.DOUBLE,
                result.getColumns()
                        .get(0)
                        .getDataType()
        );
    }

    // ==================================================
    // AVG
    // ==================================================

    @Test
    void shouldExecuteGlobalAverage() {

        SelectStatement statement =
                aggregateStatement(
                        "AVG(salary)",
                        "average_salary"
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                375000.0 / 7.0,
                (Double) result
                        .getRows()
                        .get(0)
                        .getValue(0),
                0.0001
        );
    }

    // ==================================================
    // MIN
    // ==================================================

    @Test
    void shouldExecuteGlobalMinimum() {

        SelectStatement statement =
                aggregateStatement(
                        "MIN(salary)",
                        "minimum_salary"
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                30000.0,
                result.getRows()
                        .get(0)
                        .getValue(0)
        );
    }

    // ==================================================
    // MAX
    // ==================================================

    @Test
    void shouldExecuteGlobalMaximum() {

        SelectStatement statement =
                aggregateStatement(
                        "MAX(salary)",
                        "maximum_salary"
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                70000.0,
                result.getRows()
                        .get(0)
                        .getValue(0)
        );
    }

    // ==================================================
    // GROUP BY + COUNT
    // ==================================================

    @Test
    void shouldExecuteGroupByWithCount() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem(
                                        "department"
                                ),
                                new SelectItem(
                                        "COUNT(*)",
                                        "employee_count"
                                )
                        ),
                        null,
                        new GroupByClause(
                                "department"
                        ),
                        null,
                        List.of(),
                        null,
                        null
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                3,
                result.getRows().size()
        );

        assertEquals(
                List.of(
                        "department",
                        "employee_count"
                ),
                result.getColumns()
                        .stream()
                        .map(Column::getName)
                        .toList()
        );

        /*
         * LinkedHashMap nedeniyle ilk karşılaşılan
         * department sırasını koruyoruz:
         *
         * IT, HR, Sales
         */
        assertEquals(
                "IT",
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                3L,
                result.getRows()
                        .get(0)
                        .getValue(1)
        );

        assertEquals(
                "HR",
                result.getRows()
                        .get(1)
                        .getValue(0)
        );

        assertEquals(
                2L,
                result.getRows()
                        .get(1)
                        .getValue(1)
        );

        assertEquals(
                "Sales",
                result.getRows()
                        .get(2)
                        .getValue(0)
        );

        assertEquals(
                2L,
                result.getRows()
                        .get(2)
                        .getValue(1)
        );
    }

    // ==================================================
    // GROUP BY + MULTIPLE AGGREGATES
    // ==================================================

    @Test
    void shouldExecuteGroupByWithMultipleAggregates() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem(
                                        "department"
                                ),
                                new SelectItem(
                                        "COUNT(*)",
                                        "employee_count"
                                ),
                                new SelectItem(
                                        "SUM(salary)",
                                        "total_salary"
                                ),
                                new SelectItem(
                                        "AVG(salary)",
                                        "average_salary"
                                ),
                                new SelectItem(
                                        "MIN(salary)",
                                        "minimum_salary"
                                ),
                                new SelectItem(
                                        "MAX(salary)",
                                        "maximum_salary"
                                )
                        ),
                        null,
                        new GroupByClause(
                                "department"
                        ),
                        null,
                        List.of(),
                        null,
                        null
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                3,
                result.getRows().size()
        );

        /*
         * IT:
         *
         * 30000
         * 55000
         * 70000
         */
        Row it =
                result.getRows()
                        .get(0);

        assertEquals(
                "IT",
                it.getValue(0)
        );

        assertEquals(
                3L,
                it.getValue(1)
        );

        assertEquals(
                155000.0,
                (Double) it.getValue(2),
                0.0001
        );

        assertEquals(
                155000.0 / 3.0,
                (Double) it.getValue(3),
                0.0001
        );

        assertEquals(
                30000.0,
                it.getValue(4)
        );

        assertEquals(
                70000.0,
                it.getValue(5)
        );
    }

    // ==================================================
    // HAVING
    // ==================================================

    @Test
    void shouldFilterGroupsUsingHaving() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem(
                                        "department"
                                ),
                                new SelectItem(
                                        "COUNT(*)",
                                        "employee_count"
                                )
                        ),
                        null,
                        new GroupByClause(
                                "department"
                        ),
                        new HavingClause(
                                new ComparisonExpression(
                                        "employee_count",
                                        ComparisonOperator.GREATER_THAN,
                                        2
                                )
                        ),
                        List.of(),
                        null,
                        null
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        /*
         * Sadece IT count=3
         */
        assertEquals(
                1,
                result.getRows().size()
        );

        assertEquals(
                "IT",
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                3L,
                result.getRows()
                        .get(0)
                        .getValue(1)
        );
    }

    // ==================================================
    // AGGREGATE ALIAS + ORDER BY
    // ==================================================

    @Test
    void shouldOrderByAggregateAlias() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem(
                                        "department"
                                ),
                                new SelectItem(
                                        "SUM(salary)",
                                        "total_salary"
                                )
                        ),
                        null,
                        new GroupByClause(
                                "department"
                        ),
                        null,
                        List.of(
                                new OrderByItem(
                                        "total_salary",
                                        SortDirection.DESC
                                )
                        ),
                        null,
                        null
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        /*
         * IT    = 155000
         * Sales = 125000
         * HR    = 95000
         */
        assertEquals(
                "IT",
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                155000.0,
                result.getRows()
                        .get(0)
                        .getValue(1)
        );

        assertEquals(
                "Sales",
                result.getRows()
                        .get(1)
                        .getValue(0)
        );

        assertEquals(
                "HR",
                result.getRows()
                        .get(2)
                        .getValue(0)
        );
    }

    // ==================================================
    // WHERE + GROUP BY
    // ==================================================

    @Test
    void shouldApplyWhereBeforeGrouping() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem(
                                        "department"
                                ),
                                new SelectItem(
                                        "COUNT(*)",
                                        "employee_count"
                                )
                        ),
                        new ComparisonExpression(
                                "active",
                                ComparisonOperator.EQUALS,
                                true
                        ),
                        new GroupByClause(
                                "department"
                        ),
                        null,
                        List.of(),
                        null,
                        null
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        assertEquals(
                3,
                result.getRows().size()
        );

        /*
         * Sales:
         *
         * Zeynep true
         * Can    false
         *
         * WHERE sonrası sadece 1 kişi.
         */
        Row sales =
                result.getRows()
                        .stream()
                        .filter(
                                row ->
                                        "Sales".equals(
                                                row.getValue(0)
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                1L,
                sales.getValue(1)
        );
    }

    // ==================================================
    // FINAL FULL PIPELINE
    // ==================================================

    @Test
    void shouldExecuteCompleteSelectPipeline() {

        /*
         * SQL karşılığı:
         *
         * SELECT
         *     department,
         *     COUNT(*) AS employee_count
         * FROM employees
         * WHERE active = true
         * GROUP BY department
         * HAVING employee_count > 1
         * ORDER BY employee_count DESC
         * LIMIT 1;
         */

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees",
                                "e"
                        ),
                        List.of(
                                new SelectItem(
                                        "department"
                                ),
                                new SelectItem(
                                        "COUNT(*)",
                                        "employee_count"
                                )
                        ),
                        new ComparisonExpression(
                                "active",
                                ComparisonOperator.EQUALS,
                                true
                        ),
                        new GroupByClause(
                                "department"
                        ),
                        new HavingClause(
                                new ComparisonExpression(
                                        "employee_count",
                                        ComparisonOperator.GREATER_THAN,
                                        1
                                )
                        ),
                        List.of(
                                new OrderByItem(
                                        "employee_count",
                                        SortDirection.DESC
                                )
                        ),
                        new LimitClause(
                                1
                        ),
                        null
                );

        QueryResult result =
                executor.executeStatement(
                        table,
                        rows,
                        statement
                );

        /*
         * WHERE active=true
         *
         * IT    -> 3
         * HR    -> 2
         * Sales -> 1
         *
         * HAVING > 1
         *
         * IT -> 3
         * HR -> 2
         *
         * ORDER DESC
         *
         * IT -> 3
         * HR -> 2
         *
         * LIMIT 1
         *
         * IT -> 3
         */
        assertEquals(
                1,
                result.getRows().size()
        );

        assertEquals(
                "IT",
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                3L,
                result.getRows()
                        .get(0)
                        .getValue(1)
        );

        assertEquals(
                "department",
                result.getColumns()
                        .get(0)
                        .getName()
        );

        assertEquals(
                "employee_count",
                result.getColumns()
                        .get(1)
                        .getName()
        );

        assertTrue(
                result.getExecutionTimeNanos()
                        >= 0
        );
    }

    // ==================================================
    // VALIDATION
    // ==================================================

    @Test
    void shouldRejectNonAggregateColumnWithoutGroupBy() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem(
                                        "department"
                                ),
                                new SelectItem(
                                        "COUNT(*)",
                                        "employee_count"
                                )
                        ),
                        null,
                        null,
                        null,
                        List.of(),
                        null,
                        null
                );

        assertThrows(
                QueryExecutionException.class,
                () ->
                        executor.executeStatement(
                                table,
                                rows,
                                statement
                        )
        );
    }

    @Test
    void shouldRejectColumnMissingFromGroupBy() {

        /*
         * name GROUP BY içerisinde değil.
         */
        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employees"
                        ),
                        List.of(
                                new SelectItem(
                                        "department"
                                ),
                                new SelectItem(
                                        "name"
                                ),
                                new SelectItem(
                                        "COUNT(*)",
                                        "employee_count"
                                )
                        ),
                        null,
                        new GroupByClause(
                                "department"
                        ),
                        null,
                        List.of(),
                        null,
                        null
                );

        assertThrows(
                QueryExecutionException.class,
                () ->
                        executor.executeStatement(
                                table,
                                rows,
                                statement
                        )
        );
    }

    // ==================================================
    // HELPERS
    // ==================================================

    private SelectStatement aggregateStatement(
            String expression,
            String alias
    ) {

        return new SelectStatement(
                new TableReference(
                        "employees"
                ),
                List.of(
                        new SelectItem(
                                expression,
                                alias
                        )
                ),
                null,
                null,
                null,
                List.of(),
                null,
                null
        );
    }
}