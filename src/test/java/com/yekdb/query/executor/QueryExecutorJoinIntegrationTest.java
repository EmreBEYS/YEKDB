package com.yekdb.query.executor;

import com.yekdb.database.DatabaseManager;
import com.yekdb.query.datasource.InMemoryQueryDataSource;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;
import com.yekdb.table.Table;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryExecutorJoinIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    private DatabaseManager databaseManager;
    private InMemoryQueryDataSource queryDataSource;

    @BeforeEach
    void setUp() {

        databaseManager =
                new DatabaseManager(
                        temporaryDirectory
                );

        queryDataSource =
                new InMemoryQueryDataSource();

        registerEmployeeTable();

        registerDepartmentTable();
    }

    // --------------------------------------------------
    // TEST 1
    // FULL SQL INNER JOIN
    // --------------------------------------------------

    @Test
    void shouldExecuteInnerJoinFromSqlText() {

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             queryDataSource
                     )) {

            ExecuteResult result =
                    queryExecutor.execute(
                            """
                            SELECT e.name, d.name
                            FROM employee e
                            INNER JOIN department d
                            ON e.department_id = d.id;
                            """
                    );

            assertTrue(
                    result.isSuccess()
            );

            assertTrue(
                    result.hasRows()
            );

            assertEquals(
                    3,
                    result.getRowCount()
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
    }

    // --------------------------------------------------
    // TEST 2
    // JOIN SHORTHAND
    // --------------------------------------------------

    @Test
    void shouldExecuteJoinShorthandFromSqlText() {

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             queryDataSource
                     )) {

            ExecuteResult result =
                    queryExecutor.execute(
                            """
                            SELECT e.name, d.name
                            FROM employee e
                            JOIN department d
                            ON e.department_id = d.id;
                            """
                    );

            assertTrue(
                    result.isSuccess()
            );

            assertEquals(
                    3,
                    result.getRowCount()
            );
        }
    }

    // --------------------------------------------------
    // TEST 3
    // JOIN + WHERE
    // --------------------------------------------------

    @Test
    void shouldExecuteJoinWithWhereFromSqlText() {

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             queryDataSource
                     )) {

            ExecuteResult result =
                    queryExecutor.execute(
                            """
                            SELECT e.name, d.name
                            FROM employee e
                            INNER JOIN department d
                            ON e.department_id = d.id
                            WHERE d.name = 'IT';
                            """
                    );

            assertTrue(
                    result.isSuccess()
            );

            assertEquals(
                    2,
                    result.getRowCount()
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
    }

    // --------------------------------------------------
    // TEST 4
    // JOIN + SELECT *
    // --------------------------------------------------

    @Test
    void shouldExecuteSelectAllWithJoin() {

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             queryDataSource
                     )) {

            ExecuteResult result =
                    queryExecutor.execute(
                            """
                            SELECT *
                            FROM employee e
                            INNER JOIN department d
                            ON e.department_id = d.id;
                            """
                    );

            assertTrue(
                    result.isSuccess()
            );

            assertEquals(
                    3,
                    result.getRowCount()
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
             * Toplam 5 değer.
             */
            assertEquals(
                    5,
                    result.getRows()
                            .get(0)
                            .size()
            );
        }
    }

    // --------------------------------------------------
    // TEST 5
    // NO MATCHING ROW
    // --------------------------------------------------

    @Test
    void shouldExcludeNonMatchingRowsInFullPipeline() {

        Table employeeTable =
                new Table(
                        "employee2",
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

        queryDataSource.register(
                employeeTable,
                rows
        );

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             queryDataSource
                     )) {

            ExecuteResult result =
                    queryExecutor.execute(
                            """
                            SELECT e.name, d.name
                            FROM employee2 e
                            INNER JOIN department d
                            ON e.department_id = d.id;
                            """
                    );

            assertEquals(
                    1,
                    result.getRowCount()
            );

            assertEquals(
                    "Yunus",
                    result.getRows()
                            .get(0)
                            .getValue(0)
            );
        }
    }

    // --------------------------------------------------
    // TEST 6
    // AMBIGUOUS COLUMN
    // --------------------------------------------------

    @Test
    void shouldRejectAmbiguousColumnInFullPipeline() {

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             queryDataSource
                     )) {

            QueryExecutionException exception =
                    assertThrows(
                            QueryExecutionException.class,
                            () ->
                                    queryExecutor.execute(
                                            """
                                            SELECT id
                                            FROM employee e
                                            INNER JOIN department d
                                            ON e.department_id = d.id;
                                            """
                                    )
                    );

            assertTrue(
                    exception.getMessage()
                            .contains(
                                    "Ambiguous column reference"
                            )
            );
        }
    }

    // --------------------------------------------------
    // TEST 7
    // JOIN TABLE NOT FOUND
    // --------------------------------------------------

    @Test
    void shouldFailWhenJoinTableDoesNotExist() {

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             queryDataSource
                     )) {

            assertThrows(
                    QueryExecutionException.class,
                    () ->
                            queryExecutor.execute(
                                    """
                                    SELECT e.name, d.name
                                    FROM employee e
                                    INNER JOIN missing_department d
                                    ON e.department_id = d.id;
                                    """
                            )
            );
        }
    }

    // --------------------------------------------------
    // TEST 8
    // NORMAL SELECT REGRESSION
    // --------------------------------------------------

    @Test
    void normalSelectShouldStillWorkWithoutJoin() {

        try (QueryExecutor queryExecutor =
                     new QueryExecutor(
                             databaseManager,
                             queryDataSource
                     )) {

            ExecuteResult result =
                    queryExecutor.execute(
                            """
                            SELECT *
                            FROM employee
                            WHERE department_id = 10;
                            """
                    );

            assertTrue(
                    result.isSuccess()
            );

            assertEquals(
                    2,
                    result.getRowCount()
            );

            assertEquals(
                    "Yunus",
                    result.getRows()
                            .get(0)
                            .getValue(1)
            );

            assertEquals(
                    "Ayse",
                    result.getRows()
                            .get(1)
                            .getValue(1)
            );
        }
    }

    // ==================================================
    // TEST DATA
    // ==================================================

    private void registerEmployeeTable() {

        Table employeeTable =
                new Table(
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

        List<Row> employeeRows =
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

        queryDataSource.register(
                employeeTable,
                employeeRows
        );
    }

    private void registerDepartmentTable() {

        Table departmentTable =
                new Table(
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

        List<Row> departmentRows =
                List.of(
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

        queryDataSource.register(
                departmentTable,
                departmentRows
        );
    }
}