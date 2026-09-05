package com.yekdb.procedure;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Stored procedure catalog kaydı için metadata bilgisini tutar.
 */
public final class ProcedureMetadata {

    private static final int CURRENT_VERSION = 1;

    private final String procedureName;
    private final int parameterCount;
    private final LocalDateTime createdAt;
    private final int version;

    public ProcedureMetadata(
            String procedureName,
            int parameterCount
    ) {
        this(
                procedureName,
                parameterCount,
                LocalDateTime.now(),
                CURRENT_VERSION
        );
    }

    public ProcedureMetadata(
            String procedureName,
            int parameterCount,
            LocalDateTime createdAt,
            int version
    ) {
        this.procedureName =
                ProcedureNameValidator.validate(
                        procedureName
                );

        if (parameterCount < 0) {
            throw new IllegalArgumentException(
                    "Parameter count cannot be negative."
            );
        }

        this.parameterCount = parameterCount;

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

    public String getProcedureName() {
        return procedureName;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ProcedureMetadata{" +
                "procedureName='" + procedureName + '\'' +
                ", parameterCount=" + parameterCount +
                ", createdAt=" + createdAt +
                ", version=" + version +
                '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof ProcedureMetadata that)) {
            return false;
        }

        return parameterCount == that.parameterCount
                && version == that.version
                && procedureName.equals(that.procedureName)
                && createdAt.equals(that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                procedureName,
                parameterCount,
                createdAt,
                version
        );
    }
}
