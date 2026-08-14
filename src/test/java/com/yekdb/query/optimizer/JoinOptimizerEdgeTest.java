package com.yekdb.query.optimizer;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.expression.NotExpression;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
import com.yekdb.query.statement.TableReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JoinOptimizerEdgeTest {

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
    void rightJoinShouldPreserveOriginalOrder() {

        JoinClause departmentJoin =
                createJoin(
                        JoinType.RIGHT,
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
    void fullJoinShouldPreserveOriginalOrder() {

        JoinClause departmentJoin =
                createJoin(
                        JoinType.FULL,
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
    }

    @Test
    void orPredicateShouldNotBePushdownCandidate() {

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
                        LogicalOperator.OR,
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
    void notPredicateShouldNotBePushdownCandidate() {

        JoinClause join =
                createJoin(
                        JoinType.INNER,
                        "department",
                        "d",
                        "e.department_id",
                        "d.id"
                );

        ComparisonExpression predicate =
                new ComparisonExpression(
                        ColumnExpression.parse(
                                "d.id"
                        ),
                        ComparisonOperator.GREATER_THAN,
                        5
                );

        NotExpression where =
                new NotExpression(
                        predicate
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
    void unknownRowCountShouldNotBePreferredAsSmallTable() {

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
                                "department", 100L
                        )
                );

        JoinOptimizationResult result =
                optimizer.optimize(
                        context
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
    void equalRowCountsShouldKeepStableOrder() {

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
                                "department", 100L,
                                "company", 100L
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
    void negativeRowCountShouldBeRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new JoinExecutionContext(
                        employeeReference,
                        List.of(),
                        Map.of(
                                "employee",
                                -1L
                        )
                )
        );
    }

    @Test
    void blankProjectedColumnShouldBeRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new JoinExecutionContext(
                        employeeReference,
                        List.of(),
                        Map.of(),
                        null,
                        Set.of(
                                "   "
                        )
                )
        );
    }

    @Test
    void nullContextShouldBeRejected() {

        assertThrows(
                NullPointerException.class,
                () -> optimizer.optimize(
                        null
                )
        );
    }

    @Test
    void optimizationResultCollectionsShouldBeImmutable() {

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
                        Map.of(),
                        null,
                        Set.of(
                                "e.name"
                        )
                );

        JoinOptimizationResult result =
                optimizer.optimize(
                        context
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result
                        .getOptimizedJoinClauses()
                        .add(
                                join
                        )
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result
                        .getRequiredColumns()
                        .add(
                                "e.id"
                        )
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> result
                        .getNotes()
                        .add(
                                "modified"
                        )
        );
    }

    @Test
    void originalJoinListShouldNotBeMutated() {

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

        List<JoinClause> originalJoins =
                new ArrayList<>(
                        List.of(
                                departmentJoin,
                                companyJoin
                        )
                );

        JoinExecutionContext context =
                new JoinExecutionContext(
                        employeeReference,
                        originalJoins,
                        Map.of(
                                "department", 1000L,
                                "company", 1L
                        )
                );

        optimizer.optimize(
                context
        );

        assertEquals(
                departmentJoin,
                originalJoins.get(0)
        );

        assertEquals(
                companyJoin,
                originalJoins.get(1)
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