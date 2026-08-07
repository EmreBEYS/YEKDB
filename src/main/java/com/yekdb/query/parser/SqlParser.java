package com.yekdb.query.parser;

import com.yekdb.query.statement.DeleteStatement;
import com.yekdb.query.statement.InsertStatement;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.Statement;
import com.yekdb.query.statement.UpdateStatement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SQL metnini ayrıştırarak uygun Statement nesnesini üretir.
 *
 * <p>Desteklenen temel sorgular:</p>
 *
 * <ul>
 *     <li>INSERT</li>
 *     <li>SELECT</li>
 *     <li>UPDATE</li>
 *     <li>DELETE</li>
 * </ul>
 */
public final class SqlParser {

    private final SqlTokenizer tokenizer;

    private List<SqlToken> tokens;
    private int currentPosition;

    /**
     * Varsayılan tokenizer ile yeni parser oluşturur.
     */
    public SqlParser() {
        this(new SqlTokenizer());
    }

    /**
     * Belirtilen tokenizer ile yeni parser oluşturur.
     *
     * @param tokenizer SQL tokenizer
     */
    public SqlParser(SqlTokenizer tokenizer) {
        this.tokenizer = Objects.requireNonNull(
                tokenizer,
                "SqlTokenizer cannot be null."
        );
    }

    /**
     * SQL metnini ayrıştırır.
     *
     * @param sql ayrıştırılacak SQL
     * @return oluşturulan statement
     */
    public Statement parse(String sql) {

        tokens = tokenizer.tokenize(sql);
        currentPosition = 0;

        Statement statement = switch (currentToken().getType()) {

            case INSERT -> parseInsert();

            case SELECT -> parseSelect();

            case UPDATE -> parseUpdate();

            case DELETE -> parseDelete();

            default -> throw error(
                    "Unsupported SQL statement: "
                            + currentToken().getValue()
            );
        };

        consumeOptionalSemicolon();

        expect(
                SqlTokenType.END_OF_INPUT,
                "Unexpected token after SQL statement."
        );

        return statement;
    }

    /**
     * INSERT sorgusunu ayrıştırır.
     *
     * <pre>
     * INSERT INTO users (id, name, age)
     * VALUES (1, 'Emre', 21);
     * </pre>
     *
     * Sprint 00-12:
     *
     * INSERT işlemlerinde kolon listesi zorunludur.
     * Böylece değerler tablo şemasındaki gerçek kolon
     * sırasına göre InsertExecutor tarafından
     * düzenlenebilir.
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

        String tableName = consumeIdentifier(
                "Expected table name after INTO."
        );

        /*
         * INSERT INTO users
         *                  ^
         *
         * Tablo isminden sonra kolon listesi beklenir.
         */
        expect(
                SqlTokenType.LEFT_PARENTHESIS,
                "Expected '(' before INSERT column list."
        );

        List<String> columns =
                new ArrayList<>();

        /*
         * İlk kolon zorunludur.
         *
         * INSERT INTO users ()
         *
         * kullanımı kabul edilmez.
         */
        if (check(SqlTokenType.RIGHT_PARENTHESIS)) {
            throw error(
                    "INSERT statement must contain at least one column."
            );
        }

        columns.add(
                consumeIdentifier(
                        "Expected column name in INSERT column list."
                )
        );

