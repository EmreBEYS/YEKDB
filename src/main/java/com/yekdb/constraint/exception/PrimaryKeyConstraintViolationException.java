package com.yekdb.constraint.exception;

import java.util.List;

public final class PrimaryKeyConstraintViolationException
        extends ConstraintViolationException {

    private final List<String> columns;

    public PrimaryKeyConstraintViolationException(
            List<String> columns,
            String reason
    ) {

        super(
                "PRIMARY KEY constraint violated for column(s): "
                        + String.join(", ", columns)
                        + ". Reason: "
                        + reason
        );

        this.columns =
                List.copyOf(columns);
    }

    public List<String> getColumns() {
        return columns;
    }
}