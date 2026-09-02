package com.yekdb.query.command;

import com.yekdb.query.statement.SelectStatement;

import java.util.Objects;

/**
 * EXPLAIN komutunu execution katmanina tasiyan command modelidir.
 */
public final class ExplainCommand implements Command {

    private final SelectStatement selectStatement;

    public ExplainCommand(
            SelectStatement selectStatement
    ) {

        this.selectStatement =
                Objects.requireNonNull(
                        selectStatement,
                        "SelectStatement cannot be null."
                );
    }

    public SelectStatement getSelectStatement() {
        return selectStatement;
    }
}
