package com.yekdb.query.command;

import java.util.Objects;

/**
 * CREATE VIEW SQL komutunu temsil eder.
 */
public final class CreateViewCommand implements Command {

    private final String viewName;
    private final String sourceSelect;

    public CreateViewCommand(
            String viewName,
            String sourceSelect
    ) {
        this.viewName = Objects.requireNonNull(
                viewName,
                "View name cannot be null."
        ).trim();

        if (this.viewName.isBlank()) {
            throw new IllegalArgumentException(
                    "View name cannot be blank."
            );
        }

        this.sourceSelect = Objects.requireNonNull(
                sourceSelect,
                "View source SELECT cannot be null."
        ).trim();

        if (this.sourceSelect.isBlank()) {
            throw new IllegalArgumentException(
                    "View source SELECT cannot be blank."
            );
        }
    }

    public String getViewName() {
        return viewName;
    }

    public String getSourceSelect() {
        return sourceSelect;
    }

    @Override
    public String toString() {
        return "CreateViewCommand{" +
                "viewName='" + viewName + '\'' +
                ", sourceSelect='" + sourceSelect + '\'' +
                '}';
    }
}
