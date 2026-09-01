package com.yekdb.view;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * View catalog kaydı için metadata bilgisini tutar.
 */
public final class ViewMetadata {

    private static final int CURRENT_VERSION = 1;

    private final String viewName;
    private final LocalDateTime createdAt;
    private final int version;

    public ViewMetadata(String viewName) {
        this(
                viewName,
                LocalDateTime.now(),
                CURRENT_VERSION
        );
    }

    public ViewMetadata(
            String viewName,
            LocalDateTime createdAt,
            int version
    ) {
        this.viewName =
                ViewNameValidator.validate(viewName);

        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Creation time cannot be null."
        );

        if (version <= 0) {
            throw new IllegalArgumentException(
                    "Metadata version must be greater than zero."
            );
        }

        this.version = version;
    }

    public String getViewName() {
        return viewName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ViewMetadata{" +
                "viewName='" + viewName + '\'' +
                ", createdAt=" + createdAt +
                ", version=" + version +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof ViewMetadata that)) {
            return false;
        }

        return version == that.version
                && viewName.equals(that.viewName)
                && createdAt.equals(that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                viewName,
                createdAt,
                version
        );
    }
}
