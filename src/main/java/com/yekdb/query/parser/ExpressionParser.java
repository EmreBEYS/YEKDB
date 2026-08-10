package com.yekdb.query.parser;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.expression.NotExpression;

/**
 * WHERE koşullarını Expression AST yapısına dönüştürür.
 *
 * Sprint 00-13
 *
 * Desteklenen yapılar:
 *
 * - Comparison expressions
 * - AND
 * - OR
 * - NOT
 * - Parentheses
 * - Operator precedence
 *
 * Öncelik:
 *
 * Parentheses
 * Comparison
 * NOT
 * AND
 * OR
 *
 * Örnekler:
 *
 * age > 18
 *
 * age > 18 AND city = 'Malatya'
 *
 * age > 18 OR city = 'Malatya'
 *
 * NOT active = false
 *
 * (age > 18 AND active = true)
 * OR city = 'Malatya'
 */
public final class ExpressionParser {

    /**
     * WHERE ifadesini parse eder.
     */
    public Expression parse(
            String whereClause
    ) {

        if (whereClause == null
                || whereClause.isBlank()) {

            throw new ParserException(
                    "WHERE clause cannot be null or blank."
            );
        }

        String normalized =
                whereClause.trim();

        validateExpressionStructure(
                normalized
        );

        return parseOr(
                normalized
        );
    }

    /**
     * OR en düşük öncelikli logical operatördür.
     *
     * Örnek:
     *
     * age > 18 AND active = true
     * OR
     * city = 'Malatya'
     */
    private Expression parseOr(
            String expression
    ) {

        String normalized =
                expression.trim();

        int orIndex =
                findLogicalOperator(
                        normalized,
                        "OR"
                );

        if (orIndex < 0) {

            return parseAnd(
                    normalized
            );
        }

        String leftPart =
                normalized.substring(
                        0,
                        orIndex
                ).trim();

        String rightPart =
                normalized.substring(
                        orIndex + "OR".length()
                ).trim();

        if (leftPart.isBlank()) {

            throw new ParserException(
                    "Left side of OR expression cannot be empty."
            );
        }

        if (rightPart.isBlank()) {

            throw new ParserException(
                    "Right side of OR expression cannot be empty."
            );
        }

        Expression leftExpression =
                parseAnd(
                        leftPart
                );

        Expression rightExpression =
                parseOr(
                        rightPart
                );

        return new LogicalExpression(
                leftExpression,
                LogicalOperator.OR,
                rightExpression
        );
    }

    /**
     * AND, OR operatöründen daha yüksek önceliğe sahiptir.
     */
    private Expression parseAnd(
            String expression
    ) {

        String normalized =
                expression.trim();

        int andIndex =
                findLogicalOperator(
                        normalized,
                        "AND"
                );

        if (andIndex < 0) {

            return parseNot(
                    normalized
            );
        }

        String leftPart =
                normalized.substring(
                        0,
                        andIndex
                ).trim();

        String rightPart =
                normalized.substring(
                        andIndex + "AND".length()
                ).trim();

        if (leftPart.isBlank()) {

            throw new ParserException(
                    "Left side of AND expression cannot be empty."
            );
        }

        if (rightPart.isBlank()) {

            throw new ParserException(
                    "Right side of AND expression cannot be empty."
            );
        }

        Expression leftExpression =
                parseNot(
                        leftPart
                );

        Expression rightExpression =
                parseAnd(
                        rightPart
                );

        return new LogicalExpression(
                leftExpression,
                LogicalOperator.AND,
                rightExpression
        );
    }

    /**
     * NOT operatörünü işler.
     *
     * Recursive yapı sayesinde:
     *
     * NOT NOT active = true
     *
     * gibi ifadeler desteklenir.
     */
    private Expression parseNot(
            String expression
    ) {

        String normalized =
                expression.trim();

        if (!startsWithLogicalKeyword(
                normalized,
                "NOT"
        )) {

            return parsePrimary(
                    normalized
            );
        }

        String remaining =
                normalized.substring(
                        "NOT".length()
                ).trim();

        if (remaining.isBlank()) {

            throw new ParserException(
                    "NOT expression cannot be empty."
            );
        }

        return new NotExpression(
                parseNot(
                        remaining
                )
        );
    }

    /**
     * Parentheses yapısını işler.
     *
     * Örnek:
     *
     * (age > 18 AND city = 'Malatya')
     */
    private Expression parsePrimary(
            String expression
    ) {

        String normalized =
                expression.trim();

        if (normalized.isBlank()) {

            throw new ParserException(
                    "Expression cannot be empty."
            );
        }

        if (isWrappedByParentheses(
                normalized
        )) {

            String innerExpression =
                    normalized.substring(
                            1,
                            normalized.length() - 1
                    ).trim();

            if (innerExpression.isBlank()) {

                throw new ParserException(
                        "Parenthesized expression cannot be empty."
                );
            }

            return parseOr(
                    innerExpression
            );
        }

        return parseComparison(
                normalized
        );
    }

