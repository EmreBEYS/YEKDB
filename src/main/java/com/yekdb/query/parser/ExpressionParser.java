package com.yekdb.query.parser;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;

/**
 * WHERE koşullarını Expression nesnelerine dönüştürür.
 *
 * Sprint 00-12 kapsamında ilk aşamada
 * tek karşılaştırmalı ifadeler desteklenmektedir.
 *
 * Örnekler:
 *
 * id = 1
 * age > 18
 * name != 'Ali'
 * active = true
 */
public final class ExpressionParser {

    public Expression parse(String whereClause) {

        if (whereClause == null
                || whereClause.isBlank()) {

            throw new ParserException(
                    "WHERE clause cannot be null or blank."
            );
        }

        String normalized =
                whereClause.trim();

        ParsedComparison comparison =
                findComparisonOperator(
                        normalized
                );

        String columnName =
                normalized.substring(
                        0,
                        comparison.index()
                ).trim();

        String rawValue =
                normalized.substring(
                        comparison.index()
                                + comparison.symbol().length()
                ).trim();

        validateColumnName(
                columnName
        );

        if (rawValue.isBlank()) {
            throw new ParserException(
                    "Comparison value cannot be empty."
            );
        }

        ComparisonOperator operator;

        try {
            operator =
                    ComparisonOperator.fromSymbol(
                            comparison.symbol()
                    );

        } catch (IllegalArgumentException exception) {

            throw new ParserException(
                    "Unsupported comparison operator: "
                            + comparison.symbol(),
                    exception
            );
        }

        Object expectedValue =
                parseLiteral(
                        rawValue
                );

        return new ComparisonExpression(
                columnName,
                operator,
                expectedValue
        );
    }

    private ParsedComparison findComparisonOperator(
            String expression
    ) {

        /*
         * Uzun operatörler önce kontrol edilmelidir.
         *
         * >= içindeki > karakterinin
         * erken eşleşmesini engeller.
         */
        String[] operators = {
                ">=",
                "<=",
                "!=",
                "=",
                ">",
                "<"
        };

        for (String operator : operators) {

            int index =
                    expression.indexOf(
                            operator
                    );

            if (index > 0) {

                return new ParsedComparison(
                        operator,
                        index
                );
            }
        }

        throw new ParserException(
                "No supported comparison operator found in WHERE clause: "
                        + expression
        );
    }

    private Object parseLiteral(
            String rawValue
    ) {

        String value =
                rawValue.trim();

        /*
         * String literal
         */
        if ((value.startsWith("'")
                && value.endsWith("'"))
                ||
                (value.startsWith("\"")
                        && value.endsWith("\""))) {

            if (value.length() < 2) {
                throw new ParserException(
                        "Invalid string literal: "
                                + rawValue
                );
            }

            return value.substring(
                    1,
                    value.length() - 1
            );
        }

        /*
         * Boolean
         */
        if (value.equalsIgnoreCase(
                "true"
        )) {
            return true;
        }

        if (value.equalsIgnoreCase(
                "false"
        )) {
            return false;
        }

        /*
         * NULL
         *
         * Expression seviyesinde kabul ediyoruz.
         * Storage/Row katmanının NULL desteği
         * daha sonra genişletilebilir.
         */
        if (value.equalsIgnoreCase(
                "null"
        )) {
            return null;
        }

        /*
         * Double
         */
        if (value.contains(".")) {

            try {
                return Double.parseDouble(
                        value
                );

            } catch (NumberFormatException exception) {

                throw new ParserException(
                        "Invalid numeric literal: "
                                + rawValue,
                        exception
                );
            }
        }

        /*
         * Integer / Long
         */
        try {

            long longValue =
                    Long.parseLong(
                            value
                    );

            if (longValue >= Integer.MIN_VALUE
                    && longValue <= Integer.MAX_VALUE) {

                return (int) longValue;
            }

            return longValue;

        } catch (NumberFormatException exception) {

            throw new ParserException(
                    "Unsupported literal value: "
                            + rawValue,
                    exception
            );
        }
    }

    private void validateColumnName(
            String columnName
    ) {

        if (columnName == null
                || columnName.isBlank()) {

            throw new ParserException(
                    "Column name cannot be null or blank."
            );
        }

        if (!columnName.matches(
                "[A-Za-z_][A-Za-z0-9_]*"
        )) {

            throw new ParserException(
                    "Invalid column name in WHERE clause: "
                            + columnName
            );
        }
    }

    private record ParsedComparison(
            String symbol,
            int index
    ) {
    }
}