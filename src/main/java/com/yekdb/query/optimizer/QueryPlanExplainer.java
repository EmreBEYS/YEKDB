package com.yekdb.query.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Optimizer kararini insan tarafindan okunabilir plan satirlarina donusturur.
 */
public final class QueryPlanExplainer {

    private QueryPlanExplainer() {
    }

    public static List<String> formatLines(
            OptimizedQuery optimizedQuery
    ) {

        Objects.requireNonNull(
                optimizedQuery,
                "OptimizedQuery cannot be null."
        );

        QueryPlan queryPlan =
                optimizedQuery.getQueryPlan();

        List<String> lines =
                new ArrayList<>();

        lines.add(
                "PLAN: "
                        + queryPlan.getPlanType()
        );

        lines.add(
                "INDEX: "
                        + queryPlan.getIndexName()
                        .orElse(
                                "NONE"
                        )
        );

        lines.add(
                "ORIGINAL_WHERE: "
                        + formatExpression(
                                optimizedQuery.getOriginalWhereExpression()
                        )
        );

        lines.add(
                "OPTIMIZED_WHERE: "
                        + formatExpression(
                                optimizedQuery.getOptimizedWhereExpression()
                        )
        );

        lines.add(
                "ACCESS_PREDICATE: "
                        + optimizedQuery.getAccessPredicate()
                        .map(Object::toString)
                        .orElse(
                                "NONE"
                        )
        );

        lines.add(
                "RESIDUAL_PREDICATE: "
                        + optimizedQuery.getResidualPredicate()
                        .map(Object::toString)
                        .orElse(
                                "NONE"
                        )
        );

        lines.add(
                "RULES: "
                        + formatRules(
                                optimizedQuery
                        )
        );

        for (String explanation
                : optimizedQuery.getExplanations()) {

            lines.add(
                    "DETAIL: "
                            + explanation
            );
        }

        return List.copyOf(
                lines
        );
    }

    public static String format(
            OptimizedQuery optimizedQuery
    ) {

        return String.join(
                System.lineSeparator(),
                formatLines(
                        optimizedQuery
                )
        );
    }

    private static String formatRules(
            OptimizedQuery optimizedQuery
    ) {

        if (optimizedQuery.getAppliedRules()
                .isEmpty()) {

            return "NONE";
        }

        return optimizedQuery.getAppliedRules()
                .stream()
                .map(Enum::name)
                .sorted()
                .collect(
                        Collectors.joining(
                                ", "
                        )
                );
    }

    private static String formatExpression(
            Object expression
    ) {

        if (expression == null) {
            return "NONE";
        }

        return expression.toString();
    }
}
