package com.yekdb.query.parser;

import com.yekdb.query.expression.Expression;
import com.yekdb.query.statement.DeleteStatement;
import com.yekdb.query.statement.FetchClause;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.HavingClause;
import com.yekdb.query.statement.InsertStatement;
import com.yekdb.query.statement.LimitClause;
import com.yekdb.query.statement.OrderByItem;
import com.yekdb.query.statement.SelectItem;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.SortDirection;
import com.yekdb.query.statement.Statement;
import com.yekdb.query.statement.TableReference;
import com.yekdb.query.statement.UpdateStatement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SQL metnini ayrıştırarak uygun Statement nesnesini üretir.
 *
 * Desteklenen temel sorgular:
 *
 * - INSERT
 * - SELECT
 * - UPDATE
 * - DELETE
 *
 * Sprint 00-14 SELECT desteği:
 *
 * - Table alias
 * - Column alias
 * - Qualified columns
 * - BETWEEN / NOT BETWEEN
 * - IN / NOT IN
 * - LIKE / NOT LIKE
 * - ILIKE / NOT ILIKE
 * - COUNT
 * - SUM
 * - AVG
 * - MIN
 * - MAX
 * - GROUP BY
 * - HAVING
 * - ORDER BY
 * - LIMIT
 * - FETCH FIRST
 * - FETCH NEXT
 */
public final class SqlParser {

    private final SqlTokenizer tokenizer;

    private List<SqlToken> tokens;
    private int currentPosition;

    /**
     * Varsayılan tokenizer ile parser oluşturur.
     */
    public SqlParser() {

        this(
                new SqlTokenizer()
        );
    }

    /**
     * Dışarıdan tokenizer verilerek parser oluşturur.
     */
    public SqlParser(
            SqlTokenizer tokenizer
    ) {

        this.tokenizer =
                Objects.requireNonNull(
                        tokenizer,
                        "SqlTokenizer cannot be null."
                );
    }

    /**
     * SQL metnini parse eder.
     */
    public Statement parse(
            String sql
    ) {

        tokens =
                tokenizer.tokenize(
                        sql
                );

        currentPosition = 0;

        Statement statement =
                switch (
                        currentToken().getType()
                        ) {

                    case INSERT ->
                            parseInsert();

                    case SELECT ->
                            parseSelect();

                    case UPDATE ->
                            parseUpdate();

                    case DELETE ->
                            parseDelete();

                    default ->
                            throw error(
                                    "Unsupported SQL statement: "
                                            + currentToken()
                                            .getValue()
                            );
                };

        consumeOptionalSemicolon();

        expect(
                SqlTokenType.END_OF_INPUT,
                "Unexpected token after SQL statement."
        );

        return statement;
    }

    // ==================================================
    // INSERT
    // ==================================================

