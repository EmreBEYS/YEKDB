package com.yekdb.query.parser;

import com.yekdb.query.expression.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionParserTest {

    private ExpressionParser parser;

    @BeforeEach
    void setUp() {
        parser = new ExpressionParser();
    }

    @Test
    void shouldParseSingleComparison() {

        Expression expression =
                parser.parse(
                        "age > 18"
                );

        assertInstanceOf(
                ComparisonExpression.class,
                expression
        );

        ComparisonExpression comparison =
                (ComparisonExpression) expression;

        assertEquals(
                "age",
                comparison.columnName()
        );

        assertEquals(
                ComparisonOperator.GREATER_THAN,
                comparison.operator()
        );

        assertEquals(
                18,
                comparison.expectedValue()
        );
    }

    @Test
    void shouldParseAndExpression() {

        Expression expression =
                parser.parse(
                        "age > 18 AND city = 'Malatya'"
                );

        assertInstanceOf(
                LogicalExpression.class,
                expression
        );

        LogicalExpression logical =
                (LogicalExpression) expression;

        assertEquals(
                LogicalOperator.AND,
                logical.operator()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                logical.leftExpression()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                logical.rightExpression()
        );
    }

    @Test
    void shouldParseLeftSideOfAndExpression() {

        LogicalExpression expression =
                (LogicalExpression) parser.parse(
                        "age > 18 AND city = 'Malatya'"
                );

        ComparisonExpression left =
                (ComparisonExpression)
                        expression.leftExpression();

        assertEquals(
                "age",
                left.columnName()
        );

        assertEquals(
                ComparisonOperator.GREATER_THAN,
                left.operator()
        );

        assertEquals(
                18,
                left.expectedValue()
        );
    }

    @Test
    void shouldParseRightSideOfAndExpression() {

        LogicalExpression expression =
                (LogicalExpression) parser.parse(
                        "age > 18 AND city = 'Malatya'"
                );

        ComparisonExpression right =
                (ComparisonExpression)
                        expression.rightExpression();

        assertEquals(
                "city",
                right.columnName()
        );

        assertEquals(
                ComparisonOperator.EQUALS,
                right.operator()
        );

        assertEquals(
                "Malatya",
                right.expectedValue()
        );
    }

    @Test
    void shouldParseMultipleAndExpressions() {

        Expression expression =
                parser.parse(
                        "age > 18 AND active = true AND city = 'Malatya'"
                );

        assertInstanceOf(
                LogicalExpression.class,
                expression
        );

        LogicalExpression root =
                (LogicalExpression) expression;

        assertEquals(
                LogicalOperator.AND,
                root.operator()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                root.leftExpression()
        );

        assertInstanceOf(
                LogicalExpression.class,
                root.rightExpression()
        );

        LogicalExpression right =
                (LogicalExpression)
                        root.rightExpression();

        assertEquals(
                LogicalOperator.AND,
                right.operator()
        );
    }

    @Test
    void shouldIgnoreAndInsideStringLiteral() {

        Expression expression =
                parser.parse(
                        "name = 'Yunus AND Emre'"
                );

        assertInstanceOf(
                ComparisonExpression.class,
                expression
        );

        ComparisonExpression comparison =
                (ComparisonExpression) expression;

        assertEquals(
                "Yunus AND Emre",
                comparison.expectedValue()
        );
    }

    @Test
    void shouldParseAndCaseInsensitive() {

        Expression expression =
                parser.parse(
                        "age > 18 and city = 'Malatya'"
                );

        assertInstanceOf(
                LogicalExpression.class,
                expression
        );

        LogicalExpression logical =
                (LogicalExpression) expression;

        assertEquals(
                LogicalOperator.AND,
                logical.operator()
        );
    }

    @Test
    void shouldRejectMissingRightSideOfAnd() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "age > 18 AND"
                )
        );
    }

    @Test
    void shouldRejectMissingLeftSideOfAnd() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "AND age > 18"
                )
        );
    }

    @Test
    void shouldParseOrExpression() {

        Expression expression =
                parser.parse(
                        "age > 18 OR city = 'Malatya'"
                );

        assertInstanceOf(
                LogicalExpression.class,
                expression
        );

        LogicalExpression logical =
                (LogicalExpression) expression;

        assertEquals(
                LogicalOperator.OR,
                logical.operator()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                logical.leftExpression()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                logical.rightExpression()
        );
    }

    @Test
    void shouldParseMultipleOrExpressions() {

        Expression expression =
                parser.parse(
                        "age > 18 OR city = 'Malatya' OR active = true"
                );

        assertInstanceOf(
                LogicalExpression.class,
                expression
        );

        LogicalExpression root =
                (LogicalExpression) expression;

        assertEquals(
                LogicalOperator.OR,
                root.operator()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                root.leftExpression()
        );

        assertInstanceOf(
                LogicalExpression.class,
                root.rightExpression()
        );
    }

    @Test
    void shouldIgnoreOrInsideStringLiteral() {

        Expression expression =
                parser.parse(
                        "name = 'Yunus OR Emre'"
                );

        assertInstanceOf(
                ComparisonExpression.class,
                expression
        );

        ComparisonExpression comparison =
                (ComparisonExpression) expression;

        assertEquals(
                "Yunus OR Emre",
                comparison.expectedValue()
        );
    }

    @Test
    void shouldParseOrCaseInsensitive() {

        Expression expression =
                parser.parse(
                        "age > 18 or city = 'Malatya'"
                );

        assertInstanceOf(
                LogicalExpression.class,
                expression
        );

        LogicalExpression logical =
                (LogicalExpression) expression;

        assertEquals(
                LogicalOperator.OR,
                logical.operator()
        );
    }

    @Test
    void shouldRejectMissingRightSideOfOr() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "age > 18 OR"
                )
        );
    }

    @Test
    void shouldRejectMissingLeftSideOfOr() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "OR age > 18"
                )
        );
    }

    @Test
    void shouldGiveAndHigherPrecedenceThanOr() {

        Expression expression =
                parser.parse(
                        "age > 18 AND active = true OR city = 'Malatya'"
                );

        assertInstanceOf(
                LogicalExpression.class,
                expression
        );

        LogicalExpression root =
                (LogicalExpression) expression;

        /*
         * Beklenen:
         *
         * OR
         * ├── AND
         * │   ├── age > 18
         * │   └── active = true
         * └── city = 'Malatya'
         */

        assertEquals(
                LogicalOperator.OR,
                root.operator()
        );

        assertInstanceOf(
                LogicalExpression.class,
                root.leftExpression()
        );

        LogicalExpression left =
                (LogicalExpression)
                        root.leftExpression();

        assertEquals(
                LogicalOperator.AND,
                left.operator()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                root.rightExpression()
        );
    }

    @Test
    void shouldGiveAndHigherPrecedenceOnRightSideOfOr() {

        Expression expression =
                parser.parse(
                        "age > 18 OR active = true AND city = 'Malatya'"
                );

        LogicalExpression root =
                assertInstanceOf(
                        LogicalExpression.class,
                        expression
                );

        /*
         * Beklenen:
         *
         * OR
         * ├── age > 18
         * └── AND
         *     ├── active = true
         *     └── city = 'Malatya'
         */

        assertEquals(
                LogicalOperator.OR,
                root.operator()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                root.leftExpression()
        );

        LogicalExpression right =
                assertInstanceOf(
                        LogicalExpression.class,
                        root.rightExpression()
                );

        assertEquals(
                LogicalOperator.AND,
                right.operator()
        );
    }

    @Test
    void shouldParseNotExpression() {

        Expression expression =
                parser.parse(
                        "NOT age > 18"
                );

        assertInstanceOf(
                NotExpression.class,
                expression
        );

        NotExpression notExpression =
                (NotExpression) expression;

        assertInstanceOf(
                ComparisonExpression.class,
                notExpression.expression()
        );

        ComparisonExpression comparison =
                (ComparisonExpression)
                        notExpression.expression();

        assertEquals(
                "age",
                comparison.columnName()
        );

        assertEquals(
                ComparisonOperator.GREATER_THAN,
                comparison.operator()
        );

        assertEquals(
                18,
                comparison.expectedValue()
        );
    }

    @Test
    void shouldParseNotCaseInsensitive() {

        Expression expression =
                parser.parse(
                        "not active = true"
                );

        assertInstanceOf(
                NotExpression.class,
                expression
        );
    }

    @Test
    void shouldParseDoubleNotExpression() {

        Expression expression =
                parser.parse(
                        "NOT NOT active = true"
                );

        assertInstanceOf(
                NotExpression.class,
                expression
        );

        NotExpression firstNot =
                (NotExpression) expression;

        assertInstanceOf(
                NotExpression.class,
                firstNot.expression()
        );

        NotExpression secondNot =
                (NotExpression)
                        firstNot.expression();

        assertInstanceOf(
                ComparisonExpression.class,
                secondNot.expression()
        );
    }

    @Test
    void shouldRejectEmptyNotExpression() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "NOT"
                )
        );
    }

    @Test
    void shouldNotTreatIdentifierStartingWithNotAsKeyword() {

        Expression expression =
                parser.parse(
                        "notification = true"
                );

        assertInstanceOf(
                ComparisonExpression.class,
                expression
        );

        ComparisonExpression comparison =
                (ComparisonExpression) expression;

        assertEquals(
                "notification",
                comparison.columnName()
        );
    }

    @Test
    void shouldParseSingleParenthesizedExpression() {

        Expression expression =
                parser.parse(
                        "(age > 18)"
                );

        assertInstanceOf(
                ComparisonExpression.class,
                expression
        );

        ComparisonExpression comparison =
                (ComparisonExpression) expression;

        assertEquals(
                "age",
                comparison.columnName()
        );

        assertEquals(
                ComparisonOperator.GREATER_THAN,
                comparison.operator()
        );
    }

    @Test
    void shouldParseParenthesizedAndExpression() {

        Expression expression =
                parser.parse(
                        "(age > 18 AND active = true)"
                );

        assertInstanceOf(
                LogicalExpression.class,
                expression
        );

        LogicalExpression logical =
                (LogicalExpression) expression;

        assertEquals(
                LogicalOperator.AND,
                logical.operator()
        );
    }

    @Test
    void shouldParseParenthesizedOrExpression() {

        Expression expression =
                parser.parse(
                        "(age > 18 OR city = 'Malatya')"
                );

        assertInstanceOf(
                LogicalExpression.class,
                expression
        );

        LogicalExpression logical =
                (LogicalExpression) expression;

        assertEquals(
                LogicalOperator.OR,
                logical.operator()
        );
    }

    @Test
    void shouldRejectEmptyParentheses() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "()"
                )
        );
    }

    @Test
    void shouldRejectUnbalancedParentheses() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "(age > 18"
                )
        );
    }

    @Test
    void shouldRejectUnexpectedClosingParenthesis() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "age > 18)"
                )
        );
    }

    @Test
    void shouldGiveNotHigherPrecedenceThanAnd() {

        Expression expression =
                parser.parse(
                        "NOT active = false AND age > 18"
                );

        assertInstanceOf(
                LogicalExpression.class,
                expression
        );

        LogicalExpression root =
                (LogicalExpression) expression;

        /*
         * Beklenen:
         *
         * AND
         * ├── NOT
         * │   └── active = false
         * └── age > 18
         */

        assertEquals(
                LogicalOperator.AND,
                root.operator()
        );

        assertInstanceOf(
                NotExpression.class,
                root.leftExpression()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                root.rightExpression()
        );
    }

    @Test
    void shouldGiveNotHigherPrecedenceThanOr() {

        Expression expression =
                parser.parse(
                        "NOT active = false OR age > 18"
                );

        LogicalExpression root =
                assertInstanceOf(
                        LogicalExpression.class,
                        expression
                );

        assertEquals(
                LogicalOperator.OR,
                root.operator()
        );

        assertInstanceOf(
                NotExpression.class,
                root.leftExpression()
        );
    }

    @Test
    void shouldLetParenthesesOverrideAndPrecedence() {

        Expression expression =
                parser.parse(
                        "age > 18 AND (active = true OR city = 'Malatya')"
                );

        LogicalExpression root =
                assertInstanceOf(
                        LogicalExpression.class,
                        expression
                );

        /*
         * Beklenen:
         *
         * AND
         * ├── age > 18
         * └── OR
         *     ├── active = true
         *     └── city = 'Malatya'
         */

        assertEquals(
                LogicalOperator.AND,
                root.operator()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                root.leftExpression()
        );

        LogicalExpression right =
                assertInstanceOf(
                        LogicalExpression.class,
                        root.rightExpression()
                );

        assertEquals(
                LogicalOperator.OR,
                right.operator()
        );
    }

    @Test
    void shouldLetParenthesesOverrideOrPrecedence() {

        Expression expression =
                parser.parse(
                        "(age > 18 OR active = true) AND city = 'Malatya'"
                );

        LogicalExpression root =
                assertInstanceOf(
                        LogicalExpression.class,
                        expression
                );

        /*
         * Beklenen:
         *
         * AND
         * ├── OR
         * │   ├── age > 18
         * │   └── active = true
         * └── city = 'Malatya'
         */

        assertEquals(
                LogicalOperator.AND,
                root.operator()
        );

        LogicalExpression left =
                assertInstanceOf(
                        LogicalExpression.class,
                        root.leftExpression()
                );

        assertEquals(
                LogicalOperator.OR,
                left.operator()
        );

        assertInstanceOf(
                ComparisonExpression.class,
                root.rightExpression()
        );
    }

    @Test
    void shouldParseNotParenthesizedExpression() {

        Expression expression =
                parser.parse(
                        "NOT (age < 18 OR active = false)"
                );

        NotExpression notExpression =
                assertInstanceOf(
                        NotExpression.class,
                        expression
                );

        LogicalExpression inner =
                assertInstanceOf(
                        LogicalExpression.class,
                        notExpression.expression()
                );

        assertEquals(
                LogicalOperator.OR,
                inner.operator()
        );
    }

    @Test
    void shouldParseComplexExpressionWithFullPrecedence() {

        Expression expression =
                parser.parse(
                        "(age >= 18 AND active = true) "
                                + "OR "
                                + "(city = 'Malatya' AND NOT role = 'banned')"
                );

        LogicalExpression root =
                assertInstanceOf(
                        LogicalExpression.class,
                        expression
                );

        assertEquals(
                LogicalOperator.OR,
                root.operator()
        );

        LogicalExpression left =
                assertInstanceOf(
                        LogicalExpression.class,
                        root.leftExpression()
                );

        assertEquals(
                LogicalOperator.AND,
                left.operator()
        );

        LogicalExpression right =
                assertInstanceOf(
                        LogicalExpression.class,
                        root.rightExpression()
                );

        assertEquals(
                LogicalOperator.AND,
                right.operator()
        );

        assertInstanceOf(
                NotExpression.class,
                right.rightExpression()
        );
    }

    @Test
    void shouldIgnoreLogicalKeywordsInsideStringLiteralWithParentheses() {

        Expression expression =
                parser.parse(
                        "name = 'NOT (AND OR)'"
                );

        ComparisonExpression comparison =
                assertInstanceOf(
                        ComparisonExpression.class,
                        expression
                );

        assertEquals(
                "NOT (AND OR)",
                comparison.expectedValue()
        );
    }

    @Test
    void shouldRejectUnclosedStringLiteral() {

        assertThrows(
                ParserException.class,
                () -> parser.parse(
                        "name = 'Yunus"
                )
        );
    }

}