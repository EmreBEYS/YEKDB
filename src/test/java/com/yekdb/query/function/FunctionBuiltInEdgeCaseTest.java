package com.yekdb.query.function;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 00-28 Phase 8.
 * Built-in scalar function NULL, argument count ve type
 * edge-case davranışlarını kilitler.
 */
class FunctionBuiltInEdgeCaseTest {

    private final FunctionRegistry registry =
            BuiltInFunctions.createDefaultRegistry();

    @Test
    void shouldReturnNullFromLowerWhenArgumentIsNull() {

        Object result = registry.resolve("LOWER")
                .execute(singleNullParameter());

        assertNull(result);
    }

    @Test
    void shouldReturnNullFromTrimWhenArgumentIsNull() {

        Object result = registry.resolve("TRIM")
                .execute(singleNullParameter());

        assertNull(result);
    }

    @Test
    void shouldReturnNullFromAbsWhenArgumentIsNull() {

        Object result = registry.resolve("ABS")
                .execute(singleNullParameter());

        assertNull(result);
    }

    @Test
    void shouldRejectMissingLowerArgument() {

        FunctionException exception = assertThrows(
                FunctionException.class,
                () -> registry.resolve("LOWER")
                        .execute(List.of())
        );

        assertTrue(
                exception.getMessage().contains(
                        "expects 1 argument(s) but got 0"
                )
        );
    }

    @Test
    void shouldRejectTooManyLowerArguments() {

        FunctionException exception = assertThrows(
                FunctionException.class,
                () -> registry.resolve("LOWER")
                        .execute(
                                List.of(
                                        FunctionParameter.of("A"),
                                        FunctionParameter.of("B")
                                )
                        )
        );

        assertTrue(
                exception.getMessage().contains(
                        "expects 1 argument(s) but got 2"
                )
        );
    }

    @Test
    void shouldRejectInvalidAbsArgumentType() {

        FunctionException exception = assertThrows(
                FunctionException.class,
                () -> registry.resolve("ABS")
                        .execute(
                                List.of(
                                        FunctionParameter.of("not-a-number")
                                )
                        )
        );

        assertTrue(
                exception.getMessage().contains(
                        "Expected NUMERIC function parameter"
                )
        );
    }

    private List<FunctionParameter> singleNullParameter() {

        List<FunctionParameter> parameters =
                new ArrayList<>();

        parameters.add(
                FunctionParameter.of(null)
        );

        return parameters;
    }
}
