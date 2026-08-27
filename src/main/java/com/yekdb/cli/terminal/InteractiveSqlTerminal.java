package com.yekdb.cli.terminal;

import com.yekdb.cli.command.TerminalCommand;
import com.yekdb.cli.command.TerminalCommandParser;
import com.yekdb.cli.command.TerminalCommandType;
import com.yekdb.cli.executor.SqlTerminalExecutor;
import com.yekdb.cli.metadata.TerminalMetadataService;
import com.yekdb.cli.output.QueryResultFormatter;
import com.yekdb.cli.output.TerminalErrorHandler;
import com.yekdb.cli.output.TerminalOutput;
import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.query.executor.ExecuteResult;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.Table;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * YEKDB Interactive SQL Terminal.
 *
 * REPL döngüsünü, terminal meta-command'lerini,
 * SQL statement buffering işlemini, metadata komutlarını
 * ve SQL yürütme sürecini yönetir.
 *
 * Sprint 00-23 Phase 12 kapsamında terminal UX'i
 * daha okunabilir ve kullanıcı dostu hale getirilmiştir.
 */
public final class InteractiveSqlTerminal {

    private static final String PROMPT_DATABASE_PREFIX = "yekdb[";
    private static final String PROMPT_DATABASE_SUFFIX = "]> ";

    private final TerminalConfig config;
    private final TerminalSession session;
    private final TerminalCommandParser commandParser;
    private final SqlTerminalExecutor sqlExecutor;
    private final TerminalOutput output;
    private final QueryResultFormatter resultFormatter;
    private final TerminalErrorHandler errorHandler;
    private final TerminalMetadataService metadataService;
    private final BufferedReader reader;
    private final SqlStatementBuffer statementBuffer;

    public InteractiveSqlTerminal(
            TerminalConfig config,
            TerminalSession session,
            TerminalCommandParser commandParser,
            SqlTerminalExecutor sqlExecutor,
            TerminalOutput output,
            QueryResultFormatter resultFormatter,
            TerminalErrorHandler errorHandler,
            TerminalMetadataService metadataService,
            Reader reader
    ) {

        this.config =
                Objects.requireNonNull(
                        config,
                        "config"
                );

        this.session =
                Objects.requireNonNull(
                        session,
                        "session"
                );

        this.commandParser =
                Objects.requireNonNull(
                        commandParser,
                        "commandParser"
                );

        this.sqlExecutor =
                Objects.requireNonNull(
                        sqlExecutor,
                        "sqlExecutor"
                );

        this.output =
                Objects.requireNonNull(
                        output,
                        "output"
                );

        this.resultFormatter =
                Objects.requireNonNull(
                        resultFormatter,
                        "resultFormatter"
                );

        this.errorHandler =
                Objects.requireNonNull(
                        errorHandler,
                        "errorHandler"
                );

        this.metadataService =
                Objects.requireNonNull(
                        metadataService,
                        "metadataService"
                );

        this.reader =
                new BufferedReader(
                        Objects.requireNonNull(
                                reader,
                                "reader"
                        )
                );

        this.statementBuffer =
                new SqlStatementBuffer();
    }

    /**
     * Interactive terminal REPL döngüsünü başlatır.
     */
    public void run() {

        session.start();
        printWelcome();

        while (session.isRunning()) {

            try {

                output.print(
                        getCurrentPrompt()
                );

                String input =
                        reader.readLine();

                if (input == null) {
                    stop();
                    break;
                }

                processInput(
                        input
                );

            } catch (IOException exception) {

                errorHandler.handleInputError(
                        exception
                );

                stop();
            }
        }
    }

    /**
     * Kullanıcı girdisini terminal meta-command'i
     * veya SQL statement olarak değerlendirir.
     */
    private void processInput(
            String input
    ) {

        String trimmedInput =
                input.trim();

        if (trimmedInput.isEmpty()) {
            return;
        }

        if (statementBuffer.isEmpty()
                && trimmedInput.startsWith("\\")) {

            TerminalCommand command =
                    commandParser.parse(
                            trimmedInput
                    );

            if (command.getType()
                    != TerminalCommandType.HISTORY) {

                session.addHistory(
                        trimmedInput
                );
            }

            handleTerminalCommand(
                    command
            );

            return;
        }

        processSqlInput(
                input
        );
    }

