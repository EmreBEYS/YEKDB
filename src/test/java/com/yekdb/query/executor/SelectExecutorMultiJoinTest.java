package com.yekdb.query.executor;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
import com.yekdb.query.statement.TableReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SelectExecutorMultiJoinTest {

    private MultiJoinExecutor multiJoinExecutor;

    private TableReference employeeReference;

    private List<Map<String, Object>> employeeRows;
    private List<Map<String, Object>> departmentRows;
    private List<Map<String, Object>> companyRows;

    private List<JoinClause> joinClauses;

    @BeforeEach
    void setUp() {

        multiJoinExecutor =
                new MultiJoinExecutor();

        employeeReference =
                new TableReference(
                        "employee",
                        "e"
                );

        employeeRows =
                List.of(
                        row(
                                "id", 1,
                                "name", "Emre",
                                "department_id", 10,
                                "salary", 40000
                        ),
                        row(
                                "id", 2,
                                "name", "Ayşe",
                                "department_id", 10,
                                "salary", 50000
                        ),
                        row(
                                "id", 3,
                                "name", "Ali",
                                "department_id", 20,
                                "salary", 30000
                        ),
                        row(
                                "id", 4,
                                "name", "Mert",
                                "department_id", 99,
                                "salary", 60000
                        )
                );

        departmentRows =
                List.of(
                        row(
                                "id", 10,
                                "name", "Software",
                                "company_id", 100
                        ),
                        row(
                                "id", 20,
                                "name", "Finance",
                                "company_id", 200
                        ),
                        row(
                                "id", 30,
                                "name", "Human Resources",
                                "company_id", 100
                        )
                );

        companyRows =
                List.of(
                        row(
                                "id", 100,
                                "name", "YEK Technology",
                                "active", true
                        ),
                        row(
                                "id", 200,
                                "name", "YEK Finance",
                                "active", false
                        )
                );

        JoinClause departmentJoin =
                new JoinClause(
                        JoinType.INNER,
                        "department",
                        "d",
                        new ComparisonExpression(
                                ColumnExpression.parse(
                                        "e.department_id"
                                ),
                                ComparisonOperator.EQUALS,
                                ColumnExpression.parse(
                                        "d.id"
                                )
                        )
                );

        JoinClause companyJoin =
                new JoinClause(
                        JoinType.INNER,
                        "company",
                        "c",
                        new ComparisonExpression(
                                ColumnExpression.parse(
                                        "d.company_id"
                                ),
                                ComparisonOperator.EQUALS,
                                ColumnExpression.parse(
                                        "c.id"
                                )
                        )
                );

        joinClauses =
                List.of(
                        departmentJoin,
                        companyJoin
                );
    }

    // ==================================================
    // TEST 1
    // ==================================================

    @Test
    void multipleJoinShouldReturnThreeRows() {

        List<Map<String, Object>> result =
                execute();

        /*
         * Mert department_id = 99 olduğu için
         * ilk INNER JOIN sırasında elenir.
         */
        assertEquals(
                3,
                result.size()
        );
    }

    // ==================================================
    // TEST 2
    // ==================================================

    @Test
    void multipleJoinShouldPreserveEmployeeColumns() {

        List<Map<String, Object>> result =
                execute();

        Map<String, Object> firstRow =
                result.get(0);

        assertEquals(
                1,
                firstRow.get("e.id")
        );

        assertEquals(
                "Emre",
                firstRow.get("e.name")
        );

        assertEquals(
                10,
                firstRow.get("e.department_id")
        );
    }

    // ==================================================
    // TEST 3
    // ==================================================

    @Test
    void multipleJoinShouldPreserveDepartmentColumns() {

        List<Map<String, Object>> result =
                execute();

        Map<String, Object> firstRow =
                result.get(0);

        assertEquals(
                10,
                firstRow.get("d.id")
        );

        assertEquals(
                "Software",
                firstRow.get("d.name")
        );

        assertEquals(
                100,
                firstRow.get("d.company_id")
        );
    }

    // ==================================================
    // TEST 4
    // ==================================================

    @Test
    void multipleJoinShouldPreserveCompanyColumns() {

        List<Map<String, Object>> result =
                execute();

        Map<String, Object> firstRow =
                result.get(0);

        assertEquals(
                100,
                firstRow.get("c.id")
        );

        assertEquals(
                "YEK Technology",
                firstRow.get("c.name")
        );

        assertEquals(
                true,
                firstRow.get("c.active")
        );
    }

    // ==================================================
    // TEST 5
    // ==================================================

    @Test
    void secondJoinShouldUseColumnProducedByFirstJoin() {

        List<Map<String, Object>> result =
                execute();

        Map<String, Object> firstRow =
                result.get(0);

        /*
         * İkinci JOIN:
         *
         * d.company_id = c.id
         *
         * Bu test, ilk JOIN sonucu oluşan
         * d.company_id kolonunun ikinci JOIN'e
         * aktarılabildiğini doğrular.
         */
        assertEquals(
                firstRow.get("d.company_id"),
                firstRow.get("c.id")
        );
    }

    // ==================================================
    // TEST 6
    // ==================================================

    @Test
    void multipleJoinShouldMatchFinanceEmployeeCorrectly() {

        List<Map<String, Object>> result =
                execute();

        Map<String, Object> aliRow =
                result.stream()
                        .filter(
                                row ->
                                        "Ali".equals(
                                                row.get("e.name")
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "Finance",
                aliRow.get("d.name")
        );

        assertEquals(
                "YEK Finance",
                aliRow.get("c.name")
        );

        assertEquals(
                false,
                aliRow.get("c.active")
        );
    }

    // ==================================================
    // TEST 7
    // ==================================================

    @Test
    void unmatchedEmployeeShouldBeRemovedByInnerJoin() {

        List<Map<String, Object>> result =
                execute();

        boolean containsMert =
                result.stream()
                        .anyMatch(
                                row ->
                                        "Mert".equals(
                                                row.get("e.name")
                                        )
                        );

        assertFalse(
                containsMert
        );
    }

    // ==================================================
    // TEST 8
    // ==================================================

    @Test
    void everyJoinedRowShouldContainAllThreeTableIds() {

        List<Map<String, Object>> result =
                execute();

        for (Map<String, Object> row : result) {

            assertTrue(
                    row.containsKey("e.id")
            );

            assertTrue(
                    row.containsKey("d.id")
            );

            assertTrue(
                    row.containsKey("c.id")
            );
        }
    }

    // ==================================================
    // EXECUTION
    // ==================================================

    private List<Map<String, Object>> execute() {

        List<List<Map<String, Object>>> rightTableRows =
                List.of(
                        departmentRows,
                        companyRows
                );

        return multiJoinExecutor.execute(
                employeeReference,
                employeeRows,
                joinClauses,
                rightTableRows
        );
    }

    // ==================================================
    // TEST DATA HELPER
    // ==================================================

    private Map<String, Object> row(
            Object... values
    ) {

        if (values.length % 2 != 0) {

            throw new IllegalArgumentException(
                    "Row values must contain key-value pairs."
            );
        }

        Map<String, Object> row =
                new LinkedHashMap<>();

        for (int index = 0;
             index < values.length;
             index += 2) {

            row.put(
                    (String) values[index],
                    values[index + 1]
            );
        }

        return row;
    }
}