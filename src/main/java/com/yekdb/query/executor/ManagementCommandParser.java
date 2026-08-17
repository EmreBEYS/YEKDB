package com.yekdb.query.executor;

import com.yekdb.query.command.Command;
import com.yekdb.query.command.CreateDatabaseCommand;
import com.yekdb.query.command.CreateTableCommand;
import com.yekdb.query.command.DropDatabaseCommand;
import com.yekdb.query.command.DropTableCommand;
import com.yekdb.query.command.UseDatabaseCommand;
import com.yekdb.table.Column;
import com.yekdb.table.DataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * QueryExecutor'ın geriye dönük yönetim SQL parser yolunu kapsüller.
 *
 * <p>INSERT/SELECT/UPDATE/DELETE yeni SqlParser hattını kullanırken,
 * CREATE DATABASE, USE, DROP DATABASE, CREATE TABLE ve DROP TABLE
 * komutları mevcut davranış korunarak bu sınıfta ayrıştırılır.</p>
 */
final class ManagementCommandParser {

    Command parse(String sql) {
        String upperSql = sql.toUpperCase(Locale.ROOT);

        if (upperSql.startsWith("CREATE DATABASE ")) {
            return new CreateDatabaseCommand(
                    extractValueAfterKeyword(sql, "CREATE DATABASE")
            );
        }

        if (upperSql.startsWith("USE DATABASE ")) {
            return new UseDatabaseCommand(
                    extractValueAfterKeyword(sql, "USE DATABASE")
            );
        }

        if (upperSql.startsWith("USE ")) {
            return new UseDatabaseCommand(
                    extractValueAfterKeyword(sql, "USE")
            );
        }

        if (upperSql.startsWith("DROP DATABASE ")) {
            return new DropDatabaseCommand(
                    extractValueAfterKeyword(sql, "DROP DATABASE")
            );
        }

        if (upperSql.startsWith("CREATE TABLE ")) {
            return parseCreateTableCommand(sql);
        }

        if (upperSql.startsWith("DROP TABLE ")) {
            return new DropTableCommand(
                    extractValueAfterKeyword(sql, "DROP TABLE")
            );
        }

        throw new QueryExecutionException(
                "Unsupported SQL statement: " + sql
        );
    }

    private CreateTableCommand parseCreateTableCommand(String sql) {
        int openParenthesisIndex = sql.indexOf('(');
        int closeParenthesisIndex = sql.lastIndexOf(')');

        if (openParenthesisIndex < 0
                || closeParenthesisIndex < 0
                || closeParenthesisIndex <= openParenthesisIndex) {
            throw new QueryExecutionException(
                    "Invalid CREATE TABLE statement: " + sql
            );
        }

        String tableName = sql.substring(
                "CREATE TABLE".length(),
                openParenthesisIndex
        ).trim();

        if (tableName.isBlank()) {
            throw new QueryExecutionException(
                    "CREATE TABLE statement must contain a table name."
            );
        }

        String columnDefinitionSection = sql.substring(
                openParenthesisIndex + 1,
                closeParenthesisIndex
        ).trim();

        if (columnDefinitionSection.isBlank()) {
            throw new QueryExecutionException(
                    "CREATE TABLE statement must contain columns."
            );
        }

        String remainingText = sql.substring(
                closeParenthesisIndex + 1
        ).trim();

        if (!remainingText.isEmpty()) {
            throw new QueryExecutionException(
                    "Unexpected text after CREATE TABLE definition: "
                            + remainingText
            );
        }

        return new CreateTableCommand(
                tableName,
                parseColumnDefinitions(columnDefinitionSection)
        );
    }

    private List<Column> parseColumnDefinitions(
            String columnDefinitionSection
    ) {
        List<Column> columns = new ArrayList<>();
        String[] definitions = columnDefinitionSection.split(",");

        for (String definition : definitions) {
            String normalizedDefinition = definition.trim();

            if (normalizedDefinition.isBlank()) {
                throw new QueryExecutionException(
                        "Column definition cannot be blank."
                );
            }

            String[] parts = normalizedDefinition.split("\\s+");

            if (parts.length != 2) {
                throw new QueryExecutionException(
                        "Invalid column definition: "
                                + normalizedDefinition
                );
            }

            columns.add(
                    new Column(
                            parts[0],
                            parseDataType(parts[1])
                    )
            );
        }

        if (columns.isEmpty()) {
            throw new QueryExecutionException(
                    "CREATE TABLE statement must contain valid columns."
            );
        }

        return List.copyOf(columns);
    }

    private DataType parseDataType(String value) {
        String normalizedType = value
                .trim()
                .toUpperCase(Locale.ROOT);

        return switch (normalizedType) {
            case "INT", "INTEGER" -> DataType.INT;
            case "LONG", "BIGINT" -> DataType.LONG;
            case "DOUBLE", "FLOAT", "REAL" -> DataType.DOUBLE;
            case "BOOLEAN", "BOOL" -> DataType.BOOLEAN;
            case "STRING", "TEXT", "VARCHAR" -> DataType.STRING;
            default -> throw new QueryExecutionException(
                    "Unsupported data type: " + value
            );
        };
    }

    private String extractValueAfterKeyword(
            String sql,
            String keyword
    ) {
        String value = sql.substring(keyword.length()).trim();

        if (value.isBlank()) {
            throw new QueryExecutionException(
                    keyword + " statement requires a name."
            );
        }

        if (value.contains(" ")) {
            throw new QueryExecutionException(
                    "Invalid value after "
                            + keyword
                            + ": "
                            + value
            );
        }

        return value;
    }
}
