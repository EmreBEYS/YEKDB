package com.yekdb.query.executor;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.result.QueryResult;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.HavingClause;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.TableReference;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectExecutorMultiJoinAdvancedTest {

    private SelectExecutor selectExecutor;

    private Table employeeTable;
    private Table departmentTable;
    private Table companyTable;

    private List<Row> employeeRows;
    private List<Row> departmentRows;
    private List<Row> companyRows;

    @BeforeEach
    void setUp() {

        selectExecutor =
                new SelectExecutor();

        employeeTable =
                new Table(
                        "employee",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING),
                                new Column("department_id", DataType.INT),
                                new Column("salary", DataType.INT)
                        )
                );

        departmentTable =
                new Table(
                        "department",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING),
                                new Column("company_id", DataType.INT)
                        )
                );

        companyTable =
                new Table(
                        "company",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING),
                                new Column("active", DataType.BOOLEAN)
                        )
                );

        employeeRows =
                List.of(
                        new Row(List.of(1, "Emre", 10, 40000)),
                        new Row(List.of(2, "Ayşe", 10, 50000)),
                        new Row(List.of(3, "Ali", 20, 30000)),
                        new Row(List.of(4, "Mert", 99, 60000))
                );

        departmentRows =
                List.of(
                        new Row(List.of(10, "Software", 100)),
                        new Row(List.of(20, "Finance", 200)),
                        new Row(List.of(30, "Human Resources", 100))
                );

        companyRows =
                List.of(
                        new Row(List.of(100, "YEK Technology", true)),
                        new Row(List.of(200, "YEK Finance", false))
                );
    }

    @Test
    void multiJoinShouldProjectColumnsFromThreeTables() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("e.name"),
                                new SelectItem("d.name", "department_name"),
                                new SelectItem("c.name", "company_name")
                        ),
                        null,
                        null,
                        null
                );

        QueryResult result =
                execute(statement);

        assertEquals(3, result.getRows().size());
        assertEquals("Emre", result.getRows().get(0).getValue(0));
        assertEquals("Software", result.getRows().get(0).getValue(1));
        assertEquals("YEK Technology", result.getRows().get(0).getValue(2));
    }

    @Test
    void multiJoinWhereShouldFilterFinalJoinedRows() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("e.name"),
                                new SelectItem("c.name", "company_name")
                        ),
                        new ComparisonExpression(
                                "c.active",
                                ComparisonOperator.EQUALS,
                                true
                        ),
                        null,
                        null
                );

        QueryResult result =
                execute(statement);

        assertEquals(2, result.getRows().size());
        assertEquals("Emre", result.getRows().get(0).getValue(0));
        assertEquals("Ayşe", result.getRows().get(1).getValue(0));
    }

    @Test
    void secondJoinShouldUseColumnProducedByFirstJoin() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("e.name"),
                                new SelectItem("d.company_id"),
                                new SelectItem("c.id")
                        ),
                        null,
                        null,
                        null
                );

        QueryResult result =
                execute(statement);

        assertEquals(3, result.getRows().size());
        assertEquals(100, result.getRows().get(0).getValue(1));
        assertEquals(100, result.getRows().get(0).getValue(2));
        assertEquals(200, result.getRows().get(2).getValue(1));
        assertEquals(200, result.getRows().get(2).getValue(2));
    }

    @Test
    void multiJoinGroupByCountShouldWork() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("c.name"),
                                new SelectItem("COUNT(e.id)", "employee_count")
                        ),
                        null,
                        new GroupByClause(
                                List.of("c.name")
                        ),
                        null
                );

        QueryResult result =
                execute(statement);

        assertEquals(2, result.getRows().size());
        assertEquals("YEK Technology", result.getRows().get(0).getValue(0));
        assertEquals(2L, result.getRows().get(0).getValue(1));
        assertEquals("YEK Finance", result.getRows().get(1).getValue(0));
        assertEquals(1L, result.getRows().get(1).getValue(1));
    }

    @Test
    void multiJoinShouldSupportAllAggregateFunctions() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("d.name"),
                                new SelectItem("COUNT(e.id)", "employee_count"),
                                new SelectItem("SUM(e.salary)", "total_salary"),
                                new SelectItem("AVG(e.salary)", "average_salary"),
                                new SelectItem("MIN(e.salary)", "minimum_salary"),
                                new SelectItem("MAX(e.salary)", "maximum_salary")
                        ),
                        null,
                        new GroupByClause(
                                List.of("d.name")
                        ),
                        null
                );

        QueryResult result =
                execute(statement);

        assertEquals(2, result.getRows().size());

        Row software =
                result.getRows().get(0);

        assertEquals("Software", software.getValue(0));
        assertEquals(2L, software.getValue(1));
        assertEquals(90000.0, (Double) software.getValue(2), 0.001);
        assertEquals(45000.0, (Double) software.getValue(3), 0.001);
        assertEquals(40000, software.getValue(4));
        assertEquals(50000, software.getValue(5));
    }

    @Test
    void multiJoinHavingShouldFilterAggregateGroups() {

        HavingClause havingClause =
                new HavingClause(
                        new ComparisonExpression(
                                "employee_count",
                                ComparisonOperator.GREATER_THAN_OR_EQUALS,
                                2L
                        )
                );

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("c.name"),
                                new SelectItem("COUNT(e.id)", "employee_count")
                        ),
                        null,
                        new GroupByClause(
                                List.of("c.name")
                        ),
                        havingClause
                );

        QueryResult result =
                execute(statement);

        assertEquals(1, result.getRows().size());
        assertEquals("YEK Technology", result.getRows().get(0).getValue(0));
        assertEquals(2L, result.getRows().get(0).getValue(1));
    }

    @Test
    void whereShouldRunBeforeMultiJoinAggregation() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("c.name"),
                                new SelectItem("COUNT(e.id)", "employee_count")
                        ),
                        new ComparisonExpression(
                                "e.salary",
                                ComparisonOperator.GREATER_THAN,
                                40000
                        ),
                        new GroupByClause(
                                List.of("c.name")
                        ),
                        null
                );

        QueryResult result =
                execute(statement);

        assertEquals(1, result.getRows().size());
        assertEquals("YEK Technology", result.getRows().get(0).getValue(0));
        assertEquals(1L, result.getRows().get(0).getValue(1));
    }

    @Test
    void globalAggregateAcrossMultipleJoinsShouldWork() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("COUNT(*)", "joined_count"),
                                new SelectItem("SUM(e.salary)", "salary_sum")
                        ),
                        null,
                        null,
                        null
                );

        QueryResult result =
                execute(statement);

        assertEquals(1, result.getRows().size());
        assertEquals(3L, result.getRows().get(0).getValue(0));
        assertEquals(120000.0, (Double) result.getRows().get(0).getValue(1), 0.001);
    }

    @Test
    void unqualifiedIdShouldBeAmbiguousAcrossThreeTables() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("id")
                        ),
                        null,
                        null,
                        null
                );

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () -> execute(statement)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Ambiguous column reference")
        );
    }

    @Test
    void unknownQualifiedColumnShouldFailClearly() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("x.name")
                        ),
                        null,
                        null,
                        null
                );

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () -> execute(statement)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Unknown table or alias")
        );
    }

    private QueryResult execute(
            SelectStatement statement
    ) {

        return selectExecutor.executeStatement(
                employeeTable,
                employeeRows,
                List.of(
                        departmentTable,
                        companyTable
                ),
                List.of(
                        departmentRows,
                        companyRows
                ),
                statement
        );
    }

    private SelectStatement createStatement(
            List<SelectItem> selectItems,
            Expression whereExpression,
            GroupByClause groupByClause,
            HavingClause havingClause
    ) {

        JoinClause departmentJoin =
                new JoinClause(
                        JoinType.INNER,
                        "department",
                        "d",
                        new ComparisonExpression(
                                ColumnExpression.parse("e.department_id"),
                                ComparisonOperator.EQUALS,
                                ColumnExpression.parse("d.id")
                        )
                );

        JoinClause companyJoin =
                new JoinClause(
                        JoinType.INNER,
                        "company",
                        "c",
                        new ComparisonExpression(
                                ColumnExpression.parse("d.company_id"),
                                ComparisonOperator.EQUALS,
                                ColumnExpression.parse("c.id")
                        )
                );

        return new SelectStatement(
                new TableReference("employee", "e"),
                selectItems,
                List.of(
                        departmentJoin,
                        companyJoin
                ),
                whereExpression,
                groupByClause,
                havingClause,
                List.of(),
                null,
                null
        );
    }
}
