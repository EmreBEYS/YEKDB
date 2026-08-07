package com.yekdb.query.command;

import com.yekdb.query.expression.Expression;

import java.util.Objects;

/**
 * DELETE SQL komutunu execution katmanında temsil eder.
 *
 * Örnek:
 *
 * DELETE FROM users WHERE id = 1;
 * DELETE FROM users;
 *
 * WHERE koşulu parser/mapper zincirinde Expression
 * nesnesine dönüştürülür.
 */
public final class DeleteCommand implements Command {

    private final String tableName;
    private final Expression whereExpression;

    public DeleteCommand(
            String tableName,
            Expression whereExpression
    ) {
        this.tableName =
                validateTableName(
                        tableName
                );

        this.whereExpression =
                whereExpression;
    }

    public String getTableName() {
        return tableName;
    }

    public Expression getWhereExpression() {
        return whereExpression;
    }

    public boolean hasWhereExpression() {
        return whereExpression != null;
    }

    private String validateTableName(
            String tableName
    ) {
        String normalizedName =
                Objects.requireNonNull(
                        tableName,
                        "Table name cannot be null."
                ).trim();

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be blank."
            );
        }

        if (!normalizedName.matches(
                "[A-Za-z_][A-Za-z0-9_]*"
        )) {
            throw new IllegalArgumentException(
                    "Invalid table name: "
                            + tableName
            );
        }

        return normalizedName;
    }

    @Override
    public String toString() {
        return "DeleteCommand{" +
                "tableName='" + tableName + '\'' +
                ", whereExpression=" + whereExpression +
                '}';
    }
}