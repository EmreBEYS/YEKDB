package com.yekdb.query.function;

import java.util.List;

/**
 * Represents an executable SQL function.
 *
 * Implementations of this interface are responsible for validating
 * their own arguments and returning the evaluated function result.
 */
public interface SqlFunction {

    /**
     * Returns the unique SQL-visible function name.
     *
     * Examples:
     * LOWER
     * UPPER
     * LENGTH
     * ABS
     */
    String getName();

    /**
     * Returns the category of this function.
     */
    FunctionType getType();

    /**
     * Returns the minimum number of arguments accepted by this function.
     */
    int getMinimumArgumentCount();

    /**
     * Returns the maximum number of arguments accepted by this function.
     */
    int getMaximumArgumentCount();

    /**
     * Executes the SQL function.
     *
     * @param parameters evaluated function parameters
     * @return function result, or null when the SQL result is NULL
     */
    Object execute(List<FunctionParameter> parameters);

    /**
     * Validates the number of supplied arguments.
     *
     * Implementations may perform additional type validation
     * inside execute().
     */
    default void validateArgumentCount(List<FunctionParameter> parameters) {
        if (parameters == null) {
            throw new FunctionException(
                    "Function " + getName() + " parameters cannot be null."
            );
        }

        int count = parameters.size();

        int minimum = getMinimumArgumentCount();
        int maximum = getMaximumArgumentCount();

        if (count < minimum || count > maximum) {
            if (minimum == maximum) {
                throw new FunctionException(
                        "Function " + getName()
                                + " expects " + minimum
                                + " argument(s) but got " + count + "."
                );
            }

            throw new FunctionException(
                    "Function " + getName()
                            + " expects between "
                            + minimum + " and "
                            + maximum
                            + " argument(s) but got "
                            + count + "."
            );
        }
    }
}