package com.yekdb.query.expression;

import com.yekdb.query.function.BuiltInFunctions;
import com.yekdb.query.function.FunctionRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FunctionCallExpressionTest {

    @Test
    void shouldNormalizeFunctionName() {

        FunctionCallExpression expression =
                new FunctionCallExpression(
                        " lower ",
                        new ColumnExpression("name")
                );

        assertEquals(
                "LOWER",
                expression.getFunctionName()
        );

        assertEquals(
                1,
                expression.getArgumentCount()
        );
    }

    @Test
    void shouldRejectBlankFunctionName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new FunctionCallExpression(
                        " ",
                        List.of()
                )
        );
    }

    @Test
    void shouldExposeImmutableArguments() {

        FunctionCallExpression expression =
                FunctionCallExpression.of(
                        "LOWER",
                        new ColumnExpression("name")
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> expression
                        .getArguments()
                        .add("another")
        );
    }

    @Test
    void shouldResolveLiteralFunctionArgument() {

        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        FunctionValueResolver resolver =
                new FunctionValueResolver(
                        registry
                );

        FunctionCallExpression expression =
                FunctionCallExpression.of(
                        "LOWER",
                        "MALATYA"
                );

        Object result =
                resolver.resolve(
                        expression,
                        Map.of()
                );

        assertEquals(
                "malatya",
                result
        );
    }

    @Test
    void shouldResolveColumnFunctionArgument() {

        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        FunctionValueResolver resolver =
                new FunctionValueResolver(
                        registry
                );

        FunctionCallExpression expression =
                FunctionCallExpression.of(
                        "UPPER",
                        new ColumnExpression(
                                "name"
                        )
                );

        Object result =
                resolver.resolve(
                        expression,
                        Map.of(
                                "name",
                                "yekdb"
                        )
                );

        assertEquals(
                "YEKDB",
                result
        );
    }

    @Test
    void shouldResolveNumericColumnFunctionArgument() {

        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        FunctionValueResolver resolver =
                new FunctionValueResolver(
                        registry
                );

        FunctionCallExpression expression =
                FunctionCallExpression.of(
                        "ABS",
                        new ColumnExpression(
                                "balance"
                        )
                );

        Object result =
                resolver.resolve(
                        expression,
                        Map.of(
                                "balance",
                                -250
                        )
                );

        assertEquals(
                250,
                result
        );
    }

    @Test
    void shouldResolveNestedFunctionCalls() {

        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        FunctionValueResolver resolver =
                new FunctionValueResolver(
                        registry
                );

        FunctionCallExpression trim =
                FunctionCallExpression.of(
                        "TRIM",
                        new ColumnExpression(
                                "name"
                        )
                );

        FunctionCallExpression length =
                FunctionCallExpression.of(
                        "LENGTH",
                        trim
                );

        Object result =
                resolver.resolve(
                        length,
                        Map.of(
                                "name",
                                "   YEKDB   "
                        )
                );

        assertEquals(
                5,
                result
        );
    }

    @Test
    void shouldResolveCaseInsensitiveColumnName() {

        FunctionValueResolver resolver =
                new FunctionValueResolver(
                        BuiltInFunctions
                                .createDefaultRegistry()
                );

        FunctionCallExpression expression =
                FunctionCallExpression.of(
                        "LOWER",
                        new ColumnExpression(
                                "CITY"
                        )
                );

        Object result =
                resolver.resolve(
                        expression,
                        Map.of(
                                "city",
                                "MALATYA"
                        )
                );

        assertEquals(
                "malatya",
                result
        );
    }

    @Test
    void shouldFailForUnknownFunction() {

        FunctionValueResolver resolver =
                new FunctionValueResolver(
                        BuiltInFunctions
                                .createDefaultRegistry()
                );

        FunctionCallExpression expression =
                FunctionCallExpression.of(
                        "UNKNOWN_FUNCTION",
                        "value"
                );

        assertThrows(
                RuntimeException.class,
                () -> resolver.resolve(
                        expression,
                        Map.of()
                )
        );
    }
}