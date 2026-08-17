package com.yekdb.query.parser;

import com.yekdb.query.expression.BetweenExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.InExpression;
import com.yekdb.query.expression.LikeExpression;
import com.yekdb.query.expression.LikeOperator;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.expression.NotExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * WHERE / HAVING koşullarını Expression AST yapısına dönüştürür.
 *
 * Sprint 00-13:
 *
 * - Comparison
 * - AND
 * - OR
 * - NOT
 * - Parentheses
 * - Operator precedence
 *
 * Sprint 00-14:
 *
 * - BETWEEN
 * - NOT BETWEEN
 * - IN
 * - NOT IN
 * - LIKE
 * - NOT LIKE
 * - ILIKE
 * - NOT ILIKE
 *
 * Öncelik:
 *
 * Parentheses
 * Predicate / Comparison
 * NOT
 * AND
 * OR
 */
public final class ExpressionParser {

    /**
     * Expression parse eder.
     */
    public Expression parse(
            String expression
    ) {

        if (expression == null
                || expression.isBlank()) {

            throw new ParserException(
                    "Expression cannot be null or blank."
            );
        }

        String normalized =
                expression.trim();

        validateExpressionStructure(
                normalized
        );

        return parseOr(
                normalized
        );
    }

    // ==================================================
    // OR
    // ==================================================

    /**
     * OR en düşük öncelikli logical operatördür.
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
                        orIndex
                                + "OR".length()
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

    // ==================================================
    // AND
    // ==================================================

    /**
     * AND, OR'dan daha yüksek önceliklidir.
     *
     * BETWEEN içerisindeki AND logical AND değildir.
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
                        andIndex
                                + "AND".length()
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

    // ==================================================
    // NOT
    // ==================================================

    /**
     * Prefix NOT işlemini parse eder.
     *
     * Örnek:
     *
     * NOT active = true
     *
     * NOT (age > 18)
     *
     * NOT NOT active = true
     *
     * NOT BETWEEN / NOT IN / NOT LIKE ise
     * predicate'in kendi parserı tarafından işlenir.
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

    // ==================================================
    // PRIMARY
    // ==================================================

    /**
     * Predicate veya comparison seçimini yapar.
     *
     * Önemli:
     *
     * BETWEEN / IN / LIKE klasik comparison'dan
     * önce kontrol edilmelidir.
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

        /*
         * Parentheses
         */
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

        /*
         * BETWEEN / NOT BETWEEN
         */
        if (containsBetweenPredicate(
                normalized
        )) {

            return parseBetween(
                    normalized
            );
        }

        /*
         * IN / NOT IN
         */
        if (containsInPredicate(
                normalized
        )) {

            return parseIn(
                    normalized
            );
        }

        /*
         * LIKE / NOT LIKE
         * ILIKE / NOT ILIKE
         */
        if (containsLikePredicate(
                normalized
        )) {

            return parseLike(
                    normalized
            );
        }

