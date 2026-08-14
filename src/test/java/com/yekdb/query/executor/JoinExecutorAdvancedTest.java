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

class JoinExecutorAdvancedTest {

    private JoinExecutor joinExecutor;
    private TableReference employeeTable;

    @BeforeEach
    void setUp() {

        joinExecutor =
                new JoinExecutor();

        employeeTable =
                new TableReference(
                        "employee",
                        "e"
                );
    }

    // --------------------------------------------------
    // INNER JOIN
    // --------------------------------------------------

    @Test
    void innerJoinShouldReturnOnlyMatchingRows() {

        List<Map<String, Object>> employeeRows =
                createEmployeeRows();

        List<Map<String, Object>> departmentRows =
                createDepartmentRows();

        JoinClause joinClause =
                createDepartmentJoin(
                        JoinType.INNER
                );

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        employeeRows,
                        joinClause,
                        departmentRows
                );

        /*
         * employee:
         *
         * Emre -> department 10
         * Ayşe -> department 20
         * Ali  -> department 99
         *
         * department tablosunda 10 ve 20 bulunduğu için
         * INNER JOIN yalnızca iki satır döndürmelidir.
         */
        assertEquals(
                2,
                result.size()
        );

        assertEquals(
                "Emre",
                result.get(0).get("e.name")
        );

        assertEquals(
                "Software",
                result.get(0).get("d.name")
        );

        assertEquals(
                "Ayşe",
                result.get(1).get("e.name")
        );

        assertEquals(
                "Finance",
                result.get(1).get("d.name")
        );
    }

    // --------------------------------------------------
    // LEFT JOIN
    // --------------------------------------------------

    @Test
    void leftJoinShouldPreserveUnmatchedLeftRows() {

        JoinClause joinClause =
                createDepartmentJoin(
                        JoinType.LEFT
                );

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        joinClause,
                        createDepartmentRows()
                );

        /*
         * LEFT JOIN bütün employee satırlarını
         * korumalıdır.
         */
        assertEquals(
                3,
                result.size()
        );

        assertTrue(
                result.stream()
                        .anyMatch(
                                row ->
                                        "Ali".equals(
                                                row.get("e.name")
                                        )
                        )
        );
    }

    @Test
    void leftJoinShouldProduceNullRightColumns() {

        JoinClause joinClause =
                createDepartmentJoin(
                        JoinType.LEFT
                );

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        joinClause,
                        createDepartmentRows()
                );

        /*
         * Ali'nin department_id değeri 99'dur.
         * Sağ tabloda 99 bulunmadığı için department
         * kolonlarının NULL olması gerekir.
         */
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

        assertTrue(
                aliRow.containsKey("d.id")
        );

        assertTrue(
                aliRow.containsKey("d.name")
        );

        assertNull(
                aliRow.get("d.id")
        );

        assertNull(
                aliRow.get("d.name")
        );
    }

    // --------------------------------------------------
    // RIGHT JOIN
    // --------------------------------------------------

    @Test
    void rightJoinShouldPreserveUnmatchedRightRows() {

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

        JoinClause joinClause =
                createDepartmentJoin(
                        JoinType.RIGHT
                );

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        joinClause,
                        departments
                );

        /*
         * Department 30 hiçbir employee ile eşleşmese bile
         * RIGHT JOIN nedeniyle sonuçta bulunmalıdır.
         */
        Map<String, Object> department30 =
                result.stream()
                        .filter(
                                row ->
                                        Integer.valueOf(30)
                                                .equals(
                                                        row.get("d.id")
                                                )
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "Human Resources",
                department30.get("d.name")
        );

        assertNull(
                department30.get("e.id")
        );

        assertNull(
                department30.get("e.name")
        );
    }

    // --------------------------------------------------
    // FULL JOIN
    // --------------------------------------------------

    @Test
    void fullJoinShouldPreserveBothSides() {

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

        JoinClause joinClause =
                createDepartmentJoin(
                        JoinType.FULL
                );

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        joinClause,
                        departments
                );

        /*
         * Eşleşenler:
         *
         * Emre -> Software
         * Ayşe -> Finance
         *
         * Sol eşleşmeyen:
         *
         * Ali -> NULL
         *
         * Sağ eşleşmeyen:
         *
         * NULL -> Human Resources
         *
         * Toplam 4 satır beklenir.
         */
        assertEquals(
                4,
                result.size()
        );

        assertTrue(
                result.stream()
                        .anyMatch(
                                row ->
                                        "Ali".equals(
                                                row.get("e.name")
                                        )
                                                && row.get("d.id") == null
                        )
        );

        assertTrue(
                result.stream()
                        .anyMatch(
                                row ->
                                        Integer.valueOf(30)
                                                .equals(
                                                        row.get("d.id")
                                                )
                                                && row.get("e.id") == null
                        )
        );
    }

    @Test
    void fullJoinShouldNotDuplicateMatchingRows() {

        List<Map<String, Object>> employees =
                List.of(
                        Map.of(
                                "id", 1,
                                "name", "Emre",
                                "department_id", 10
                        )
                );

        List<Map<String, Object>> departments =
                List.of(
                        Map.of(
                                "id", 10,
                                "name", "Software"
                        )
                );

        JoinClause joinClause =
                createDepartmentJoin(
                        JoinType.FULL
                );

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        employees,
                        joinClause,
                        departments
                );

        /*
         * Tek employee ve tek department birbirleriyle
         * eşleştiği için sonuç yalnızca bir satır olmalıdır.
         */
        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "Emre",
                result.get(0).get("e.name")
        );

        assertEquals(
                "Software",
                result.get(0).get("d.name")
        );
    }

    @Test
    void joinedRowsShouldContainTableNameAndAlias() {

        JoinClause joinClause =
                createDepartmentJoin(
                        JoinType.INNER
                );

        List<Map<String, Object>> result =
                joinExecutor.execute(
                        employeeTable,
                        createEmployeeRows(),
                        joinClause,
                        createDepartmentRows()
                );

        Map<String, Object> row =
                result.get(0);

        /*
         * Hem gerçek tablo adı hem alias üzerinden
         * qualified erişim korunmalıdır.
         */
        assertEquals(
                1,
                row.get("employee.id")
        );

        assertEquals(
                1,
                row.get("e.id")
        );

        assertEquals(
                10,
                row.get("department.id")
        );

        assertEquals(
                10,
                row.get("d.id")
        );
    }

    // --------------------------------------------------
    // TEST VERİLERİ
    // --------------------------------------------------

    /**
     * Testlerde kullanılan employee verilerini üretir.
     */
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
                ),
                Map.of(
                        "id", 3,
                        "name", "Ali",
                        "department_id", 99
                )
        );
    }

    /**
     * Testlerde kullanılan department verilerini üretir.
     */
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

    /**
     * employee ile department arasında kullanılacak
     * JOIN koşulunu oluşturur.
     *
     * e.department_id = d.id
     */
    private JoinClause createDepartmentJoin(
            JoinType joinType
    ) {

        ComparisonExpression condition =
                new ComparisonExpression(
                        ColumnExpression.parse(
                                "e.department_id"
                        ),
                        ComparisonOperator.EQUALS,
                        ColumnExpression.parse(
                                "d.id"
                        )
                );

        return new JoinClause(
                joinType,
                "department",
                "d",
                condition
        );
    }
}