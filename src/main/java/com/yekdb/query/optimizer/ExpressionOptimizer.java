package com.yekdb.query.optimizer;

import com.yekdb.query.expression.BooleanConstantExpression;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.expression.NotExpression;

/**
 * WHERE expression ağacını optimize eden V2 bileşenidir.
 *
 * Phase 3 itibarıyla sabit boolean predicate'ler güvenli biçimde
 * sadeleştirilir.
 */
public final class ExpressionOptimizer {

    /**
     * Expression'ı mevcut semantiği koruyarak optimize eder.
     */
    public Expression optimize(
            Expression expression
    ) {

        if (expression == null) {
            return null;
        }

        if (expression instanceof BooleanConstantExpression) {
            return expression;
        }

        if (expression instanceof LogicalExpression logicalExpression) {
            return optimizeLogical(
                    logicalExpression
            );
        }

        if (expression instanceof NotExpression notExpression) {
            return optimizeNot(
                    notExpression
            );
        }

        return expression;
    }

    private Expression optimizeLogical(
            LogicalExpression expression
    ) {

        Expression left =
                optimize(
                        expression.leftExpression()
                );

        Expression right =
                optimize(
                        expression.rightExpression()
                );

        if (left == expression.leftExpression()
                && right == expression.rightExpression()
                && !(left instanceof BooleanConstantExpression)
                && !(right instanceof BooleanConstantExpression)) {

            return expression;
        }

        if (expression.operator()
                == LogicalOperator.AND) {

            return optimizeAnd(
                    left,
                    right
            );
        }

        return optimizeOr(
                left,
                right
        );
    }

    private Expression optimizeAnd(
            Expression left,
            Expression right
    ) {

        if (isConstantFalse(
                left
        )
                || isConstantFalse(
                right
        )) {

            return new BooleanConstantExpression(
                    false
            );
        }

        if (isConstantTrue(
                left
        )) {

            return right;
        }

        if (isConstantTrue(
                right
        )) {

            return left;
        }

        return new LogicalExpression(
                left,
                LogicalOperator.AND,
                right
        );
    }

    private Expression optimizeOr(
            Expression left,
            Expression right
    ) {

        if (isConstantTrue(
                left
        )
                || isConstantTrue(
                right
        )) {

            return new BooleanConstantExpression(
                    true
            );
        }

        if (isConstantFalse(
                left
        )) {

            return right;
        }

        if (isConstantFalse(
                right
        )) {

            return left;
        }

        return new LogicalExpression(
                left,
                LogicalOperator.OR,
                right
        );
    }

    private Expression optimizeNot(
            NotExpression expression
    ) {

        Expression optimized =
                optimize(
                        expression.expression()
                );

        if (optimized instanceof BooleanConstantExpression booleanConstantExpression) {

            return new BooleanConstantExpression(
                    !booleanConstantExpression.value()
            );
        }

        if (optimized == expression.expression()) {
            return expression;
        }

        return new NotExpression(
                optimized
        );
    }

    private boolean isConstantTrue(
            Expression expression
    ) {

        return expression instanceof BooleanConstantExpression booleanConstantExpression
                && booleanConstantExpression.value();
    }

    private boolean isConstantFalse(
            Expression expression
    ) {

        return expression instanceof BooleanConstantExpression booleanConstantExpression
                && !booleanConstantExpression.value();
    }
}
