package com.yekdb.query.optimizer;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.expression.NotExpression;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Sprint 00-16 rule-based JOIN optimizer.
 *
 * Bu optimizer cost-based değildir.
 *
 * Temel hedefler:
 *
 * - JOIN condition validation
 * - Cartesian JOIN prevention
 * - Predicate pushdown analizi
 * - Projection pruning analizi
 * - Güvenli INNER JOIN reorder
 * - Small-table-first tercihi
 *
 * LEFT / RIGHT / FULL JOIN sırası değiştirilmez.
 */
public final class JoinOptimizer {

    /**
     * JOIN execution context'i analiz eder ve güvenli
     * optimization sonucunu üretir.
     */
    public JoinOptimizationResult optimize(
            JoinExecutionContext context
    ) {

        Objects.requireNonNull(
                context,
                "JOIN execution context cannot be null."
        );

        List<JoinClause> originalJoins =
                context.getJoinClauses();

        EnumSet<JoinOptimizationRule> appliedRules =
                EnumSet.noneOf(
                        JoinOptimizationRule.class
                );

        List<String> notes =
                new ArrayList<>();

        validateJoinConditions(
                originalJoins
        );

        appliedRules.add(
                JoinOptimizationRule.CONDITION_VALIDATION
        );

        appliedRules.add(
                JoinOptimizationRule.CARTESIAN_PREVENTION
        );

        Map<String, List<Expression>> pushedPredicates =
                analyzePredicatePushdown(
                        context.getWhereExpression()
                );

        if (!pushedPredicates.isEmpty()) {

            appliedRules.add(
                    JoinOptimizationRule.PREDICATE_PUSHDOWN
            );

            notes.add(
                    "Single-table WHERE predicates were marked as push-down candidates."
            );
        }

        Set<String> requiredColumns =
                collectRequiredColumns(
                        context
                );

        if (!requiredColumns.isEmpty()) {

            appliedRules.add(
                    JoinOptimizationRule.PROJECTION_PRUNING
            );
        }

        List<JoinClause> optimizedJoins =
                new ArrayList<>(
                        originalJoins
                );

        boolean joinOrderChanged =
                false;

        if (canSafelyReorder(
                context,
                originalJoins
        )) {

            List<JoinClause> reordered =
                    reorderByRowCount(
                            context,
                            originalJoins
                    );

            if (!sameJoinOrder(
                    originalJoins,
                    reordered
            )) {

                optimizedJoins =
                        reordered;

                joinOrderChanged =
                        true;

                appliedRules.add(
                        JoinOptimizationRule.INNER_JOIN_REORDER
                );

                appliedRules.add(
                        JoinOptimizationRule.SMALL_TABLE_FIRST
                );

                notes.add(
                        "Independent INNER JOIN branches were reordered by ascending row count."
                );
            }

        } else if (containsOuterJoin(
                originalJoins
        )) {

            notes.add(
                    "JOIN reorder was skipped because outer JOIN order must be preserved."
            );

        } else if (originalJoins.size() > 1) {

            notes.add(
                    "JOIN reorder was skipped because the JOIN chain contains dependencies."
            );
        }

        return new JoinOptimizationResult(
                optimizedJoins,
                appliedRules,
                pushedPredicates,
                requiredColumns,
                notes,
                joinOrderChanged
        );
    }

    // ==================================================
    // CONDITION VALIDATION
    // ==================================================

