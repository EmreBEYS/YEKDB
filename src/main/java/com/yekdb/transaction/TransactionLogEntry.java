package com.yekdb.transaction;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Kalici transaction log dosyasindaki tek bir satiri temsil eder.
 */
public final class TransactionLogEntry {

    private final long transactionId;
    private final TransactionStatus status;
    private final TransactionAccessMode accessMode;
    private final TransactionIsolationLevel isolationLevel;
    private final Instant startedAt;
    private final Instant completedAt;

    public TransactionLogEntry(
            long transactionId,
            TransactionStatus status,
            TransactionAccessMode accessMode,
            TransactionIsolationLevel isolationLevel,
            Instant startedAt,
            Instant completedAt
    ) {

        if (transactionId <= 0) {
            throw new IllegalArgumentException(
                    "Transaction id must be positive."
            );
        }

        this.transactionId =
                transactionId;

        this.status =
                Objects.requireNonNull(
                        status,
                        "Status cannot be null."
                );

        this.accessMode =
                Objects.requireNonNull(
                        accessMode,
                        "AccessMode cannot be null."
                );

        this.isolationLevel =
                Objects.requireNonNull(
                        isolationLevel,
                        "IsolationLevel cannot be null."
                );

        this.startedAt =
                Objects.requireNonNull(
                        startedAt,
                        "StartedAt cannot be null."
                );

        if (status == TransactionStatus.ACTIVE && completedAt != null) {
            throw new IllegalArgumentException(
                    "Active transaction log entries cannot have CompletedAt."
            );
        }

        if (status != TransactionStatus.ACTIVE && completedAt == null) {
            throw new IllegalArgumentException(
                    "Completed transaction log entries must have CompletedAt."
            );
        }

        this.completedAt =
                completedAt;
    }

    public static TransactionLogEntry parse(
            String line
    ) {

        Objects.requireNonNull(
                line,
                "Line cannot be null."
        );

        Map<String, String> fields =
                new LinkedHashMap<>();

        String[] parts =
                line.split(
                        "\\|"
                );

        for (String part : parts) {
            String[] keyValue =
                    part.split(
                            "=",
                            2
                    );

            if (keyValue.length != 2 || keyValue[0].isBlank()) {
                throw new TransactionDurabilityException(
                        "Malformed transaction log entry."
                );
            }

            fields.put(
                    keyValue[0],
                    keyValue[1]
            );
        }

        try {
            return new TransactionLogEntry(
                    Long.parseLong(
                            required(
                                    fields,
                                    "txId"
                            )
                    ),
                    TransactionStatus.valueOf(
                            required(
                                    fields,
                                    "status"
                            )
                    ),
                    TransactionAccessMode.valueOf(
                            required(
                                    fields,
                                    "accessMode"
                            )
                    ),
                    TransactionIsolationLevel.valueOf(
                            required(
                                    fields,
                                    "isolationLevel"
                            )
                    ),
                    Instant.parse(
                            required(
                                    fields,
                                    "startedAt"
                            )
                    ),
                    parseCompletedAt(
                            required(
                                    fields,
                                    "completedAt"
                            )
                    )
            );
        } catch (RuntimeException exception) {
            if (exception instanceof TransactionDurabilityException) {
                throw exception;
            }

            throw new TransactionDurabilityException(
                    "Malformed transaction log entry."
            );
        }
    }

    public long getTransactionId() {
        return transactionId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public TransactionAccessMode getAccessMode() {
        return accessMode;
    }

    public TransactionIsolationLevel getIsolationLevel() {
        return isolationLevel;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public boolean isActive() {
        return status == TransactionStatus.ACTIVE;
    }

    private static String required(
            Map<String, String> fields,
            String name
    ) {

        String value =
                fields.get(
                        name
                );

        if (value == null || value.isBlank()) {
            throw new TransactionDurabilityException(
                    "Malformed transaction log entry."
            );
        }

        return value;
    }

    private static Instant parseCompletedAt(
            String value
    ) {

        if ("null".equals(value)) {
            return null;
        }

        return Instant.parse(
                value
        );
    }
}
