package com.yekdb.query.executor;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.result.QueryResult;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.TableReference;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectExecutorMultiJoinIntegrationTest {

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
                        )
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
    void multiJoinSelectAllShouldReturnAllThreeTableSchemas() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("*")
                        ),
                        null
                );

        QueryResult result =
                execute(statement);

        assertEquals(3, result.getRows().size());

        assertEquals(
                List.of(
                        "e.id",
                        "e.name",
                        "e.department_id",
                        "e.salary",
                        "d.id",
                        "d.name",
                        "d.company_id",
                        "c.id",
                        "c.name",
                        "c.active"
                ),
                result.getColumns()
                        .stream()
                        .map(Column::getName)
                        .toList()
        );
    }

    @Test
    void multiJoinProjectionAliasShouldBePreserved() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("e.name", "employee_name"),
                                new SelectItem("d.name", "department_name"),
                                new SelectItem("c.name", "company_name")
                        ),
                        null
                );

        QueryResult result =
                execute(statement);

        assertEquals(
                List.of(
                        "employee_name",
                        "department_name",
                        "company_name"
                ),
                result.getColumns()
                        .stream()
                        .map(Column::getName)
                        .toList()
        );
    }

    @Test
    void unqualifiedIdShouldBeAmbiguousAcrossThreeTables() {

        SelectStatement statement =
                createStatement(
                        List.of(
                                new SelectItem("id")
                        ),
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
            com.yekdb.query.expression.Expression whereExpression
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
                null,
                null,
                List.of(),
                null,
                null
        );
    }
}
