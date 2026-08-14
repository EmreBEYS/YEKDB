package com.yekdb.query.executor;

import com.yekdb.query.evaluator.ExpressionEvaluator;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
import com.yekdb.query.statement.TableReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JoinWhereRegressionTest {

    private JoinExecutor joinExecutor;
    private ExpressionEvaluator expressionEvaluator;
    private TableReference employeeTable;

    @BeforeEach
    void setUp() {

        joinExecutor =
                new JoinExecutor();

        expressionEvaluator =
                new ExpressionEvaluator();

        employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );
    }

    @Test
    void whereShouldFilterInnerJoinUsingQualifiedColumn() {

        List<Map<String, Object>> joinedRows =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createDepartmentJoin(
                                JoinType.INNER
                        ),
                        createDepartmentRows()
                );

        ComparisonExpression where =
                new ComparisonExpression(
                        "d.name",
                        ComparisonOperator.EQUALS,
                        "Software"
                );

        List<Map<String, Object>> result =
                applyWhere(
                        joinedRows,
                        where
                );

        assertEquals(
                2,
                result.size()
        );

        assertTrue(
                result.stream()
                        .allMatch(
                                row ->
                                        "Software".equals(
                                                row.get("d.name")
                                        )
                        )
        );
    }

    @Test
    void whereShouldFilterUsingLeftTableColumn() {

        List<Map<String, Object>> joinedRows =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createDepartmentJoin(
                                JoinType.INNER
                        ),
                        createDepartmentRows()
                );

        ComparisonExpression where =
                new ComparisonExpression(
                        "e.salary",
                        ComparisonOperator.GREATER_THAN,
                        40000
                );

        List<Map<String, Object>> result =
                applyWhere(
                        joinedRows,
                        where
                );

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "Ayşe",
                result.get(0).get("e.name")
        );
    }

    @Test
    void whereShouldWorkAfterLeftJoin() {

        List<Map<String, Object>> joinedRows =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createDepartmentJoin(
                                JoinType.LEFT
                        ),
                        createDepartmentRows()
                );

        ComparisonExpression where =
                new ComparisonExpression(
                        "d.name",
                        ComparisonOperator.EQUALS,
                        "Software"
                );

        List<Map<String, Object>> result =
                applyWhere(
                        joinedRows,
                        where
                );

        /*
         * LEFT JOIN eşleşmeyen satırı üretmiş olsa bile
         * WHERE d.name = 'Software' yalnızca eşleşen
         * Software satırlarını bırakmalıdır.
         */
        assertEquals(
                2,
                result.size()
        );
    }

    @Test
    void whereShouldWorkAfterRightJoin() {

        List<Map<String, Object>> departments =
                List.of(
                        Map.of(
                                "id", 10,
                                "name", "Software"
                        ),
                        Map.of(
                                "id", 20,
                                "name", "Finance"
                        ),
                        Map.of(
                                "id", 30,
                                "name", "Human Resources"
                        )
                );

        List<Map<String, Object>> joinedRows =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createDepartmentJoin(
                                JoinType.RIGHT
                        ),
                        departments
                );

        ComparisonExpression where =
                new ComparisonExpression(
                        "d.name",
                        ComparisonOperator.EQUALS,
                        "Human Resources"
                );

        List<Map<String, Object>> result =
                applyWhere(
                        joinedRows,
                        where
                );

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                30,
                result.get(0).get("d.id")
        );

        assertNull(
                result.get(0).get("e.id")
        );
    }

    @Test
    void whereShouldWorkAfterFullJoin() {

        List<Map<String, Object>> departments =
                List.of(
                        Map.of(
                                "id", 10,
                                "name", "Software"
                        ),
                        Map.of(
                                "id", 20,
                                "name", "Finance"
                        ),
                        Map.of(
                                "id", 30,
                                "name", "Human Resources"
                        )
                );

        List<Map<String, Object>> joinedRows =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createDepartmentJoin(
                                JoinType.FULL
                        ),
                        departments
                );

        ComparisonExpression where =
                new ComparisonExpression(
                        "e.name",
                        ComparisonOperator.EQUALS,
                        "Ali"
                );

        List<Map<String, Object>> result =
                applyWhere(
                        joinedRows,
                        where
                );

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "Ali",
                result.get(0).get("e.name")
        );

        assertNull(
                result.get(0).get("d.id")
        );
    }

    @Test
    void whereShouldSupportAndExpressionAfterJoin() {

        List<Map<String, Object>> joinedRows =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createDepartmentJoin(
                                JoinType.INNER
                        ),
                        createDepartmentRows()
                );

        ComparisonExpression salaryCondition =
                new ComparisonExpression(
                        "e.salary",
                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                        40000
                );

        ComparisonExpression departmentCondition =
                new ComparisonExpression(
                        "d.name",
                        ComparisonOperator.EQUALS,
                        "Software"
                );

        LogicalExpression where =
                new LogicalExpression(
                        salaryCondition,
                        LogicalOperator.AND,
                        departmentCondition
                );

        List<Map<String, Object>> result =
                applyWhere(
                        joinedRows,
                        where
                );

        assertEquals(
                2,
                result.size()
        );
    }

    @Test
    void nullWhereShouldPreserveJoinedRows() {

        List<Map<String, Object>> joinedRows =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createDepartmentJoin(
                                JoinType.INNER
                        ),
                        createDepartmentRows()
                );

        List<Map<String, Object>> result =
                applyWhere(
                        joinedRows,
                        null
                );

        assertEquals(
                joinedRows.size(),
                result.size()
        );
    }

    // --------------------------------------------------
    // WHERE
    // --------------------------------------------------

    /**
     * JOIN sonucu oluşan satırlara WHERE filtresi uygular.
     */
    private List<Map<String, Object>> applyWhere(
            List<Map<String, Object>> rows,
            com.yekdb.query.expression.Expression expression
    ) {

        if (expression == null) {
            return new ArrayList<>(rows);
        }

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (Map<String, Object> row : rows) {

            if (expressionEvaluator.evaluate(
                    expression,
                    row
            )) {

                result.add(row);
            }
        }

        return result;
    }

    // --------------------------------------------------
    // JOIN
    // --------------------------------------------------

    /**
     * e.department_id = d.id
     */
    private JoinClause createDepartmentJoin(
            JoinType joinType
    ) {

        return new JoinClause(
                joinType,
                "department",
                "d",
                new ComparisonExpression(
                        com.yekdb.query.expression.ColumnExpression.parse(
                                "e.department_id"
                        ),
                        ComparisonOperator.EQUALS,
                        com.yekdb.query.expression.ColumnExpression.parse(
                                "d.id"
                        )
                )
        );
    }

    // --------------------------------------------------
    // TEST VERİLERİ
    // --------------------------------------------------

    private List<Map<String, Object>> createEmployeeRows() {

        return List.of(
                Map.of(
                        "id", 1,
                        "name", "Emre",
                        "department_id", 10,
                        "salary", 40000
                ),
                Map.of(
                        "id", 2,
                        "name", "Ayşe",
                        "department_id", 10,
                        "salary", 50000
                ),
                Map.of(
                        "id", 3,
                        "name", "Ali",
                        "department_id", 99,
                        "salary", 30000
                )
        );
    }

    private List<Map<String, Object>> createDepartmentRows() {

        return List.of(
                Map.of(
                        "id", 10,
                        "name", "Software"
                ),
                Map.of(
                        "id", 20,
                        "name", "Finance"
                )
        );
    }
    @Test
    void nullValueShouldNotMatchGreaterThanAfterRightJoin() {

        List<Map<String, Object>> departments =
                List.of(
                        Map.of(
                                "id", 10,
                                "name", "Software"
                        ),
                        Map.of(
                                "id", 30,
                                "name", "Human Resources"
                        )
                );

        List<Map<String, Object>> joinedRows =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createDepartmentJoin(
                                JoinType.RIGHT
                        ),
                        departments
                );

        ComparisonExpression where =
                new ComparisonExpression(
                        "e.salary",
                        ComparisonOperator.GREATER_THAN,
                        35000
                );

        List<Map<String, Object>> result =
                applyWhere(
                        joinedRows,
                        where
                );

        /*
         * Human Resources satırında e.salary NULL'dır.
         *
         * NULL > 35000 false olmalı ve sorgu
         * exception üretmeden devam etmelidir.
         */
        assertEquals(
                2,
                result.size()
        );

        assertTrue(
                result.stream()
                        .noneMatch(
                                row ->
                                        Integer.valueOf(30)
                                                .equals(
                                                        row.get("d.id")
                                                )
                        )
        );
    }
}