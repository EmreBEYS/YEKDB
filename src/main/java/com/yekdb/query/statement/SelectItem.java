package com.yekdb.query.statement;

import java.util.Objects;

public final class SelectItem {

    private final String expression;
    private final String alias;

    public SelectItem(String expression) {
        this(expression, null);
    }

    public SelectItem(String expression, String alias) {
        this.expression = Objects.requireNonNull(
                expression,
                "expression cannot be null"
        );

        this.alias = normalizeAlias(alias);
    }

    public String getExpression() {
        return expression;
    }

    public String getAlias() {
        return alias;
    }

    public boolean hasAlias() {
        return alias != null && !alias.isBlank();
    }

    public String getOutputName() {
        return hasAlias() ? alias : expression;
    }

    private static String normalizeAlias(String alias) {
        if (alias == null) {
            return null;
        }

        String normalized = alias.trim();

        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public String toString() {
        if (hasAlias()) {
            return expression + " AS " + alias;
        }

        return expression;
    }

}
