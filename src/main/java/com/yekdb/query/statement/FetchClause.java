package com.yekdb.query.statement;

import java.util.Objects;

/**
 * SQL FETCH ifadesini temsil eder.
 *
 * Desteklenen örnekler:
 *
 * FETCH FIRST 10 ROWS ONLY
 * FETCH NEXT 10 ROWS ONLY
 *
 * Sprint 00-14
 */
public final class FetchClause {

    public enum Mode {
        FIRST,
        NEXT
    }

    private final Mode mode;
    private final int rowCount;

    public FetchClause(
            Mode mode,
            int rowCount
    ) {

        this.mode =
                Objects.requireNonNull(
                        mode,
                        "FETCH mode cannot be null."
                );

        if (rowCount < 0) {
            throw new IllegalArgumentException(
                    "FETCH row count cannot be negative."
            );
        }

        this.rowCount =
                rowCount;
    }

    public Mode getMode() {
        return mode;
    }

    public int getRowCount() {
        return rowCount;
    }

    public boolean isFirst() {
        return mode == Mode.FIRST;
    }

    public boolean isNext() {
        return mode == Mode.NEXT;
    }

    @Override
    public String toString() {

        return "FETCH "
                + mode
                + " "
                + rowCount
                + " ROWS ONLY";
    }
}