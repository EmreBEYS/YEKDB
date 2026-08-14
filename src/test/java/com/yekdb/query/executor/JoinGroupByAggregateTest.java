package com.yekdb.query.executor;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
import com.yekdb.query.statement.TableReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JoinGroupByAggregateTest {

    private JoinExecutor joinExecutor;
    private GroupByExecutor groupByExecutor;
    private AggregateExecutor aggregateExecutor;

    private TableReference departmentTable;

    @BeforeEach
    void setUp() {

        joinExecutor =
                new JoinExecutor();

        groupByExecutor =
                new GroupByExecutor();

        aggregateExecutor =
                new AggregateExecutor();

        departmentTable =
                new TableReference(
                        "department",
                        "d"
                );
    }

    @Test
    void shouldGroupJoinedRowsByQualifiedColumn() {

        List<Map<String, Object>> joinedRows =
                createLeftJoinResult();

        GroupByClause groupByClause =
                new GroupByClause(
                        List.of(
                                "d.name"
                        )
                );

        Map<List<Object>, List<Map<String, Object>>> groups =
                groupByExecutor.executeJoinedRows(
                        joinedRows,
                        groupByClause
                );

        /*
         * Software
         * Finance
         * Human Resources
         *
         * olmak üzere üç grup beklenir.
         */
        assertEquals(
                3,
                groups.size()
        );

        assertTrue(
                groups.containsKey(
                        List.of("Software")
                )
        );

        assertTrue(
                groups.containsKey(
                        List.of("Finance")
                )
        );

        assertTrue(
                groups.containsKey(
                        List.of("Human Resources")
                )
        );
    }

    @Test
    void countStarShouldCountAllRowsInGroup() {

        Map<List<Object>, List<Map<String, Object>>> groups =
                createGroups();

        List<Map<String, Object>> softwareGroup =
                groups.get(
                        List.of("Software")
                );

        Object result =
                aggregateExecutor.executeJoinedRows(
                        softwareGroup,
                        AggregateExecutor.AggregateFunction.COUNT,
                        "*"
                );

        /*
         * Software departmanında iki employee vardır.
         */
        assertEquals(
                2L,
                result
        );
    }

    @Test
    void countColumnShouldIgnoreNullValues() {

        Map<List<Object>, List<Map<String, Object>>> groups =
                createGroups();

        List<Map<String, Object>> hrGroup =
                groups.get(
                        List.of("Human Resources")
                );

        Object result =
                aggregateExecutor.executeJoinedRows(
                        hrGroup,
                        AggregateExecutor.AggregateFunction.COUNT,
                        "e.id"
                );

        /*
         * Human Resources departmanında employee yoktur.
         *
         * LEFT JOIN sonucu e.id = NULL olduğu için
         * COUNT(e.id) değeri 0 olmalıdır.
         */
        assertEquals(
                0L,
                result
        );
    }

    @Test
    void sumShouldCalculateJoinedNumericColumn() {

        Map<List<Object>, List<Map<String, Object>>> groups =
                createGroups();

        List<Map<String, Object>> softwareGroup =
                groups.get(
                        List.of("Software")
                );

        Object result =
                aggregateExecutor.executeJoinedRows(
                        softwareGroup,
                        AggregateExecutor.AggregateFunction.SUM,
                        "e.salary"
                );

        assertEquals(
                90000.0,
                (Double) result,
                0.001
        );
    }

    @Test
    void averageShouldCalculateJoinedNumericColumn() {

        Map<List<Object>, List<Map<String, Object>>> groups =
                createGroups();

        List<Map<String, Object>> softwareGroup =
                groups.get(
                        List.of("Software")
                );

        Object result =
                aggregateExecutor.executeJoinedRows(
                        softwareGroup,
                        AggregateExecutor.AggregateFunction.AVG,
                        "e.salary"
                );

        assertEquals(
                45000.0,
                (Double) result,
                0.001
        );
    }

    @Test
    void minShouldReturnMinimumJoinedValue() {

        Map<List<Object>, List<Map<String, Object>>> groups =
                createGroups();

        List<Map<String, Object>> softwareGroup =
                groups.get(
                        List.of("Software")
                );

        Object result =
                aggregateExecutor.executeJoinedRows(
                        softwareGroup,
                        AggregateExecutor.AggregateFunction.MIN,
                        "e.salary"
                );

        assertEquals(
                40000,
                result
        );
    }

    @Test
    void maxShouldReturnMaximumJoinedValue() {

        Map<List<Object>, List<Map<String, Object>>> groups =
                createGroups();

        List<Map<String, Object>> softwareGroup =
                groups.get(
                        List.of("Software")
                );

        Object result =
                aggregateExecutor.executeJoinedRows(
                        softwareGroup,
                        AggregateExecutor.AggregateFunction.MAX,
                        "e.salary"
                );

        assertEquals(
                50000,
                result
        );
    }

    @Test
    void aggregateShouldIgnoreNullValuesProducedByLeftJoin() {

        Map<List<Object>, List<Map<String, Object>>> groups =
                createGroups();

        List<Map<String, Object>> hrGroup =
                groups.get(
                        List.of("Human Resources")
                );

        Object sum =
                aggregateExecutor.executeJoinedRows(
                        hrGroup,
                        AggregateExecutor.AggregateFunction.SUM,
                        "e.salary"
                );

        Object average =
                aggregateExecutor.executeJoinedRows(
                        hrGroup,
                        AggregateExecutor.AggregateFunction.AVG,
                        "e.salary"
                );

        Object minimum =
                aggregateExecutor.executeJoinedRows(
                        hrGroup,
                        AggregateExecutor.AggregateFunction.MIN,
                        "e.salary"
                );

        Object maximum =
                aggregateExecutor.executeJoinedRows(
                        hrGroup,
                        AggregateExecutor.AggregateFunction.MAX,
                        "e.salary"
                );

        /*
         * Employee bulunmayan grupta yalnızca
         * outer JOIN NULL değerleri vardır.
         */
        assertEquals(
                0.0,
                (Double) sum,
                0.001
        );

        assertEquals(
                0.0,
                (Double) average,
                0.001
        );

        assertNull(minimum);
        assertNull(maximum);
    }

    // --------------------------------------------------
    // TEST YARDIMCILARI
    // --------------------------------------------------

    private Map<List<Object>, List<Map<String, Object>>> createGroups() {

        GroupByClause groupByClause =
                new GroupByClause(
                        List.of(
                                "d.name"
                        )
                );

        return groupByExecutor.executeJoinedRows(
                createLeftJoinResult(),
                groupByClause
        );
    }

    /**
     * department LEFT JOIN employee sonucu oluşturur.
     *
     * Böylece employee bulunmayan Human Resources
     * departmanında e.* kolonları NULL olur.
     */
    private List<Map<String, Object>> createLeftJoinResult() {

        JoinClause joinClause =
                createEmployeeJoin();

        return joinExecutor.execute(
                departmentTable,
                createDepartmentRows(),
                joinClause,
                createEmployeeRows()
        );
    }

    /**
     * d.id = e.department_id
     */
    private JoinClause createEmployeeJoin() {

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
                JoinType.LEFT,
                "employee",
                "e",
                condition
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
                ),
                Map.of(
                        "id", 30,
                        "name", "Human Resources"
                )
        );
    }

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
                        "department_id", 20,
                        "salary", 30000
                )
        );
    }
}