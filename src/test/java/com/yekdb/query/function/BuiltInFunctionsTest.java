package com.yekdb.query.function;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuiltInFunctionsTest {

    @Test
    void shouldRegisterAllBuiltInFunctions() {
        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        assertEquals(5, registry.size());

        assertTrue(registry.contains("LOWER"));
        assertTrue(registry.contains("UPPER"));
        assertTrue(registry.contains("LENGTH"));
        assertTrue(registry.contains("TRIM"));
        assertTrue(registry.contains("ABS"));
    }

    @Test
    void shouldResolveAllBuiltInFunctionsCaseInsensitively() {
        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        assertEquals(
                "LOWER",
                registry.resolve("lower").getName()
        );

        assertEquals(
                "UPPER",
                registry.resolve("Upper").getName()
        );

        assertEquals(
                "LENGTH",
                registry.resolve("length").getName()
        );

        assertEquals(
                "TRIM",
                registry.resolve("TrIm").getName()
        );

        assertEquals(
                "ABS",
                registry.resolve("abs").getName()
        );
    }

    @Test
    void shouldExecuteLowerFunctionThroughRegistry() {
        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        SqlFunction function =
                registry.resolve("LOWER");

        Object result = function.execute(
                List.of(
                        FunctionParameter.of("MALATYA")
                )
        );

        assertEquals("malatya", result);
    }

    @Test
    void shouldExecuteUpperFunctionThroughRegistry() {
        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        SqlFunction function =
                registry.resolve("UPPER");

        Object result = function.execute(
                List.of(
                        FunctionParameter.of("yekdb")
                )
        );

        assertEquals("YEKDB", result);
    }

    @Test
    void shouldExecuteLengthFunctionThroughRegistry() {
        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        SqlFunction function =
                registry.resolve("LENGTH");

        Object result = function.execute(
                List.of(
                        FunctionParameter.of("YEKDB")
                )
        );

        assertEquals(5, result);
    }

    @Test
    void shouldExecuteTrimFunctionThroughRegistry() {
        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        SqlFunction function =
                registry.resolve("TRIM");

        Object result = function.execute(
                List.of(
                        FunctionParameter.of("   YEKDB   ")
                )
        );

        assertEquals("YEKDB", result);
    }

    @Test
    void shouldExecuteAbsFunctionThroughRegistry() {
        FunctionRegistry registry =
                BuiltInFunctions.createDefaultRegistry();

        SqlFunction function =
                registry.resolve("ABS");

        Object result = function.execute(
                List.of(
                        FunctionParameter.of(-250)
                )
        );

        assertEquals(250, result);
    }
}