        while (match(SqlTokenType.COMMA)) {

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
         * Aynı kolonun birden fazla kez yazılmasını
         * parser seviyesinde engelliyoruz.
         */
        long distinctColumnCount =
                columns.stream()
                        .map(String::toLowerCase)
                        .distinct()
                        .count();

        if (distinctColumnCount != columns.size()) {
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

        if (!check(SqlTokenType.RIGHT_PARENTHESIS)) {

            do {

                values.add(
                        parseLiteralValue()
                );

            } while (match(SqlTokenType.COMMA));
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

        /*
         * Her INSERT kolonu için tam olarak
         * bir değer bulunmalıdır.
         */
        if (columns.size() != values.size()) {

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

    /**
     * SELECT sorgusunu ayrıştırır.
     *
     * <pre>
     * SELECT * FROM users;
     * SELECT id, name FROM users;
     * </pre>
     */
    private SelectStatement parseSelect() {

        expect(
                SqlTokenType.SELECT,
                "Expected SELECT keyword."
        );

        List<String> selectedColumns =
                new ArrayList<>();

        if (match(SqlTokenType.ASTERISK)) {

            selectedColumns.add("*");

        } else {

            selectedColumns.add(
                    consumeIdentifier(
                            "Expected column name after SELECT."
                    )
            );

            while (match(SqlTokenType.COMMA)) {

                selectedColumns.add(
                        consumeIdentifier(
                                "Expected column name after ','."
                        )
                );
            }
        }

        expect(
                SqlTokenType.FROM,
                "Expected FROM after selected columns."
        );

        String tableName = consumeIdentifier(
                "Expected table name after FROM."
        );

        return new SelectStatement(
                tableName,
                selectedColumns
        );
    }

    /**
     * UPDATE sorgusunu ayrıştırır.
     *
     * <pre>
     * UPDATE users
     * SET name = 'Emre', age = 22
     * WHERE id = 1;
     * </pre>
     *
     * WHERE bölümü bu aşamada metin olarak UpdateStatement
     * içerisinde tutulur. StatementCommandMapper -> UpdateMapper
     * zincirinde ExpressionParser tarafından Expression'a çevrilir.
     */
    private UpdateStatement parseUpdate() {

        expect(
                SqlTokenType.UPDATE,
                "Expected UPDATE keyword."
        );

        String tableName = consumeIdentifier(
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
                                            existingColumn.equalsIgnoreCase(
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

        } while (match(SqlTokenType.COMMA));

        String whereClause = null;

        if (match(SqlTokenType.WHERE)) {

            whereClause =
                    readWhereClause();
        }

        return new UpdateStatement(
                tableName,
                updatedValues,
                whereClause
        );
    }

    /**
     * DELETE sorgusunu ayrıştırır.
     *
     * <pre>
     * DELETE FROM users WHERE id = 1;
     * DELETE FROM users;
     * </pre>
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

        String whereClause = null;

        if (match(SqlTokenType.WHERE)) {

            whereClause =
                    readWhereClause();
        }

        return new DeleteStatement(
                tableName,
                whereClause
        );
    }

    /**
     * SQL sabit değerini Java nesnesine dönüştürür.
     */
    private Object parseLiteralValue() {

        SqlToken token =
                currentToken();

        return switch (token.getType()) {

            case STRING_LITERAL -> {

                advance();

                yield token.getValue();
            }

            case NUMBER_LITERAL -> {

                advance();

                yield parseNumber(token);
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

            default -> throw error(
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

            if (value.contains(".")) {

                return Double.parseDouble(
                        value
                );
            }

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
                    "Invalid numeric value: "
                            + value,
                    exception
            );
        }
    }

    /**
     * WHERE anahtar kelimesinden sonraki tokenları
     * metinsel koşula dönüştürür.
     */
    private String readWhereClause() {

        if (check(SqlTokenType.SEMICOLON)
                || check(SqlTokenType.END_OF_INPUT)) {

            throw error(
                    "WHERE clause cannot be empty."
            );
        }

        StringBuilder builder =
                new StringBuilder();

        while (!check(SqlTokenType.SEMICOLON)
                && !check(SqlTokenType.END_OF_INPUT)) {

            SqlToken token =
                    advance();

            if (!builder.isEmpty()) {

                builder.append(' ');
            }

            builder.append(
                    formatTokenValue(token)
            );
        }

        return builder.toString();
    }

    /**
     * WHERE içerisindeki tokenı yeniden
     * SQL metnine çevirir.
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

    /**
     * Identifier tokenını tüketerek
     * değerini döndürür.
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

    /**
     * Beklenen token varsa tüketir.
     */
    private SqlToken expect(
            SqlTokenType expectedType,
            String errorMessage
    ) {

        if (!check(expectedType)) {

            throw error(
                    errorMessage
                            + " Found: "
                            + currentToken().getType()
                            + " ('"
                            + currentToken().getValue()
                            + "')."
            );
        }

        return advance();
    }

    /**
     * Mevcut token verilen türdeyse tüketir.
     */
    private boolean match(
            SqlTokenType tokenType
    ) {

        if (!check(tokenType)) {

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
     * Mevcut tokenın belirtilen türde
     * olup olmadığını kontrol eder.
     */
    private boolean check(
            SqlTokenType tokenType
    ) {

        return currentToken().getType()
                == tokenType;
    }

    /**
     * Mevcut tokenı döndürüp
     * sonraki tokena geçer.
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
     * Mevcut tokenı döndürür.
     */
    private SqlToken currentToken() {

        return tokens.get(
                currentPosition
        );
    }

    /**
     * Mevcut parser konumuyla hata oluşturur.
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