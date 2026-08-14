package com.yekdb.query.optimizer;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
import com.yekdb.query.statement.TableReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JoinOptimizerTest {

    private JoinOptimizer optimizer;
    private TableReference employeeReference;

    @BeforeEach
    void setUp() {

        optimizer =
                new JoinOptimizer();

        employeeReference =
                new TableReference(
                        "employee",
                        "e"
                );
    }

    @Test
    void validInnerJoinShouldPassConditionValidation() {

        JoinClause join =
                createJoin(
                        JoinType.INNER,
                        "department",
                        "d",
                        "e.department_id",
                        "d.id"
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        List.of(join),
                        Map.of(
                                "employee", 1000L,
                                "department", 10L
                        )
                );

        JoinOptimizationResult result =
                optimizer.optimize(
                        context
                );

        assertNotNull(result);

        assertTrue(
                result.wasRuleApplied(
                        JoinOptimizationRule.CONDITION_VALIDATION
                )
        );

        assertTrue(
                result.wasRuleApplied(
                        JoinOptimizationRule.CARTESIAN_PREVENTION
                )
        );
    }

    @Test
    void nullJoinConditionShouldPreventCartesianJoin() {

        JoinClause join =
                new JoinClause(
                        JoinType.INNER,
                        "department",
                        "d",
                        null
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        List.of(join),
                        Map.of()
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> optimizer.optimize(
                                context
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "Cartesian JOIN"
                        )
        );
    }

    @Test
    void columnToValueJoinConditionShouldBeRejected() {

        JoinClause join =
                new JoinClause(
                        JoinType.INNER,
                        "department",
                        "d",
                        new ComparisonExpression(
                                ColumnExpression.parse(
                                        "e.department_id"
                                ),
                                ComparisonOperator.EQUALS,
                                10
                        )
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        List.of(join),
                        Map.of()
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> optimizer.optimize(
                                context
                        )
                );

        assertTrue(
                exception.getMessage()
                        .contains(
                                "compare two columns"
                        )
        );
    }

    @Test
    void qualifiedWherePredicateShouldBePushdownCandidate() {

        JoinClause join =
                createJoin(
                        JoinType.INNER,
                        "department",
                        "d",
                        "e.department_id",
                        "d.id"
                );

        ComparisonExpression where =
                new ComparisonExpression(
                        ColumnExpression.parse(
                                "d.id"
                        ),
                        ComparisonOperator.GREATER_THAN,
                        5
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        List.of(join),
                        Map.of(),
                        where,
                        Set.of(
                                "e.name",
                                "d.id"
                        )
                );

        JoinOptimizationResult result =
                optimizer.optimize(
                        context
                );

        assertTrue(
                result.wasRuleApplied(
                        JoinOptimizationRule.PREDICATE_PUSHDOWN
                )
        );

        assertTrue(
                result.getPushedPredicates()
                        .containsKey(
                                "d"
                        )
        );

        assertEquals(
                1,
                result.getPushedPredicates()
                        .get(
                                "d"
                        )
                        .size()
        );
    }

    @Test
    void unqualifiedWherePredicateShouldNotBePushedDown() {

        JoinClause join =
                createJoin(
                        JoinType.INNER,
                        "department",
                        "d",
                        "e.department_id",
                        "d.id"
                );

        ComparisonExpression where =
                new ComparisonExpression(
                        "salary",
                        ComparisonOperator.GREATER_THAN,
                        50000
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        List.of(join),
                        Map.of(),
                        where,
                        Set.of()
                );

        JoinOptimizationResult result =
                optimizer.optimize(
                        context
                );

        assertFalse(
                result.wasRuleApplied(
                        JoinOptimizationRule.PREDICATE_PUSHDOWN
                )
        );

        assertTrue(
                result.getPushedPredicates()
                        .isEmpty()
        );
    }

    @Test
    void projectionPruningShouldCollectRequiredColumns() {

        JoinClause join =
                createJoin(
                        JoinType.INNER,
                        "department",
                        "d",
                        "e.department_id",
                        "d.id"
                );

        ComparisonExpression where =
                new ComparisonExpression(
                        ColumnExpression.parse(
                                "e.salary"
                        ),
                        ComparisonOperator.GREATER_THAN,
                        40000
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        List.of(join),
                        Map.of(),
                        where,
                        Set.of(
                                "e.name",
                                "d.name"
                        )
                );

        JoinOptimizationResult result =
                optimizer.optimize(
                        context
                );

        assertTrue(
                result.wasRuleApplied(
                        JoinOptimizationRule.PROJECTION_PRUNING
                )
        );

        assertTrue(
                result.getRequiredColumns()
                        .contains(
                                "e.name"
                        )
        );

        assertTrue(
                result.getRequiredColumns()
                        .contains(
                                "d.name"
                        )
        );

        assertTrue(
                result.getRequiredColumns()
                        .contains(
                                "e.department_id"
                        )
        );

        assertTrue(
                result.getRequiredColumns()
                        .contains(
                                "d.id"
                        )
        );

        assertTrue(
                result.getRequiredColumns()
                        .contains(
                                "e.salary"
                        )
        );
    }

    @Test
    void independentInnerJoinsShouldBeReorderedByRowCount() {

        JoinClause departmentJoin =
                createJoin(
                        JoinType.INNER,
                        "department",
                        "d",
                        "e.department_id",
                        "d.id"
                );

        JoinClause companyJoin =
                createJoin(
                        JoinType.INNER,
                        "company",
                        "c",
                        "e.company_id",
                        "c.id"
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        List.of(
                                departmentJoin,
                                companyJoin
                        ),
                        Map.of(
                                "department", 1000L,
                                "company", 10L
                        )
                );

        JoinOptimizationResult result =
                optimizer.optimize(
                        context
                );

        assertTrue(
                result.isJoinOrderChanged()
        );

        assertTrue(
                result.wasRuleApplied(
                        JoinOptimizationRule.INNER_JOIN_REORDER
                )
        );

        assertTrue(
                result.wasRuleApplied(
                        JoinOptimizationRule.SMALL_TABLE_FIRST
                )
        );

        assertEquals(
                "company",
                result.getOptimizedJoinClauses()
                        .get(0)
                        .getTableName()
        );

        assertEquals(
                "department",
                result.getOptimizedJoinClauses()
                        .get(1)
                        .getTableName()
        );
    }

    @Test
    void dependentInnerJoinChainShouldPreserveOriginalOrder() {

        JoinClause departmentJoin =
                createJoin(
                        JoinType.INNER,
                        "department",
                        "d",
                        "e.department_id",
                        "d.id"
                );

        JoinClause companyJoin =
                createJoin(
                        JoinType.INNER,
                        "company",
                        "c",
                        "d.company_id",
                        "c.id"
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        List.of(
                                departmentJoin,
                                companyJoin
                        ),
                        Map.of(
                                "department", 5000L,
                                "company", 1L
                        )
                );

        JoinOptimizationResult result =
                optimizer.optimize(
                        context
                );

        assertFalse(
                result.isJoinOrderChanged()
        );

        assertEquals(
                "department",
                result.getOptimizedJoinClauses()
                        .get(0)
                        .getTableName()
        );

        assertEquals(
                "company",
                result.getOptimizedJoinClauses()
                        .get(1)
                        .getTableName()
        );
    }

    @Test
    void outerJoinShouldNeverBeReordered() {

        JoinClause departmentJoin =
                createJoin(
                        JoinType.LEFT,
                        "department",
                        "d",
                        "e.department_id",
                        "d.id"
                );

        JoinClause companyJoin =
                createJoin(
                        JoinType.INNER,
                        "company",
                        "c",
                        "e.company_id",
                        "c.id"
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        List.of(
                                departmentJoin,
                                companyJoin
                        ),
                        Map.of(
                                "department", 5000L,
                                "company", 1L
                        )
                );

        JoinOptimizationResult result =
                optimizer.optimize(
                        context
                );

        assertFalse(
                result.isJoinOrderChanged()
        );

        assertFalse(
                result.wasRuleApplied(
                        JoinOptimizationRule.INNER_JOIN_REORDER
                )
        );

        assertEquals(
                departmentJoin,
                result.getOptimizedJoinClauses()
                        .get(0)
        );

        assertEquals(
                companyJoin,
                result.getOptimizedJoinClauses()
                        .get(1)
        );
    }

    @Test
    void multipleAndPredicatesShouldBeCollectedForPushdown() {

        JoinClause join =
                createJoin(
                        JoinType.INNER,
                        "department",
                        "d",
                        "e.department_id",
                        "d.id"
                );

        ComparisonExpression employeePredicate =
                new ComparisonExpression(
                        ColumnExpression.parse(
                                "e.salary"
                        ),
                        ComparisonOperator.GREATER_THAN,
                        50000
                );

        ComparisonExpression departmentPredicate =
                new ComparisonExpression(
                        ColumnExpression.parse(
                                "d.id"
                        ),
                        ComparisonOperator.GREATER_THAN,
                        5
                );

        LogicalExpression where =
                new LogicalExpression(
                        employeePredicate,
                        LogicalOperator.AND,
                        departmentPredicate
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        List.of(join),
                        Map.of(),
                        where,
                        Set.of()
                );

        JoinOptimizationResult result =
                optimizer.optimize(
                        context
                );

        assertEquals(
                1,
                result.getPushedPredicates()
                        .get(
                                "e"
                        )
                        .size()
        );

        assertEquals(
                1,
                result.getPushedPredicates()
                        .get(
                                "d"
                        )
                        .size()
        );
    }

    private JoinClause createJoin(
            JoinType joinType,
            String tableName,
            String alias,
            String leftColumn,
            String rightColumn
    ) {

        return new JoinClause(
                joinType,
                tableName,
                alias,
                new ComparisonExpression(
                        ColumnExpression.parse(
                                leftColumn
                        ),
                        ComparisonOperator.EQUALS,
                        ColumnExpression.parse(
                                rightColumn
                        )
                )
        );
    }
}