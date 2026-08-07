package com.yekdb.query.mapper;

import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.parser.ExpressionParser;
import com.yekdb.query.statement.UpdateStatement;

import java.util.Objects;

/**
 * UpdateStatement nesnesini execution katmanında
 * kullanılacak UpdateCommand nesnesine dönüştürür.
 */
public final class UpdateMapper {

    private final ExpressionParser expressionParser;

    public UpdateMapper() {
        this(
                new ExpressionParser()
        );
    }

    public UpdateMapper(
            ExpressionParser expressionParser
    ) {
        this.expressionParser =
                Objects.requireNonNull(
                        expressionParser,
                        "ExpressionParser cannot be null."
                );
    }

    public UpdateCommand map(
            UpdateStatement statement
    ) {

        Objects.requireNonNull(
                statement,
                "UpdateStatement cannot be null."
        );

        Expression whereExpression =
                null;

        if (statement.hasWhereClause()) {

            whereExpression =
                    expressionParser.parse(
                            statement.getWhereClause()
                    );
        }

        return new UpdateCommand(
                statement.getTableName(),
                statement.getUpdatedValues(),
                whereExpression
        );
    }
}