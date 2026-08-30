package com.yekdb.query.parser;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.FunctionCallExpression;
import com.yekdb.query.expression.FunctionComparisonExpression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.statement.SelectStatement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 00-28 Phase 6 - SQL function parser tests.
 */
class ExpressionParserFunctionTest {

    private final ExpressionParser expressionParser =
            new ExpressionParser();

    @Test
    void shouldParseLowerFunctionComparison() {

        Expression expression =
                expressionParser.parse(
                        "LOWER(city) = 'malatya'"
                );

        FunctionComparisonExpression comparison =
                assertInstanceOf(
                        FunctionComparisonExpression.class,
                        expression
                );

        assertEquals(
                "LOWER",
                comparison.getLeftFunction().getFunctionName()
        );

        assertEquals(
                ComparisonOperator.EQUALS,
                comparison.getOperator()
        );

        assertEquals(
                "malatya",
                comparison.getExpectedValue()
        );
    }

    @Test
    void shouldParseColumnFunctionArgument() {

        FunctionComparisonExpression comparison =
                (FunctionComparisonExpression)
                        expressionParser.parse(
                                "UPPER(name) = 'YEKDB'"
                        );

        Object argument =
                comparison.getLeftFunction()
                        .getArguments()
                        .get(0);

        ColumnExpression column =
                assertInstanceOf(
                        ColumnExpression.class,
                        argument
                );

        assertEquals(
                "name",
                column.getColumnName()
        );
    }

    @Test
    void shouldParseNumericFunctionComparison() {

        FunctionComparisonExpression comparison =
                (FunctionComparisonExpression)
                        expressionParser.parse(
                                "ABS(balance) > 100"
                        );

        assertEquals(
                "ABS",
                comparison.getLeftFunction().getFunctionName()
        );

        assertEquals(
                ComparisonOperator.GREATER_THAN,
                comparison.getOperator()
        );

        assertEquals(
                100,
                comparison.getExpectedValue()
        );
    }

    @Test
    void shouldParseNestedFunctionCall() {

        FunctionComparisonExpression comparison =
                (FunctionComparisonExpression)
                        expressionParser.parse(
                                "LENGTH(TRIM(name)) = 5"
                        );

        FunctionCallExpression length =
                comparison.getLeftFunction();

        assertEquals(
                "LENGTH",
                length.getFunctionName()
        );

        FunctionCallExpression trim =
                assertInstanceOf(
                        FunctionCallExpression.class,
                        length.getArguments().get(0)
                );

        assertEquals(
                "TRIM",
                trim.getFunctionName()
        );

        assertInstanceOf(
                ColumnExpression.class,
                trim.getArguments().get(0)
        );
    }

    @Test
    void shouldNormalizeFunctionNameCase() {

        FunctionComparisonExpression comparison =
                (FunctionComparisonExpression)
                        expressionParser.parse(
                                "lower(city) = 'malatya'"
                        );

        assertEquals(
                "LOWER",
                comparison.getLeftFunction().getFunctionName()
        );
    }

    @Test
    void shouldParseLiteralFunctionArgument() {

        FunctionComparisonExpression comparison =
                (FunctionComparisonExpression)
                        expressionParser.parse(
                                "LOWER('YEKDB') = 'yekdb'"
                        );

        assertEquals(
                "YEKDB",
                comparison.getLeftFunction()
                        .getArguments()
                        .get(0)
        );
    }

    @Test
    void shouldParseFunctionResultComparedWithColumn() {

        FunctionComparisonExpression comparison =
                (FunctionComparisonExpression)
                        expressionParser.parse(
                                "LENGTH(name) = expected_length"
                        );

        ColumnExpression rightColumn =
                assertInstanceOf(
                        ColumnExpression.class,
                        comparison.getExpectedValue()
                );

        assertEquals(
                "expected_length",
                rightColumn.getColumnName()
        );
    }

    @Test
    void shouldParseFunctionResultComparedWithFunctionResult() {

        FunctionComparisonExpression comparison =
                (FunctionComparisonExpression)
                        expressionParser.parse(
                                "LENGTH(name) = LENGTH(city)"
                        );

        FunctionCallExpression rightFunction =
                assertInstanceOf(
                        FunctionCallExpression.class,
                        comparison.getExpectedValue()
                );

        assertEquals(
                "LENGTH",
                rightFunction.getFunctionName()
        );

        assertTrue(
                comparison.hasFunctionRightOperand()
        );
    }

    @Test
    void shouldCombineFunctionComparisonWithLogicalExpression() {

        Expression expression =
                expressionParser.parse(
                        "LOWER(city) = 'malatya' AND age >= 18"
                );

        LogicalExpression logical =
                assertInstanceOf(
                        LogicalExpression.class,
                        expression
                );

        assertInstanceOf(
                FunctionComparisonExpression.class,
                logical.leftExpression()
        );
    }

    @Test
    void shouldParseFunctionComparisonThroughSqlParserWhereClause() {

        SelectStatement statement =
                assertInstanceOf(
                        SelectStatement.class,
                        new SqlParser().parse(
                                "SELECT * FROM users "
                                        + "WHERE LOWER(city) = 'malatya';"
                        )
                );

        FunctionComparisonExpression comparison =
                assertInstanceOf(
                        FunctionComparisonExpression.class,
                        statement.getWhereExpression()
                );

        assertEquals(
                "LOWER",
                comparison.getLeftFunction().getFunctionName()
        );
    }
}
