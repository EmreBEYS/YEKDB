package com.yekdb.transaction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Transaction baslangic ve tamamlanma kararlarini database klasoru altinda
 * kalici bir log dosyasina yazar ve recovery icin tekrar okur.
 */
public final class TransactionDurabilityLog {

    public static final String FILE_NAME =
            "transaction.log";

    private static final String HEADER =
            "YEKDB_TRANSACTION_LOG_V1";

    public void appendBegin(
            Path databasePath,
            TransactionContext transaction
    ) {

        Objects.requireNonNull(
                databasePath,
                "DatabasePath cannot be null."
        );

        Objects.requireNonNull(
                transaction,
                "Transaction cannot be null."
        );

        if (!transaction.isActive()) {
            throw new IllegalArgumentException(
                    "Only active transactions can be logged as started."
            );
        }

        append(
                databasePath,
                formatBegin(transaction)
        );
    }

    public void appendCompletion(
            Path databasePath,
            TransactionContext transaction
    ) {

        Objects.requireNonNull(
                databasePath,
                "DatabasePath cannot be null."
        );

        Objects.requireNonNull(
                transaction,
                "Transaction cannot be null."
        );

        if (transaction.isActive()) {
            throw new IllegalArgumentException(
                    "Only completed transactions can be logged."
            );
        }

        append(
                databasePath,
                formatCompletion(transaction)
        );
    }

    public void appendRecoveredRollback(
            Path databasePath,
            TransactionLogEntry activeEntry,
            Instant completedAt
    ) {

        Objects.requireNonNull(
                databasePath,
                "DatabasePath cannot be null."
        );

        Objects.requireNonNull(
                activeEntry,
                "ActiveEntry cannot be null."
        );

        Objects.requireNonNull(
                completedAt,
                "CompletedAt cannot be null."
        );

        if (!activeEntry.isActive()) {
            throw new IllegalArgumentException(
                    "Only active transaction log entries can be recovered."
            );
        }

        append(
                databasePath,
                formatRecoveredRollback(
                        activeEntry,
                        completedAt
                )
        );
    }

    public List<TransactionLogEntry> readEntries(
            Path databasePath
    ) {

        return readLines(
                databasePath
        ).stream()
                .filter(line -> !line.isBlank())
                .filter(line -> !HEADER.equals(line))
                .map(TransactionLogEntry::parse)
                .toList();
    }

    public List<TransactionLogEntry> readIncompleteTransactions(
            Path databasePath
    ) {

        Map<Long, TransactionLogEntry> activeTransactions =
                new LinkedHashMap<>();

        for (TransactionLogEntry entry : readEntries(databasePath)) {
            if (entry.isActive()) {
                activeTransactions.put(
                        entry.getTransactionId(),
                        entry
                );
            } else {
                activeTransactions.remove(
                        entry.getTransactionId()
                );
            }
        }

        return List.copyOf(
                activeTransactions.values()
        );
    }

    public List<String> readLines(
            Path databasePath
    ) {

        Objects.requireNonNull(
                databasePath,
                "DatabasePath cannot be null."
        );

        Path logFile =
                databasePath
                        .resolve(FILE_NAME)
                        .normalize();

        if (!Files.isRegularFile(logFile)) {
            return List.of();
        }

        try {
            return Files.readAllLines(
                    logFile,
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new TransactionDurabilityException(
                    "Transaction durability log could not be read."
            );
        }
    }

    private void append(
            Path databasePath,
            String entry
    ) {

        Path logFile =
                databasePath
                        .resolve(FILE_NAME)
                        .normalize();

        try {

            Files.createDirectories(
                    databasePath
            );

            ensureHeader(
                    logFile
            );

            Files.writeString(
                    logFile,
                    entry + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );

        } catch (IOException exception) {
            throw new TransactionDurabilityException(
                    "Transaction durability log could not be written."
            );
        }
    }

    private void ensureHeader(
            Path logFile
    ) throws IOException {

        if (Files.exists(logFile)) {
            return;
        }

        Files.writeString(
                logFile,
                HEADER + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }

    private String formatBegin(
            TransactionContext transaction
    ) {

        return "txId="
                + transaction.getTransactionId()
                + "|status="
                + transaction.getStatus().name()
                + "|accessMode="
                + transaction.getAccessMode().name()
                + "|isolationLevel="
                + transaction.getIsolationLevel().name()
                + "|startedAt="
                + transaction.getStartedAt()
                + "|completedAt=null";
    }

    private String formatRecoveredRollback(
            TransactionLogEntry entry,
            Instant completedAt
    ) {

        return "txId="
                + entry.getTransactionId()
                + "|status="
                + TransactionStatus.ROLLED_BACK.name()
                + "|accessMode="
                + entry.getAccessMode().name()
                + "|isolationLevel="
                + entry.getIsolationLevel().name()
                + "|startedAt="
                + entry.getStartedAt()
                + "|completedAt="
                + completedAt;
    }

    private String formatCompletion(
            TransactionContext transaction
    ) {

        Instant completedAt =
                transaction.getCompletedAt();

        return "txId="
                + transaction.getTransactionId()
                + "|status="
                + transaction.getStatus().name()
                + "|accessMode="
                + transaction.getAccessMode().name()
                + "|isolationLevel="
                + transaction.getIsolationLevel().name()
                + "|startedAt="
                + transaction.getStartedAt()
                + "|completedAt="
                + completedAt;
    }
}

