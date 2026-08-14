package com.yekdb.query.optimizer;

import com.yekdb.query.expression.Expression;
import com.yekdb.query.statement.JoinClause;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * JoinOptimizer tarafından üretilen immutable optimization sonucudur.
 */
public final class JoinOptimizationResult {

    private final List<JoinClause> optimizedJoinClauses;
    private final Set<JoinOptimizationRule> appliedRules;
    private final Map<String, List<Expression>> pushedPredicates;
    private final Set<String> requiredColumns;
    private final List<String> notes;
    private final boolean joinOrderChanged;

    public JoinOptimizationResult(
            List<JoinClause> optimizedJoinClauses,
            Set<JoinOptimizationRule> appliedRules,
            Map<String, List<Expression>> pushedPredicates,
            Set<String> requiredColumns,
            List<String> notes,
            boolean joinOrderChanged
    ) {

        Objects.requireNonNull(
                optimizedJoinClauses,
                "Optimized JOIN clause list cannot be null."
        );

        Objects.requireNonNull(
                appliedRules,
                "Applied rule set cannot be null."
        );

        Objects.requireNonNull(
                pushedPredicates,
                "Pushed predicate map cannot be null."
        );

        Objects.requireNonNull(
                requiredColumns,
                "Required column set cannot be null."
        );

        Objects.requireNonNull(
                notes,
                "Optimization note list cannot be null."
        );

        this.optimizedJoinClauses =
                List.copyOf(
                        new ArrayList<>(
                                optimizedJoinClauses
                        )
                );

        this.appliedRules =
                appliedRules.isEmpty()
                        ? Collections.emptySet()
                        : Collections.unmodifiableSet(
                        EnumSet.copyOf(
                                appliedRules
                        )
                );

        Map<String, List<Expression>> predicateCopy =
                new LinkedHashMap<>();

        for (Map.Entry<String, List<Expression>> entry
                : pushedPredicates.entrySet()) {

            predicateCopy.put(
                    entry.getKey(),
                    List.copyOf(
                            entry.getValue()
                    )
            );
        }

        this.pushedPredicates =
                Collections.unmodifiableMap(
                        predicateCopy
                );

        this.requiredColumns =
                Collections.unmodifiableSet(
                        new LinkedHashSet<>(
                                requiredColumns
                        )
                );

        this.notes =
                List.copyOf(
                        new ArrayList<>(
                                notes
                        )
                );

        this.joinOrderChanged =
                joinOrderChanged;
    }

    public List<JoinClause> getOptimizedJoinClauses() {
        return optimizedJoinClauses;
    }

    public Set<JoinOptimizationRule> getAppliedRules() {
        return appliedRules;
    }

    public boolean wasRuleApplied(
            JoinOptimizationRule rule
    ) {
        return appliedRules.contains(
                rule
        );
    }

    public Map<String, List<Expression>> getPushedPredicates() {
        return pushedPredicates;
    }

    public Set<String> getRequiredColumns() {
        return requiredColumns;
    }

    public List<String> getNotes() {
        return notes;
    }

    public boolean isJoinOrderChanged() {
        return joinOrderChanged;
    }
}
