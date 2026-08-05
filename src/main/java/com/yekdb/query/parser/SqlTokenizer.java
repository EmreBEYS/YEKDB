package com.yekdb.query.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQL metnini anlamlı token parçalarına ayırır.
 *
 * <p>Desteklenen temel yapılar:</p>
 *
 * <ul>
 *     <li>SQL anahtar kelimeleri</li>
 *     <li>Tanımlayıcılar</li>
 *     <li>Metin, sayı, boolean ve null değerleri</li>
 *     <li>Karşılaştırma operatörleri</li>
 *     <li>Parantez, virgül, yıldız ve noktalı virgül</li>
 * </ul>
 */
public final class SqlTokenizer {

    private static final Map<String, SqlTokenType> KEYWORDS = Map.ofEntries(
            Map.entry("SELECT", SqlTokenType.SELECT),
            Map.entry("INSERT", SqlTokenType.INSERT),
            Map.entry("UPDATE", SqlTokenType.UPDATE),
            Map.entry("DELETE", SqlTokenType.DELETE),
            Map.entry("INTO", SqlTokenType.INTO),
            Map.entry("VALUES", SqlTokenType.VALUES),
            Map.entry("FROM", SqlTokenType.FROM),
            Map.entry("SET", SqlTokenType.SET),
            Map.entry("WHERE", SqlTokenType.WHERE),
            Map.entry("CREATE", SqlTokenType.CREATE),
            Map.entry("DROP", SqlTokenType.DROP),
            Map.entry("DATABASE", SqlTokenType.DATABASE),
            Map.entry("TABLE", SqlTokenType.TABLE),
            Map.entry("USE", SqlTokenType.USE)
    );

    private String sql;
    private int position;

    /**
     * Verilen SQL metnini token listesine dönüştürür.
     *
     * @param sql tokenize edilecek SQL
     * @return değiştirilemez token listesi
     */
    public List<SqlToken> tokenize(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new ParserException(
                    "SQL cannot be null or blank."
            );
        }

        this.sql = sql;
        this.position = 0;

        List<SqlToken> tokens = new ArrayList<>();

        while (!isAtEnd()) {
            char current = currentCharacter();

            if (Character.isWhitespace(current)) {
                position++;
                continue;
            }

            if (isIdentifierStart(current)) {
                tokens.add(readWord());
                continue;
            }

            if (Character.isDigit(current)
                    || isNegativeNumberStart()) {

                tokens.add(readNumber());
                continue;
            }

            if (current == '\'') {
                tokens.add(readStringLiteral());
                continue;
            }

            tokens.add(readSymbol());
        }

        tokens.add(
                new SqlToken(
                        SqlTokenType.END_OF_INPUT,
                        ""
                )
        );