    /**
     * INSERT sorgusunu ayrıştırır.
     *
     * Örnek:
     *
     * INSERT INTO users (id, name, age)
     * VALUES (1, 'Emre', 21);
     */
    private InsertStatement parseInsert() {

        expect(
                SqlTokenType.INSERT,
                "Expected INSERT keyword."
        );

        expect(
                SqlTokenType.INTO,
                "Expected INTO after INSERT."
        );

        String tableName =
                consumeIdentifier(
                        "Expected table name after INTO."
                );

        expect(
                SqlTokenType.LEFT_PARENTHESIS,
                "Expected '(' before INSERT column list."
        );

        List<String> columns =
                new ArrayList<>();

        if (check(
                SqlTokenType.RIGHT_PARENTHESIS
        )) {

            throw error(
                    "INSERT statement must contain at least one column."
            );
        }

        columns.add(
                consumeIdentifier(
                        "Expected column name in INSERT column list."
                )
        );

        while (match(
                SqlTokenType.COMMA
        )) {

            columns.add(
                    consumeIdentifier(
                            "Expected column name after ','."
                    )
            );
        }

        expect(
                SqlTokenType.RIGHT_PARENTHESIS,
                "Expected ')' after INSERT column list."
        );

        /*
         * Duplicate kolon kontrolü.
         */
        long distinctColumnCount =
                columns.stream()
                        .map(
                                String::toLowerCase
                        )
                        .distinct()
                        .count();

        if (distinctColumnCount
                != columns.size()) {

            throw error(
                    "INSERT column list contains duplicate columns."
            );
        }

        expect(
                SqlTokenType.VALUES,
                "Expected VALUES after INSERT column list."
        );

        expect(
                SqlTokenType.LEFT_PARENTHESIS,
                "Expected '(' after VALUES."
        );

        List<Object> values =
                new ArrayList<>();

        if (!check(
                SqlTokenType.RIGHT_PARENTHESIS
        )) {

            do {

                values.add(
                        parseLiteralValue()
                );

            } while (
                    match(
                            SqlTokenType.COMMA
                    )
            );
        }

        expect(
                SqlTokenType.RIGHT_PARENTHESIS,
                "Expected ')' after INSERT values."
        );

        if (values.isEmpty()) {

            throw error(
                    "INSERT statement must contain at least one value."
            );
        }

        if (columns.size()
                != values.size()) {

            throw error(
                    "INSERT column count and value count must be equal. "
                            + "Columns: "
                            + columns.size()
                            + ", values: "
                            + values.size()
                            + "."
            );
        }

        return new InsertStatement(
                tableName,
                columns,
                values
        );
    }

    // ==================================================
    // SELECT
    // ==================================================

