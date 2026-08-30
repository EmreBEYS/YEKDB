package com.yekdb.query.expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Represents a scalar SQL function call inside an expression.
 *
 * Examples:
 *
 * LOWER(name)
 * ABS(balance)
 * LENGTH(TRIM(name))
 *
 * Arguments may currently be:
 *
 * - literal values
 * - ColumnExpression
 * - FunctionCallExpression
 *
 * This allows nested SQL function calls without coupling the
 * expression model directly to the parser or evaluator.
 */
public final class FunctionCallExpression {

    private final String functionName;
    private final List<Object> arguments;

    public FunctionCallExpression(
            String functionName,
            List<?> arguments
    ) {
        this.functionName =
                normalizeFunctionName(
                        functionName
                );

        Objects.requireNonNull(
                arguments,
                "Function arguments cannot be null."
        );

        this.arguments =
                Collections.unmodifiableList(
                        new ArrayList<>(arguments)
                );
    }

    /**
     * Convenience constructor for a single argument.
     */
    public FunctionCallExpression(
            String functionName,
            Object argument
    ) {
        this(
                functionName,
                List.of(argument)
        );
    }

    /**
     * Convenience factory.
     */
    public static FunctionCallExpression of(
            String functionName,
            Object... arguments
    ) {
        Objects.requireNonNull(
                arguments,
                "Function arguments cannot be null."
        );

        return new FunctionCallExpression(
                functionName,
                List.of(arguments)
        );
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<Object> getArguments() {
        return arguments;
    }

    public int getArgumentCount() {
        return arguments.size();
    }

    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    private static String normalizeFunctionName(
            String functionName
    ) {
        if (functionName == null
                || functionName.isBlank()) {

            throw new IllegalArgumentException(
                    "Function name cannot be null or blank."
            );
        }

        return functionName
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {

        return functionName
                + "("
                + arguments.stream()
                .map(String::valueOf)
                .reduce(
                        (left, right) ->
                                left + ", " + right
                )
                .orElse("")
                + ")";
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof FunctionCallExpression that)) {
            return false;
        }

        return functionName.equals(
                that.functionName
        ) && arguments.equals(
                that.arguments
        );
    }

    @Override
    public int hashCode() {

        return Objects.hash(
                functionName,
                arguments
        );
    }
}