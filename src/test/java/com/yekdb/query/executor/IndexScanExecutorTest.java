package com.yekdb.query.executor;

import com.yekdb.index.Index;
import com.yekdb.index.IndexMetadata;
import com.yekdb.index.IndexType;
import com.yekdb.index.RecordPointer;
import com.yekdb.query.expression.BetweenExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.optimizer.QueryOptimizer;
import com.yekdb.query.optimizer.QueryPlan;
import com.yekdb.query.optimizer.QueryPlanType;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 00-29 Phase 13 INDEX_SCAN ve optimizer
 * entegrasyon testleri.
 */
class IndexScanExecutorTest {

    private Table usersTable;

    private List<Row> rows;

    private Map<RecordPointer, Row> rowStore;

    private Index<Integer> idIndex;

    @BeforeEach
    void setUp() {

        usersTable =
                new Table(
                        "users",
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
                                        "age",
                                        DataType.INT
                                )
                        )
                );

        rows =
                List.of(
                        new Row(
                                List.of(
                                        10,
                                        "Ali",
                                        18
                                )
                        ),
                        new Row(
                                List.of(
                                        20,
                                        "Ayse",
                                        22
                                )
                        ),
                        new Row(
                                List.of(
                                        30,
                                        "Mehmet",
                                        27
                                )
                        ),
                        new Row(
                                List.of(
                                        40,
                                        "Emre",
                                        31
                                )
                        ),
                        new Row(
                                List.of(
                                        50,
                                        "Zeynep",
                                        36
                                )
                        )
                );

        idIndex =
                new Index<>(
                        new IndexMetadata(
                                1L,
                                "idx_users_id",
                                "test_db",
                                "users",
                                "id",
                                IndexType.UNIQUE
                        )
                );

        rowStore =
                new HashMap<>();

        for (int i = 0;
             i < rows.size();
             i++) {

            Row row =
                    rows.get(i);

            int id =
                    (Integer) row.getValue(0);

            RecordPointer pointer =
                    new RecordPointer(
                            i,
                            0
                    );

            idIndex.insert(
                    id,
                    pointer
            );

            rowStore.put(
                    pointer,
                    row
            );
        }
    }

    @Test
    void shouldCreateIndexScanPlanForEqualityPredicate() {

        QueryOptimizer optimizer =
                new QueryOptimizer();

        QueryPlan plan =
                optimizer.optimize(
                        usersTable,
                        new ComparisonExpression(
                                "id",
                                ComparisonOperator.EQUALS,
                                20
                        ),
                        List.of(idIndex)
                );

        assertEquals(
                QueryPlanType.INDEX_SCAN,
                plan.getPlanType()
        );

        assertTrue(
                plan.usesIndex()
        );

        assertEquals(
                "idx_users_id",
                plan.getIndexName()
                        .orElseThrow()
        );
    }

    @Test
    void shouldCreateIndexScanPlanForGreaterThanPredicate() {

        QueryPlan plan =
                new QueryOptimizer()
                        .optimize(
                                usersTable,
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.GREATER_THAN,
                                        20
                                ),
                                List.of(idIndex)
                        );

        assertEquals(
                QueryPlanType.INDEX_SCAN,
                plan.getPlanType()
        );
    }

    @Test
    void shouldCreateIndexScanPlanForBetweenPredicate() {

        QueryPlan plan =
                new QueryOptimizer()
                        .optimize(
                                usersTable,
                                new BetweenExpression(
                                        "id",
                                        20,
                                        40
                                ),
                                List.of(idIndex)
                        );

        assertEquals(
                QueryPlanType.INDEX_SCAN,
                plan.getPlanType()
        );
    }

    @Test
    void shouldFallbackWhenIndexDoesNotExist() {

        QueryPlan plan =
                new QueryOptimizer()
                        .optimize(
                                usersTable,
                                new ComparisonExpression(
                                        "age",
                                        ComparisonOperator.EQUALS,
                                        22
                                ),
                                List.of(idIndex)
                        );

        assertEquals(
                QueryPlanType.FULL_TABLE_SCAN,
                plan.getPlanType()
        );
    }

    @Test
    void shouldFallbackForNotEqualsPredicate() {

        QueryPlan plan =
                new QueryOptimizer()
                        .optimize(
                                usersTable,
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.NOT_EQUALS,
                                        20
                                ),
                                List.of(idIndex)
                        );

        assertEquals(
                QueryPlanType.FULL_TABLE_SCAN,
                plan.getPlanType()
        );
    }

    @Test
    void shouldCreateIndexScanPlanForAndExpressionWithIndexedPredicate() {

        QueryPlan plan =
                new QueryOptimizer()
                        .optimize(
                                usersTable,
                                new LogicalExpression(
                                        new ComparisonExpression(
                                                "id",
                                                ComparisonOperator.GREATER_THAN,
                                                10
                                        ),
                                        LogicalOperator.AND,
                                        new ComparisonExpression(
                                                "age",
                                                ComparisonOperator.GREATER_THAN,
                                                20
                                        )
                                ),
                                List.of(idIndex)
                        );

        assertEquals(
                QueryPlanType.INDEX_SCAN,
                plan.getPlanType()
        );

        assertEquals(
                "idx_users_id",
                plan.getIndexName()
                        .orElseThrow()
        );
    }

    @Test
    void shouldExecuteEqualityIndexScan() {

        IndexScanExecutor executor =
                new IndexScanExecutor();

        QueryResult result =
                executor.execute(
                        usersTable,
                        idIndex,
                        new ComparisonExpression(
                                "id",
                                ComparisonOperator.EQUALS,
                                30
                        ),
                        rowStore::get
                );

        assertEquals(
                1,
                result.getRows()
                        .size()
        );

        assertEquals(
                30,
                result.getRows()
                        .get(0)
                        .getValue(0)
        );
    }

    @Test
    void shouldExecuteGreaterThanIndexScan() {

        QueryResult result =
                new IndexScanExecutor()
                        .execute(
                                usersTable,
                                idIndex,
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.GREATER_THAN,
                                        30
                                ),
                                rowStore::get
                        );

        assertEquals(
                2,
                result.getRows()
                        .size()
        );

        assertEquals(
                40,
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                50,
                result.getRows()
                        .get(1)
                        .getValue(0)
        );
    }

    @Test
    void shouldExecuteGreaterThanOrEqualIndexScan() {

        QueryResult result =
                new IndexScanExecutor()
                        .execute(
                                usersTable,
                                idIndex,
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                                        30
                                ),
                                rowStore::get
                        );

        assertEquals(
                3,
                result.getRows()
                        .size()
        );
    }

    @Test
    void shouldExecuteLessThanIndexScan() {

        QueryResult result =
                new IndexScanExecutor()
                        .execute(
                                usersTable,
                                idIndex,
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.LESS_THAN,
                                        30
                                ),
                                rowStore::get
                        );

        assertEquals(
                2,
                result.getRows()
                        .size()
        );
    }

    @Test
    void shouldExecuteLessThanOrEqualIndexScan() {

        QueryResult result =
                new IndexScanExecutor()
                        .execute(
                                usersTable,
                                idIndex,
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.LESS_THAN_OR_EQUALS,
                                        30
                                ),
                                rowStore::get
                        );

        assertEquals(
                3,
                result.getRows()
                        .size()
        );
    }

    @Test
    void shouldExecuteBetweenIndexScan() {

        QueryResult result =
                new IndexScanExecutor()
                        .execute(
                                usersTable,
                                idIndex,
                                new BetweenExpression(
                                        "id",
                                        20,
                                        40
                                ),
                                rowStore::get
                        );

        assertEquals(
                3,
                result.getRows()
                        .size()
        );

        assertEquals(
                20,
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                40,
                result.getRows()
                        .get(2)
                        .getValue(0)
        );
    }

    @Test
    void shouldExecuteSelectExecutorUsingIndexPath() {

        SelectExecutor executor =
                new SelectExecutor();

        QueryResult result =
                executor.execute(
                        usersTable,
                        rows,
                        new ComparisonExpression(
                                "id",
                                ComparisonOperator.EQUALS,
                                40
                        ),
                        List.of(idIndex),
                        rowStore::get
                );

        assertEquals(
                1,
                result.getRows()
                        .size()
        );

        assertEquals(
                "Emre",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );
    }

    @Test
    void shouldExecuteAndExpressionUsingIndexPathAndResidualFilter() {

        SelectExecutor executor =
                new SelectExecutor();

        QueryResult result =
                executor.execute(
                        usersTable,
                        rows,
                        new LogicalExpression(
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.GREATER_THAN,
                                        10
                                ),
                                LogicalOperator.AND,
                                new ComparisonExpression(
                                        "age",
                                        ComparisonOperator.LESS_THAN,
                                        30
                                )
                        ),
                        List.of(idIndex),
                        rowStore::get
                );

        assertEquals(
                2,
                result.getRows()
                        .size()
        );

        assertEquals(
                "Ayse",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );

        assertEquals(
                "Mehmet",
                result.getRows()
                        .get(1)
                        .getValue(1)
        );
    }

    @Test
    void shouldExecuteAndRangeBoundsUsingSingleRangeIndexPath() {

        SelectExecutor executor =
                new SelectExecutor();

        QueryResult result =
                executor.execute(
                        usersTable,
                        rows,
                        new LogicalExpression(
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                                        20
                                ),
                                LogicalOperator.AND,
                                new ComparisonExpression(
                                        "id",
                                        ComparisonOperator.LESS_THAN_OR_EQUALS,
                                        40
                                )
                        ),
                        List.of(idIndex),
                        rowStore::get
                );

        assertEquals(
                3,
                result.getRows()
                        .size()
        );

        assertEquals(
                20,
                result.getRows()
                        .get(0)
                        .getValue(0)
        );

        assertEquals(
                40,
                result.getRows()
                        .get(2)
                        .getValue(0)
        );
    }

    @Test
    void shouldFallbackToFullTableScanThroughSelectExecutor() {

        SelectExecutor executor =
                new SelectExecutor();

        QueryResult result =
                executor.execute(
                        usersTable,
                        rows,
                        new ComparisonExpression(
                                "age",
                                ComparisonOperator.GREATER_THAN,
                                25
                        ),
                        List.of(idIndex),
                        rowStore::get
                );

        /*
         * age üzerinde index olmadığı için
         * Full Table Scan kullanılmalıdır.
         */
        assertEquals(
                3,
                result.getRows()
                        .size()
        );

        assertEquals(
                "Mehmet",
                result.getRows()
                        .get(0)
                        .getValue(1)
        );
    }
}