    /**
     * SELECT sorgusunu ayrıştırır.
     *
     * Clause sırası:
     *
     * SELECT
     * FROM
     * WHERE
     * GROUP BY
     * HAVING
     * ORDER BY
     * LIMIT / FETCH
     */
    private SelectStatement parseSelect() {

        expect(
                SqlTokenType.SELECT,
                "Expected SELECT keyword."
        );

        // ----------------------------------------------
        // SELECT ITEMS
        // ----------------------------------------------

        List<SelectItem> selectItems =
                new ArrayList<>();

        /*
         * SELECT *
         */
        if (match(
                SqlTokenType.ASTERISK
        )) {

            selectItems.add(
                    new SelectItem(
                            "*"
                    )
            );

        } else {

            selectItems.add(
                    parseSelectItem()
            );

            while (match(
                    SqlTokenType.COMMA
            )) {

                selectItems.add(
                        parseSelectItem()
                );
            }
        }

        // ----------------------------------------------
        // FROM
        // ----------------------------------------------

        expect(
                SqlTokenType.FROM,
                "Expected FROM after selected columns."
        );

        String tableName =
                consumeIdentifier(
                        "Expected table name after FROM."
                );

        String tableAlias =
                null;

        /*
         * FROM users AS u
         */
        if (match(
                SqlTokenType.AS
        )) {

            tableAlias =
                    consumeIdentifier(
                            "Expected table alias after AS."
                    );

            /*
             * FROM users u
             */
        } else if (check(
                SqlTokenType.IDENTIFIER
        )) {

            tableAlias =
                    consumeIdentifier(
                            "Expected table alias."
                    );
        }

        TableReference table =
                new TableReference(
                        tableName,
                        tableAlias
                );

        // ----------------------------------------------
        // WHERE
        // ----------------------------------------------

        Expression whereExpression =
                null;

        if (match(
                SqlTokenType.WHERE
        )) {

            String whereClause =
                    readSelectClauseExpression();

            whereExpression =
                    new ExpressionParser()
                            .parse(
                                    whereClause
                            );
        }

        // ----------------------------------------------
        // GROUP BY
        // ----------------------------------------------

        GroupByClause groupByClause =
                null;

        if (match(
                SqlTokenType.GROUP
        )) {

            expect(
                    SqlTokenType.BY,
                    "Expected BY after GROUP."
            );

            List<String> groupByColumns =
                    new ArrayList<>();

            /*
             * GROUP BY department
             */
            groupByColumns.add(
                    parseColumnReference()
            );

            /*
             * GROUP BY department, city
             */
            while (match(
                    SqlTokenType.COMMA
            )) {

                groupByColumns.add(
                        parseColumnReference()
                );
            }

            groupByClause =
                    new GroupByClause(
                            groupByColumns
                    );
        }

        // ----------------------------------------------
        // HAVING
        // ----------------------------------------------

        HavingClause havingClause =
                null;

        if (match(
                SqlTokenType.HAVING
        )) {

            if (groupByClause == null) {

                throw error(
                        "HAVING requires GROUP BY."
                );
            }

            String havingClauseText =
                    readSelectClauseExpression();

            Expression havingExpression =
                    new ExpressionParser()
                            .parse(
                                    havingClauseText
                            );

            havingClause =
                    new HavingClause(
                            havingExpression
                    );
        }

        // ----------------------------------------------
        // ORDER BY
        // ----------------------------------------------

        List<OrderByItem> orderByItems =
                new ArrayList<>();

        if (match(
                SqlTokenType.ORDER
        )) {

            expect(
                    SqlTokenType.BY,
                    "Expected BY after ORDER."
            );

            orderByItems.add(
                    parseOrderByItem()
            );

            while (match(
                    SqlTokenType.COMMA
            )) {

                orderByItems.add(
                        parseOrderByItem()
                );
            }
        }

        // ----------------------------------------------
        // LIMIT
        // ----------------------------------------------

        LimitClause limitClause =
                null;

        FetchClause fetchClause =
                null;

        if (match(
                SqlTokenType.LIMIT
        )) {

            int rowCount =
                    parseRowCount(
                            "Expected integer value after LIMIT."
                    );

            limitClause =
                    new LimitClause(
                            rowCount
                    );
        }

        // ----------------------------------------------
        // FETCH
        // ----------------------------------------------

        if (match(
                SqlTokenType.FETCH
        )) {

            FetchClause.Mode mode;

            if (match(
                    SqlTokenType.FIRST
            )) {

                mode =
                        FetchClause.Mode.FIRST;

            } else if (match(
                    SqlTokenType.NEXT
            )) {

                mode =
                        FetchClause.Mode.NEXT;

            } else {

                throw error(
                        "Expected FIRST or NEXT after FETCH."
                );
            }

            int rowCount =
                    parseRowCount(
                            "Expected row count after FETCH FIRST/NEXT."
                    );

            /*
             * FETCH FIRST 10 ROW ONLY
             * FETCH FIRST 10 ROWS ONLY
             *
             * İkisini de kabul ediyoruz.
             */
            if (!match(
                    SqlTokenType.ROW
            )
                    && !match(
                    SqlTokenType.ROWS
            )) {

                throw error(
                        "Expected ROW or ROWS after FETCH row count."
                );
            }

            expect(
                    SqlTokenType.ONLY,
                    "Expected ONLY after FETCH ROW/ROWS."
            );

            fetchClause =
                    new FetchClause(
                            mode,
                            rowCount
                    );
        }

        return new SelectStatement(
                table,
                selectItems,
                whereExpression,
                groupByClause,
                havingClause,
                orderByItems,
                limitClause,
                fetchClause
        );
    }

    // ==================================================
    // SELECT ITEM
    // ==================================================

    /**
     * SELECT item parse eder.
     *
     * Destek:
     *
     * name
     * u.name
     * name AS username
     * name username
     *
     * COUNT(*)
     * COUNT(*) AS total
     * SUM(salary)
     * AVG(age)
     * MIN(age)
     * MAX(age)
     */
    private SelectItem parseSelectItem() {

        String expression;

        if (isAggregateFunction()) {

            expression =
                    parseAggregateExpression();

        } else {

            expression =
                    parseColumnReference();
        }

        String alias =
                null;

        /*
         * SELECT name AS username
         *
         * SELECT COUNT(*) AS total
         */
        if (match(
                SqlTokenType.AS
        )) {

            alias =
                    consumeIdentifier(
                            "Expected column alias after AS."
                    );

            /*
             * SELECT name username
             *
             * SELECT COUNT(*) total
             */
        } else if (check(
                SqlTokenType.IDENTIFIER
        )) {

            alias =
                    consumeIdentifier(
                            "Expected column alias."
                    );
        }

        return new SelectItem(
                expression,
                alias
        );
    }

