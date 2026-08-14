package com.yekdb.query.executor;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
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

import static org.junit.jupiter.api.Assertions.*;

class SelectExecutorJoinAdvancedTest {

    private SelectExecutor selectExecutor;

    private Table departmentTable;
    private Table employeeTable;

    private List<Row> departmentRows;
    private List<Row> employeeRows;

    private TableReference departmentReference;

    @BeforeEach
    void setUp() {

        selectExecutor =
                new SelectExecutor();

        departmentTable =
                createDepartmentTable();

        employeeTable =
                createEmployeeTable();

        departmentRows =
                createDepartmentRows();

        employeeRows =
                createEmployeeRows();

        departmentReference =
                new TableReference(
                        "department",
                        "d"
                );
    }

    @Test
    void joinGroupByCountShouldReturnGroupedResults() {

        SelectStatement statement =
                createStatement(
                        JoinType.INNER,
                        List.of(
                                new SelectItem(
                                        "d.name"
                                ),
                                new SelectItem(
                                        "COUNT(e.id)",
                                        "employee_count"
                                )
                        ),
                        null,
                        new GroupByClause(
                                List.of(
                                        "d.name"
                                )
                        ),
                        null
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        departmentTable,
                        departmentRows,
                        employeeTable,
                        employeeRows,
                        statement
                );

        /*
         * Software -> 2
         * Finance  -> 1
         */
        assertEquals(
                2,
                result.getRows().size()
        );

        assertEquals(
                "Software",
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

        assertEquals(
                "Finance",
                result.getRows()
                        .get(1)
                        .getValue(0)
        );

        assertEquals(
                1L,
                result.getRows()
                        .get(1)
                        .getValue(1)
        );
    }

    @Test
    void joinGroupByAverageShouldCalculateAggregate() {

        SelectStatement statement =
                createStatement(
                        JoinType.INNER,
                        List.of(
                                new SelectItem(
                                        "d.name"
                                ),
                                new SelectItem(
                                        "AVG(e.salary)",
                                        "average_salary"
                                )
                        ),
                        null,
                        new GroupByClause(
                                List.of(
                                        "d.name"
                                )
                        ),
                        null
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        departmentTable,
                        departmentRows,
                        employeeTable,
                        employeeRows,
                        statement
                );

        assertEquals(
                2,
                result.getRows().size()
        );

        /*
         * Software:
         *
         * 40000 + 50000
         * ----------------
         *        2
         *
         * = 45000
         */
        assertEquals(
                45000.0,
                (Double) result.getRows()
                        .get(0)
                        .getValue(1),
                0.001
        );

        assertEquals(
                30000.0,
                (Double) result.getRows()
                        .get(1)
                        .getValue(1),
                0.001
        );
    }

    @Test
    void joinGroupByShouldSupportMultipleAggregates() {

        SelectStatement statement =
                createStatement(
                        JoinType.INNER,
                        List.of(
                                new SelectItem(
                                        "d.name"
                                ),
                                new SelectItem(
                                        "COUNT(e.id)",
                                        "employee_count"
                                ),
                                new SelectItem(
                                        "SUM(e.salary)",
                                        "total_salary"
                                ),
                                new SelectItem(
                                        "AVG(e.salary)",
                                        "average_salary"
                                ),
                                new SelectItem(
                                        "MIN(e.salary)",
                                        "minimum_salary"
                                ),
                                new SelectItem(
                                        "MAX(e.salary)",
                                        "maximum_salary"
                                )
                        ),
                        null,
                        new GroupByClause(
                                List.of(
                                        "d.name"
                                )
                        ),
                        null
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        departmentTable,
                        departmentRows,
                        employeeTable,
                        employeeRows,
                        statement
                );

        Row software =
                result.getRows()
                        .get(0);

        assertEquals(
                "Software",
                software.getValue(0)
        );

        assertEquals(
                2L,
                software.getValue(1)
        );

        assertEquals(
                90000.0,
                (Double) software.getValue(2),
                0.001
        );

        assertEquals(
                45000.0,
                (Double) software.getValue(3),
                0.001
        );

        assertEquals(
                40000,
                software.getValue(4)
        );

        assertEquals(
                50000,
                software.getValue(5)
        );
    }

    @Test
    void joinHavingShouldFilterAggregateGroups() {

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
                        JoinType.INNER,
                        List.of(
                                new SelectItem(
                                        "d.name"
                                ),
                                new SelectItem(
                                        "COUNT(e.id)",
                                        "employee_count"
                                )
                        ),
                        null,
                        new GroupByClause(
                                List.of(
                                        "d.name"
                                )
                        ),
                        havingClause
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        departmentTable,
                        departmentRows,
                        employeeTable,
                        employeeRows,
                        statement
                );

        /*
         * employee_count >= 2 koşulunu
         * yalnızca Software sağlamalıdır.
         */
        assertEquals(
                1,
                result.getRows().size()
        );

        assertEquals(
                "Software",
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

    @Test
    void whereShouldRunBeforeJoinAggregation() {

        ComparisonExpression where =
                new ComparisonExpression(
                        "e.salary",
                        ComparisonOperator.GREATER_THAN,
                        40000
                );

        SelectStatement statement =
                createStatement(
                        JoinType.INNER,
                        List.of(
                                new SelectItem(
                                        "d.name"
                                ),
                                new SelectItem(
                                        "COUNT(e.id)",
                                        "employee_count"
                                )
                        ),
                        where,
                        new GroupByClause(
                                List.of(
                                        "d.name"
                                )
                        ),
                        null
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        departmentTable,
                        departmentRows,
                        employeeTable,
                        employeeRows,
                        statement
                );

        /*
         * Salary > 40000 sonrasında yalnızca
         * Ayşe kalır.
         *
         * Software -> 1
         */
        assertEquals(
                1,
                result.getRows().size()
        );

        assertEquals(
                "Software",
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                1L,
                result.getRows()
                        .get(0)
                        .getValue(1)
        );
    }

    @Test
    void leftJoinGroupByCountShouldPreserveEmptyDepartment() {

        SelectStatement statement =
                createStatement(
                        JoinType.LEFT,
                        List.of(
                                new SelectItem(
                                        "d.name"
                                ),
                                new SelectItem(
                                        "COUNT(e.id)",
                                        "employee_count"
                                )
                        ),
                        null,
                        new GroupByClause(
                                List.of(
                                        "d.name"
                                )
                        ),
                        null
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        departmentTable,
                        departmentRows,
                        employeeTable,
                        employeeRows,
                        statement
                );

        /*
         * Software        -> 2
         * Finance         -> 1
         * Human Resources -> 0
         */
        assertEquals(
                3,
                result.getRows().size()
        );

        Row hr =
                result.getRows()
                        .get(2);

        assertEquals(
                "Human Resources",
                hr.getValue(0)
        );

        /*
         * LEFT JOIN sonucu e.id NULL olduğu için
         * COUNT(e.id) değeri 0 olmalıdır.
         */
        assertEquals(
                0L,
                hr.getValue(1)
        );
    }

    @Test
    void globalJoinCountShouldWorkWithoutGroupBy() {

        SelectStatement statement =
                createStatement(
                        JoinType.INNER,
                        List.of(
                                new SelectItem(
                                        "COUNT(e.id)",
                                        "employee_count"
                                )
                        ),
                        null,
                        null,
                        null
                );

        QueryResult result =
                selectExecutor.executeStatement(
                        departmentTable,
                        departmentRows,
                        employeeTable,
                        employeeRows,
                        statement
                );

        assertEquals(
                1,
                result.getRows().size()
        );

        /*
         * Department ile eşleşen toplam
         * üç employee vardır.
         */
        assertEquals(
                3L,
                result.getRows()
                        .get(0)
                        .getValue(0)
        );
    }

    // ==================================================
    // STATEMENT
    // ==================================================

    /**
     * Testlerde kullanılan JOIN-aware SelectStatement
     * nesnesini oluşturur.
     */
    private SelectStatement createStatement(
            JoinType joinType,
            List<SelectItem> selectItems,
            com.yekdb.query.expression.Expression whereExpression,
            GroupByClause groupByClause,
            HavingClause havingClause
    ) {

        JoinClause joinClause =
                createEmployeeJoin(
                        joinType
                );

        return new SelectStatement(
                departmentReference,
                selectItems,
                List.of(
                        joinClause
                ),
                whereExpression,
                groupByClause,
                havingClause,
                List.of(),
                null,
                null
        );
    }

    /**
     * d.id = e.department_id
     */
    private JoinClause createEmployeeJoin(
            JoinType joinType
    ) {

        ComparisonExpression condition =
                new ComparisonExpression(
                        ColumnExpression.parse(
                                "d.id"
                        ),
                        ComparisonOperator.EQUALS,
                        ColumnExpression.parse(
                                "e.department_id"
                        )
                );

        return new JoinClause(
                joinType,
                "employee",
                "e",
                condition
        );
    }

    // ==================================================
    // TABLES
    // ==================================================

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
                        ),
                        new Column(
                                "salary",
                                DataType.INT
                        )
                )
        );
    }

    // ==================================================
    // ROWS
    // ==================================================

    private List<Row> createDepartmentRows() {

        return List.of(
                new Row(
                        List.of(
                                10,
                                "Software"
                        )
                ),
                new Row(
                        List.of(
                                20,
                                "Finance"
                        )
                ),
                new Row(
                        List.of(
                                30,
                                "Human Resources"
                        )
                )
        );
    }

    private List<Row> createEmployeeRows() {

        return List.of(
                new Row(
                        List.of(
                                1,
                                "Emre",
                                10,
                                40000
                        )
                ),
                new Row(
                        List.of(
                                2,
                                "Ayşe",
                                10,
                                50000
                        )
                ),
                new Row(
                        List.of(
                                3,
                                "Ali",
                                20,
                                30000
                        )
                ),
                new Row(
                        List.of(
                                4,
                                "Mert",
                                99,
                                60000
                        )
                )
        );
    }
}