        /*
         * =
         * !=
         * >
         * >=
         * <
         * <=
         */
        return parseComparison(
                normalized
        );
    }

    // ==================================================
    // BETWEEN
    // ==================================================

    /**
     * BETWEEN / NOT BETWEEN parse eder.
     *
     * age BETWEEN 18 AND 30
     *
     * age NOT BETWEEN 18 AND 30
     */
    private Expression parseBetween(
            String expression
    ) {

        String normalized =
                expression.trim();

        int betweenIndex =
                findKeywordOutsideQuotes(
                        normalized,
                        "BETWEEN"
                );

        if (betweenIndex <= 0) {

            throw new ParserException(
                    "Invalid BETWEEN expression: "
                            + expression
            );
        }

        String leftPart =
                normalized.substring(
                        0,
                        betweenIndex
                ).trim();

        boolean negated =
                false;

        /*
         * age NOT BETWEEN ...
         */
        if (endsWithKeyword(
                leftPart,
                "NOT"
        )) {

            negated =
                    true;

            leftPart =
                    leftPart.substring(
                            0,
                            leftPart.length()
                                    - "NOT".length()
                    ).trim();
        }

        validateColumnName(
                leftPart
        );

        String remaining =
                normalized.substring(
                        betweenIndex
                                + "BETWEEN".length()
                ).trim();

        int andIndex =
                findKeywordOutsideQuotes(
                        remaining,
                        "AND"
                );

        if (andIndex < 0) {

            throw new ParserException(
                    "BETWEEN expression must contain AND."
            );
        }

        String lowerRaw =
                remaining.substring(
                        0,
                        andIndex
                ).trim();

        String upperRaw =
                remaining.substring(
                        andIndex
                                + "AND".length()
                ).trim();

        if (lowerRaw.isBlank()) {

            throw new ParserException(
                    "BETWEEN lower bound cannot be empty."
            );
        }

        if (upperRaw.isBlank()) {

            throw new ParserException(
                    "BETWEEN upper bound cannot be empty."
            );
        }

        Object lowerBound =
                SqlLiteralParser.parseRaw(
                        lowerRaw
                );

        Object upperBound =
                SqlLiteralParser.parseRaw(
                        upperRaw
                );

        return new BetweenExpression(
                leftPart,
                lowerBound,
                upperBound,
                negated
        );
    }

    private boolean containsBetweenPredicate(
            String expression
    ) {

        return findKeywordOutsideQuotes(
                expression,
                "BETWEEN"
        ) > 0;
    }

    // ==================================================
    // IN
    // ==================================================

    /**
     * IN / NOT IN parse eder.
     *
     * city IN ('Malatya', 'Ankara')
     *
     * age NOT IN (18, 19, 20)
     */
    private Expression parseIn(
            String expression
    ) {

        String normalized =
                expression.trim();

        int inIndex =
                findKeywordOutsideQuotes(
                        normalized,
                        "IN"
                );

        if (inIndex <= 0) {

            throw new ParserException(
                    "Invalid IN expression: "
                            + expression
            );
        }

        String leftPart =
                normalized.substring(
                        0,
                        inIndex
                ).trim();

        boolean negated =
                false;

        /*
         * city NOT IN (...)
         */
        if (endsWithKeyword(
                leftPart,
                "NOT"
        )) {

            negated =
                    true;

            leftPart =
                    leftPart.substring(
                            0,
                            leftPart.length()
                                    - "NOT".length()
                    ).trim();
        }

        validateColumnName(
                leftPart
        );

        String valuesPart =
                normalized.substring(
                        inIndex
                                + "IN".length()
                ).trim();

        if (!valuesPart.startsWith("(")
                || !valuesPart.endsWith(")")) {

            throw new ParserException(
                    "IN expression values must be enclosed in parentheses."
            );
        }

        String innerValues =
                valuesPart.substring(
                        1,
                        valuesPart.length() - 1
                ).trim();

        if (innerValues.isBlank()) {

            throw new ParserException(
                    "IN expression must contain at least one value."
            );
        }

        List<String> rawValues =
                splitCommaSeparatedValues(
                        innerValues
                );

        List<Object> values =
                new ArrayList<>();

        for (String rawValue : rawValues) {

            if (rawValue.isBlank()) {

                throw new ParserException(
                        "IN expression cannot contain an empty value."
                );
            }

            values.add(
                    SqlLiteralParser.parseRaw(
                            rawValue
                    )
            );
        }

        return new InExpression(
                leftPart,
                values,
                negated
        );
    }

    private boolean containsInPredicate(
            String expression
    ) {

        return findKeywordOutsideQuotes(
                expression,
                "IN"
        ) > 0;
    }

    /**
     * IN listesini virgüllere böler.
     *
     * String literal içerisindeki virgüller bölünmez.
     *
     * Örnek:
     *
     * 'Malatya', 'Ankara'
     *
     * 'Kul, Yunus', 'Ali'
     */
    private List<String> splitCommaSeparatedValues(
            String expression
    ) {

        List<String> values =
                new ArrayList<>();

        StringBuilder currentValue =
                new StringBuilder();

        boolean insideSingleQuote =
                false;

        boolean insideDoubleQuote =
                false;

        int parenthesesDepth =
                0;

        for (int i = 0;
             i < expression.length();
             i++) {

            char current =
                    expression.charAt(i);

            if (current == '\''
                    && !insideDoubleQuote) {

                insideSingleQuote =
                        !insideSingleQuote;

                currentValue.append(
                        current
                );

                continue;
            }

            if (current == '"'
                    && !insideSingleQuote) {

                insideDoubleQuote =
                        !insideDoubleQuote;

                currentValue.append(
                        current
                );

                continue;
            }

            if (!insideSingleQuote
                    && !insideDoubleQuote) {

                if (current == '(') {

                    parenthesesDepth++;

                } else if (current == ')') {

                    parenthesesDepth--;

                } else if (current == ','
                        && parenthesesDepth == 0) {

                    values.add(
                            currentValue
                                    .toString()
                                    .trim()
                    );

                    currentValue.setLength(
                            0
                    );

                    continue;
                }
            }

            currentValue.append(
                    current
            );
        }

        values.add(
                currentValue
                        .toString()
                        .trim()
        );

        return values;
    }

    // ==================================================
    // LIKE / ILIKE
    // ==================================================

    /**
     * LIKE ailesini parse eder.
     *
     * name LIKE 'A%'
     *
     * name NOT LIKE 'A%'
     *
     * name ILIKE 'a%'
     *
     * name NOT ILIKE 'a%'
     */
    private Expression parseLike(
            String expression
    ) {

        String normalized =
                expression.trim();

        /*
         * Önce ILIKE aranmalı.
         *
         * matchesKeywordAt sayesinde LIKE,
         * ILIKE içerisinden yanlışlıkla yakalanmaz.
         */
        int operatorIndex =
                findKeywordOutsideQuotes(
                        normalized,
                        "ILIKE"
                );

        LikeOperator operator;

        String operatorKeyword;

        if (operatorIndex > 0) {

            operatorKeyword =
                    "ILIKE";

            String leftPart =
                    normalized.substring(
                            0,
                            operatorIndex
                    ).trim();

            boolean negated =
                    endsWithKeyword(
                            leftPart,
                            "NOT"
                    );

            if (negated) {

                operator =
                        LikeOperator.NOT_ILIKE;

            } else {

                operator =
                        LikeOperator.ILIKE;
            }

            return buildLikeExpression(
                    normalized,
                    operatorIndex,
                    operatorKeyword,
                    operator,
                    negated
            );
        }

        operatorIndex =
                findKeywordOutsideQuotes(
                        normalized,
                        "LIKE"
                );

        if (operatorIndex <= 0) {

            throw new ParserException(
                    "Invalid LIKE expression: "
                            + expression
            );
        }

        operatorKeyword =
                "LIKE";

        String leftPart =
                normalized.substring(
                        0,
                        operatorIndex
                ).trim();

        boolean negated =
                endsWithKeyword(
                        leftPart,
                        "NOT"
                );

        if (negated) {

            operator =
                    LikeOperator.NOT_LIKE;

        } else {

            operator =
                    LikeOperator.LIKE;
        }

        return buildLikeExpression(
                normalized,
                operatorIndex,
                operatorKeyword,
                operator,
                negated
        );
    }

    /**
     * LIKE expression ortak oluşturma işlemi.
     */
    private Expression buildLikeExpression(
            String expression,
            int operatorIndex,
            String operatorKeyword,
            LikeOperator operator,
            boolean negated
    ) {

        String columnPart =
                expression.substring(
                        0,
                        operatorIndex
                ).trim();

        /*
         * name NOT LIKE
         *      ^^^
         *
         * NOT bölümünü kolon isminden çıkar.
         */
        if (negated) {

            columnPart =
                    columnPart.substring(
                            0,
                            columnPart.length()
                                    - "NOT".length()
                    ).trim();
        }

        validateColumnName(
                columnPart
        );

        String rawPattern =
                expression.substring(
                        operatorIndex
                                + operatorKeyword.length()
                ).trim();

        if (rawPattern.isBlank()) {

            throw new ParserException(
                    "LIKE pattern cannot be empty."
            );
        }

        Object parsedPattern =
                SqlLiteralParser.parseRaw(
                        rawPattern
                );

        if (!(parsedPattern
                instanceof String pattern)) {

            throw new ParserException(
                    "LIKE pattern must be a string literal."
            );
        }

        return new LikeExpression(
                columnPart,
                pattern,
                operator
        );
    }

    private boolean containsLikePredicate(
            String expression
    ) {

        return findKeywordOutsideQuotes(
                expression,
                "ILIKE"
        ) > 0
                ||
                findKeywordOutsideQuotes(
                        expression,
                        "LIKE"
                ) > 0;
    }

    // ==================================================
    // NORMAL COMPARISON
    // ==================================================

    /**
     * Normal karşılaştırmalı expression.
     *
     * age > 18
     *
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

        } catch (
                IllegalArgumentException exception
        ) {

            throw new ParserException(
                    "Unsupported comparison operator: "
                            + comparison.symbol(),
                    exception
            );
        }

        Object expectedValue =
                SqlLiteralParser.parseRaw(
                        rawValue
                );

        return new ComparisonExpression(
                columnName,
                operator,
                expectedValue
        );
    }

    /**
     * Comparison operatörünü bulur.
     */
    private ParsedComparison findComparisonOperator(
            String expression
    ) {

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
     * Comparison operatorünü literal ve nested
     * parentheses dışında arar.
     */
    private int findComparisonOperatorIndex(
            String expression,
            String operator
    ) {

        boolean insideSingleQuote =
                false;

        boolean insideDoubleQuote =
                false;

        int parenthesesDepth =
                0;

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

    // ==================================================
    // LOGICAL OPERATOR SEARCH
    // ==================================================

    /**
     * AND / OR operatörünü yalnızca root seviyede arar.
     *
     * String literal ve parentheses içerisindeki
     * keywordler dikkate alınmaz.
     *
     * BETWEEN içindeki AND özel olarak atlanır.
     */
    private int findLogicalOperator(
            String expression,
            String operator
    ) {

        boolean insideSingleQuote =
                false;

        boolean insideDoubleQuote =
                false;

        int parenthesesDepth =
                0;

        /*
         * age BETWEEN 18 AND 30 AND active = true
         *
         * İlk AND BETWEEN'e aittir.
         */
        boolean betweenAndPending =
                false;

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

            if (parenthesesDepth != 0) {

                continue;
            }

            /*
             * BETWEEN gördüğümüzde sıradaki AND
             * logical operator değildir.
             */
            if (operator.equalsIgnoreCase(
                    "AND"
            )
                    && matchesKeywordAt(
                    expression,
                    i,
                    "BETWEEN"
            )) {

                betweenAndPending =
                        true;

                i += "BETWEEN".length()
                        - 1;

                continue;
            }

            if (!matchesKeywordAt(
                    expression,
                    i,
                    operator
            )) {

                continue;
            }

            if (operator.equalsIgnoreCase(
                    "AND"
            )
                    && betweenAndPending) {

                betweenAndPending =
                        false;

                i += operator.length()
                        - 1;

                continue;
            }

            return i;
        }

        return -1;
    }

    // ==================================================
    // KEYWORD HELPERS
    // ==================================================

    /**
     * Keyword'ü quote ve nested parentheses dışında arar.
     */
    private int findKeywordOutsideQuotes(
            String expression,
            String keyword
    ) {

        boolean insideSingleQuote =
                false;

        boolean insideDoubleQuote =
                false;

        int parenthesesDepth =
                0;

        for (int i = 0;
             i <= expression.length()
                     - keyword.length();
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

            if (matchesKeywordAt(
                    expression,
                    i,
                    keyword
            )) {

                return i;
            }
        }

        return -1;
    }

    /**
     * Belirtilen konumda keyword var mı kontrol eder.
     */
    private boolean matchesKeywordAt(
            String expression,
            int index,
            String keyword
    ) {

        if (index < 0
                || index + keyword.length()
                > expression.length()) {

            return false;
        }

        if (!expression.regionMatches(
                true,
                index,
                keyword,
                0,
                keyword.length()
        )) {

            return false;
        }

        boolean validLeftBoundary =
                index == 0
                        || !isIdentifierCharacter(
                        expression.charAt(
                                index - 1
                        )
                );

        int endIndex =
                index
                        + keyword.length();

        boolean validRightBoundary =
                endIndex
                        == expression.length()
                        || !isIdentifierCharacter(
                        expression.charAt(
                                endIndex
                        )
                );

        return validLeftBoundary
                && validRightBoundary;
    }

    /**
     * Expression keyword ile başlıyor mu?
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
     * Expression keyword ile bitiyor mu?
     */
    private boolean endsWithKeyword(
            String expression,
            String keyword
    ) {

        String normalized =
                expression.trim();

        if (normalized.length()
                < keyword.length()) {

            return false;
        }

        int start =
                normalized.length()
                        - keyword.length();

        if (!normalized.regionMatches(
                true,
                start,
                keyword,
                0,
                keyword.length()
        )) {

            return false;
        }

        return start == 0
                || !isIdentifierCharacter(
                normalized.charAt(
                        start - 1
                )
        );
    }

    // ==================================================
    // PARENTHESES
    // ==================================================

    /**
     * Expression tamamen parantezlerle sarılmış mı?
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

        boolean insideSingleQuote =
                false;

        boolean insideDoubleQuote =
                false;

        int depth =
                0;

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

                if (depth == 0
                        && i
                        < expression.length() - 1) {

                    return false;
                }
            }
        }

        return depth == 0;
    }

    // ==================================================
    // LITERAL
    // ==================================================

    /**
     * SQL literal değerini Java değerine dönüştürür.
     */


    // ==================================================
    // COLUMN VALIDATION
    // ==================================================

    /**
     * Kolon ismini doğrular.
     *
     * Destek:
     *
     * age
     * department
     * u.age
     * employees.salary
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
                        + "(\\.[A-Za-z_][A-Za-z0-9_]*)?"
        )) {

            throw new ParserException(
                    "Invalid column name in expression: "
                            + columnName
            );
        }
    }

    private boolean isIdentifierCharacter(
            char character
    ) {

        return Character.isLetterOrDigit(
                character
        )
                || character == '_';
    }

    // ==================================================
    // STRUCTURE VALIDATION
    // ==================================================

    /**
     * Quote ve parentheses yapısını doğrular.
     */
    private void validateExpressionStructure(
            String expression
    ) {

        boolean insideSingleQuote =
                false;

        boolean insideDoubleQuote =
                false;

        int parenthesesDepth =
                0;

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
                    "Unclosed string literal in expression."
            );
        }

        if (parenthesesDepth != 0) {

            throw new ParserException(
                    "Unbalanced parentheses in expression."
            );
        }
    }

    // ==================================================
    // INTERNAL RECORD
    // ==================================================

    private record ParsedComparison(
            String symbol,
            int index
    ) {
    }
}