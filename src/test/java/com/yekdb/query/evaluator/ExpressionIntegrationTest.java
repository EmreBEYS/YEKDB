package com.yekdb.query.evaluator;

import com.yekdb.query.expression.Expression;
import com.yekdb.query.parser.ExpressionParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionIntegrationTest {

    private ExpressionParser parser;
    private ExpressionEvaluator evaluator;
    private Map<String, Object> row;

    @BeforeEach
    void setUp() {

        parser = new ExpressionParser();
        evaluator = new ExpressionEvaluator();

        row = new HashMap<>();

        row.put("id", 1);
        row.put("name", "Yunus");
        row.put("age", 21);
        row.put("city", "Malatya");
        row.put("active", true);
        row.put("role", "user");
        row.put("salary", 50000.0);
    }

    @Test
    void shouldParseAndEvaluateSimpleComparison() {

        Expression expression =
                parser.parse(
                        "age > 18"
                );

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertTrue(result);
    }

    @Test
    void shouldParseAndEvaluateAndExpression() {

        Expression expression =
                parser.parse(
                        "age > 18 AND city = 'Malatya'"
                );

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseForAndExpression() {

        Expression expression =
                parser.parse(
                        "age > 18 AND city = 'Ankara'"
                );

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertFalse(result);
    }

    @Test
    void shouldParseAndEvaluateOrExpression() {

        Expression expression =
                parser.parse(
                        "age < 18 OR city = 'Malatya'"
                );

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertTrue(result);
    }

    @Test
    void shouldParseAndEvaluateNotExpression() {

        Expression expression =
                parser.parse(
                        "NOT active = false"
                );

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertTrue(result);
    }

    @Test
    void shouldRespectAndPrecedenceOverOr() {

        Expression expression =
                parser.parse(
                        "age > 18 AND active = true OR city = 'Ankara'"
                );

        /*
         * Beklenen:
         *
         * (age > 18 AND active = true)
         * OR city = 'Ankara'
         *
         * true AND true = true
         * true OR false = true
         */

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertTrue(result);
    }

    @Test
    void shouldRespectParenthesesOverDefaultPrecedence() {

        Expression expression =
                parser.parse(
                        "age > 18 AND "
                                + "(active = false OR city = 'Malatya')"
                );

        /*
         * true AND
         * (false OR true)
         *
         * true AND true
         *
         * true
         */

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertTrue(result);
    }

    @Test
    void shouldEvaluateNotParenthesizedExpression() {

        Expression expression =
                parser.parse(
                        "NOT (age < 18 OR active = false)"
                );

        /*
         * age < 18       -> false
         * active = false -> false
         *
         * false OR false -> false
         *
         * NOT false -> true
         */

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertTrue(result);
    }

    @Test
    void shouldEvaluateComplexExpression() {

        Expression expression =
                parser.parse(
                        "(age >= 18 AND active = true) "
                                + "OR "
                                + "(city = 'Ankara' "
                                + "AND NOT role = 'banned')"
                );

        /*
         * Sol taraf:
         *
         * age >= 18       -> true
         * active = true   -> true
         *
         * true AND true   -> true
         *
         * Sağ tarafın sonucu artık önemli değil.
         *
         * true OR ...
         *
         * true
         */

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertTrue(result);
    }

    @Test
    void shouldEvaluateComplexExpressionAsFalse() {

        Expression expression =
                parser.parse(
                        "(age < 18 AND active = true) "
                                + "OR "
                                + "(city = 'Ankara' "
                                + "AND role = 'admin')"
                );

        /*
         * Sol:
         *
         * false AND true
         * -> false
         *
         * Sağ:
         *
         * false AND false
         * -> false
         *
         * false OR false
         * -> false
         */

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertFalse(result);
    }

    @Test
    void shouldEvaluateDoubleNotExpression() {

        Expression expression =
                parser.parse(
                        "NOT NOT active = true"
                );

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertTrue(result);
    }

    @Test
    void shouldPreserveLogicalKeywordsInsideStringLiteral() {

        Map<String, Object> specialRow =
                new HashMap<>(
                        row
                );

        specialRow.put(
                "name",
                "Yunus AND Emre OR NOT"
        );

        Expression expression =
                parser.parse(
                        "name = 'Yunus AND Emre OR NOT'"
                );

        boolean result =
                evaluator.evaluate(
                        expression,
                        specialRow
                );

        assertTrue(result);
    }

    @Test
    void shouldEvaluateNumericOrdering() {

        Expression expression =
                parser.parse(
                        "salary >= 49999"
                );

        boolean result =
                evaluator.evaluate(
                        expression,
                        row
                );

        assertTrue(result);
    }

    @Test
    void shouldThrowWhenReferencedColumnDoesNotExist() {

        Expression expression =
                parser.parse(
                        "unknownColumn = 1"
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        expression,
                        row
                )
        );
    }
}