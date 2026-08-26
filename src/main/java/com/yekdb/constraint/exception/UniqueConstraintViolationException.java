package com.yekdb.constraint.exception;

import java.util.List;

public final class UniqueConstraintViolationException
        extends ConstraintViolationException {

    private final List<String> columns;

    public UniqueConstraintViolationException(
            List<String> columns
    ) {

        super(
                "UNIQUE constraint violated for column(s): "
                        + String.join(", ", columns)
        );

        this.columns =
                List.copyOf(columns);
    }

    public List<String> getColumns() {
        return columns;
    }
}