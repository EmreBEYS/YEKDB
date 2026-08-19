package com.yekdb.query.optimizer;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QueryOptimizer ve QueryPlan sınıflarının birim testleri.
 */
class QueryOptimizerTest {

    private QueryOptimizer queryOptimizer;
    private Table usersTable;

    @BeforeEach
    void setUp() {
        queryOptimizer = new QueryOptimizer();

        usersTable = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING),
                        new Column("age", DataType.INT)
                )
        );
    }

    @Test
    void optimizeWithoutWhere_shouldCreateFullTableScanPlan() {
        QueryPlan plan = queryOptimizer.optimize(
                usersTable,
                null
        );

        assertNotNull(plan);

        assertEquals(
                QueryPlanType.FULL_TABLE_SCAN,
                plan.getPlanType()
        );

        assertFalse(plan.usesIndex());
        assertFalse(plan.getIndexName().isPresent());
        assertEquals(null, plan.getWhereExpression());

        assertMessageExists(
                plan.getExplanation()
        );
    }

    @Test
    void optimizeWithWhere_shouldCreateFullTableScanPlan() {
        Expression whereExpression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        QueryPlan plan = queryOptimizer.optimize(
                usersTable,
                whereExpression
        );

        assertNotNull(plan);

        assertEquals(
                QueryPlanType.FULL_TABLE_SCAN,
                plan.getPlanType()
        );

        assertEquals(
                whereExpression,
                plan.getWhereExpression()
        );

        assertFalse(plan.usesIndex());
        assertFalse(plan.getIndexName().isPresent());

        assertMessageExists(
                plan.getExplanation()
        );
    }

    @Test
    void optimize_shouldRejectNullTable() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> queryOptimizer.optimize(
                                null,
                                null
                        )
                );

        assertMessageExists(
                exception.getMessage()
        );
    }

    @Test
    void queryPlan_shouldStoreFullTableScanValues() {
        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        QueryPlan plan = new QueryPlan(
                QueryPlanType.FULL_TABLE_SCAN,
                expression,
                null,
                "Full table scan planı."
        );

        assertEquals(
                QueryPlanType.FULL_TABLE_SCAN,
                plan.getPlanType()
        );

        assertEquals(
                expression,
                plan.getWhereExpression()
        );

        assertFalse(plan.usesIndex());
        assertFalse(plan.getIndexName().isPresent());

        assertEquals(
                "Full table scan planı.",
                plan.getExplanation()
        );
    }

    @Test
    void queryPlan_shouldStoreIndexScanValues() {
        Expression expression =
                new ComparisonExpression(
                        "id",
                        ComparisonOperator.EQUALS,
                        1
                );

        QueryPlan plan = new QueryPlan(
                QueryPlanType.INDEX_SCAN,
                expression,
                "users_id_idx",
                "ID index kullanılacak."
        );

        assertEquals(
                QueryPlanType.INDEX_SCAN,
                plan.getPlanType()
        );

        assertTrue(plan.usesIndex());
        assertTrue(plan.getIndexName().isPresent());

        assertEquals(
                "users_id_idx",
                plan.getIndexName().orElseThrow()
        );

        assertEquals(
                expression,
                plan.getWhereExpression()
        );
    }

    @Test
    void queryPlan_shouldTrimIndexNameAndExplanation() {
        QueryPlan plan = new QueryPlan(
                QueryPlanType.INDEX_SCAN,
                null,
                "  users_id_idx  ",
                "  Index taraması yapılacak.  "
        );

        assertEquals(
                "users_id_idx",
                plan.getIndexName().orElseThrow()
        );

        assertEquals(
                "Index taraması yapılacak.",
                plan.getExplanation()
        );
    }

    @Test
    void queryPlan_shouldRejectNullPlanType() {
        assertThrows(
                NullPointerException.class,
                () -> new QueryPlan(
                        null,
                        null,
                        null,
                        "Plan"
                )
        );
    }

    @Test
    void queryPlan_shouldRejectBlankExplanation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QueryPlan(
                        QueryPlanType.FULL_TABLE_SCAN,
                        null,
                        null,
                        "   "
                )
        );
    }

    @Test
    void indexScanPlan_shouldRequireIndexName() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> new QueryPlan(
                                QueryPlanType.INDEX_SCAN,
                                null,
                                null,
                                "Index planı"
                        )
                );

        assertMessageExists(
                exception.getMessage()
        );
    }

    @Test
    void indexScanPlan_shouldRejectBlankIndexName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new QueryPlan(
                        QueryPlanType.INDEX_SCAN,
                        null,
                        "   ",
                        "Index planı"
                )
        );
    }

    @Test
    void fullTableScanPlan_shouldAllowNullIndexName() {
        QueryPlan plan = new QueryPlan(
                QueryPlanType.FULL_TABLE_SCAN,
                null,
                null,
                "Tüm tablo taranacak."
        );

        assertFalse(plan.getIndexName().isPresent());
        assertFalse(plan.usesIndex());
    }

    @Test
    void queryPlanToString_shouldContainSummaryInformation() {
        QueryPlan plan = new QueryPlan(
                QueryPlanType.INDEX_SCAN,
                null,
                "users_id_idx",
                "Index taraması"
        );

        String value = plan.toString();

        assertTrue(
                value.contains("INDEX_SCAN")
        );

        assertTrue(
                value.contains("users_id_idx")
        );

        assertTrue(
                value.contains("Index taraması")
        );
    }

    private static void assertMessageExists(
            String message
    ) {
        assertNotNull(message);
        assertFalse(message.isBlank());
    }
}