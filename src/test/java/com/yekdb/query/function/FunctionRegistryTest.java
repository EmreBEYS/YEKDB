package com.yekdb.query.function;

import com.yekdb.query.function.builtin.LowerFunction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FunctionRegistryTest {

    @Test
    void shouldStartEmpty() {
        FunctionRegistry registry = new FunctionRegistry();

        assertTrue(registry.isEmpty());
        assertEquals(0, registry.size());
    }

    @Test
    void shouldRegisterFunction() {
        FunctionRegistry registry = new FunctionRegistry();

        registry.register(new LowerFunction());

        assertFalse(registry.isEmpty());
        assertEquals(1, registry.size());
        assertTrue(registry.contains("LOWER"));
    }

    @Test
    void shouldResolveFunctionCaseInsensitively() {
        FunctionRegistry registry = new FunctionRegistry();

        registry.register(new LowerFunction());

        SqlFunction upperCase = registry.resolve("LOWER");
        SqlFunction lowerCase = registry.resolve("lower");
        SqlFunction mixedCase = registry.resolve("LoWeR");

        assertSame(upperCase, lowerCase);
        assertSame(upperCase, mixedCase);

        assertEquals("LOWER", upperCase.getName());
    }

    @Test
    void shouldDetectFunctionCaseInsensitively() {
        FunctionRegistry registry = new FunctionRegistry();

        registry.register(new LowerFunction());

        assertTrue(registry.contains("LOWER"));
        assertTrue(registry.contains("lower"));
        assertTrue(registry.contains("Lower"));
    }

    @Test
    void shouldRejectDuplicateFunctionRegistration() {
        FunctionRegistry registry = new FunctionRegistry();

        registry.register(new LowerFunction());

        FunctionException exception = assertThrows(
                FunctionException.class,
                () -> registry.register(new LowerFunction())
        );

        assertTrue(
                exception.getMessage()
                        .contains("already registered")
        );
    }

    @Test
    void shouldRejectUnknownFunction() {
        FunctionRegistry registry = new FunctionRegistry();

        FunctionException exception = assertThrows(
                FunctionException.class,
                () -> registry.resolve("UNKNOWN_FUNCTION")
        );

        assertTrue(
                exception.getMessage()
                        .contains("Unknown function")
        );
    }

    @Test
    void shouldRejectBlankFunctionName() {
        FunctionRegistry registry = new FunctionRegistry();

        assertThrows(
                FunctionException.class,
                () -> registry.resolve(" ")
        );
    }

    @Test
    void shouldClearRegistry() {
        FunctionRegistry registry = new FunctionRegistry();

        registry.register(new LowerFunction());

        assertEquals(1, registry.size());

        registry.clear();

        assertTrue(registry.isEmpty());
        assertEquals(0, registry.size());
    }
}