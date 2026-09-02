package com.yekdb.query.optimizer;

import com.yekdb.index.Index;
import com.yekdb.index.IndexMetadata;
import com.yekdb.index.IndexType;
import com.yekdb.query.expression.BetweenExpression;
import com.yekdb.query.expression.BooleanConstantExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Query Optimization V2 Phase 1 hazırlık testleri.
 */
class QueryOptimizerV2Test {

    private QueryOptimizer queryOptimizer;
    private Table usersTable;
    private Index<Integer> idIndex;
    private Index<Integer> ageIndex;

    @BeforeEach
    void setUp() {

        queryOptimizer =
                new QueryOptimizer();

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

        ageIndex =
                new Index<>(
                        new IndexMetadata(
                                2L,
                                "idx_users_age",
                                "test_db",
                                "users",
                                "age",
                                IndexType.NON_UNIQUE
                        )
                );
    }

    @Test
    void optimizeQueryShouldPreserveFullTableScanDecision() {

        Expression whereExpression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        OptimizedQuery optimizedQuery =
                queryOptimizer.optimizeQuery(
                        new OptimizationContext(
                                usersTable,
                                whereExpression,
                                List.of(
                                        idIndex
                                )
                        )
                );

        assertEquals(
                QueryPlanType.FULL_TABLE_SCAN,
                optimizedQuery.getQueryPlan()
                        .getPlanType()
        );

        assertSame(
                whereExpression,
                optimizedQuery.getOriginalWhereExpression()
        );

        assertSame(
                whereExpression,
                optimizedQuery.getOptimizedWhereExpression()
        );

        assertFalse(
                optimizedQuery.getAccessPredicate()
                        .isPresent()
        );

        assertSame(
                whereExpression,
                optimizedQuery.getResidualPredicate()
                        .orElseThrow()
        );

        assertTrue(
                optimizedQuery.wasRuleApplied(
                        QueryOptimizationRule.EXPRESSION_OPTIMIZATION
                )
        );

        assertFalse(
                optimizedQuery.wasRuleApplied(
                        QueryOptimizationRule.INDEX_SELECTION
                )
        );
    }

    @Test
    void optimizeQueryShouldPreserveIndexScanDecision() {

        Expression whereExpression =
                new ComparisonExpression(
                        "id",
                        ComparisonOperator.EQUALS,
                        10
                );

        OptimizedQuery optimizedQuery =
                queryOptimizer.optimizeQuery(
                        new OptimizationContext(
                                usersTable,
                                whereExpression,
                                List.of(
                                        idIndex
                                )
                        )
                );

        assertEquals(
                QueryPlanType.INDEX_SCAN,
                optimizedQuery.getQueryPlan()
                        .getPlanType()
        );

        assertEquals(
                "idx_users_id",
                optimizedQuery.getQueryPlan()
                        .getIndexName()
                        .orElseThrow()
        );

        assertSame(
                whereExpression,
                optimizedQuery.getAccessPredicate()
                        .orElseThrow()
        );

        assertFalse(
                optimizedQuery.getResidualPredicate()
                        .isPresent()
        );

        assertTrue(
                optimizedQuery.wasRuleApplied(
                        QueryOptimizationRule.INDEX_SELECTION
                )
        );
    }

    @Test
    void optimizeQueryShouldSelectIndexPredicateFromAndExpression() {

        Expression idPredicate =
                new ComparisonExpression(
                        "id",
                        ComparisonOperator.EQUALS,
                        10
                );

        Expression agePredicate =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        Expression whereExpression =
                new LogicalExpression(
                        idPredicate,
                        LogicalOperator.AND,
                        agePredicate
                );

        OptimizedQuery optimizedQuery =
                queryOptimizer.optimizeQuery(
                        new OptimizationContext(
                                usersTable,
                                whereExpression,
                                List.of(
                                        idIndex
                                )
                        )
                );

        assertEquals(
                QueryPlanType.INDEX_SCAN,
                optimizedQuery.getQueryPlan()
                        .getPlanType()
        );

        assertSame(
                idPredicate,
                optimizedQuery.getAccessPredicate()
                        .orElseThrow()
        );

        assertSame(
                agePredicate,
                optimizedQuery.getResidualPredicate()
                        .orElseThrow()
        );

        assertTrue(
                optimizedQuery.wasRuleApplied(
                        QueryOptimizationRule.INDEX_SELECTION
                )
        );
    }