    // ==================================================
    // AGGREGATE FUNCTIONS
    // ==================================================

    /**
     * Mevcut tokenın aggregate fonksiyon başlangıcı
     * olup olmadığını kontrol eder.
     *
     * COUNT(
     * SUM(
     * AVG(
     * MIN(
     * MAX(
     */
    private boolean isAggregateFunction() {

        if (!check(
                SqlTokenType.IDENTIFIER
        )) {

            return false;
        }

        String value =
                currentToken()
                        .getValue()
                        .toUpperCase();

        boolean aggregateFunction =
                value.equals(
                        "COUNT"
                )
                        || value.equals(
                        "SUM"
                )
                        || value.equals(
                        "AVG"
                )
                        || value.equals(
                        "MIN"
                )
                        || value.equals(
                        "MAX"
                );

        if (!aggregateFunction) {

            return false;
        }

        return checkNext(
                SqlTokenType.LEFT_PARENTHESIS
        );
    }

    /**
     * Aggregate expression parse eder.
     *
     * COUNT(*)
     * COUNT(id)
     * SUM(salary)
     * AVG(age)
     * MIN(age)
     * MAX(age)
     *
     * Qualified column:
     *
     * SUM(e.salary)
     */
    private String parseAggregateExpression() {

        String functionName =
                currentToken()
                        .getValue()
                        .toUpperCase();

        advance();

        expect(
                SqlTokenType.LEFT_PARENTHESIS,
                "Expected '(' after aggregate function."
        );

        String argument;

        /*
         * COUNT(*)
         */
        if (match(
                SqlTokenType.ASTERISK
        )) {

            if (!functionName.equals(
                    "COUNT"
            )) {

                throw error(
                        functionName
                                + "(*) is not supported. "
                                + "Only COUNT(*) accepts '*'."
                );
            }

            argument = "*";

        } else {

            /*
             * COUNT(id)
             * SUM(salary)
             * AVG(e.salary)
             */
            argument =
                    parseColumnReference();
        }

        expect(
                SqlTokenType.RIGHT_PARENTHESIS,
                "Expected ')' after aggregate argument."
        );

        return functionName
                + "("
                + argument
                + ")";
    }

    // ==================================================
    // COLUMN REFERENCE
    // ==================================================

    /**
     * Kolon referansı parse eder.
     *
     * name
     *
     * veya
     *
     * u.name
     */
    private String parseColumnReference() {

        String firstPart =
                consumeIdentifier(
                        "Expected column name."
                );

        /*
         * Basit kolon:
         *
         * name
         */
        if (!match(
                SqlTokenType.DOT
        )) {

            return firstPart;
        }

        /*
         * Qualified column:
         *
         * u.name
         */
        String secondPart =
                consumeIdentifier(
                        "Expected column name after '.'."
                );

        return firstPart
                + "."
                + secondPart;
    }

    // ==================================================
    // ORDER BY
    // ==================================================

    /**
     * ORDER BY item parse eder.
     *
     * age
     * age ASC
     * age DESC
     * u.age DESC
     */
    private OrderByItem parseOrderByItem() {

        String columnName =
                parseColumnReference();

        SortDirection direction =
                SortDirection.ASC;

        if (match(
                SqlTokenType.ASC
        )) {

            direction =
                    SortDirection.ASC;

        } else if (match(
                SqlTokenType.DESC
        )) {

            direction =
                    SortDirection.DESC;
        }

        return new OrderByItem(
                columnName,
                direction
        );
    }

    // ==================================================
    // LIMIT / FETCH
    // ==================================================

