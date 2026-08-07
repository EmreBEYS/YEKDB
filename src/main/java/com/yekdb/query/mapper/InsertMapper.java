package com.yekdb.query.mapper;

import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.statement.InsertStatement;

import java.util.Objects;

/**
 * InsertStatement modelini InsertCommand modeline dönüştürür.
 */
public final class InsertMapper {

    public InsertCommand map(
            InsertStatement statement
    ) {
        Objects.requireNonNull(
                statement,
                "InsertStatement cannot be null."
        );

        return new InsertCommand(
                statement.getTableName(),
                statement.getColumns(),
                statement.getValues()
        );
    }
}