    /**
     * Tek karşılaştırmalı expression oluşturur.
     *
     * Örnek:
     *
     * age > 18
     * city = 'Malatya'
     */
    private Expression parseComparison(
            String expression
    ) {

        String normalized =
                expression.trim();

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

    /**
     * AND / OR operatörünü sadece expression'ın
     * ana seviyesinde arar.
     *
     * String literal veya parentheses içerisindeki
     * logical operatörler dikkate alınmaz.
     */
    private int findLogicalOperator(
            String expression,
            String operator
    ) {

        boolean insideSingleQuote = false;
        boolean insideDoubleQuote = false;

        int parenthesesDepth = 0;

        for (int i = 0;
             i <= expression.length()
                     - operator.length();
             i++) {

            char current =
                    expression.charAt(i);

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

            if (current == '(') {

                parenthesesDepth++;

                continue;
            }

            if (current == ')') {

                parenthesesDepth--;

                if (parenthesesDepth < 0) {

                    throw new ParserException(
                            "Unexpected closing parenthesis."
                    );
                }

                continue;
            }

            /*
             * Sadece root seviyesindeki
             * AND / OR operatörleri aranır.
             */
            if (parenthesesDepth != 0) {

                continue;
            }

            boolean matches =
                    expression.regionMatches(
                            true,
                            i,
                            operator,
                            0,
                            operator.length()
                    );

            if (!matches) {

                continue;
            }

            boolean validLeftBoundary =
                    i == 0
                            || !isIdentifierCharacter(
                            expression.charAt(
                                    i - 1
                            )
                    );

            int endIndex =
                    i + operator.length();

            boolean validRightBoundary =
                    endIndex
                            == expression.length()
                            || !isIdentifierCharacter(
                            expression.charAt(
                                    endIndex
                            )
                    );

            if (validLeftBoundary
                    && validRightBoundary) {

                return i;
            }
        }

        return -1;
    }

    /**
     * Expression'ın logical keyword ile
     * başlayıp başlamadığını kontrol eder.
     *
     * NOT active = true  -> true
     *
     * notification = 1  -> false
     */
    private boolean startsWithLogicalKeyword(
            String expression,
            String keyword
    ) {

        if (expression.length()
                < keyword.length()) {

            return false;
        }

        if (!expression.regionMatches(
                true,
                0,
                keyword,
                0,
                keyword.length()
        )) {

            return false;
        }

        if (expression.length()
                == keyword.length()) {

            return true;
        }

        char nextCharacter =
                expression.charAt(
                        keyword.length()
                );

        return !isIdentifierCharacter(
                nextCharacter
        );
    }

    /**
     * Bir expression'ın tamamen
     * parentheses ile sarılı olup olmadığını kontrol eder.
     *
     * (age > 18)                  -> true
     *
     * (age > 18 AND active=true) -> true
     *
     * (age > 18) OR active=true  -> false
     */
    private boolean isWrappedByParentheses(
            String expression
    ) {

        if (expression.length() < 2
                || expression.charAt(0) != '('
                || expression.charAt(
                expression.length() - 1
        ) != ')') {

            return false;
        }

        boolean insideSingleQuote = false;
        boolean insideDoubleQuote = false;

        int depth = 0;

        for (int i = 0;
             i < expression.length();
             i++) {

            char current =
                    expression.charAt(i);

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

            if (current == '(') {

                depth++;
            }

            if (current == ')') {

                depth--;

                /*
                 * İlk açılan parantez expression bitmeden
                 * kapanıyorsa ifade tamamen parantezle
                 * sarılmış değildir.
                 */
                if (depth == 0
                        && i
                        < expression.length() - 1) {

                    return false;
                }
            }
        }

        return depth == 0;
    }

    /**
     * Identifier karakterlerini kontrol eder.
     */
    private boolean isIdentifierCharacter(
            char character
    ) {

        return Character.isLetterOrDigit(
                character
        )
                || character == '_';
    }

    /**
     * Karşılaştırma operatörünü bulur.
     */
    private ParsedComparison findComparisonOperator(
            String expression
    ) {

        /*
         * Uzun operatörler önce kontrol edilmelidir.
         *
         * >= içindeki >
         * <= içindeki <
         *
         * karakterlerinin erken eşleşmesini engeller.
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
                    findComparisonOperatorIndex(
                            expression,
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

    /**
     * Comparison operatorünü literal dışında arar.
     */
    private int findComparisonOperatorIndex(
            String expression,
            String operator
    ) {

        boolean insideSingleQuote = false;
        boolean insideDoubleQuote = false;

        int parenthesesDepth = 0;

        for (int i = 0;
             i <= expression.length()
                     - operator.length();
             i++) {

            char current =
                    expression.charAt(i);

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

            if (current == '(') {

                parenthesesDepth++;

                continue;
            }

            if (current == ')') {

                parenthesesDepth--;

                continue;
            }

            if (parenthesesDepth != 0) {

                continue;
            }

            if (expression.startsWith(
                    operator,
                    i
            )) {

                return i;
            }
        }

        return -1;
    }

    /**
     * SQL literal değerini Java nesnesine dönüştürür.
     */
    private Object parseLiteral(
            String rawValue
    ) {

        String value =
                rawValue.trim();

        /*
         * String
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

    /**
     * Column ismini doğrular.
     */
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

    /**
     * Parentheses ve quote yapılarının
     * temel doğrulamasını gerçekleştirir.
     */
    private void validateExpressionStructure(
            String expression
    ) {

        boolean insideSingleQuote = false;
        boolean insideDoubleQuote = false;

        int parenthesesDepth = 0;

        for (int i = 0;
             i < expression.length();
             i++) {

            char current =
                    expression.charAt(i);

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

            if (current == '(') {

                parenthesesDepth++;
            }

            if (current == ')') {

                parenthesesDepth--;

                if (parenthesesDepth < 0) {

                    throw new ParserException(
                            "Unexpected closing parenthesis."
                    );
                }
            }
        }

        if (insideSingleQuote
                || insideDoubleQuote) {

            throw new ParserException(
                    "Unclosed string literal in WHERE clause."
            );
        }

        if (parenthesesDepth != 0) {

            throw new ParserException(
                    "Unbalanced parentheses in WHERE clause."
            );
        }
    }

    /**
     * Bulunan comparison operator bilgisini tutar.
     */
    private record ParsedComparison(
            String symbol,
            int index
    ) {
    }
}