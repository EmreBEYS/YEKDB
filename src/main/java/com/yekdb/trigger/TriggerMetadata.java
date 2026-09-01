package com.yekdb.trigger;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Trigger catalog kaydı için metadata bilgisini tutar.
 */
public final class TriggerMetadata {

    private static final int CURRENT_VERSION = 1;

    private final String triggerName;
    private final String tableName;
    private final LocalDateTime createdAt;
    private final int version;

    public TriggerMetadata(
            String triggerName,
            String tableName
    ) {
        this(
                triggerName,
                tableName,
                LocalDateTime.now(),
                CURRENT_VERSION
        );
    }

    public TriggerMetadata(
            String triggerName,
            String tableName,
            LocalDateTime createdAt,
            int version
    ) {
        this.triggerName =
                TriggerNameValidator.validate(
                        triggerName
                );

        this.tableName =
                TriggerNameValidator.validateObjectName(
                        tableName,
                        "Table name"
                );

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

    public String getTriggerName() {
        return triggerName;
    }

    public String getTableName() {
        return tableName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "TriggerMetadata{" +
                "triggerName='" + triggerName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", createdAt=" + createdAt +
                ", version=" + version +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof TriggerMetadata that)) {
            return false;
        }

        return version == that.version
                && triggerName.equals(that.triggerName)
                && tableName.equals(that.tableName)
                && createdAt.equals(that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                triggerName,
                tableName,
                createdAt,
                version
        );
    }
}
