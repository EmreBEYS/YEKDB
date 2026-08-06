package com.yekdb.query.optimizer;

import com.yekdb.query.expression.Expression;

import java.util.Objects;
import java.util.Optional;

/**
 * QueryOptimizer tarafından oluşturulan sorgu yürütme planıdır.
 */
public final class QueryPlan {

    private final QueryPlanType planType;
    private final Expression whereExpression;
    private final String indexName;
    private final String explanation;

    public QueryPlan(
            QueryPlanType planType,
            Expression whereExpression,
            String indexName,
            String explanation
    ) {
        this.planType = Objects.requireNonNull(
                planType,
                "Plan türü null olamaz."
        );

        this.whereExpression = whereExpression;
        this.indexName = normalizeNullable(indexName);

        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException(
                    "Plan açıklaması boş olamaz."
            );
        }

        this.explanation = explanation.trim();

        if (planType == QueryPlanType.INDEX_SCAN
                && this.indexName == null) {
            throw new IllegalArgumentException(
                    "INDEX_SCAN planı için index adı gereklidir."
            );
        }
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public QueryPlanType getPlanType() {
        return planType;
    }

    public Expression getWhereExpression() {
        return whereExpression;
    }

    public Optional<String> getIndexName() {
        return Optional.ofNullable(indexName);
    }

    public String getExplanation() {
        return explanation;
    }

    public boolean usesIndex() {
        return planType == QueryPlanType.INDEX_SCAN;
    }

    @Override
    public String toString() {
        return "QueryPlan{" +
                "planType=" + planType +
                ", indexName='" + indexName + '\'' +
                ", explanation='" + explanation + '\'' +
                '}';
    }
}