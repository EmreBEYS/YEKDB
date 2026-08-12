package com.yekdb.query.expression;

import java.util.Objects;

/**
 * İki expression nesnesini AND veya OR operatörüyle birleştirir.
 *
 * <p>Örnek:</p>
 *
 * <pre>
 * age > 18 AND city = "Malatya"
 * </pre>
 *
 * @param leftExpression  sol taraftaki expression
 * @param operator        mantıksal operatör
 * @param rightExpression sağ taraftaki expression
 */
public record LogicalExpression(
        Expression leftExpression,
        LogicalOperator operator,
        Expression rightExpression
) implements Expression {

    public LogicalExpression {

        Objects.requireNonNull(
                leftExpression,
                "Left expression cannot be null."
        );

        Objects.requireNonNull(
                operator,
                "Logical operator cannot be null."
        );

        Objects.requireNonNull(
                rightExpression,
                "Right expression cannot be null."
        );
    }
}