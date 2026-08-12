package com.yekdb.query.expression;

import java.util.Objects;

/**
 * Bir expression sonucunu tersine çeviren NOT ifadesini temsil eder.
 *
 * <p>Örnek:</p>
 *
 * <pre>
 * NOT age > 18
 * </pre>
 *
 * @param expression tersine çevrilecek expression
 */
public record NotExpression(
        Expression expression
) implements Expression {

    public NotExpression {

        Objects.requireNonNull(
                expression,
                "NOT expression cannot be null."
        );
    }
}