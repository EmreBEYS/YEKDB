package com.yekdb.query.parser;

import com.yekdb.query.expression.ColumnExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.statement.DeleteStatement;
import com.yekdb.query.statement.FetchClause;
import com.yekdb.query.statement.GroupByClause;
import com.yekdb.query.statement.HavingClause;
import com.yekdb.query.statement.InsertStatement;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.JoinType;
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
 *
 * Sprint 00-15 JOIN Foundation:
 *
 * - INNER JOIN
 * - JOIN (INNER JOIN shorthand)
 * - Table aliases on joined tables
 * - Qualified ON column references
 * - Column-to-column equality conditions
 */
public final class SqlParser {

    private final SqlTokenizer tokenizer;

    private SqlTokenCursor tokenCursor;

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

        tokenCursor =
                new SqlTokenCursor(
                        tokenizer.tokenize(
                                sql
                        )
                );

        Statement statement =
                switch (
                        tokenCursor.current().getType()
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
                            throw tokenCursor.error(
                                    "Unsupported SQL statement: "
                                            + tokenCursor.current()
                                            .getValue()
                            );
                };

        tokenCursor.consumeOptionalSemicolon();

        tokenCursor.expect(
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

        tokenCursor.expect(
                SqlTokenType.INSERT,
                "Expected INSERT keyword."
        );

        tokenCursor.expect(
                SqlTokenType.INTO,
                "Expected INTO after INSERT."
        );

        String tableName =
                tokenCursor.consumeIdentifier(
                        "Expected table name after INTO."
                );

        tokenCursor.expect(
                SqlTokenType.LEFT_PARENTHESIS,
                "Expected '(' before INSERT column list."
        );

        List<String> columns =
                new ArrayList<>();

        if (tokenCursor.check(
                SqlTokenType.RIGHT_PARENTHESIS
        )) {

            throw tokenCursor.error(
                    "INSERT statement must contain at least one column."
            );
        }

        columns.add(
                tokenCursor.consumeIdentifier(
                        "Expected column name in INSERT column list."
                )
        );

        while (tokenCursor.match(
                SqlTokenType.COMMA
        )) {

            columns.add(
                    tokenCursor.consumeIdentifier(
                            "Expected column name after ','."
                    )
            );
        }

        tokenCursor.expect(
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

            throw tokenCursor.error(
                    "INSERT column list contains duplicate columns."
            );
        }

        tokenCursor.expect(
                SqlTokenType.VALUES,
                "Expected VALUES after INSERT column list."
        );

        tokenCursor.expect(
                SqlTokenType.LEFT_PARENTHESIS,
                "Expected '(' after VALUES."
        );

        List<Object> values =
                new ArrayList<>();

        if (!tokenCursor.check(
                SqlTokenType.RIGHT_PARENTHESIS
        )) {

            do {

                values.add(
                        parseLiteralValue()
                );

            } while (
                    tokenCursor.match(
                            SqlTokenType.COMMA
                    )
            );
        }

        tokenCursor.expect(
                SqlTokenType.RIGHT_PARENTHESIS,
                "Expected ')' after INSERT values."
        );

        if (values.isEmpty()) {

            throw tokenCursor.error(
                    "INSERT statement must contain at least one value."
            );
        }

        if (columns.size()
                != values.size()) {

            throw tokenCursor.error(
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
     * INNER JOIN / JOIN
     * ON
     * WHERE
     * GROUP BY
     * HAVING
     * ORDER BY
     * LIMIT / FETCH
     */
    private SelectStatement parseSelect() {

        tokenCursor.expect(
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
        if (tokenCursor.match(
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

            while (tokenCursor.match(
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

        tokenCursor.expect(
                SqlTokenType.FROM,
                "Expected FROM after selected columns."
        );

        String tableName =
                tokenCursor.consumeIdentifier(
                        "Expected table name after FROM."
                );

        String tableAlias =
                null;

        /*
         * FROM users AS u
         */
        if (tokenCursor.match(
                SqlTokenType.AS
        )) {

            tableAlias =
                    tokenCursor.consumeIdentifier(
                            "Expected table alias after AS."
                    );

            /*
             * FROM users u
             */
        } else if (tokenCursor.check(
                SqlTokenType.IDENTIFIER
        )) {

            tableAlias =
                    tokenCursor.consumeIdentifier(
                            "Expected table alias."
                    );
        }

        TableReference table =
                new TableReference(
                        tableName,
                        tableAlias
                );

        // ----------------------------------------------
        // SPRINT 00-15 - JOIN
        // ----------------------------------------------

        List<JoinClause> joins =
                new ArrayList<>();

        /*
         * Sprint 00-15:
         *
         * SELECT ...
         * FROM employee e
         * INNER JOIN department d
         * ON e.department_id = d.id
         *
         * SQL'deki sade JOIN de INNER JOIN olarak
         * değerlendirilir:
         *
         * JOIN department d ON ...
         */
        if (tokenCursor.check(
                SqlTokenType.INNER
        )
                || tokenCursor.check(
                SqlTokenType.JOIN
        )) {

            joins.add(
                    parseInnerJoin()
            );
        }

        /*
         * Sprint 00-15 executor yalnızca tek JOIN
         * desteklediği için parser katmanında da
         * ikinci JOIN açık şekilde reddedilir.
         */
        if (tokenCursor.check(
                SqlTokenType.INNER
        )
                || tokenCursor.check(
                SqlTokenType.JOIN
        )) {

            throw tokenCursor.error(
                    "Sprint 00-15 supports exactly one JOIN per SELECT statement."
            );
        }

        // ----------------------------------------------
        // WHERE
        // ----------------------------------------------

        Expression whereExpression =
                null;

        if (tokenCursor.match(
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

        if (tokenCursor.match(
                SqlTokenType.GROUP
        )) {

            tokenCursor.expect(
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
            while (tokenCursor.match(
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

        if (tokenCursor.match(
                SqlTokenType.HAVING
        )) {

            if (groupByClause == null) {

                throw tokenCursor.error(
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

        if (tokenCursor.match(
                SqlTokenType.ORDER
        )) {

            tokenCursor.expect(
                    SqlTokenType.BY,
                    "Expected BY after ORDER."
            );

            orderByItems.add(
                    parseOrderByItem()
            );

            while (tokenCursor.match(
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

        if (tokenCursor.match(
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

        if (tokenCursor.match(
                SqlTokenType.FETCH
        )) {

            FetchClause.Mode mode;

            if (tokenCursor.match(
                    SqlTokenType.FIRST
            )) {

                mode =
                        FetchClause.Mode.FIRST;

            } else if (tokenCursor.match(
                    SqlTokenType.NEXT
            )) {

                mode =
                        FetchClause.Mode.NEXT;

            } else {

                throw tokenCursor.error(
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
            if (!tokenCursor.match(
                    SqlTokenType.ROW
            )
                    && !tokenCursor.match(
                    SqlTokenType.ROWS
            )) {

                throw tokenCursor.error(
                        "Expected ROW or ROWS after FETCH row count."
                );
            }

            tokenCursor.expect(
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
                joins,
                whereExpression,
                groupByClause,
                havingClause,
                orderByItems,
                limitClause,
                fetchClause
        );
    }


    // ==================================================
    // SPRINT 00-15 - JOIN
    // ==================================================

    /**
     * Tek bir INNER JOIN ifadesini parse eder.
     *
     * Desteklenen biçimler:
     *
     * INNER JOIN department d
     * ON e.department_id = d.id
     *
     * JOIN department d
     * ON e.department_id = d.id
     *
     * Sprint 00-15 kapsamında JOIN, INNER JOIN ile
     * aynı anlama gelir.
     */
    private JoinClause parseInnerJoin() {

        /*
         * INNER opsiyoneldir.
         *
         * INNER JOIN ...
         * JOIN ...
         */
        tokenCursor.match(
                SqlTokenType.INNER
        );

        tokenCursor.expect(
                SqlTokenType.JOIN,
                "Expected JOIN keyword."
        );

        String tableName =
                tokenCursor.consumeIdentifier(
                        "Expected table name after JOIN."
                );

        String alias =
                null;

        /*
         * JOIN department AS d
         */
        if (tokenCursor.match(
                SqlTokenType.AS
        )) {

            alias =
                    tokenCursor.consumeIdentifier(
                            "Expected table alias after AS."
                    );

            /*
             * JOIN department d
             */
        } else if (tokenCursor.check(
                SqlTokenType.IDENTIFIER
        )) {

            alias =
                    tokenCursor.consumeIdentifier(
                            "Expected table alias after JOIN table name."
                    );
        }

        tokenCursor.expect(
                SqlTokenType.ON,
                "Expected ON after JOIN table reference."
        );

        Expression condition =
                parseJoinCondition();

        return new JoinClause(
                JoinType.INNER,
                tableName,
                alias,
                condition
        );
    }

    /**
     * Sprint 00-15 JOIN condition parser.
     *
     * Şimdilik güvenli ve açık biçimde yalnızca
     * kolon-kolon equality karşılaştırması desteklenir:
     *
     * e.department_id = d.id
     *
     * Daha karmaşık ON ifadeleri sonraki JOIN
     * sprintlerinde ExpressionParser ile genişletilebilir.
     */
    private Expression parseJoinCondition() {

        String leftReference =
                parseColumnReference();

        tokenCursor.expect(
                SqlTokenType.EQUALS,
                "Expected '=' in JOIN ON condition."
        );

        String rightReference =
                parseColumnReference();

        return new ComparisonExpression(
                ColumnExpression.parse(
                        leftReference
                ),
                ComparisonOperator.EQUALS,
                ColumnExpression.parse(
                        rightReference
                )
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
        if (tokenCursor.match(
                SqlTokenType.AS
        )) {

            alias =
                    tokenCursor.consumeIdentifier(
                            "Expected column alias after AS."
                    );

            /*
             * SELECT name username
             *
             * SELECT COUNT(*) total
             */
        } else if (tokenCursor.check(
                SqlTokenType.IDENTIFIER
        )) {

            alias =
                    tokenCursor.consumeIdentifier(
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

        if (!tokenCursor.check(
                SqlTokenType.IDENTIFIER
        )) {

            return false;
        }

        String value =
                tokenCursor.current()
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

        return tokenCursor.checkNext(
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
                tokenCursor.current()
                        .getValue()
                        .toUpperCase();

        tokenCursor.advance();

        tokenCursor.expect(
                SqlTokenType.LEFT_PARENTHESIS,
                "Expected '(' after aggregate function."
        );

        String argument;

        /*
         * COUNT(*)
         */
        if (tokenCursor.match(
                SqlTokenType.ASTERISK
        )) {

            if (!functionName.equals(
                    "COUNT"
            )) {

                throw tokenCursor.error(
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

        tokenCursor.expect(
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
                tokenCursor.consumeIdentifier(
                        "Expected column name."
                );

        /*
         * Basit kolon:
         *
         * name
         */
        if (!tokenCursor.match(
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
                tokenCursor.consumeIdentifier(
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

        if (tokenCursor.match(
                SqlTokenType.ASC
        )) {

            direction =
                    SortDirection.ASC;

        } else if (tokenCursor.match(
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

        if (!tokenCursor.check(
                SqlTokenType.NUMBER_LITERAL
        )) {

            throw tokenCursor.error(
                    errorMessage
            );
        }

        SqlToken token =
                tokenCursor.advance();

        String rawValue =
                token.getValue();

        /*
         * LIMIT 10.5 geçersiz.
         */
        if (rawValue.contains(
                "."
        )) {

            throw tokenCursor.error(
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

                throw tokenCursor.error(
                        "Row count cannot be negative."
                );
            }

            if (value > Integer.MAX_VALUE) {

                throw tokenCursor.error(
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

        tokenCursor.expect(
                SqlTokenType.UPDATE,
                "Expected UPDATE keyword."
        );

        String tableName =
                tokenCursor.consumeIdentifier(
                        "Expected table name after UPDATE."
                );

        tokenCursor.expect(
                SqlTokenType.SET,
                "Expected SET after table name."
        );

        Map<String, Object> updatedValues =
                new LinkedHashMap<>();

        do {

            String columnName =
                    tokenCursor.consumeIdentifier(
                            "Expected column name in SET clause."
                    );

            tokenCursor.expect(
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

                throw tokenCursor.error(
                        "Column appears more than once in SET clause: "
                                + columnName
                );
            }

            updatedValues.put(
                    columnName,
                    value
            );

        } while (
                tokenCursor.match(
                        SqlTokenType.COMMA
                )
        );

        String whereClause =
                null;

        if (tokenCursor.match(
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

        tokenCursor.expect(
                SqlTokenType.DELETE,
                "Expected DELETE keyword."
        );

        tokenCursor.expect(
                SqlTokenType.FROM,
                "Expected FROM after DELETE."
        );

        String tableName =
                tokenCursor.consumeIdentifier(
                        "Expected table name after FROM."
                );

        String whereClause =
                null;

        if (tokenCursor.match(
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
                tokenCursor.current();

        Object value =
                SqlLiteralParser.parse(token);

        tokenCursor.advance();

        return value;
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

        if (tokenCursor.check(
                SqlTokenType.SEMICOLON
        )
                || tokenCursor.check(
                SqlTokenType.END_OF_INPUT
        )
                || isSelectClauseBoundary()) {

            throw tokenCursor.error(
                    "Clause expression cannot be empty."
            );
        }

        StringBuilder builder =
                new StringBuilder();

        int parenthesisDepth = 0;

        while (!tokenCursor.check(
                SqlTokenType.END_OF_INPUT
        )
                && !tokenCursor.check(
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
                    tokenCursor.advance();

            if (token.is(
                    SqlTokenType.LEFT_PARENTHESIS
            )) {

                parenthesisDepth++;

            } else if (token.is(
                    SqlTokenType.RIGHT_PARENTHESIS
            )) {

                parenthesisDepth--;

                if (parenthesisDepth < 0) {

                    throw tokenCursor.error(
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

            throw tokenCursor.error(
                    "Unbalanced parentheses in clause expression."
            );
        }

        String result =
                builder.toString()
                        .trim();

        if (result.isBlank()) {

            throw tokenCursor.error(
                    "Clause expression cannot be empty."
            );
        }

        return result;
    }

    /**
     * SELECT expression okumayı durduran clause'lar.
     */
    private boolean isSelectClauseBoundary() {

        return tokenCursor.check(
                SqlTokenType.GROUP
        )
                || tokenCursor.check(
                SqlTokenType.HAVING
        )
                || tokenCursor.check(
                SqlTokenType.ORDER
        )
                || tokenCursor.check(
                SqlTokenType.LIMIT
        )
                || tokenCursor.check(
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

        /*
         * Qualified column:
         *
         * d.name
         * e.department_id
         *
         * DOT öncesine ve sonrasına boşluk konmaz.
         */
        if (token.is(
                SqlTokenType.DOT
        )) {

            builder.append(
                    '.'
            );

            return;
        }

        /*
         * Önceki token DOT ise yeni identifier
         * doğrudan devam eder:
         *
         * d. + name -> d.name
         */
        boolean previousTokenWasDot =
                !builder.isEmpty()
                        && builder.charAt(
                        builder.length() - 1
                ) == '.';

        if (!builder.isEmpty()
                && !previousTokenWasDot) {

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

        if (tokenCursor.check(
                SqlTokenType.SEMICOLON
        )
                || tokenCursor.check(
                SqlTokenType.END_OF_INPUT
        )) {

            throw tokenCursor.error(
                    "WHERE clause cannot be empty."
            );
        }

        StringBuilder builder =
                new StringBuilder();

        while (!tokenCursor.check(
                SqlTokenType.SEMICOLON
        )
                && !tokenCursor.check(
                SqlTokenType.END_OF_INPUT
        )) {

            SqlToken token =
                    tokenCursor.advance();

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

}
