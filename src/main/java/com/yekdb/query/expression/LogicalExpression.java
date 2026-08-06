package com.yekdb.query.expression;

import java.util.Objects;

/**
 * İki expression nesnesini AND veya OR operatörüyle birleştirir.
 *
 * Örnek:
 *
 * age > 18 AND city = "Malatya"
 *
 * @param leftExpression sol taraftaki expression
 * @param operator mantıksal operatör
 * @param rightExpression sağ taraftaki expression
 */

public record LogicalExpression(Expression leftExpression, LogicalOperator operator, Expression rightExpression) implements Expression {
    public LogicalExpression{
        Objects.requireNonNull(leftExpression,"The left expression cannot be null.");
        Objects.requireNonNull(operator,"A logical operator cannot be null.");
        Objects.requireNonNull(rightExpression,"The right expression cannot be null.");
    }
}
