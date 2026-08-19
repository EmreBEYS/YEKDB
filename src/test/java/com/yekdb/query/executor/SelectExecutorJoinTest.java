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

import static org.junit.jupiter.api.Assertions.*;

class SelectExecutorJoinTest {

    private SelectExecutor selectExecutor;

    private Table employeeTable;
    private Table departmentTable;

    private List<Row> employeeRows;
    private List<Row> departmentRows;

    @BeforeEach
    void setUp() {

        selectExecutor =
                new SelectExecutor();

        employeeTable =
                createEmployeeTable();

        departmentTable =
                createDepartmentTable();

        employeeRows =
                createEmployeeRows();

        departmentRows =
                createDepartmentRows();
    }

    // --------------------------------------------------
    // TEST 1
    // BASIC INNER JOIN + PROJECTION
    // --------------------------------------------------

    @Test
    void shouldExecuteInnerJoinWithQualifiedProjection() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employee",
                                "e"
                        ),
                        List.of(
                                new SelectItem(
                                        "e.name"
                                ),
                                new SelectItem(
                                        "d.name"
                                )
                        ),
                        List.of(
                                createDepartmentJoin()
                        )
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        employeeTable,
                        employeeRows,
                        departmentTable,
                        departmentRows,
                        statement
                );

        assertEquals(
                3,
                result.getRows().size()
        );

        assertEquals(
                2,
                result.getColumns().size()
        );

        assertEquals(
                "e.name",
                result.getColumns()
                        .get(0)
                        .getName()
        );

        assertEquals(
                "d.name",
                result.getColumns()
                        .get(1)
                        .getName()
        );

        assertEquals(
                "Yunus",
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                "IT",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );

        assertEquals(
                "Ali",
                result.getRows()
                        .get(1)
                        .getValue(0)
        );

        assertEquals(
                "HR",
                result.getRows()
                        .get(1)
                        .getValue(1)
        );
    }

    // --------------------------------------------------
    // TEST 2
    // INNER JOIN EXCLUDES NON-MATCHING ROW
    // --------------------------------------------------

    @Test
    void shouldExcludeRowsWithoutMatchingDepartment() {

        List<Row> rows =
                List.of(
                        new Row(
                                List.of(
                                        1,
                                        "Yunus",
                                        10
                                )
                        ),
                        new Row(
                                List.of(
                                        2,
                                        "Unknown",
                                        999
                                )
                        )
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employee",
                                "e"
                        ),
                        List.of(
                                new SelectItem(
                                        "e.name"
                                ),
                                new SelectItem(
                                        "d.name"
                                )
                        ),
                        List.of(
                                createDepartmentJoin()
                        )
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        employeeTable,
                        rows,
                        departmentTable,
                        departmentRows,
                        statement
                );

        assertEquals(
                1,
                result.getRows().size()
        );

        assertEquals(
                "Yunus",
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                "IT",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );
    }

    // --------------------------------------------------
    // TEST 3
    // WHERE AFTER JOIN
    // --------------------------------------------------

    @Test
    void shouldApplyWhereAfterJoin() {

        ComparisonExpression whereExpression =
                new ComparisonExpression(
                        new ColumnExpression(
                                "d",
                                "name"
                        ),
                        ComparisonOperator.EQUALS,
                        "IT"
                );

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employee",
                                "e"
                        ),
                        List.of(
                                new SelectItem(
                                        "e.name"
                                ),
                                new SelectItem(
                                        "d.name"
                                )
                        ),
                        List.of(
                                createDepartmentJoin()
                        ),
                        whereExpression
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        employeeTable,
                        employeeRows,
                        departmentTable,
                        departmentRows,
                        statement
                );

        assertEquals(
                2,
                result.getRows().size()
        );

        assertEquals(
                "Yunus",
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                "IT",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );

        assertEquals(
                "Ayse",
                result.getRows()
                        .get(1)
                        .getValue(0)
        );

        assertEquals(
                "IT",
                result.getRows()
                        .get(1)
                        .getValue(1)
        );
    }

    // --------------------------------------------------
    // TEST 4
    // SELECT *
    // --------------------------------------------------

    @Test
    void shouldSelectAllColumnsFromJoinedTables() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employee",
                                "e"
                        ),
                        List.of(
                                new SelectItem(
                                        "*"
                                )
                        ),
                        List.of(
                                createDepartmentJoin()
                        )
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        employeeTable,
                        employeeRows,
                        departmentTable,
                        departmentRows,
                        statement
                );

        assertEquals(
                3,
                result.getRows().size()
        );

        /*
         * employee:
         *
         * id
         * name
         * department_id
         *
         * department:
         *
         * id
         * name
         *
         * Toplam = 5
         */
        assertEquals(
                5,
                result.getColumns().size()
        );

        assertEquals(
                "e.id",
                result.getColumns()
                        .get(0)
                        .getName()
        );

        assertEquals(
                "e.name",
                result.getColumns()
                        .get(1)
                        .getName()
        );

        assertEquals(
                "e.department_id",
                result.getColumns()
                        .get(2)
                        .getName()
        );

        assertEquals(
                "d.id",
                result.getColumns()
                        .get(3)
                        .getName()
        );

        assertEquals(
                "d.name",
                result.getColumns()
                        .get(4)
                        .getName()
        );
    }

    // --------------------------------------------------
    // TEST 5
    // TABLE NAME INSTEAD OF ALIAS
    // --------------------------------------------------

    @Test
    void shouldAllowRealTableNamesInProjection() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employee",
                                "e"
                        ),
                        List.of(
                                new SelectItem(
                                        "employee.name"
                                ),
                                new SelectItem(
                                        "department.name"
                                )
                        ),
                        List.of(
                                createDepartmentJoin()
                        )
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        employeeTable,
                        employeeRows,
                        departmentTable,
                        departmentRows,
                        statement
                );

        assertEquals(
                3,
                result.getRows().size()
        );

        assertEquals(
                "Yunus",
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                "IT",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );
    }

    // --------------------------------------------------
    // TEST 6
    // AMBIGUOUS COLUMN
    // --------------------------------------------------

    @Test
    void shouldRejectAmbiguousUnqualifiedColumn() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employee",
                                "e"
                        ),
                        List.of(
                                new SelectItem(
                                        "id"
                                )
                        ),
                        List.of(
                                createDepartmentJoin()
                        )
                );

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () ->
                                selectExecutor.executeStatement(
                                        employeeTable,
                                        employeeRows,
                                        departmentTable,
                                        departmentRows,
                                        statement
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Ambiguous column reference"
                        )
        );
    }

    // --------------------------------------------------
    // TEST 7
    // UNIQUE UNQUALIFIED COLUMN
    // --------------------------------------------------

    @Test
    void shouldAllowUniqueUnqualifiedColumn() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employee",
                                "e"
                        ),
                        List.of(
                                new SelectItem(
                                        "department_id"
                                )
                        ),
                        List.of(
                                createDepartmentJoin()
                        )
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        employeeTable,
                        employeeRows,
                        departmentTable,
                        departmentRows,
                        statement
                );

        assertEquals(
                3,
                result.getRows().size()
        );

        assertEquals(
                10,
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                20,
                result.getRows()
                        .get(1)
                        .getValue(0)
        );
    }

    // --------------------------------------------------
    // TEST 8
    // OLD SELECT OVERLOAD MUST REJECT JOIN
    // --------------------------------------------------

    @Test
    void oldExecuteStatementShouldRejectJoinStatement() {

        SelectStatement statement =
                new SelectStatement(
                        new TableReference(
                                "employee",
                                "e"
                        ),
                        List.of(
                                new SelectItem(
                                        "e.name"
                                )
                        ),
                        List.of(
                                createDepartmentJoin()
                        )
                );

        QueryExecutionException exception =
                assertThrows(
                        QueryExecutionException.class,
                        () ->
                                selectExecutor.executeStatement(
                                        employeeTable,
                                        employeeRows,
                                        statement
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "JOIN-aware executeStatement"
                        )
        );
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private JoinClause createDepartmentJoin() {

        return new JoinClause(
                JoinType.INNER,
                "department",
                "d",
                new ComparisonExpression(
                        new ColumnExpression(
                                "e",
                                "department_id"
                        ),
                        ComparisonOperator.EQUALS,
                        new ColumnExpression(
                                "d",
                                "id"
                        )
                )
        );
    }

    private Table createEmployeeTable() {

        return new Table(
                "employee",
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
                                "department_id",
                                DataType.INT
                        )
                )
        );
    }

    private Table createDepartmentTable() {

        return new Table(
                "department",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        ),
                        new Column(
                                "name",
                                DataType.STRING
                        )
                )
        );
    }

    private List<Row> createEmployeeRows() {

        return List.of(
                new Row(
                        List.of(
                                1,
                                "Yunus",
                                10
                        )
                ),
                new Row(
                        List.of(
                                2,
                                "Ali",
                                20
                        )
                ),
                new Row(
                        List.of(
                                3,
                                "Ayse",
                                10
                        )
                )
        );
    }

    private List<Row> createDepartmentRows() {

        return List.of(
                new Row(
                        List.of(
                                10,
                                "IT"
                        )
                ),
                new Row(
                        List.of(
                                20,
                                "HR"
                        )
                )
        );
    }
}