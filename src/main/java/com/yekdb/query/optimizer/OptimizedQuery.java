package com.yekdb.query.optimizer;

import com.yekdb.query.expression.Expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Query Optimization V2 sonucunu taşır.
 *
 * Eski QueryPlan execution hattı için korunur; V2 alanları ise ilerleyen
 * phase'lerde expression rewrite, residual predicate ve EXPLAIN çıktısını
 * beslemek için kullanılır.
 */
public final class OptimizedQuery {

    private final QueryPlan queryPlan;
    private final Expression originalWhereExpression;
    private final Expression optimizedWhereExpression;
    private final Expression accessPredicate;
    private final Expression residualPredicate;
    private final Set<QueryOptimizationRule> appliedRules;
    private final List<String> explanations;

    public OptimizedQuery(
            QueryPlan queryPlan,
            Expression originalWhereExpression,
            Expression optimizedWhereExpression,
            Expression accessPredicate,
            Expression residualPredicate,
            Set<QueryOptimizationRule> appliedRules,
            List<String> explanations
    ) {

        this.queryPlan =
                Objects.requireNonNull(
                        queryPlan,
                        "QueryPlan cannot be null."
                );

        this.originalWhereExpression =
                originalWhereExpression;

        this.optimizedWhereExpression =
                optimizedWhereExpression;

        this.accessPredicate =
                accessPredicate;

        this.residualPredicate =
                residualPredicate;

        Objects.requireNonNull(
                appliedRules,
                "Applied rule set cannot be null."
        );

        this.appliedRules =
                appliedRules.isEmpty()
                        ? Collections.emptySet()
                        : Collections.unmodifiableSet(
                                EnumSet.copyOf(
                                        appliedRules
                                )
                        );

        this.explanations =
                List.copyOf(
                        new ArrayList<>(
                                Objects.requireNonNull(
                                        explanations,
                                        "Explanation list cannot be null."
                                )
                        )
                );
    }

    public QueryPlan getQueryPlan() {
        return queryPlan;
    }

    public Expression getOriginalWhereExpression() {
        return originalWhereExpression;
    }

    public Expression getOptimizedWhereExpression() {
        return optimizedWhereExpression;
    }

    public Optional<Expression> getAccessPredicate() {
        return Optional.ofNullable(
                accessPredicate
        );
    }

    public Optional<Expression> getResidualPredicate() {
        return Optional.ofNullable(
                residualPredicate
        );
    }

    public Set<QueryOptimizationRule> getAppliedRules() {
        return appliedRules;
    }

    public boolean wasRuleApplied(
            QueryOptimizationRule rule
    ) {

        return appliedRules.contains(
                rule
        );
    }

    public List<String> getExplanations() {
        return explanations;
    }

    public List<String> getExplainLines() {
        return QueryPlanExplainer.formatLines(
                this
        );
    }

    public String getExplainText() {
        return QueryPlanExplainer.format(
                this
        );
    }
}
