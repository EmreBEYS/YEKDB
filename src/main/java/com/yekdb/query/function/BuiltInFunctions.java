package com.yekdb.query.function;

import com.yekdb.query.function.builtin.AbsFunction;
import com.yekdb.query.function.builtin.LengthFunction;
import com.yekdb.query.function.builtin.LowerFunction;
import com.yekdb.query.function.builtin.TrimFunction;
import com.yekdb.query.function.builtin.UpperFunction;

import java.util.Objects;

/**
 * Central registration point for YEKDB built-in SQL functions.
 *
 * Sprint 00-28 initially provides the following scalar functions:
 *
 * LOWER
 * UPPER
 * LENGTH
 * TRIM
 * ABS
 */
public final class BuiltInFunctions {

    private BuiltInFunctions() {
        // Utility class
    }

    /**
     * Registers all built-in SQL functions into the supplied registry.
     *
     * @param registry target function registry
     */
    public static void registerAll(FunctionRegistry registry) {
        Objects.requireNonNull(registry, "registry");

        registry.register(new LowerFunction());
        registry.register(new UpperFunction());
        registry.register(new LengthFunction());
        registry.register(new TrimFunction());
        registry.register(new AbsFunction());
    }

    /**
     * Creates a new FunctionRegistry containing all YEKDB
     * built-in SQL functions.
     *
     * @return populated function registry
     */
    public static FunctionRegistry createDefaultRegistry() {
        FunctionRegistry registry = new FunctionRegistry();

        registerAll(registry);

        return registry;
    }
}