    /**
     * Sprint 00-16 kapsamında JOIN ON koşulları
     * gerçek column-to-column comparison olmalıdır.
     */
    private void validateJoinConditions(
            List<JoinClause> joinClauses
    ) {

        Objects.requireNonNull(
                joinClauses,
                "JOIN clause list cannot be null."
        );

        for (JoinClause joinClause
                : joinClauses) {

            if (joinClause == null) {

                throw new IllegalArgumentException(
                        "JOIN clause cannot be null."
                );
            }

            if (joinClause.getJoinType() == null) {

                throw new IllegalArgumentException(
                        "JOIN type cannot be null."
                );
            }

            if (joinClause.getTableName() == null
                    || joinClause.getTableName().isBlank()) {

                throw new IllegalArgumentException(
                        "JOIN table name cannot be null or blank."
                );
            }

            Expression condition =
                    joinClause.getCondition();

            if (condition == null) {

                throw new IllegalArgumentException(
                        "JOIN condition cannot be null. Cartesian JOIN is not allowed."
                );
            }

            if (!(condition
                    instanceof ComparisonExpression comparisonExpression)) {

                throw new IllegalArgumentException(
                        "JOIN condition must be a comparison expression."
                );
            }

            if (!comparisonExpression
                    .isColumnToColumnComparison()) {

                throw new IllegalArgumentException(
                        "JOIN condition must compare two columns."
                );
            }

            ColumnExpression left =
                    comparisonExpression
                            .getLeftColumnExpression();

            ColumnExpression right =
                    comparisonExpression
                            .getRightColumnExpression();

            if (left == null
                    || right == null) {

                throw new IllegalArgumentException(
                        "JOIN condition columns cannot be null."
                );
            }

            if (!left.isQualified()
                    || !right.isQualified()) {

                throw new IllegalArgumentException(
                        "JOIN condition columns must be qualified."
                );
            }
        }
    }

    // ==================================================
    // PREDICATE PUSHDOWN
    // ==================================================

    /**
     * Yalnızca güvenli tek tablo predicate'lerini ayırır.
     *
     * AND parçalanabilir.
     * OR ve NOT optimizer tarafından push-down edilmez.
     */
    private Map<String, List<Expression>> analyzePredicatePushdown(
            Expression expression
    ) {

        Map<String, List<Expression>> result =
                new LinkedHashMap<>();

        collectPushablePredicates(
                expression,
                result
        );

        return result;
    }

    private void collectPushablePredicates(
            Expression expression,
            Map<String, List<Expression>> target
    ) {

        if (expression == null) {
            return;
        }

        if (expression
                instanceof ComparisonExpression comparisonExpression) {

            if (!comparisonExpression
                    .isColumnToValueComparison()) {
                return;
            }

            ColumnExpression column =
                    comparisonExpression
                            .getLeftColumnExpression();

            if (!column.isQualified()) {
                return;
            }

            String qualifier =
                    normalize(
                            column.getQualifier()
                    );

            target.computeIfAbsent(
                    qualifier,
                    ignored ->
                            new ArrayList<>()
            ).add(
                    comparisonExpression
            );

            return;
        }

        if (expression
                instanceof LogicalExpression logicalExpression) {

            if (logicalExpression.operator()
                    != LogicalOperator.AND) {
                return;
            }

            collectPushablePredicates(
                    logicalExpression.leftExpression(),
                    target
            );

            collectPushablePredicates(
                    logicalExpression.rightExpression(),
                    target
            );

            return;
        }

        if (expression instanceof NotExpression) {
            /*
             * NOT predicate'leri semantik güvenlik nedeniyle
             * bu sprintte push-down edilmez.
             */
        }
    }

    // ==================================================
    // PROJECTION PRUNING
    // ==================================================

    /**
     * Query sonucunda ve JOIN yürütmesinde ihtiyaç duyulan
     * kolonların birleşimini çıkarır.
     */
    private Set<String> collectRequiredColumns(
            JoinExecutionContext context
    ) {

        Set<String> requiredColumns =
                new LinkedHashSet<>(
                        context.getProjectedColumns()
                );

        for (JoinClause joinClause
                : context.getJoinClauses()) {

            Expression condition =
                    joinClause.getCondition();

            if (condition
                    instanceof ComparisonExpression comparisonExpression) {

                requiredColumns.add(
                        comparisonExpression
                                .getLeftQualifiedName()
                );

                String rightColumn =
                        comparisonExpression
                                .getRightQualifiedName();

                if (rightColumn != null) {

                    requiredColumns.add(
                            rightColumn
                    );
                }
            }
        }

        collectExpressionColumns(
                context.getWhereExpression(),
                requiredColumns
        );

        return requiredColumns;
    }

    private void collectExpressionColumns(
            Expression expression,
            Set<String> target
    ) {

        if (expression == null) {
            return;
        }

        if (expression
                instanceof ComparisonExpression comparisonExpression) {

            target.add(
                    comparisonExpression
                            .getLeftQualifiedName()
            );

            String rightColumn =
                    comparisonExpression
                            .getRightQualifiedName();

            if (rightColumn != null) {

                target.add(
                        rightColumn
                );
            }

            return;
        }

        if (expression
                instanceof LogicalExpression logicalExpression) {

            collectExpressionColumns(
                    logicalExpression.leftExpression(),
                    target
            );

            collectExpressionColumns(
                    logicalExpression.rightExpression(),
                    target
            );

            return;
        }

        if (expression
                instanceof NotExpression notExpression) {

            collectExpressionColumns(
                    notExpression.expression(),
                    target
            );
        }
    }

