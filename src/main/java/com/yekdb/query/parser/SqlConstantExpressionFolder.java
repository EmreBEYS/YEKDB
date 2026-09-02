package com.yekdb.query.parser;

/**
 * SQL içerisindeki sabit numeric ifadeleri literal değere indirger.
 *
 * Desteklenen kapsam bilinçli olarak dardır:
 *
 * - Sayısal literal
 * - +, -, *, /
 * - Parantez
 * - Unary + / -
 *
 * Kolon, function call veya string içeren ifadeler bu sınıfın konusu değildir.
 */
final class SqlConstantExpressionFolder {

    private SqlConstantExpressionFolder() {
    }

    static Object parseRaw(
            String rawValue
    ) {

        if (rawValue == null) {
            throw new ParserException(
                    "Literal value cannot be null."
            );
        }

        String value =
                rawValue.trim();

        if (containsFoldableNumericOperator(
                value
        )) {

            return new NumericParser(
                    value
            ).parse();
        }

        return SqlLiteralParser.parseRaw(
                value
        );
    }

    private static boolean containsFoldableNumericOperator(
            String value
    ) {

        if (value.isBlank()) {
            return false;
        }

        boolean insideSingleQuote =
                false;

        boolean insideDoubleQuote =
                false;

        for (int index = 0;
             index < value.length();
             index++) {

            char current =
                    value.charAt(
                            index
                    );

            if (current == '\''
                    && !insideDoubleQuote) {

                insideSingleQuote =
                        !insideSingleQuote;

                continue;
            }

            if (current == '"'
                    && !insideSingleQuote) {

                insideDoubleQuote =
                        !insideDoubleQuote;

                continue;
            }

            if (insideSingleQuote
                    || insideDoubleQuote) {
                continue;
            }

            if (current == '*'
                    || current == '/'
                    || current == '('
                    || current == ')') {

                return true;
            }

            if ((current == '+'
                    || current == '-')
                    && !isUnarySign(
                    value,
                    index
            )) {

                return true;
            }
        }

        return false;
    }

    private static boolean isUnarySign(
            String value,
            int index
    ) {

        if (index == 0) {
            return true;
        }

        int previousIndex =
                index - 1;

        while (previousIndex >= 0
                && Character.isWhitespace(
                value.charAt(
                        previousIndex
                )
        )) {

            previousIndex--;
        }

        if (previousIndex < 0) {
            return true;
        }

        char previous =
                value.charAt(
                        previousIndex
                );

        return previous == '('
                || previous == '+'
                || previous == '-'
                || previous == '*'
                || previous == '/';
    }

    private static final class NumericParser {

        private final String expression;
        private int position;

        private NumericParser(
                String expression
        ) {

            this.expression =
                    expression;
        }

        private Object parse() {

            double result =
                    parseExpression();

            skipWhitespace();

            if (!isAtEnd()) {
                throw new ParserException(
                        "Invalid constant numeric expression: "
                                + expression
                );
            }

            if (Double.isNaN(result)
                    || Double.isInfinite(result)) {

                throw new ParserException(
                        "Invalid constant numeric expression result: "
                                + expression
                );
            }

            if (isWholeNumber(
                    result
            )) {

                long longValue =
                        (long) result;

                if (longValue >= Integer.MIN_VALUE
                        && longValue <= Integer.MAX_VALUE) {

                    return (int) longValue;
                }

                return longValue;
            }

            return result;
        }

        private double parseExpression() {

            double value =
                    parseTerm();

            while (true) {

                skipWhitespace();

                if (match(
                        '+'
                )) {

                    value += parseTerm();

                } else if (match(
                        '-'
                )) {

                    value -= parseTerm();

                } else {

                    return value;
                }
            }
        }

        private double parseTerm() {

            double value =
                    parseFactor();

            while (true) {

                skipWhitespace();

                if (match(
                        '*'
                )) {

                    value *= parseFactor();

                } else if (match(
                        '/'
                )) {

                    double divisor =
                            parseFactor();

                    if (divisor == 0.0d) {
                        throw new ParserException(
                                "Division by zero in constant numeric expression."
                        );
                    }

                    value /= divisor;

                } else {

                    return value;
                }
            }
        }

        private double parseFactor() {

            skipWhitespace();

            if (match(
                    '+'
            )) {

                return parseFactor();
            }

            if (match(
                    '-'
            )) {

                return -parseFactor();
            }

            if (match(
                    '('
            )) {

                double value =
                        parseExpression();

                skipWhitespace();

                if (!match(
                        ')'
                )) {

                    throw new ParserException(
                            "Unbalanced parentheses in constant numeric expression."
                    );
                }

                return value;
            }

            return parseNumber();
        }

        private double parseNumber() {

            skipWhitespace();

            int start =
                    position;

            boolean foundDigit =
                    false;

            while (!isAtEnd()
                    && Character.isDigit(
                    current()
            )) {

                foundDigit =
                        true;

                position++;
            }

            if (!isAtEnd()
                    && current() == '.') {

                position++;

                while (!isAtEnd()
                        && Character.isDigit(
                        current()
                )) {

                    foundDigit =
                            true;

                    position++;
                }
            }

            if (!foundDigit) {
                throw new ParserException(
                        "Expected numeric literal in constant expression: "
                                + expression
                );
            }

            try {
                return Double.parseDouble(
                        expression.substring(
                                start,
                                position
                        )
                );
            } catch (NumberFormatException exception) {
                throw new ParserException(
                        "Invalid numeric literal in constant expression: "
                                + expression,
                        exception
                );
            }
        }

        private boolean match(
                char expected
        ) {

            if (isAtEnd()
                    || current() != expected) {

                return false;
            }

            position++;
            return true;
        }

        private void skipWhitespace() {

            while (!isAtEnd()
                    && Character.isWhitespace(
                    current()
            )) {

                position++;
            }
        }

        private boolean isAtEnd() {

            return position
                    >= expression.length();
        }

        private char current() {

            return expression.charAt(
                    position
            );
        }

        private boolean isWholeNumber(
                double value
        ) {

            return Math.rint(
                    value
            ) == value
                    && value >= Long.MIN_VALUE
                    && value <= Long.MAX_VALUE;
        }
    }
}
