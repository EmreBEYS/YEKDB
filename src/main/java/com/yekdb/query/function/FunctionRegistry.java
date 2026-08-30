package com.yekdb.query.function;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Registry responsible for storing and resolving SQL functions.
 *
 * Function names are case-insensitive.
 */
public final class FunctionRegistry {

    private final Map<String, SqlFunction> functions;

    public FunctionRegistry() {
        this.functions = new LinkedHashMap<>();
    }

    /**
     * Registers a SQL function.
     *
     * Function names are normalized to uppercase so function
     * lookup is case-insensitive.
     *
     * @param function function to register
     */
    public void register(SqlFunction function) {
        Objects.requireNonNull(function, "function");

        String normalizedName = normalizeName(function.getName());

        if (functions.containsKey(normalizedName)) {
            throw new FunctionException(
                    "Function '" + normalizedName + "' is already registered."
            );
        }

        functions.put(normalizedName, function);
    }

    /**
     * Resolves a registered SQL function.
     *
     * @param name SQL function name
     * @return resolved function
     * @throws FunctionException when the function does not exist
     */
    public SqlFunction resolve(String name) {
        String normalizedName = normalizeName(name);

        SqlFunction function = functions.get(normalizedName);

        if (function == null) {
            throw new FunctionException(
                    "Unknown function '" + normalizedName + "'."
            );
        }

        return function;
    }

    /**
     * Returns true when a function with the supplied name exists.
     */
    public boolean contains(String name) {
        return functions.containsKey(normalizeName(name));
    }

    /**
     * Returns the number of registered functions.
     */
    public int size() {
        return functions.size();
    }

    /**
     * Returns true when no functions are registered.
     */
    public boolean isEmpty() {
        return functions.isEmpty();
    }

    /**
     * Returns all registered functions.
     *
     * The returned collection cannot be modified.
     */
    public Collection<SqlFunction> getFunctions() {
        return Collections.unmodifiableCollection(functions.values());
    }

    /**
     * Removes all registered functions.
     *
     * Primarily useful for isolated tests.
     */
    public void clear() {
        functions.clear();
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new FunctionException(
                    "Function name cannot be null or blank."
            );
        }

        return name.trim().toUpperCase(Locale.ROOT);
    }
}