package com.yekdb.query.command;

import java.util.Objects;

/**
 * DROP VIEW SQL komutunu temsil eder.
 */
public final class DropViewCommand implements Command {

    private final String viewName;

    public DropViewCommand(String viewName) {
        this.viewName = Objects.requireNonNull(
                viewName,
                "View name cannot be null."
        ).trim();

        if (this.viewName.isBlank()) {
            throw new IllegalArgumentException(
                    "View name cannot be blank."
            );
        }
    }

    public String getViewName() {
        return viewName;
    }

    @Override
    public String toString() {
        return "DropViewCommand{" +
                "viewName='" + viewName + '\'' +
                '}';
    }
}