        return List.copyOf(tokens);
    }

    /**
     * Anahtar kelime veya identifier okur.
     */
    private SqlToken readWord() {
        int start = position;

        position++;

        while (!isAtEnd()
                && isIdentifierPart(currentCharacter())) {
            position++;
        }

        String value = sql.substring(start, position);
        String upperValue = value.toUpperCase(Locale.ROOT);

        if ("TRUE".equals(upperValue)
                || "FALSE".equals(upperValue)) {

            return new SqlToken(
                    SqlTokenType.BOOLEAN_LITERAL,
                    upperValue.toLowerCase(Locale.ROOT)
            );
        }

        if ("NULL".equals(upperValue)) {
            return new SqlToken(
                    SqlTokenType.NULL_LITERAL,
                    "null"
            );
        }

        SqlTokenType keywordType = KEYWORDS.get(upperValue);

        if (keywordType != null) {
            return new SqlToken(
                    keywordType,
                    upperValue
            );
        }

        return new SqlToken(
                SqlTokenType.IDENTIFIER,
                value
        );
    }

    /**
     * Tam sayı veya ondalıklı sayı okur.
     *
     * <p>Negatif sayılar da desteklenir.</p>
     */
    private SqlToken readNumber() {
        int start = position;

        if (currentCharacter() == '-') {
            position++;
        }

        while (!isAtEnd()
                && Character.isDigit(currentCharacter())) {
            position++;
        }

        if (!isAtEnd()
                && currentCharacter() == '.'
                && hasNextCharacter()
                && Character.isDigit(nextCharacter())) {

            position++;

            while (!isAtEnd()
                    && Character.isDigit(currentCharacter())) {
                position++;
            }
        }

        return new SqlToken(
                SqlTokenType.NUMBER_LITERAL,
                sql.substring(start, position)
        );
    }

    /**
     * Tek tırnak içerisindeki metin değerini okur.
     *
     * <p>SQL biçimindeki iki tek tırnak kaçışını destekler:</p>
     *
     * <pre>
     * 'Emre''nin'
     * </pre>
     */
    private SqlToken readStringLiteral() {
        position++;

        StringBuilder builder = new StringBuilder();

        while (!isAtEnd()) {
            char current = currentCharacter();

            if (current == '\'') {
                if (hasNextCharacter()
                        && nextCharacter() == '\'') {

                    builder.append('\'');
                    position += 2;
                    continue;
                }

                position++;

                return new SqlToken(
                        SqlTokenType.STRING_LITERAL,
                        builder.toString()
                );
            }

            builder.append(current);
            position++;
        }

        throw tokenizationError(
                "Unterminated string literal."
        );
    }

    /**
     * Operatör veya noktalama işareti okur.
     */
    private SqlToken readSymbol() {
        char current = currentCharacter();

        return switch (current) {
            case '=' -> singleCharacterToken(
                    SqlTokenType.EQUALS
            );

            case '!' -> readNotEquals();

            case '>' -> readGreaterThanOperator();

            case '<' -> readLessThanOperator();

            case ',' -> singleCharacterToken(
                    SqlTokenType.COMMA
            );

            case '(' -> singleCharacterToken(
                    SqlTokenType.LEFT_PARENTHESIS
            );

            case ')' -> singleCharacterToken(
                    SqlTokenType.RIGHT_PARENTHESIS
            );

            case '*' -> singleCharacterToken(
                    SqlTokenType.ASTERISK
            );

            case ';' -> singleCharacterToken(
                    SqlTokenType.SEMICOLON
            );

            default -> throw tokenizationError(
                    "Unexpected character: '" + current + "'."
            );
        };
    }

    private SqlToken readNotEquals() {
        if (!hasNextCharacter()
                || nextCharacter() != '=') {

            throw tokenizationError(
                    "Expected '=' after '!'."
            );
        }

        position += 2;

        return new SqlToken(
                SqlTokenType.NOT_EQUALS,
                "!="
        );
    }

    private SqlToken readGreaterThanOperator() {
        position++;

        if (!isAtEnd()
                && currentCharacter() == '=') {

            position++;

            return new SqlToken(
                    SqlTokenType.GREATER_THAN_OR_EQUALS,
                    ">="
            );
        }

        return new SqlToken(
                SqlTokenType.GREATER_THAN,
                ">"
        );
    }

    private SqlToken readLessThanOperator() {
        position++;

        if (!isAtEnd()) {
            if (currentCharacter() == '=') {
                position++;

                return new SqlToken(
                        SqlTokenType.LESS_THAN_OR_EQUALS,
                        "<="
                );
            }

            if (currentCharacter() == '>') {
                position++;

                return new SqlToken(
                        SqlTokenType.NOT_EQUALS,
                        "<>"
                );
            }
        }

        return new SqlToken(
                SqlTokenType.LESS_THAN,
                "<"
        );
    }

    private SqlToken singleCharacterToken(
            SqlTokenType tokenType
    ) {
        String value = String.valueOf(
                currentCharacter()
        );

        position++;

        return new SqlToken(
                tokenType,
                value
        );
    }

    /**
     * '-' karakterinin negatif bir sayının başlangıcı
     * olup olmadığını kontrol eder.
     */
    private boolean isNegativeNumberStart() {
        return currentCharacter() == '-'
                && hasNextCharacter()
                && Character.isDigit(nextCharacter());
    }

    private boolean isIdentifierStart(char character) {
        return Character.isLetter(character)
                || character == '_';
    }

    private boolean isIdentifierPart(char character) {
        return Character.isLetterOrDigit(character)
                || character == '_';
    }

    private char currentCharacter() {
        return sql.charAt(position);
    }

    private char nextCharacter() {
        return sql.charAt(position + 1);
    }

    private boolean hasNextCharacter() {
        return position + 1 < sql.length();
    }

    private boolean isAtEnd() {
        return position >= sql.length();
    }

    private ParserException tokenizationError(
            String message
    ) {
        return new ParserException(
                message + " Position: " + position + "."
        );
    }
}