    /**
     * LIMIT / FETCH satır sayısını parse eder.
     *
     * Yalnızca 0 veya pozitif integer kabul edilir.
     */
    private int parseRowCount(
            String errorMessage
    ) {

        if (!check(
                SqlTokenType.NUMBER_LITERAL
        )) {

            throw error(
                    errorMessage
            );
        }

        SqlToken token =
                advance();

        String rawValue =
                token.getValue();

        /*
         * LIMIT 10.5 geçersiz.
         */
        if (rawValue.contains(
                "."
        )) {

            throw error(
                    "Row count must be an integer: "
                            + rawValue
            );
        }

        try {

            long value =
                    Long.parseLong(
                            rawValue
                    );

            if (value < 0) {

                throw error(
                        "Row count cannot be negative."
                );
            }

            if (value > Integer.MAX_VALUE) {

                throw error(
                        "Row count is too large: "
                                + rawValue
                );
            }

            return (int) value;

        } catch (
                NumberFormatException exception
        ) {

            throw new ParserException(
                    "Invalid row count: "
                            + rawValue,
                    exception
            );
        }
    }

    // ==================================================
    // UPDATE
    // ==================================================

    /**
     * UPDATE sorgusunu ayrıştırır.
     *
     * UPDATE users
     * SET name = 'Emre', age = 22
     * WHERE id = 1;
     */
    private UpdateStatement parseUpdate() {

        expect(
                SqlTokenType.UPDATE,
                "Expected UPDATE keyword."
        );

        String tableName =
                consumeIdentifier(
                        "Expected table name after UPDATE."
                );

        expect(
                SqlTokenType.SET,
                "Expected SET after table name."
        );

        Map<String, Object> updatedValues =
                new LinkedHashMap<>();

        do {

            String columnName =
                    consumeIdentifier(
                            "Expected column name in SET clause."
                    );

            expect(
                    SqlTokenType.EQUALS,
                    "Expected '=' after column name."
            );

            Object value =
                    parseLiteralValue();

            boolean duplicateColumn =
                    updatedValues.keySet()
                            .stream()
                            .anyMatch(
                                    existingColumn ->
                                            existingColumn
                                                    .equalsIgnoreCase(
                                                            columnName
                                                    )
                            );

            if (duplicateColumn) {

                throw error(
                        "Column appears more than once in SET clause: "
                                + columnName
                );
            }

            updatedValues.put(
                    columnName,
                    value
            );

        } while (
                match(
                        SqlTokenType.COMMA
                )
        );

        String whereClause =
                null;

        if (match(
                SqlTokenType.WHERE
        )) {

            /*
             * UPDATE tarafında SELECT clause'ları
             * bulunmadığından eski readWhereClause()
             * kullanılmaya devam edilir.
             */
            whereClause =
                    readWhereClause();
        }

        return new UpdateStatement(
                tableName,
                updatedValues,
                whereClause
        );
    }

    // ==================================================
    // DELETE
    // ==================================================

    /**
     * DELETE sorgusunu ayrıştırır.
     *
     * DELETE FROM users WHERE id = 1;
     * DELETE FROM users;
     */
    private DeleteStatement parseDelete() {

        expect(
                SqlTokenType.DELETE,
                "Expected DELETE keyword."
        );

        expect(
                SqlTokenType.FROM,
                "Expected FROM after DELETE."
        );

        String tableName =
                consumeIdentifier(
                        "Expected table name after FROM."
                );

        String whereClause =
                null;

        if (match(
                SqlTokenType.WHERE
        )) {

            whereClause =
                    readWhereClause();
        }

        return new DeleteStatement(
                tableName,
                whereClause
        );
    }

    // ==================================================
    // LITERAL VALUES
    // ==================================================

