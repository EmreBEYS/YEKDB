package com.yekdb.query.expression;

/**
 * Optimizer tarafından sadeleştirilebilen sabit boolean predicate'i temsil eder.
 *
 * Örnekler:
 *
 * 1 = 1
 * true = true
 */
public record BooleanConstantExpression(
        boolean value
) implements Expression {

    @Override
    public String toString() {
        return Boolean.toString(
                value
        );
    }
}
