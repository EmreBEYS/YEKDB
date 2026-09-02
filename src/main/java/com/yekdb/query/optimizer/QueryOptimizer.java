package com.yekdb.query.optimizer;

import com.yekdb.index.Index;
import com.yekdb.index.IndexMetadata;
import com.yekdb.query.expression.BetweenExpression;
import com.yekdb.query.expression.BooleanConstantExpression;
import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.storage.table.Table;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Sorgular için temel yürütme planı oluşturur.
 *
 * Phase 13 ile birlikte uygun B+ Tree index bulunduğunda
 * equality ve range predicate'leri INDEX_SCAN planına dönüştürülebilir.
 */
public final class QueryOptimizer {

    private final ExpressionOptimizer expressionOptimizer;

    /**
     * Varsayılan QueryOptimizer oluşturur.
     */
    public QueryOptimizer() {
        this(
                new ExpressionOptimizer()
        );
    }

    public QueryOptimizer(
            ExpressionOptimizer expressionOptimizer
    ) {

        this.expressionOptimizer =
                Objects.requireNonNull(
                        expressionOptimizer,
                        "ExpressionOptimizer cannot be null."
                );
    }

    /**
     * Eski optimizer API.
     *
     * Index listesi sağlanmadığı için geriye dönük uyumluluk amacıyla
     * Full Table Scan üretmeye devam eder.
     */
    public QueryPlan optimize(
            Table table,
            Expression whereExpression
    ) {

        return optimize(
                table,
                whereExpression,
                List.of()
        );
    }

    /**
     * Verilen sorgu ve kullanılabilir index listesi için execution planı üretir.
     */
    public QueryPlan optimize(
            Table table,
            Expression whereExpression,
            List<Index<?>> availableIndexes
    ) {

        return optimizeQuery(
                new OptimizationContext(
                        table,
                        whereExpression,
                        availableIndexes
                )
        ).getQueryPlan();
    }

    /**
     * Query Optimization V2 için tam optimization sonucunu üretir.
     *
     * Phase 1 kapsamında expression optimizer no-op çalışır ve eski
     * QueryPlan davranışı korunur.
     */
    public OptimizedQuery optimizeQuery(
            OptimizationContext context
    ) {

        Objects.requireNonNull(
                context,
                "OptimizationContext cannot be null."
        );

        Objects.requireNonNull(
                context.getTable(),
                "Table cannot be null."
        );

        Objects.requireNonNull(
                context.getAvailableIndexes(),
                "Available index list cannot be null."
        );

        Expression originalWhereExpression =
                context.getWhereExpression();

        Expression optimizedWhereExpression =
                expressionOptimizer.optimize(
                        originalWhereExpression
                );

        Set<QueryOptimizationRule> appliedRules =
                EnumSet.of(
                        QueryOptimizationRule.EXPRESSION_OPTIMIZATION
                );

        List<String> explanations =
                new ArrayList<>();

        QueryPlanDecision queryPlanDecision =
                createQueryPlanDecision(
                        context.getTable(),
                        optimizedWhereExpression,
                        context.getAvailableIndexes()
                );

        QueryPlan queryPlan =
                queryPlanDecision.queryPlan();

        explanations.add(
                queryPlan.getExplanation()
        );

        Expression accessPredicate =
                queryPlanDecision.accessPredicate();

        Expression residualPredicate =
                queryPlanDecision.residualPredicate();

        if (queryPlan.usesIndex()) {

            appliedRules.add(
                    QueryOptimizationRule.INDEX_SELECTION
            );
        } else if (queryPlan.getPlanType()
                == QueryPlanType.EMPTY_RESULT
                || optimizedWhereExpression
                instanceof BooleanConstantExpression) {

            residualPredicate =
                    null;
        }

        return new OptimizedQuery(
                queryPlan,
                originalWhereExpression,
                optimizedWhereExpression,
                accessPredicate,
                residualPredicate,
                appliedRules,
                explanations
        );
    }

    private QueryPlan createQueryPlan(
            Table table,
            Expression whereExpression,
            List<Index<?>> availableIndexes
    ) {

        return createQueryPlanDecision(
                table,
                whereExpression,
                availableIndexes
        ).queryPlan();
    }

