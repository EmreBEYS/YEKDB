package com.yekdb.query.statement;

import java.util.Objects;

/**
 * EXPLAIN SELECT statement modelidir.
 */
public final class ExplainStatement implements Statement {

    private final SelectStatement selectStatement;
    private final ExplainMode mode;

    public ExplainStatement(
            SelectStatement selectStatement
    ) {

        this(
                selectStatement,
                ExplainMode.PLAN
        );
    }

    public ExplainStatement(
            SelectStatement selectStatement,
            ExplainMode mode
    ) {

        this.selectStatement =
                Objects.requireNonNull(
                        selectStatement,
                        "SelectStatement cannot be null."
                );

        this.mode =
                Objects.requireNonNull(
                        mode,
                        "ExplainMode cannot be null."
                );
    }

    public SelectStatement getSelectStatement() {
        return selectStatement;
    }

    public ExplainMode getMode() {
        return mode;
    }

    public boolean isAnalyze() {
        return mode.executesQuery();
    }

    @Override
    public StatementType getType() {
        return StatementType.EXPLAIN;
    }
}
