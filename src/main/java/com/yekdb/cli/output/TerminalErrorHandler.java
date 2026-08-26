package com.yekdb.cli.output;

import com.yekdb.constraint.exception.ConstraintViolationException;
import com.yekdb.query.executor.QueryExecutionException;
import com.yekdb.storage.exception.TableNotFoundException;

import java.io.IOException;
import java.util.Objects;

/**
 * Interactive terminal içerisinde oluşan hataların
 * kullanıcıya güvenli ve tutarlı biçimde
 * gösterilmesini yönetir.
 *
 * Terminal REPL katmanının exception formatting
 * sorumluluğunu üstlenir.
 */
public final class TerminalErrorHandler {

    private final TerminalOutput output;
    private final boolean debugEnabled;

    public TerminalErrorHandler(
            TerminalOutput output,
            boolean debugEnabled
    ) {

        this.output =
                Objects.requireNonNull(
                        output,
                        "TerminalOutput cannot be null."
                );

        this.debugEnabled =
                debugEnabled;
    }

    /**
     * SQL / query execution sırasında oluşan
     * hataları işler.
     */
    public void handleSqlError(
            RuntimeException exception
    ) {

        Objects.requireNonNull(
                exception,
                "Exception cannot be null."
        );

        String message =
                resolveMessage(
                        exception
                );

        /*
         * Sprint 00-24 Phase 7:
         *
         * Constraint katmanından terminale doğrudan ulaşan
         * violation exception'ları kullanıcıya normal SQL
         * hatası olarak gösterilir. QueryExecutor üzerinden
         * gelen constraint hataları ise QueryExecutionException
         * içerisinde aynı mesajı korur.
         */
        if (exception instanceof ConstraintViolationException) {

            output.error(
                    "ERROR: " + message
            );

            printDebug(
                    exception
            );

            return;
        }

        /*
         * QueryExecutionException kullanıcıya
         * doğrudan anlamlı bir SQL hatası verir.
         */
        if (exception instanceof QueryExecutionException) {

            output.error(
                    "ERROR: " + message
            );

            printDebug(
                    exception
            );

            return;
        }

        /*
         * TableNotFoundException doğrudan kullanıcıya
         * anlamlı tablo bulunamadı mesajı verir.
         */
        if (exception instanceof TableNotFoundException) {

            output.error(
                    "ERROR: " + message
            );

            printDebug(
                    exception
            );

            return;
        }

        /*
         * IllegalArgumentException genellikle
         * validation / geçersiz input hatasıdır.
         */
        if (exception instanceof IllegalArgumentException) {

            output.error(
                    "ERROR: " + message
            );

            printDebug(
                    exception
            );

            return;
        }

        /*
         * IllegalStateException aktif database,
         * datasource veya terminal state problemi
         * gibi durumlarda oluşabilir.
         */
        if (exception instanceof IllegalStateException) {

            output.error(
                    "ERROR: " + message
            );

            printDebug(
                    exception
            );

            return;
        }

        /*
         * Beklenmeyen runtime exception.
         *
         * Terminal kapanmaz ancak kullanıcıya
         * kontrollü mesaj gösterilir.
         */
        output.error(
                "ERROR: Unexpected terminal failure: "
                        + message
        );

        printDebug(
                exception
        );
    }

    /**
     * Terminal input / Reader hatalarını işler.
     */
    public void handleInputError(
            IOException exception
    ) {

        Objects.requireNonNull(
                exception,
                "IOException cannot be null."
        );

        output.error(
                "ERROR: Terminal input failure: "
                        + resolveMessage(
                        exception
                )
        );

        printDebug(
                exception
        );
    }

    /**
     * Exception mesajını güvenli biçimde çözer.
     *
     * Null veya blank message durumunda
     * exception class adı kullanılır.
     */
    private String resolveMessage(
            Throwable throwable
    ) {

        String message =
                throwable.getMessage();

        if (message == null
                || message.isBlank()) {

            return throwable
                    .getClass()
                    .getSimpleName();
        }

        return message.trim();
    }

    /**
     * Debug mode aktifse stack trace gösterilir.
     */
    private void printDebug(
            Throwable throwable
    ) {

        if (!debugEnabled) {
            return;
        }

        throwable.printStackTrace();
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }
}