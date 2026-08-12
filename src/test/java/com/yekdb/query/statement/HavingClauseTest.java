package com.yekdb.query.statement;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HavingClauseTest {

    @Test
    void shouldCreateHavingClause() {

        ComparisonExpression expression =
                new ComparisonExpression(
                        "count",
                        ComparisonOperator.GREATER_THAN,
                        2
                );

        HavingClause clause =
                new HavingClause(
                        expression
                );

        assertSame(
                expression,
                clause.getExpression()
        );
    }

    @Test
    void shouldRejectNullExpression() {

        assertThrows(
                NullPointerException.class,
                () -> new HavingClause(
                        null
                )
        );
    }

    @Test
    void shouldCreateCorrectToString() {

        ComparisonExpression expression =
                new ComparisonExpression(
                        "count",
                        ComparisonOperator.GREATER_THAN,
                        2
                );

        HavingClause clause =
                new HavingClause(
                        expression
                );

        assertTrue(
                clause.toString()
                        .startsWith(
                                "HAVING "
                        )
        );
    }
}