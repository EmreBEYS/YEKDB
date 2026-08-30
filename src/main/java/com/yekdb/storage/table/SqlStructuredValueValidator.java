package com.yekdb.storage.table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * V1 textual JSON, one-dimensional PostgreSQL-style ARRAY and HSTORE validator.
 *
 * Values remain UTF-8 strings in the existing row format. The schema still
 * enforces structure and ARRAY element type constraints on INSERT/UPDATE.
 */
public final class SqlStructuredValueValidator {

    private SqlStructuredValueValidator() {
        // Utility class.
    }

    public static boolean isValid(Column column, String value) {
        if (value == null) {
            return true;
        }
        if (column == null) {
            return false;
        }

        return switch (column.getDataType()) {
            case JSON -> isValidJson(value);
            case ARRAY -> isValidArray(value, column.getArrayElementType());
            case HSTORE -> isValidHstore(value);
            default -> true;
        };
    }

    public static boolean isValidJson(String value) {
        if (value == null) {
            return true;
        }

        try {
            JsonCursor cursor = new JsonCursor(value);
            cursor.skipWhitespace();
            cursor.readValue();
            cursor.skipWhitespace();
            return cursor.atEnd();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    public static boolean isValidArray(
            String value,
            ColumnTypeDefinition elementType
    ) {
        if (value == null) {
            return true;
        }
        if (elementType == null) {
            return false;
        }

        List<ArrayElement> elements;
        try {
            elements = parseArray(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        for (ArrayElement element : elements) {
            if (element.sqlNull()) {
                continue;
            }
            if (!isValidArrayElement(element.value(), elementType)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidArrayElement(
            String value,
            ColumnTypeDefinition elementType
    ) {
        try {
            return switch (elementType.dataType()) {
                case INT -> {
                    Integer.parseInt(value);
                    yield true;
                }
                case LONG -> {
                    Long.parseLong(value);
                    yield true;
                }
                case DOUBLE -> {
                    double parsed = Double.parseDouble(value);
                    yield Double.isFinite(parsed);
                }
                case NUMERIC -> {
                    BigDecimal parsed = new BigDecimal(value);
                    yield Column.acceptsNumericDefinition(
                            parsed,
                            elementType.precision(),
                            elementType.scale()
                    );
                }
                case BOOLEAN ->
                        "true".equalsIgnoreCase(value)
                                || "false".equalsIgnoreCase(value);
                case CHAR, VARCHAR ->
                        value.length() <= elementType.length();
                case STRING, TEXT -> true;
                case UUID, DATE, TIME, TIMESTAMP, INTERVAL ->
                        SqlTemporalValueValidator.isValid(elementType.dataType(), value);
                case ARRAY, JSON, HSTORE, UDT -> false;
            };
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /**
     * Validates a PostgreSQL-style HSTORE textual value.
     *
     * Supported V1 examples:
     *
     * "brand"=>"ASUS", "model"=>"TUF"
     * brand=>ASUS, active=>true
     * note=>NULL
     *
     * HSTORE values are kept as UTF-8 strings in the existing row format.
     * Duplicate keys are rejected in V1 to keep deterministic semantics.
     */
    public static boolean isValidHstore(String value) {
        if (value == null) {
            return true;
        }

        String candidate = value.trim();
        if (candidate.isEmpty()) {
            return false;
        }

        try {
            List<HstorePair> pairs = parseHstore(candidate);
            if (pairs.isEmpty()) {
                return false;
            }

            Set<String> keys = new HashSet<>();
            for (HstorePair pair : pairs) {
                if (pair.key() == null || pair.key().isBlank()) {
                    return false;
                }
                if (!keys.add(pair.key())) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static List<HstorePair> parseHstore(String text) {
        List<HstorePair> result = new ArrayList<>();
        int index = 0;

        while (index < text.length()) {
            ParsedToken key = readHstoreToken(text, index, false);
            index = skipWhitespace(text, key.nextIndex());

            if (index + 1 >= text.length()
                    || text.charAt(index) != '='
                    || text.charAt(index + 1) != '>') {
                throw new IllegalArgumentException("HSTORE pair requires =>.");
            }
            index += 2;
            index = skipWhitespace(text, index);

            ParsedToken value = readHstoreToken(text, index, true);
            index = skipWhitespace(text, value.nextIndex());

            result.add(new HstorePair(key.value(), value.value(), value.sqlNull()));

            if (index == text.length()) {
                break;
            }

            if (text.charAt(index) != ',') {
                throw new IllegalArgumentException("HSTORE pairs must be comma-separated.");
            }
            index++;
            index = skipWhitespace(text, index);
            if (index >= text.length()) {
                throw new IllegalArgumentException("Trailing comma in HSTORE value.");
            }
        }

        return result;
    }

    private static ParsedToken readHstoreToken(
            String text,
            int start,
            boolean allowNull
    ) {
        int index = skipWhitespace(text, start);
        if (index >= text.length()) {
            throw new IllegalArgumentException("Missing HSTORE token.");
        }

        if (text.charAt(index) == '"') {
            StringBuilder value = new StringBuilder();
            index++;
            boolean escaped = false;

            while (index < text.length()) {
                char c = text.charAt(index++);
                if (escaped) {
                    value.append(c);
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    return new ParsedToken(value.toString(), false, index);
                }
                value.append(c);
            }

            throw new IllegalArgumentException("Unterminated HSTORE quoted token.");
        }

        int tokenStart = index;
        while (index < text.length()) {
            char c = text.charAt(index);
            if (c == ',' || (c == '=' && index + 1 < text.length() && text.charAt(index + 1) == '>')) {
                break;
            }
            index++;
        }

        String raw = text.substring(tokenStart, index).trim();
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("Empty HSTORE token.");
        }

        boolean sqlNull = allowNull && "NULL".equalsIgnoreCase(raw);
        return new ParsedToken(sqlNull ? null : raw, sqlNull, index);
    }

    private static int skipWhitespace(String text, int index) {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private record ParsedToken(String value, boolean sqlNull, int nextIndex) {
    }

    private record HstorePair(String key, String value, boolean sqlNull) {
    }

    private static List<ArrayElement> parseArray(String rawValue) {
        String value = rawValue.trim();
        if (value.length() < 2 || value.charAt(0) != '{' || value.charAt(value.length() - 1) != '}') {
            throw new IllegalArgumentException("ARRAY value must use {..} syntax.");
        }

        String body = value.substring(1, value.length() - 1);
        List<ArrayElement> result = new ArrayList<>();

        if (body.isBlank()) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        boolean elementWasQuoted = false;

        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (quoted && c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                quoted = !quoted;
                elementWasQuoted = true;
                continue;
            }

            if (c == ',' && !quoted) {
                result.add(toArrayElement(current.toString(), elementWasQuoted));
                current.setLength(0);
                elementWasQuoted = false;
                continue;
            }

            current.append(c);
        }

        if (quoted || escaped) {
            throw new IllegalArgumentException("Unterminated ARRAY quoted element.");
        }

        result.add(toArrayElement(current.toString(), elementWasQuoted));
        return result;
    }

    private static ArrayElement toArrayElement(String raw, boolean quoted) {
        String value = quoted ? raw : raw.trim();
        if (!quoted && value.isEmpty()) {
            throw new IllegalArgumentException("ARRAY element cannot be empty.");
        }

        boolean sqlNull = !quoted && "NULL".equalsIgnoreCase(value);
        return new ArrayElement(value, sqlNull);
    }

    private record ArrayElement(String value, boolean sqlNull) {
    }

    private static final class JsonCursor {
        private final String text;
        private int index;

        private JsonCursor(String text) {
            this.text = text == null ? "" : text;
        }

        private boolean atEnd() {
            return index == text.length();
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private void readValue() {
            skipWhitespace();
            if (index >= text.length()) {
                fail();
            }

            char c = text.charAt(index);
            switch (c) {
                case '{' -> readObject();
                case '[' -> readArray();
                case '"' -> readString();
                case 't' -> readLiteral("true");
                case 'f' -> readLiteral("false");
                case 'n' -> readLiteral("null");
                default -> readNumber();
            }
        }

        private void readObject() {
            expect('{');
            skipWhitespace();
            if (peek('}')) {
                index++;
                return;
            }

            while (true) {
                skipWhitespace();
                readString();
                skipWhitespace();
                expect(':');
                readValue();
                skipWhitespace();

                if (peek('}')) {
                    index++;
                    return;
                }
                expect(',');
            }
        }

        private void readArray() {
            expect('[');
            skipWhitespace();
            if (peek(']')) {
                index++;
                return;
            }

            while (true) {
                readValue();
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return;
                }
                expect(',');
            }
        }

        private void readString() {
            expect('"');
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return;
                }
                if (c == '\\') {
                    if (index >= text.length()) {
                        fail();
                    }
                    char escape = text.charAt(index++);
                    if (escape == 'u') {
                        for (int i = 0; i < 4; i++) {
                            if (index >= text.length() || Character.digit(text.charAt(index++), 16) < 0) {
                                fail();
                            }
                        }
                    } else if ("\"\\/bfnrt".indexOf(escape) < 0) {
                        fail();
                    }
                } else if (c < 0x20) {
                    fail();
                }
            }
            fail();
        }

        private void readLiteral(String literal) {
            if (!text.startsWith(literal, index)) {
                fail();
            }
            index += literal.length();
        }

        private void readNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }

            if (peek('0')) {
                index++;
            } else {
                readDigits(true);
            }

            if (peek('.')) {
                index++;
                readDigits(true);
            }

            if (peek('e') || peek('E')) {
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                readDigits(true);
            }

            if (start == index) {
                fail();
            }
        }

        private void readDigits(boolean requireOne) {
            int start = index;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (requireOne && start == index) {
                fail();
            }
        }

        private boolean peek(char c) {
            return index < text.length() && text.charAt(index) == c;
        }

        private void expect(char c) {
            skipWhitespace();
            if (!peek(c)) {
                fail();
            }
            index++;
        }

        private void fail() {
            throw new IllegalArgumentException("Invalid JSON value.");
        }
    }
}