    /**
     * SQL satırını statement buffer'a ekler.
     */
    private void processSqlInput(
            String input
    ) {

        statementBuffer.append(
                input
        );

        if (!statementBuffer.isComplete()) {
            return;
        }

        String sql =
                statementBuffer.consume();

        session.addHistory(
                sql
        );

        executeSql(
                sql
        );
    }

    /**
     * SQL statement'ını gerçek YEKDB query engine'e gönderir.
     */
    private void executeSql(
            String sql
    ) {

        try {

            ExecuteResult result =
                    sqlExecutor.execute(
                            sql
                    );

            String formattedResult =
                    resultFormatter.format(
                            result
                    );

            if (!formattedResult.isBlank()) {

                output.println(
                        formattedResult
                );
            }

        } catch (RuntimeException exception) {

            errorHandler.handleSqlError(
                    exception
            );
        }
    }

    /**
     * Buffer durumuna göre normal veya continuation prompt döndürür.
     *
     * Aktif veritabanı varsa prompt içerisinde veritabanı adı gösterilir:
     * yekdb[database_name]>
     */
    private String getCurrentPrompt() {

        if (!statementBuffer.isEmpty()) {
            return config.getContinuationPrompt();
        }

        try {

            String databaseName =
                    metadataService.getCurrentDatabaseName();

            return PROMPT_DATABASE_PREFIX
                    + databaseName
                    + PROMPT_DATABASE_SUFFIX;

        } catch (IllegalStateException exception) {

            return config.getPrompt();
        }
    }

    /**
     * Terminal meta-command'lerini ilgili operasyona yönlendirir.
     */
    private void handleTerminalCommand(
            TerminalCommand command
    ) {

        TerminalCommandType type =
                command.getType();

        switch (type) {

            case HELP ->
                    handleHelp(
                            command
                    );

            case QUIT ->
                    handleQuit(
                            command
                    );

            case CLEAR ->
                    handleClear(
                            command
                    );

            case HISTORY ->
                    handleHistory(
                            command
                    );

            case TABLES ->
                    handleTables(
                            command
                    );

            case DESCRIBE ->
                    handleDescribe(
                            command
                    );

            case UNKNOWN ->
                    printUnknownCommand();
        }
    }

    private void handleHelp(
            TerminalCommand command
    ) {

        if (command.hasArguments()) {
            printUsage("\\help");
            return;
        }

        printHelp();
    }

    private void handleQuit(
            TerminalCommand command
    ) {

        if (command.hasArguments()) {
            printUsage("\\q");
            return;
        }

        stop();
    }

    private void handleClear(
            TerminalCommand command
    ) {

        if (command.hasArguments()) {
            printUsage("\\clear");
            return;
        }

        clearTerminal();
    }

    /**
     * \history komutunu işler.
     */
    private void handleHistory(
            TerminalCommand command
    ) {

        if (!command.hasArguments()) {

            printHistory();
            return;
        }

        if (command.getArguments().size() == 1
                && "clear".equalsIgnoreCase(
                command.getArgument(0)
        )) {

            session.clearHistory();

            output.println(
                    "History cleared."
            );

            return;
        }

        printUsage(
                "\\history [clear]"
        );
    }

    /**
     * Terminal yardım ekranını gösterir.
     */
    private void printHelp() {

        output.println();
        output.println("YEKDB Terminal Commands");
        output.println("-----------------------");
        output.println("\\help, \\h             Show this help");
        output.println("\\q, \\quit             Quit terminal");
        output.println("\\clear                 Clear terminal");
        output.println("\\history               Show command history");
        output.println("\\history clear         Clear command history");
        output.println("\\tables                List tables");
        output.println("\\describe <table>      Describe table");
        output.println("\\d <table>             Describe table");
        output.println();
        output.println("SQL statements must end with ';'.");
        output.println();
    }

