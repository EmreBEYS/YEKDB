package com.yekdb.query.expression;

import java.util.Objects;

/**
 * Bir expression sonucunu tersine çeviren NOT ifadesini temsil eder.
 *
 * Örnek:
 *
 * NOT age > 18
 *
 * @param expression tersine çevrilecek expression
 */

public record NotExpression(Expression expression) implements Expression {
    public NotExpression{
        Objects.requireNonNull(expression,"The content of the expression 'NOT' cannot be null.");
    }
}