    /**
     * SQL literal değerini Java nesnesine dönüştürür.
     */
    private Object parseLiteralValue() {

        SqlToken token =
                currentToken();

        return switch (
                token.getType()
                ) {

            case STRING_LITERAL -> {

                advance();

                yield token.getValue();
            }

            case NUMBER_LITERAL -> {

                advance();

                yield parseNumber(
                        token
                );
            }

            case BOOLEAN_LITERAL -> {

                advance();

                yield Boolean.parseBoolean(
                        token.getValue()
                );
            }

            case NULL_LITERAL -> {

                advance();

                yield null;
            }

            default ->
                    throw error(
                            "Expected literal value but found: "
                                    + token.getValue()
                    );
        };
    }

    /**
     * Sayısal tokenı uygun Java sayı türüne dönüştürür.
     */
    private Number parseNumber(
            SqlToken token
    ) {

        String value =
                token.getValue();

        try {

            if (value.contains(
                    "."
            )) {

                return Double.parseDouble(
                        value
                );
            }

            long longValue =
                    Long.parseLong(
                            value
                    );

            if (longValue
                    >= Integer.MIN_VALUE
                    && longValue
                    <= Integer.MAX_VALUE) {

                return (int) longValue;
            }

            return longValue;

        } catch (
                NumberFormatException exception
        ) {

            throw new ParserException(
                    "Invalid numeric value: "
                            + value,
                    exception
            );
        }
    }

    // ==================================================
    // SELECT CLAUSE EXPRESSION
    // ==================================================

    /**
     * SELECT içerisindeki WHERE / HAVING expression
     * metnini okur.
     *
     * Önemli fark:
     *
     * Eski readWhereClause() noktalı virgüle kadar
     * her şeyi okuyordu.
     *
     * Bu metod ise:
     *
     * GROUP BY
     * HAVING
     * ORDER BY
     * LIMIT
     * FETCH
     *
     * başladığında okumayı bırakır.
     */
    private String readSelectClauseExpression() {

        if (check(
                SqlTokenType.SEMICOLON
        )
                || check(
                SqlTokenType.END_OF_INPUT
        )
                || isSelectClauseBoundary()) {

            throw error(
                    "Clause expression cannot be empty."
            );
        }

        StringBuilder builder =
                new StringBuilder();

        int parenthesisDepth = 0;

        while (!check(
                SqlTokenType.END_OF_INPUT
        )
                && !check(
                SqlTokenType.SEMICOLON
        )) {

            /*
             * Yeni SELECT clause'una geldiysek dur.
             *
             * Sadece parantez dışındayken boundary
             * olarak kabul ediyoruz.
             */
            if (parenthesisDepth == 0
                    && isSelectClauseBoundary()) {

                break;
            }

            SqlToken token =
                    advance();

            if (token.is(
                    SqlTokenType.LEFT_PARENTHESIS
            )) {

                parenthesisDepth++;

            } else if (token.is(
                    SqlTokenType.RIGHT_PARENTHESIS
            )) {

                parenthesisDepth--;

                if (parenthesisDepth < 0) {

                    throw error(
                            "Unexpected ')' in clause expression."
                    );
                }
            }

            appendExpressionToken(
                    builder,
                    token
            );
        }

        if (parenthesisDepth != 0) {

            throw error(
                    "Unbalanced parentheses in clause expression."
            );
        }

        String result =
                builder.toString()
                        .trim();

        if (result.isBlank()) {

            throw error(
                    "Clause expression cannot be empty."
            );
        }

        return result;
    }

    /**
     * SELECT expression okumayı durduran clause'lar.
     */
    private boolean isSelectClauseBoundary() {

        return check(
                SqlTokenType.GROUP
        )
                || check(
                SqlTokenType.HAVING
        )
                || check(
                SqlTokenType.ORDER
        )
                || check(
                SqlTokenType.LIMIT
        )
                || check(
                SqlTokenType.FETCH
        );
    }

    /**
     * Expression tokenını tekrar SQL metnine ekler.
     */
    private void appendExpressionToken(
            StringBuilder builder,
            SqlToken token
    ) {

        if (!builder.isEmpty()) {

            builder.append(
                    ' '
            );
        }

        builder.append(
                formatTokenValue(
                        token
                )
        );
    }

    // ==================================================
    // UPDATE / DELETE WHERE READER
    // ==================================================