    // ==================================================
    // JOIN REORDER
    // ==================================================

    /**
     * Reorder yalnızca bütün JOIN'ler INNER olduğunda ve
     * her JOIN doğrudan base tabloya bağlı bağımsız bir
     * branch olduğunda güvenlidir.
     *
     * Örnek güvenli:
     *
     * e.department_id = d.id
     * e.company_id    = c.id
     *
     * Örnek bağımlı:
     *
     * e.department_id = d.id
     * d.company_id    = c.id
     *
     * İkinci örnekte c, d sonucuna bağlı olduğu için
     * sıra değiştirilmez.
     */
    private boolean canSafelyReorder(
            JoinExecutionContext context,
            List<JoinClause> joinClauses
    ) {

        if (joinClauses.size() < 2) {
            return false;
        }

        for (JoinClause joinClause
                : joinClauses) {

            if (joinClause.getJoinType()
                    != JoinType.INNER) {

                return false;
            }

            if (!(joinClause.getCondition()
                    instanceof ComparisonExpression comparisonExpression)) {

                return false;
            }

            ColumnExpression left =
                    comparisonExpression
                            .getLeftColumnExpression();

            ColumnExpression right =
                    comparisonExpression
                            .getRightColumnExpression();

            if (!isBaseToCurrentRightTable(
                    context,
                    joinClause,
                    left,
                    right
            )) {

                return false;
            }
        }

        return true;
    }

    private boolean isBaseToCurrentRightTable(
            JoinExecutionContext context,
            JoinClause joinClause,
            ColumnExpression left,
            ColumnExpression right
    ) {

        if (left == null
                || right == null
                || !left.isQualified()
                || !right.isQualified()) {

            return false;
        }

        String leftQualifier =
                left.getQualifier();

        String rightQualifier =
                right.getQualifier();

        boolean leftIsBase =
                context.matchesBaseTable(
                        leftQualifier
                );

        boolean rightIsBase =
                context.matchesBaseTable(
                        rightQualifier
                );

        boolean leftIsCurrentRight =
                matchesJoinTable(
                        joinClause,
                        leftQualifier
                );

        boolean rightIsCurrentRight =
                matchesJoinTable(
                        joinClause,
                        rightQualifier
                );

        return (leftIsBase
                && rightIsCurrentRight)
                ||
                (rightIsBase
                        && leftIsCurrentRight);
    }

    private boolean matchesJoinTable(
            JoinClause joinClause,
            String qualifier
    ) {

        if (qualifier == null
                || qualifier.isBlank()) {
            return false;
        }

        String normalized =
                normalize(
                        qualifier
                );

        if (normalize(
                joinClause.getTableName()
        ).equals(
                normalized
        )) {

            return true;
        }

        String alias =
                joinClause.getAlias();

        return alias != null
                && !alias.isBlank()
                && normalize(
                alias
        ).equals(
                normalized
        );
    }

    private List<JoinClause> reorderByRowCount(
            JoinExecutionContext context,
            List<JoinClause> joinClauses
    ) {

        List<JoinClause> reordered =
                new ArrayList<>(
                        joinClauses
                );

        reordered.sort(
                Comparator.comparingLong(
                        context::getRowCount
                )
        );

        return reordered;
    }

    private boolean containsOuterJoin(
            List<JoinClause> joinClauses
    ) {

        return joinClauses
                .stream()
                .anyMatch(
                        joinClause ->
                                joinClause.getJoinType()
                                        != JoinType.INNER
                );
    }

    private boolean sameJoinOrder(
            List<JoinClause> left,
            List<JoinClause> right
    ) {

        if (left.size()
                != right.size()) {
            return false;
        }

        for (int index = 0;
             index < left.size();
             index++) {

            if (left.get(index)
                    != right.get(index)) {
                return false;
            }
        }

        return true;
    }

    private String normalize(
            String value
    ) {

        return Objects.requireNonNull(
                        value,
                        "Identifier cannot be null."
                )
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }
}
