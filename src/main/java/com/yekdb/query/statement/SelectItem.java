package com.yekdb.query.statement;

import com.yekdb.query.expression.FunctionCallExpression;

import java.util.Objects;

public final class SelectItem {

    private final String expression;
    private final String alias;
    private final FunctionCallExpression functionExpression;

    public SelectItem(String expression) {
        this(expression, null, null);
    }

    public SelectItem(String expression, String alias) {
        this(expression, alias, null);
    }

    public SelectItem(
            String expression,
            String alias,
            FunctionCallExpression functionExpression
    ) {
        this.expression = Objects.requireNonNull(
                expression,
                "expression cannot be null"
        );

        this.alias = normalizeAlias(alias);
        this.functionExpression = functionExpression;
    }

    public static SelectItem function(
            FunctionCallExpression functionExpression,
            String alias
    ) {
        Objects.requireNonNull(
                functionExpression,
                "functionExpression cannot be null"
        );

        return new SelectItem(
                functionExpression.toString(),
                alias,
                functionExpression
        );
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

    public boolean isFunctionExpression() {
        return functionExpression != null;
    }

    public FunctionCallExpression getFunctionExpression() {
        return functionExpression;
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
