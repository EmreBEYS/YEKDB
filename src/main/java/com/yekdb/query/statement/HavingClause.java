package com.yekdb.query.statement;

import com.yekdb.query.expression.Expression;

import java.util.Objects;

/**
 * SQL HAVING ifadesini temsil eder.
 *
 * HAVING, GROUP BY sonrasında oluşturulan
 * gruplar / aggregate sonuçları üzerinde
 * filtreleme yapmak için kullanılır.
 *
 * Örnek:
 *
 * HAVING COUNT(*) > 2
 *
 * HAVING AVG(salary) > 50000
 *
 * Sprint 00-14
 */
public final class HavingClause {

    private final Expression expression;

    public HavingClause(
            Expression expression
    ) {

        this.expression =
                Objects.requireNonNull(
                        expression,
                        "HAVING expression cannot be null."
                );
    }

    public Expression getExpression() {

        return expression;
    }

    @Override
    public String toString() {

        return "HAVING "
                + expression;
    }
}