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

class MultiJoinExecutorTest {

    private MultiJoinExecutor multiJoinExecutor;
    private TableReference employeeTable;

    @BeforeEach
    void setUp() {

        multiJoinExecutor =
                new MultiJoinExecutor();

        employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );
    }

    @Test
    void shouldJoinThreeTables() {

        List<Map<String, Object>> result =
                multiJoinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createJoinClauses(),
                        createRightTableRows()
                );

        /*
         * Employee -> Department -> Company zincirinde
         * iki employee satırı da tam olarak eşleşmektedir.
         */
        assertEquals(
                2,
                result.size()
        );

        Map<String, Object> firstRow =
                result.get(0);

        assertEquals(
                "Emre",
                firstRow.get("e.name")
        );

        assertEquals(
                "Software",
                firstRow.get("d.name")
        );

        assertEquals(
                "YEK Technology",
                firstRow.get("c.name")
        );
    }

    @Test
    void shouldPreservePreviousQualifiedColumns() {

        List<Map<String, Object>> result =
                multiJoinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createJoinClauses(),
                        createRightTableRows()
                );

        Map<String, Object> row =
                result.get(0);

        /*
         * İkinci JOIN çalıştıktan sonra ilk JOIN'den
         * gelen qualifier bilgileri kaybolmamalıdır.
         */
        assertTrue(
                row.containsKey("e.id")
        );

        assertTrue(
                row.containsKey("e.name")
        );

        assertTrue(
                row.containsKey("d.id")
        );

        assertTrue(
                row.containsKey("d.name")
        );

        assertTrue(
                row.containsKey("d.company_id")
        );

        assertTrue(
                row.containsKey("c.id")
        );

        assertTrue(
                row.containsKey("c.name")
        );
    }

    @Test
    void secondJoinShouldUsePreviousJoinColumn() {

        List<Map<String, Object>> result =
                multiJoinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createJoinClauses(),
                        createRightTableRows()
                );

        /*
         * İkinci JOIN koşulu:
         *
         * d.company_id = c.id
         *
         * İlk JOIN sonucundaki d.company_id kolonunun
         * ikinci JOIN sırasında bulunabilmesi gerekir.
         */
        Map<String, Object> softwareEmployee =
                result.stream()
                        .filter(
                                row ->
                                        "Emre".equals(
                                                row.get("e.name")
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                100,
                softwareEmployee.get(
                        "d.company_id"
                )
        );

        assertEquals(
                100,
                softwareEmployee.get(
                        "c.id"
                )
        );

        assertEquals(
                "YEK Technology",
                softwareEmployee.get(
                        "c.name"
                )
        );
    }

    @Test
    void differentCompaniesShouldResolveCorrectly() {

        List<Map<String, Object>> result =
                multiJoinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        createJoinClauses(),
                        createRightTableRows()
                );

        Map<String, Object> ayseRow =
                result.stream()
                        .filter(
                                row ->
                                        "Ayşe".equals(
                                                row.get("e.name")
                                        )
                        )
                        .findFirst()
                        .orElseThrow();

        /*
         * Finance departmanı company_id 200'e bağlıdır.
         */
        assertEquals(
                "Finance",
                ayseRow.get("d.name")
        );

        assertEquals(
                200,
                ayseRow.get("d.company_id")
        );

        assertEquals(
                "YEK Finance",
                ayseRow.get("c.name")
        );
    }

    @Test
    void mismatchedJoinAndRightTableCountsShouldThrow() {

        List<JoinClause> joins =
                createJoinClauses();

        List<List<Map<String, Object>>> invalidRightRows =
                List.of(
                        createDepartmentRows()
                );

        /*
         * İki JOIN clause olduğu halde yalnızca
         * bir sağ tablo veri kümesi verilmiştir.
         */
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        multiJoinExecutor.execute(
                                employeeTable,
                                createEmployeeRows(),
                                joins,
                                invalidRightRows
                        )
        );
    }

    // --------------------------------------------------
    // JOIN CLAUSE
    // --------------------------------------------------

    /**
     * Multiple JOIN zincirini oluşturur.
     *
     * employee e
     *
     * INNER JOIN department d
     * ON e.department_id = d.id
     *
     * INNER JOIN company c
     * ON d.company_id = c.id
     */
    private List<JoinClause> createJoinClauses() {

        ComparisonExpression employeeDepartmentCondition =
                new ComparisonExpression(
                        ColumnExpression.parse(
                                "e.department_id"
                        ),
                        ComparisonOperator.EQUALS,
                        ColumnExpression.parse(
                                "d.id"
                        )
                );

        ComparisonExpression departmentCompanyCondition =
                new ComparisonExpression(
                        ColumnExpression.parse(
                                "d.company_id"
                        ),
                        ComparisonOperator.EQUALS,
                        ColumnExpression.parse(
                                "c.id"
                        )
                );

        JoinClause departmentJoin =
                new JoinClause(
                        JoinType.INNER,
                        "department",
                        "d",
                        employeeDepartmentCondition
                );

        JoinClause companyJoin =
                new JoinClause(
                        JoinType.INNER,
                        "company",
                        "c",
                        departmentCompanyCondition
                );

        return List.of(
                departmentJoin,
                companyJoin
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
                        "department_id", 10
                ),
                Map.of(
                        "id", 2,
                        "name", "Ayşe",
                        "department_id", 20
                )
        );
    }

    private List<Map<String, Object>> createDepartmentRows() {

        return List.of(
                Map.of(
                        "id", 10,
                        "name", "Software",
                        "company_id", 100
                ),
                Map.of(
                        "id", 20,
                        "name", "Finance",
                        "company_id", 200
                )
        );
    }

    private List<Map<String, Object>> createCompanyRows() {

        return List.of(
                Map.of(
                        "id", 100,
                        "name", "YEK Technology"
                ),
                Map.of(
                        "id", 200,
                        "name", "YEK Finance"
                )
        );
    }

    private List<List<Map<String, Object>>> createRightTableRows() {

        return List.of(
                createDepartmentRows(),
                createCompanyRows()
        );
    }
}