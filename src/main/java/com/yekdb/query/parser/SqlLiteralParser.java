package com.yekdb.query.parser;

/**
 * SQL literal tokenlarını Java değerlerine dönüştürür.
 *
 * <p>Bu sınıf yalnızca literal dönüşümünden sorumludur;
 * token akışını yönetmez.</p>
 */
final class SqlLiteralParser {

    private SqlLiteralParser() {
        // Utility sınıfı.
    }

    static Object parse(SqlToken token) {
        return switch (token.getType()) {
            case STRING_LITERAL -> token.getValue();

            case NUMBER_LITERAL -> parseNumber(token);

            case BOOLEAN_LITERAL ->
                    Boolean.parseBoolean(token.getValue());

            case NULL_LITERAL -> null;

            default -> throw new ParserException(
                    "Expected literal value but found: "
                            + token.getValue()
            );
        };
    }

    private static Number parseNumber(SqlToken token) {
        String value = token.getValue();

        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }

            long longValue = Long.parseLong(value);

            if (longValue >= Integer.MIN_VALUE
                    && longValue <= Integer.MAX_VALUE) {
                return (int) longValue;
            }

            return longValue;

        } catch (NumberFormatException exception) {
            throw new ParserException(
                    "Invalid numeric value: " + value,
                    exception
            );
        }
    }

    /**
     * ExpressionParser gibi string-tabanlı parser katmanlarından gelen
     * ham SQL literal değerini Java değerine dönüştürür.
     */
    static Object parseRaw(String rawValue) {
        if (rawValue == null) {
            throw new ParserException("Literal value cannot be null.");
        }

        String value = rawValue.trim();

        if ((value.startsWith("\'") && value.endsWith("\'"))
                || (value.startsWith("\"") && value.endsWith("\""))) {

            if (value.length() < 2) {
                throw new ParserException(
                        "Invalid string literal: " + rawValue
                );
            }

            return value.substring(1, value.length() - 1);
        }

        if (value.equalsIgnoreCase("true")) {
            return true;
        }

        if (value.equalsIgnoreCase("false")) {
            return false;
        }

        if (value.equalsIgnoreCase("null")) {
            return null;
        }

        if (value.contains(".")) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                throw new ParserException(
                        "Invalid numeric literal: " + rawValue,
                        exception
                );
            }
        }

        try {
            long longValue = Long.parseLong(value);

            if (longValue >= Integer.MIN_VALUE
                    && longValue <= Integer.MAX_VALUE) {
                return (int) longValue;
            }

            return longValue;

        } catch (NumberFormatException exception) {
            throw new ParserException(
                    "Unsupported literal value: " + rawValue,
                    exception
            );
        }
    }
}
