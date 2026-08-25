package com.yekdb.cli.output;

import com.yekdb.query.executor.QueryExecutionException;
import com.yekdb.storage.exception.TableNotFoundException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalErrorHandlerTest {

    @Test
    void shouldFormatQueryExecutionException() {

        ByteArrayOutputStream errorBuffer =
                new ByteArrayOutputStream();

        TerminalOutput output =
                createTerminalOutput(
                        errorBuffer
                );

        TerminalErrorHandler errorHandler =
                new TerminalErrorHandler(
                        output,
                        false
                );

        errorHandler.handleSqlError(
                new QueryExecutionException(
                        "Table not found: users"
                )
        );

        String errorText =
                errorBuffer.toString();

        assertTrue(
                errorText.contains(
                        "ERROR: Table not found: users"
                )
        );
    }

    @Test
    void shouldFormatTableNotFoundException() {

        ByteArrayOutputStream errorBuffer =
                new ByteArrayOutputStream();

        TerminalOutput output =
                createTerminalOutput(
                        errorBuffer
                );

        TerminalErrorHandler errorHandler =
                new TerminalErrorHandler(
                        output,
                        false
                );

        errorHandler.handleSqlError(
                new TableNotFoundException(
                        "Table not found: missing_table"
                )
        );

        String errorText =
                errorBuffer.toString();

        assertTrue(
                errorText.contains(
                        "ERROR: Table not found: missing_table"
                )
        );
    }

    @Test
    void shouldFormatIllegalArgumentException() {

        ByteArrayOutputStream errorBuffer =
                new ByteArrayOutputStream();

        TerminalOutput output =
                createTerminalOutput(
                        errorBuffer
                );

        TerminalErrorHandler errorHandler =
                new TerminalErrorHandler(
                        output,
                        false
                );

        errorHandler.handleSqlError(
                new IllegalArgumentException(
                        "Invalid column name."
                )
        );

        String errorText =
                errorBuffer.toString();

        assertTrue(
                errorText.contains(
                        "ERROR: Invalid column name."
                )
        );
    }

    @Test
    void shouldFormatIllegalStateException() {

        ByteArrayOutputStream errorBuffer =
                new ByteArrayOutputStream();

        TerminalOutput output =
                createTerminalOutput(
                        errorBuffer
                );

        TerminalErrorHandler errorHandler =
                new TerminalErrorHandler(
                        output,
                        false
                );

        errorHandler.handleSqlError(
                new IllegalStateException(
                        "No database selected."
                )
        );

        String errorText =
                errorBuffer.toString();

        assertTrue(
                errorText.contains(
                        "ERROR: No database selected."
                )
        );
    }

    @Test
    void shouldFormatUnexpectedRuntimeException() {

        ByteArrayOutputStream errorBuffer =
                new ByteArrayOutputStream();

        TerminalOutput output =
                createTerminalOutput(
                        errorBuffer
                );

        TerminalErrorHandler errorHandler =
                new TerminalErrorHandler(
                        output,
                        false
                );

        errorHandler.handleSqlError(
                new RuntimeException(
                        "Unexpected failure"
                )
        );

        String errorText =
                errorBuffer.toString();

        assertTrue(
                errorText.contains(
                        "ERROR: Unexpected terminal failure: "
                                + "Unexpected failure"
                )
        );
    }

    @Test
    void shouldFormatInputIOException() {

        ByteArrayOutputStream errorBuffer =
                new ByteArrayOutputStream();

        TerminalOutput output =
                createTerminalOutput(
                        errorBuffer
                );

        TerminalErrorHandler errorHandler =
                new TerminalErrorHandler(
                        output,
                        false
                );

        errorHandler.handleInputError(
                new IOException(
                        "Input stream closed"
                )
        );

        String errorText =
                errorBuffer.toString();

        assertTrue(
                errorText.contains(
                        "ERROR: Terminal input failure: "
                                + "Input stream closed"
                )
        );
    }

    /**
     * TerminalOutput'un error stream'ini
     * test buffer'ına yönlendirir.
     */
    private TerminalOutput createTerminalOutput(
            ByteArrayOutputStream errorBuffer
    ) {

        PrintStream outputStream =
                new PrintStream(
                        new ByteArrayOutputStream()
                );

        PrintStream errorStream =
                new PrintStream(
                        errorBuffer
                );

        return new TerminalOutput(
                outputStream,
                errorStream
        );
    }
}