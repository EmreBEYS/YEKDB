package com.yekdb.query.executor;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
import com.yekdb.query.statement.TableReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JoinExecutorTest {

    private JoinExecutor joinExecutor;

    @BeforeEach
    void setUp() {

        joinExecutor =
                new JoinExecutor();
    }

    // --------------------------------------------------
    // TEST 1
    // INNER JOIN MATCH
    // --------------------------------------------------

    @Test
    void shouldJoinMatchingRows() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        List<Map<String, Object>> employeeRows =
                List.of(
                        Map.of(
                                "id", 1,
                                "name", "Yunus",
                                "department_id", 10
                        ),

                        Map.of(
                                "id", 2,
                                "name", "Ali",
                                "department_id", 20
                        )
                );

        List<Map<String, Object>> departmentRows =
                List.of(
                        Map.of(
                                "id", 10,
                                "name", "IT"
                        ),

                        Map.of(
                                "id", 20,
                                "name", "HR"
                        )
                );

        JoinClause joinClause =
                new JoinClause(
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

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        employeeRows,
                        joinClause,
                        departmentRows
                );

        assertEquals(
                2,
                result.size()
        );
    }

    // --------------------------------------------------
    // TEST 2
    // NON-MATCHING ROWS
    // --------------------------------------------------

    @Test
    void shouldExcludeNonMatchingRows() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        List<Map<String, Object>> employeeRows =
                List.of(
                        Map.of(
                                "id", 1,
                                "department_id", 10
                        ),

                        Map.of(
                                "id", 2,
                                "department_id", 999
                        )
                );

        List<Map<String, Object>> departmentRows =
                List.of(
                        Map.of(
                                "id", 10,
                                "name", "IT"
                        )
                );

        JoinClause joinClause =
                new JoinClause(
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

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        employeeRows,
                        joinClause,
                        departmentRows
                );

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                1,
                result.get(0)
                        .get("e.id")
        );

        assertEquals(
                10,
                result.get(0)
                        .get("d.id")
        );
    }

    // --------------------------------------------------
    // TEST 3
    // ALIAS COLUMN RESOLUTION
    // --------------------------------------------------

    @Test
    void shouldResolveAliasQualifiedColumns() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        List<Map<String, Object>> employeeRows =
                List.of(
                        Map.of(
                                "id", 1,
                                "name", "Yunus",
                                "department_id", 10
                        )
                );

        List<Map<String, Object>> departmentRows =
                List.of(
                        Map.of(
                                "id", 10,
                                "name", "Engineering"
                        )
                );

        JoinClause joinClause =
                new JoinClause(
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

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        employeeRows,
                        joinClause,
                        departmentRows
                );

        assertEquals(
                1,
                result.size()
        );

        Map<String, Object> row =
                result.get(0);

        assertEquals(
                "Yunus",
                row.get("e.name")
        );

        assertEquals(
                "Engineering",
                row.get("d.name")
        );

        /*
         * Gerçek tablo isimleri de mevcut olmalı.
         */
        assertEquals(
                "Yunus",
                row.get("employee.name")
        );

        assertEquals(
                "Engineering",
                row.get("department.name")
        );
    }

    // --------------------------------------------------
    // TEST 4
    // MULTIPLE MATCHES
    // --------------------------------------------------

    @Test
    void shouldReturnMultipleMatchingRows() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        List<Map<String, Object>> employeeRows =
                List.of(
                        Map.of(
                                "id", 1,
                                "name", "Yunus",
                                "department_id", 10
                        ),

                        Map.of(
                                "id", 2,
                                "name", "Ali",
                                "department_id", 10
                        ),

                        Map.of(
                                "id", 3,
                                "name", "Ayse",
                                "department_id", 20
                        )
                );

        List<Map<String, Object>> departmentRows =
                List.of(
                        Map.of(
                                "id", 10,
                                "name", "IT"
                        ),

                        Map.of(
                                "id", 20,
                                "name", "HR"
                        )
                );

        JoinClause joinClause =
                new JoinClause(
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

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        employeeRows,
                        joinClause,
                        departmentRows
                );

        assertEquals(
                3,
                result.size()
        );
    }
    // --------------------------------------------------