    private QueryPlanDecision createQueryPlanDecision(
            Table table,
            Expression whereExpression,
            List<Index<?>> availableIndexes
    ) {

        if (whereExpression == null) {

            return new QueryPlanDecision(
                    new QueryPlan(
                            QueryPlanType.FULL_TABLE_SCAN,
                            null,
                            null,
                            "No WHERE condition exists, so the table will be scanned fully."
                    ),
                    null,
                    null
            );
        }

        if (whereExpression instanceof BooleanConstantExpression booleanConstantExpression) {

            if (booleanConstantExpression.value()) {

                return new QueryPlanDecision(
                        new QueryPlan(
                                QueryPlanType.FULL_TABLE_SCAN,
                                null,
                                null,
                                "WHERE condition is constant TRUE, so no row filter is required."
                        ),
                        null,
                        null
                );
            }

            return new QueryPlanDecision(
                    new QueryPlan(
                            QueryPlanType.EMPTY_RESULT,
                            whereExpression,
                            null,
                            "WHERE condition is constant FALSE, so an empty result can be returned without scanning storage."
                    ),
                    null,
                    null
            );
        }

        IndexAccessPath accessPath =
                findBestIndexAccessPath(
                        table,
                        whereExpression,
                        availableIndexes
                );

        if (accessPath == null) {

            return new QueryPlanDecision(
                    new QueryPlan(
                            QueryPlanType.FULL_TABLE_SCAN,
                            whereExpression,
                            null,
                            "WHERE expression is not eligible for a B+ Tree index access path."
                    ),
                    null,
                    whereExpression
            );
        }

        Expression residualPredicate =
                createResidualPredicate(
                        whereExpression,
                        accessPath.consumedPredicates()
                );

        return new QueryPlanDecision(
                new QueryPlan(
                        QueryPlanType.INDEX_SCAN,
                        accessPath.accessPredicate(),
                        accessPath.index()
                                .getMetadata()
                                .getIndexName(),
                        "B+ Tree index '"
                                + accessPath.index()
                                .getMetadata()
                                .getIndexName()
                                + "' will be used for column '"
                                + accessPath.columnName()
                                + "'."
                ),
                accessPath.accessPredicate(),
                residualPredicate
        );
    }

    private IndexAccessPath findBestIndexAccessPath(
            Table table,
            Expression whereExpression,
            List<Index<?>> availableIndexes
    ) {

        List<Expression> candidates =
                flattenAndPredicates(
                whereExpression
        );

        IndexAccessPath bestAccessPath =
                null;

        IndexAccessPath rangeAccessPath =
                findRangeIndexAccessPath(
                        table,
                        candidates,
                        availableIndexes
                );

        if (rangeAccessPath != null) {
            bestAccessPath =
                    rangeAccessPath;
        }

        for (Expression candidate : candidates) {

            String indexedColumn =
                    findIndexableColumn(
                            candidate
                    );

            if (indexedColumn == null) {
                continue;
            }

            Index<?> index =
                    findMatchingIndex(
                            table,
                            indexedColumn,
                            availableIndexes
                    );

            if (index != null) {

                IndexAccessPath accessPath =
                        new IndexAccessPath(
                        candidate,
                        indexedColumn,
                        index,
                        List.of(
                                candidate
                        ),
                        scoreAccessPath(
                                candidate,
                                index
                        )
                );

                if (isBetterAccessPath(
                        accessPath,
                        bestAccessPath
                )) {

                    bestAccessPath =
                            accessPath;
                }
            }
        }

        return bestAccessPath;
    }

    private IndexAccessPath findRangeIndexAccessPath(
            Table table,
            List<Expression> candidates,
            List<Index<?>> availableIndexes
    ) {

        for (Expression lowerCandidate : candidates) {

            if (!(lowerCandidate instanceof ComparisonExpression lowerComparison)
                    || !isLowerBoundComparison(
                    lowerComparison
            )) {

                continue;
            }

            String columnName =
                    lowerComparison
                            .getLeftColumnExpression()
                            .getColumnName();

            Index<?> index =
                    findMatchingIndex(
                            table,
                            columnName,
                            availableIndexes
                    );

            if (index == null) {
                continue;
            }

            for (Expression upperCandidate : candidates) {

                if (upperCandidate == lowerCandidate
                        || !(upperCandidate instanceof ComparisonExpression upperComparison)
                        || !isUpperBoundComparison(
                        upperComparison
                )
                        || !isSameLeftColumn(
                        lowerComparison,
                        upperComparison
                )
                        || !hasComparableBoundsInOrder(
                        lowerComparison.expectedValue(),
                        upperComparison.expectedValue()
                )) {

                    continue;
                }

                return new IndexAccessPath(
                        new BetweenExpression(
                                columnName,
                                lowerComparison.expectedValue(),
                                upperComparison.expectedValue()
                        ),
                        columnName,
                        index,
                        List.of(
                                lowerCandidate,
                                upperCandidate
                        ),
                        220
                );
            }
        }

        return null;
    }

    private List<Expression> flattenAndPredicates(
            Expression expression
    ) {

        List<Expression> result =
                new ArrayList<>();

        collectAndPredicates(
                expression,
                result
        );

        return result;
    }

    private void collectAndPredicates(
            Expression expression,
            List<Expression> result
    ) {

        if (expression instanceof LogicalExpression logicalExpression
                && logicalExpression.operator()
                == LogicalOperator.AND) {

            collectAndPredicates(
                    logicalExpression.leftExpression(),
                    result
            );

            collectAndPredicates(
                    logicalExpression.rightExpression(),
                    result
            );

            return;
        }

        result.add(
                expression
        );
    }