    @Test
    void optimizeQueryShouldBuildRangeAccessPredicateFromAndBounds() {

        Expression lowerBound =
                new ComparisonExpression(
                        "id",
                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                        10
                );

        Expression upperBound =
                new ComparisonExpression(
                        "id",
                        ComparisonOperator.LESS_THAN_OR_EQUALS,
                        30
                );

        Expression whereExpression =
                new LogicalExpression(
                        lowerBound,
                        LogicalOperator.AND,
                        upperBound
                );

        OptimizedQuery optimizedQuery =
                queryOptimizer.optimizeQuery(
                        new OptimizationContext(
                                usersTable,
                                whereExpression,
                                List.of(
                                        idIndex
                                )
                        )
                );

        assertEquals(
                QueryPlanType.INDEX_SCAN,
                optimizedQuery.getQueryPlan()
                        .getPlanType()
        );

        BetweenExpression accessPredicate =
                (BetweenExpression) optimizedQuery
                        .getAccessPredicate()
                        .orElseThrow();

        assertEquals(
                "id",
                accessPredicate.getColumnName()
        );

        assertEquals(
                10,
                accessPredicate.getLowerBound()
        );

        assertEquals(
                30,
                accessPredicate.getUpperBound()
        );

        assertFalse(
                optimizedQuery.getResidualPredicate()
                        .isPresent()
        );
    }

    @Test
    void optimizeQueryShouldPreferUniqueEqualityOverNonUniqueRange() {

        Expression lowerBound =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                        18
                );

        Expression upperBound =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.LESS_THAN_OR_EQUALS,
                        30
                );

        Expression idPredicate =
                new ComparisonExpression(
                        "id",
                        ComparisonOperator.EQUALS,
                        20
                );

        Expression whereExpression =
                new LogicalExpression(
                        new LogicalExpression(
                                lowerBound,
                                LogicalOperator.AND,
                                upperBound
                        ),
                        LogicalOperator.AND,
                        idPredicate
                );

        OptimizedQuery optimizedQuery =
                queryOptimizer.optimizeQuery(
                        new OptimizationContext(
                                usersTable,
                                whereExpression,
                                List.of(
                                        ageIndex,
                                        idIndex
                                )
                        )
                );

        assertEquals(
                QueryPlanType.INDEX_SCAN,
                optimizedQuery.getQueryPlan()
                        .getPlanType()
        );

        assertEquals(
                "idx_users_id",
                optimizedQuery.getQueryPlan()
                        .getIndexName()
                        .orElseThrow()
        );

        assertSame(
                idPredicate,
                optimizedQuery.getAccessPredicate()
                        .orElseThrow()
        );

        assertTrue(
                optimizedQuery.getResidualPredicate()
                        .orElseThrow()
                        instanceof LogicalExpression
        );
    }

    @Test
    void optimizedQueryExplainTextShouldIncludePlanAccessAndResidualDetails() {

        Expression idPredicate =
                new ComparisonExpression(
                        "id",
                        ComparisonOperator.EQUALS,
                        20
                );

        Expression agePredicate =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        OptimizedQuery optimizedQuery =
                queryOptimizer.optimizeQuery(
                        new OptimizationContext(
                                usersTable,
                                new LogicalExpression(
                                        idPredicate,
                                        LogicalOperator.AND,
                                        agePredicate
                                ),
                                List.of(
                                        idIndex
                                )
                        )
                );

        String explainText =
                optimizedQuery.getExplainText();

        assertTrue(
                explainText.contains(
                        "PLAN: INDEX_SCAN"
                )
        );

        assertTrue(
                explainText.contains(
                        "INDEX: idx_users_id"
                )
        );

        assertTrue(
                explainText.contains(
                        "ACCESS_PREDICATE: id EQUALS 20"
                )
        );

        assertTrue(
                explainText.contains(
                        "RESIDUAL_PREDICATE: age GREATER_THAN 18"
                )
        );

        assertTrue(
                explainText.contains(
                        "INDEX_SELECTION"
                )
        );
    }

    @Test
    void optimizeQueryShouldRejectNullContext() {

        assertThrows(
                NullPointerException.class,
                () -> queryOptimizer.optimizeQuery(
                        null
                )
        );
    }

    @Test
    void optimizeQueryShouldCreateEmptyResultPlanForConstantFalse() {

        OptimizedQuery optimizedQuery =
                queryOptimizer.optimizeQuery(
                        new OptimizationContext(
                                usersTable,
                                new BooleanConstantExpression(
                                        false
                                ),
                                List.of(
                                        idIndex
                                )
                        )
                );

        assertEquals(
                QueryPlanType.EMPTY_RESULT,
                optimizedQuery.getQueryPlan()
                        .getPlanType()
        );

        assertFalse(
                optimizedQuery.getResidualPredicate()
                        .isPresent()
        );
    }

    @Test
    void optimizeQueryShouldUseFullTableScanForConstantTrueWithoutResidualPredicate() {

        OptimizedQuery optimizedQuery =
                queryOptimizer.optimizeQuery(
                        new OptimizationContext(
                                usersTable,
                                new BooleanConstantExpression(
                                        true
                                ),
                                List.of(
                                        idIndex
                                )
                        )
                );

        assertEquals(
                QueryPlanType.FULL_TABLE_SCAN,
                optimizedQuery.getQueryPlan()
                        .getPlanType()
        );

        assertFalse(
                optimizedQuery.getAccessPredicate()
                        .isPresent()
        );

        assertFalse(
                optimizedQuery.getResidualPredicate()
                        .isPresent()
        );

        assertFalse(
                optimizedQuery.wasRuleApplied(
                        QueryOptimizationRule.INDEX_SELECTION
                )
        );
    }

}
