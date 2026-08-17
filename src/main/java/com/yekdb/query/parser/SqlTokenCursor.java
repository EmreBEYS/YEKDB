package com.yekdb.query.parser;

import java.util.List;
import java.util.Objects;

/**
 * SqlParser tarafından kullanılan token akışının mevcut konumunu yönetir.
 *
 * <p>Parser sınıfının token gezinme, lookahead ve hata konumu gibi
 * düşük seviyeli ayrıntılardan ayrılmasını sağlar.</p>
 */
final class SqlTokenCursor {

    private final List<SqlToken> tokens;
    private int position;

    SqlTokenCursor(List<SqlToken> tokens) {
        this.tokens = List.copyOf(
                Objects.requireNonNull(tokens, "Token list cannot be null.")
        );

        if (this.tokens.isEmpty()) {
            throw new IllegalArgumentException("Token list cannot be empty.");
        }

        this.position = 0;
    }

    SqlToken current() {
        return tokens.get(position);
    }

    SqlToken advance() {
        SqlToken token = current();

        if (!check(SqlTokenType.END_OF_INPUT)) {
            position++;
        }

        return token;
    }

    boolean check(SqlTokenType tokenType) {
        return current().getType() == tokenType;
    }

    boolean checkNext(SqlTokenType tokenType) {
        int nextPosition = position + 1;

        if (nextPosition >= tokens.size()) {
            return false;
        }

        return tokens.get(nextPosition).getType() == tokenType;
    }

    boolean match(SqlTokenType tokenType) {
        if (!check(tokenType)) {
            return false;
        }

        advance();
        return true;
    }

    SqlToken expect(
            SqlTokenType expectedType,
            String errorMessage
    ) {
        if (!check(expectedType)) {
            throw error(
                    errorMessage
                            + " Found: "
                            + current().getType()
                            + " ('"
                            + current().getValue()
                            + "')."
            );
        }

        return advance();
    }

    String consumeIdentifier(String errorMessage) {
        return expect(
                SqlTokenType.IDENTIFIER,
                errorMessage
        ).getValue();
    }

    void consumeOptionalSemicolon() {
        match(SqlTokenType.SEMICOLON);
    }

    ParserException error(String message) {
        return new ParserException(
                message
                        + " Token position: "
                        + position
                        + "."
        );
    }
}