    private Expression createResidualPredicate(
            Expression whereExpression,
            List<Expression> consumedPredicates
    ) {

        List<Expression> predicates =
                flattenAndPredicates(
                        whereExpression
                );

        List<Expression> residualPredicates =
                new ArrayList<>();

        boolean removed =
                false;

        for (Expression predicate : predicates) {

            if (isConsumedPredicate(
                    predicate,
                    consumedPredicates
            )) {

                removed =
                        true;

                continue;
            }

            residualPredicates.add(
                    predicate
            );
        }

        if (!removed
                || residualPredicates.isEmpty()) {

            return null;
        }

        Expression result =
                residualPredicates.get(0);

        for (int index = 1;
             index < residualPredicates.size();
             index++) {

            result =
                    new LogicalExpression(
                            result,
                            LogicalOperator.AND,
                            residualPredicates.get(index)
                    );
        }

        return result;
    }

    private boolean isConsumedPredicate(
            Expression predicate,
            List<Expression> consumedPredicates
    ) {

        for (Expression consumedPredicate : consumedPredicates) {

            if (predicate == consumedPredicate) {
                return true;
            }
        }

        return false;
    }

    private boolean isLowerBoundComparison(
            ComparisonExpression comparison
    ) {

        return comparison.isColumnToValueComparison()
                && comparison.expectedValue()
                instanceof Comparable<?>
                && (comparison.operator()
                == ComparisonOperator.GREATER_THAN
                || comparison.operator()
                == ComparisonOperator.GREATER_THAN_OR_EQUALS);
    }

    private boolean isUpperBoundComparison(
            ComparisonExpression comparison
    ) {

        return comparison.isColumnToValueComparison()
                && comparison.expectedValue()
                instanceof Comparable<?>
                && (comparison.operator()
                == ComparisonOperator.LESS_THAN
                || comparison.operator()
                == ComparisonOperator.LESS_THAN_OR_EQUALS);
    }

    private boolean isSameLeftColumn(
            ComparisonExpression left,
            ComparisonExpression right
    ) {

        return left.getLeftColumnExpression()
                .getColumnName()
                .equalsIgnoreCase(
                        right.getLeftColumnExpression()
                                .getColumnName()
                );
    }

    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private boolean hasComparableBoundsInOrder(
            Object lowerBound,
            Object upperBound
    ) {

        if (!(lowerBound instanceof Comparable lowerComparable)
                || !(upperBound instanceof Comparable upperComparable)) {

            return false;
        }

        try {
            return lowerComparable.compareTo(
                    upperComparable
            ) <= 0;
        } catch (ClassCastException exception) {
            return false;
        }
    }

    private boolean isBetterAccessPath(
            IndexAccessPath candidate,
            IndexAccessPath current
    ) {

        return current == null
                || candidate.score()
                > current.score();
    }

    private int scoreAccessPath(
            Expression accessPredicate,
            Index<?> index
    ) {

        if (accessPredicate instanceof ComparisonExpression comparison
                && comparison.operator()
                == ComparisonOperator.EQUALS) {

            if (index.getMetadata()
                    .getIndexType()
                    .isPrimary()) {

                return 420;
            }

            if (index.getMetadata()
                    .getIndexType()
                    .isUnique()) {

                return 400;
            }

            return 320;
        }

        if (accessPredicate instanceof BetweenExpression) {
            return 220;
        }

        return 120;
    }

    /**
     * Expression doğrudan B+ Tree access predicate ise kolon adını döndürür.
     */
    private String findIndexableColumn(
            Expression expression
    ) {

        if (expression
                instanceof ComparisonExpression comparison) {

            if (!comparison.isColumnToValueComparison()
                    || comparison.operator()
                    == ComparisonOperator.NOT_EQUALS
                    || !(comparison.expectedValue()
                    instanceof Comparable<?>)) {

                return null;
            }

            return comparison
                    .getLeftColumnExpression()
                    .getColumnName();
        }

        if (expression
                instanceof BetweenExpression between) {

            if (between.isNegated()
                    || !(between.getLowerBound()
                    instanceof Comparable<?>)
                    || !(between.getUpperBound()
                    instanceof Comparable<?>)) {

                return null;
            }

            return ColumnExpression
                    .parse(
                            between.getColumnName()
                    )
                    .getColumnName();
        }

        return null;
    }

    /**
     * Tablo ve kolon metadata'sı ile eşleşen ilk index'i seçer.
     */
    private Index<?> findMatchingIndex(
            Table table,
            String columnName,
            List<Index<?>> availableIndexes
    ) {

        for (Index<?> index : availableIndexes) {

            if (index == null) {
                continue;
            }

            IndexMetadata metadata =
                    index.getMetadata();

            if (metadata == null
                    || !metadata.isValid()) {
                continue;
            }

            if (metadata.getTableName()
                    .equalsIgnoreCase(
                            table.getTableName()
                    )
                    && metadata.belongsToColumn(
                    columnName
            )) {

                return index;
            }
        }

        return null;
    }

    private record IndexAccessPath(
            Expression accessPredicate,
            String columnName,
            Index<?> index,
            List<Expression> consumedPredicates,
            int score
    ) {
    }

    private record QueryPlanDecision(
            QueryPlan queryPlan,
            Expression accessPredicate,
            Expression residualPredicate
    ) {
    }
}
