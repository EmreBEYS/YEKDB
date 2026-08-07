package com.yekdb.query.mapper;

import com.yekdb.query.command.DeleteCommand;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.parser.ExpressionParser;
import com.yekdb.query.statement.DeleteStatement;

import java.util.Objects;

/**
 * DeleteStatement nesnesini DeleteCommand
 * nesnesine dönüştürür.
 *
 * WHERE koşulu varsa ExpressionParser kullanılarak
 * Expression modeline çevrilir.
 */
public final class DeleteMapper {

    private final ExpressionParser expressionParser;

    public DeleteMapper() {
        this(
                new ExpressionParser()
        );
    }

    public DeleteMapper(
            ExpressionParser expressionParser
    ) {
        this.expressionParser =
                Objects.requireNonNull(
                        expressionParser,
                        "ExpressionParser cannot be null."
                );
    }

    public DeleteCommand map(
            DeleteStatement statement
    ) {
        Objects.requireNonNull(
                statement,
                "DeleteStatement cannot be null."
        );

        Expression whereExpression =
                null;

        if (statement.hasWhereClause()) {
            whereExpression =
                    expressionParser.parse(
                            statement.getWhereClause()
                    );
        }

        return new DeleteCommand(
                statement.getTableName(),
                whereExpression
        );
    }
}