package com.yekdb.query.statement;

import java.util.Objects;

/**
 * EXPLAIN SELECT statement modelidir.
 */
public final class ExplainStatement implements Statement {

    private final SelectStatement selectStatement;

    public ExplainStatement(
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

    @Override
    public StatementType getType() {
        return StatementType.EXPLAIN;
    }
}