    /**
     * Terminal ekranını ANSI escape sequence ile temizler.
     */
    private void clearTerminal() {

        output.print(
                "\033[H\033[2J"
        );

        output.getOutputStream().flush();
    }

    /**
     * Terminal history'sini numaralı şekilde gösterir.
     */
    private void printHistory() {

        if (!session.hasHistory()) {

            output.println(
                    "History is empty."
            );

            return;
        }

        output.println(
                "Command history:"
        );

        int index = 1;

        for (String historyEntry
                : session.getHistory()) {

            output.println(
                    index
                            + "  "
                            + formatHistoryEntry(
                            historyEntry
                    )
            );

            index++;
        }

        output.println(
                formatCount(
                        session.getHistorySize(),
                        "entry",
                        "entries"
                )
        );
    }

    /**
     * Multi-line SQL statement'ları history ekranında tek satıra dönüştürür.
     */
    private String formatHistoryEntry(
            String historyEntry
    ) {

        if (historyEntry == null
                || historyEntry.isBlank()) {

            return "";
        }

        return historyEntry
                .replaceAll(
                        "\\R+",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    /**
     * Aktif veritabanındaki tabloları listeler.
     */
    private void handleTables(
            TerminalCommand command
    ) {

        if (command.hasArguments()) {
            printUsage("\\tables");
            return;
        }

        try {

            String databaseName =
                    metadataService.getCurrentDatabaseName();

            List<String> tableNames =
                    metadataService.listTables();

            if (tableNames.isEmpty()) {

                output.println(
                        "No tables found in database '"
                                + databaseName
                                + "'."
                );

                return;
            }

            output.println(
                    "Tables in database '"
                            + databaseName
                            + "':"
            );

            int index = 1;

            for (String tableName : tableNames) {

                output.println(
                        index
                                + "  "
                                + tableName
                );

                index++;
            }

            output.println(
                    formatCount(
                            tableNames.size(),
                            "table",
                            "tables"
                    )
            );

        } catch (RuntimeException exception) {

            errorHandler.handleSqlError(
                    exception
            );
        }
    }

    /**
     * \describe veya \d terminal komutunu işler.
     */
    private void handleDescribe(
            TerminalCommand command
    ) {

        if (command.getArguments().size() != 1) {

            printUsage(
                    "\\describe <table>"
            );

            return;
        }

        String tableName =
                command.getArgument(
                        0
                );

        try {

            Table table =
                    metadataService.describeTable(
                            tableName
                    );

            printTableDescription(
                    table
            );

        } catch (RuntimeException exception) {

            errorHandler.handleSqlError(
                    exception
            );
        }
    }

    /**
     * Tablo şemasını terminalde hizalı ve okunabilir biçimde gösterir.
     */
    private void printTableDescription(
            Table table
    ) {

        List<Column> columns =
                table.getColumns();

        int columnNameWidth =
                "Column".length();

        int typeWidth =
                "Type".length();

        int constraintWidth =
                "Constraints".length();

        List<String> constraintDescriptions =
                new ArrayList<>(columns.size());

        for (Column column : columns) {

            String description =
                    formatColumnConstraints(
                            table,
                            column.getName()
                    );

            constraintDescriptions.add(
                    description
            );

            columnNameWidth =
                    Math.max(
                            columnNameWidth,
                            column.getName().length()
                    );

            typeWidth =
                    Math.max(
                            typeWidth,
                            column.getDataType()
                                    .name()
                                    .length()
                    );

            constraintWidth =
                    Math.max(
                            constraintWidth,
                            description.length()
                    );
        }

        String border =
                "+-"
                        + "-".repeat(columnNameWidth)
                        + "-+-"
                        + "-".repeat(typeWidth)
                        + "-+-"
                        + "-".repeat(constraintWidth)
                        + "-+";

        String rowFormat =
                "| %-"
                        + columnNameWidth
                        + "s | %-"
                        + typeWidth
                        + "s | %-"
                        + constraintWidth
                        + "s |";

        output.println(
                "Table: "
                        + table.getTableName()
        );

        output.println(border);
        output.println(
                String.format(
                        rowFormat,
                        "Column",
                        "Type",
                        "Constraints"
                )
        );
        output.println(border);

        for (int index = 0;
             index < columns.size();
             index++) {

            Column column =
                    columns.get(index);

            output.println(
                    String.format(
                            rowFormat,
                            column.getName(),
                            column.getDataType().name(),
                            constraintDescriptions.get(index)
                    )
            );
        }

        output.println(border);
        output.println(
                formatCount(
                        columns.size(),
                        "column",
                        "columns"
                )
        );
    }

    /**
     * Sprint 00-24 Phase 7:
     * Bir kolonun dahil olduğu constraint'leri terminalde
     * okunabilir biçime dönüştürür.
     */
    private String formatColumnConstraints(
            Table table,
            String columnName
    ) {

        List<String> descriptions =
                new ArrayList<>();

        for (Constraint constraint :
                table.getConstraints()) {

            boolean containsColumn =
                    constraint.columns()
                            .stream()
                            .anyMatch(value ->
                                    value.equalsIgnoreCase(
                                            columnName
                                    )
                            );

            if (!containsColumn) {
                continue;
            }

            descriptions.add(
                    formatConstraint(
                            constraint
                    )
            );
        }

        if (descriptions.isEmpty()) {
            return "-";
        }

        return String.join(
                ", ",
                descriptions
        );
    }

    private String formatConstraint(
            Constraint constraint
    ) {

        ConstraintType type =
                constraint.type();

        if (type == ConstraintType.NOT_NULL) {
            return "NOT NULL";
        }

        if (constraint.columns().size() == 1) {

            return switch (type) {
                case UNIQUE -> "UNIQUE";
                case PRIMARY_KEY -> "PRIMARY KEY";
                case NOT_NULL -> "NOT NULL";
                case FOREIGN_KEY -> "FOREIGN KEY";
            };
        }

        String columns =
                String.join(
                        ",",
                        constraint.columns()
                );

        return switch (type) {
            case PRIMARY_KEY ->
                    "PRIMARY KEY(" + columns + ")";
            case UNIQUE ->
                    "UNIQUE(" + columns + ")";
            case NOT_NULL ->
                    "NOT NULL";
            case FOREIGN_KEY ->
                    "FOREIGN KEY(" + columns + ")";
        };
    }

    private void printUnknownCommand() {

        output.error(
                "ERROR: Unknown terminal command. "
                        + "Type \\help for help."
        );
    }

    private void printUsage(
            String usage
    ) {

        output.println(
                "Usage: "
                        + usage
        );
    }

    private String formatCount(
            int count,
            String singular,
            String plural
    ) {

        return count
                + " "
                + (count == 1
                ? singular
                : plural);
    }

    /**
     * Terminal session'ını sonlandırır.
     */
    private void stop() {

        statementBuffer.clear();
        session.stop();

        output.println(
                "Bye."
        );
    }

    /**
     * Terminal başlangıç mesajlarını gösterir.
     */
    private void printWelcome() {

        output.println(
                "YEKDB Interactive SQL Terminal"
        );

        output.println(
                "Enter SQL statements terminated with ';'."
        );

        output.println(
                "Type \\help for terminal commands."
        );

        output.println();
    }

    public TerminalConfig getConfig() {
        return config;
    }

    public TerminalSession getSession() {
        return session;
    }

    public TerminalCommandParser getCommandParser() {
        return commandParser;
    }

    public SqlTerminalExecutor getSqlExecutor() {
        return sqlExecutor;
    }

    public TerminalOutput getOutput() {
        return output;
    }

    public QueryResultFormatter getResultFormatter() {
        return resultFormatter;
    }

    public TerminalErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public TerminalMetadataService getMetadataService() {
        return metadataService;
    }

    public SqlStatementBuffer getStatementBuffer() {
        return statementBuffer;
    }
}
