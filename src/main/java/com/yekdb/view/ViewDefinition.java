package com.yekdb.view;

import java.util.Objects;

/**
 * YEKDB view tanımını temsil eder.
 *
 * <p>View fiziksel veri saklamaz. Kaynak SELECT ifadesi sonraki
 * phase'lerde parser ve executor tarafından genişletilerek çalıştırılır.</p>
 */
public final class ViewDefinition {

    private final String viewName;
    private final String sourceSelect;

    public ViewDefinition(
            String viewName,
            String sourceSelect
    ) {
        this.viewName =
                ViewNameValidator.validate(viewName);

        if (sourceSelect == null || sourceSelect.isBlank()) {
            throw new IllegalArgumentException(
                    "View source SELECT cannot be null or blank."
            );
        }

        this.sourceSelect = sourceSelect.trim();
    }

    public String getViewName() {
        return viewName;
    }

    public String getSourceSelect() {
        return sourceSelect;
    }

    @Override
    public String toString() {
        return "ViewDefinition{" +
                "viewName='" + viewName + '\'' +
                ", sourceSelect='" + sourceSelect + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof ViewDefinition that)) {
            return false;
        }

        return viewName.equals(that.viewName)
                && sourceSelect.equals(that.sourceSelect);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                viewName,
                sourceSelect
        );
    }
}