// TEST 5
// EMPTY LEFT TABLE
// --------------------------------------------------

    @Test
    void shouldReturnEmptyResultWhenLeftRowsAreEmpty() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        List<Map<String, Object>> employeeRows =
                List.of();

        List<Map<String, Object>> departmentRows =
                List.of(
                        Map.of(
                                "id", 10,
                                "name", "IT"
                        )
                );

        JoinClause joinClause =
                createDepartmentJoinClause();

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        employeeRows,
                        joinClause,
                        departmentRows
                );

        assertTrue(
                result.isEmpty()
        );
    }


// --------------------------------------------------
// TEST 6
// EMPTY RIGHT TABLE
// --------------------------------------------------

    @Test
    void shouldReturnEmptyResultWhenRightRowsAreEmpty() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        List<Map<String, Object>> employeeRows =
                List.of(
                        Map.of(
                                "id", 1,
                                "department_id", 10
                        )
                );

        List<Map<String, Object>> departmentRows =
                List.of();

        JoinClause joinClause =
                createDepartmentJoinClause();

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        employeeRows,
                        joinClause,
                        departmentRows
                );

        assertTrue(
                result.isEmpty()
        );
    }


// --------------------------------------------------
// TEST 7
// NO MATCH
// --------------------------------------------------

    @Test
    void shouldReturnEmptyResultWhenNoRowsMatch() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        List<Map<String, Object>> employeeRows =
                List.of(
                        Map.of(
                                "id", 1,
                                "department_id", 999
                        )
                );

        List<Map<String, Object>> departmentRows =
                List.of(
                        Map.of(
                                "id", 10,
                                "name", "IT"
                        )
                );

        JoinClause joinClause =
                createDepartmentJoinClause();

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        employeeRows,
                        joinClause,
                        departmentRows
                );

        assertTrue(
                result.isEmpty()
        );
    }


// --------------------------------------------------
// TEST 8
// INTEGER VS LONG
// --------------------------------------------------

    @Test
    void shouldMatchDifferentNumericTypesWithSameValue() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        List<Map<String, Object>> employeeRows =
                List.of(
                        Map.of(
                                "id", 1,
                                "department_id", 10
                        )
                );

        List<Map<String, Object>> departmentRows =
                List.of(
                        Map.of(
                                "id", 10L,
                                "name", "IT"
                        )
                );

        JoinClause joinClause =
                createDepartmentJoinClause();

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        employeeRows,
                        joinClause,
                        departmentRows
                );

        assertEquals(
                1,
                result.size()
        );
    }


// --------------------------------------------------
// TEST 9
// NULL LEFT TABLE
// --------------------------------------------------

    @Test
    void shouldRejectNullLeftTable() {

        JoinClause joinClause =
                createDepartmentJoinClause();

        assertThrows(
                NullPointerException.class,
                () ->
                        joinExecutor.execute(
                                null,
                                List.of(),
                                joinClause,
                                List.of()
                        )
        );
    }


// --------------------------------------------------
// TEST 10
// NULL LEFT ROWS
// --------------------------------------------------

    @Test
    void shouldRejectNullLeftRows() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        JoinClause joinClause =
                createDepartmentJoinClause();

        assertThrows(
                NullPointerException.class,
                () ->
                        joinExecutor.execute(
                                employeeTable,
                                null,
                                joinClause,
                                List.of()
                        )
        );
    }


// --------------------------------------------------
// TEST 11
// NULL JOIN CLAUSE
// --------------------------------------------------

    @Test
    void shouldRejectNullJoinClause() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        assertThrows(
                NullPointerException.class,
                () ->
                        joinExecutor.execute(
                                employeeTable,
                                List.of(),
                                null,
                                List.of()
                        )
        );
    }


// --------------------------------------------------
// TEST 12
// NULL RIGHT ROWS
// --------------------------------------------------

    @Test
    void shouldRejectNullRightRows() {

        TableReference employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );

        JoinClause joinClause =
                createDepartmentJoinClause();

        assertThrows(
                NullPointerException.class,
                () ->
                        joinExecutor.execute(
                                employeeTable,
                                List.of(),
                                joinClause,
                                null
                        )
        );
    }


// --------------------------------------------------
// TEST HELPER
// --------------------------------------------------

    private JoinClause createDepartmentJoinClause() {

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
}