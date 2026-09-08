package com.yekdb.query.statement;

/**
 * EXPLAIN komutunun sorguyu yalnizca planlayacagini veya gercekten
 * calistirarak runtime olcumleri uretecegini belirtir.
 */
public enum ExplainMode {

    PLAN(false),
    ANALYZE(true);

    private final boolean executesQuery;

    ExplainMode(boolean executesQuery) {
        this.executesQuery = executesQuery;
    }

    public boolean executesQuery() {
        return executesQuery;
    }
}
