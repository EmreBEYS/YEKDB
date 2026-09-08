package com.yekdb.query.command;

import com.yekdb.query.statement.SelectStatement;
import com.yekdb.query.statement.ExplainMode;

import java.util.Objects;

/**
 * EXPLAIN komutunu execution katmanina tasiyan command modelidir.
 */
public final class ExplainCommand implements Command {

    private final SelectStatement selectStatement;
    private final ExplainMode mode;

    public ExplainCommand(
            SelectStatement selectStatement
    ) {

        this(
                selectStatement,
                ExplainMode.PLAN
        );
    }

    public ExplainCommand(
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
}
