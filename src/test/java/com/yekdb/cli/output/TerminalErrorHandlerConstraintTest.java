package com.yekdb.cli.output;

import com.yekdb.constraint.exception.NotNullConstraintViolationException;
import com.yekdb.constraint.exception.PrimaryKeyConstraintViolationException;
import com.yekdb.constraint.exception.UniqueConstraintViolationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalErrorHandlerConstraintTest {

    @Test
    void shouldRenderNotNullViolationAsSqlError() {

        String error = handle(
                new NotNullConstraintViolationException("email")
        );

        assertTrue(error.contains("ERROR:"));
        assertTrue(error.contains("NOT NULL"));
        assertTrue(error.contains("email"));
    }

    @Test
    void shouldRenderUniqueViolationAsSqlError() {

        String error = handle(
                new UniqueConstraintViolationException(
                        List.of("username")
                )
        );

        assertTrue(error.contains("ERROR:"));
        assertTrue(error.contains("UNIQUE"));
        assertTrue(error.contains("username"));
    }

    @Test
    void shouldRenderPrimaryKeyViolationAsSqlError() {

        String error = handle(
                new PrimaryKeyConstraintViolationException(
                        List.of("id"),
                        "Duplicate PRIMARY KEY value."
                )
        );

        assertTrue(error.contains("ERROR:"));
        assertTrue(error.contains("PRIMARY KEY"));
        assertTrue(error.contains("id"));
    }

    private String handle(RuntimeException exception) {

        ByteArrayOutputStream outputBytes =
                new ByteArrayOutputStream();

        ByteArrayOutputStream errorBytes =
                new ByteArrayOutputStream();

        TerminalOutput output =
                new TerminalOutput(
                        new PrintStream(outputBytes),
                        new PrintStream(errorBytes)
                );

        TerminalErrorHandler handler =
                new TerminalErrorHandler(
                        output,
                        false
                );

        handler.handleSqlError(exception);

        return errorBytes.toString(
                StandardCharsets.UTF_8
        );
    }
}