    /**
     * UPDATE / DELETE WHERE clause reader.
     *
     * SELECT için kullanılmaz çünkü SELECT içerisinde
     * WHERE sonrasında GROUP BY / HAVING / ORDER BY /
     * LIMIT / FETCH bulunabilir.
     */
    private String readWhereClause() {

        if (check(
                SqlTokenType.SEMICOLON
        )
                || check(
                SqlTokenType.END_OF_INPUT
        )) {

            throw error(
                    "WHERE clause cannot be empty."
            );
        }

        StringBuilder builder =
                new StringBuilder();

        while (!check(
                SqlTokenType.SEMICOLON
        )
                && !check(
                SqlTokenType.END_OF_INPUT
        )) {

            SqlToken token =
                    advance();

            if (!builder.isEmpty()) {

                builder.append(
                        ' '
                );
            }

            builder.append(
                    formatTokenValue(
                            token
                    )
            );
        }

        return builder.toString();
    }

    // ==================================================
    // TOKEN -> SQL TEXT
    // ==================================================

    /**
     * Tokenı tekrar SQL text biçimine çevirir.
     */
    private String formatTokenValue(
            SqlToken token
    ) {

        if (token.is(
                SqlTokenType.STRING_LITERAL
        )) {

            return "'"
                    + token.getValue()
                    .replace(
                            "'",
                            "''"
                    )
                    + "'";
        }

        return token.getValue();
    }

    // ==================================================
    // IDENTIFIER
    // ==================================================

    /**
     * Identifier tokenını tüketir.
     */
    private String consumeIdentifier(
            String errorMessage
    ) {

        SqlToken token =
                expect(
                        SqlTokenType.IDENTIFIER,
                        errorMessage
                );

        return token.getValue();
    }

    // ==================================================
    // LOOKAHEAD
    // ==================================================

    /**
     * Bir sonraki tokenı tüketmeden kontrol eder.
     */
    private boolean checkNext(
            SqlTokenType tokenType
    ) {

        int nextPosition =
                currentPosition + 1;

        if (nextPosition
                >= tokens.size()) {

            return false;
        }

        return tokens
                .get(
                        nextPosition
                )
                .getType()
                == tokenType;
    }

    // ==================================================
    // TOKEN HELPERS
    // ==================================================

    /**
     * Beklenen token varsa tüketir.
     */
    private SqlToken expect(
            SqlTokenType expectedType,
            String errorMessage
    ) {

        if (!check(
                expectedType
        )) {

            throw error(
                    errorMessage
                            + " Found: "
                            + currentToken()
                            .getType()
                            + " ('"
                            + currentToken()
                            .getValue()
                            + "')."
            );
        }

        return advance();
    }

    /**
     * Token verilen türdeyse tüketir.
     */
    private boolean match(
            SqlTokenType tokenType
    ) {

        if (!check(
                tokenType
        )) {

            return false;
        }

        advance();

        return true;
    }

    /**
     * Noktalı virgül varsa tüketir.
     */
    private void consumeOptionalSemicolon() {

        match(
                SqlTokenType.SEMICOLON
        );
    }

    /**
     * Mevcut token tipini kontrol eder.
     */
    private boolean check(
            SqlTokenType tokenType
    ) {

        return currentToken()
                .getType()
                == tokenType;
    }

    /**
     * Mevcut tokenı döndürür ve ilerler.
     */
    private SqlToken advance() {

        SqlToken token =
                currentToken();

        if (!check(
                SqlTokenType.END_OF_INPUT
        )) {

            currentPosition++;
        }

        return token;
    }

    /**
     * Mevcut token.
     */
    private SqlToken currentToken() {

        return tokens.get(
                currentPosition
        );
    }

    // ==================================================
    // ERROR
    // ==================================================

    /**
     * ParserException oluşturur.
     */
    private ParserException error(
            String message
    ) {

        return new ParserException(
                message
                        + " Token position: "
                        + currentPosition
                        + "."
        );
    }
}