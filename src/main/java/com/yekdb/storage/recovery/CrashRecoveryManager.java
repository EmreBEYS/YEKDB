package com.yekdb.storage.recovery;

import com.yekdb.storage.file.DataFile;
import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.record.page.Page;
import com.yekdb.storage.wal.WalCorruptionException;
import com.yekdb.storage.wal.WalManager;
import com.yekdb.storage.wal.WalRecord;
import com.yekdb.storage.wal.WalRecordType;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fiziksel WAL icin analysis, redo ve undo kurtarmasini yurutur.
 *
 * <p>Transaction siniri bulunmayan 01-01 kayitlari geriye uyumluluk
 * amaciyla ortuk tamamlanmis kabul edilir. Acik BEGIN kaydi bulunan
 * transaction'lar ise yalnizca COMMIT ile winner olur.</p>
 */
public final class CrashRecoveryManager {

    public CrashRecoveryResult recover(
            DataFile dataFile,
            WalManager walManager
    ) throws IOException {

        List<WalRecord> records = walManager.readAll();
        Analysis analysis = analyze(records);
        Counters counters = new Counters();

        applyFinalPageImages(
                dataFile,
                records,
                analysis,
                counters
        );

        repairHeaderPageCount(dataFile, counters);
        dataFile.sync();

        for (long transactionId : analysis.incomplete) {
            walManager.appendTransactionRollback(transactionId);
        }

        return new CrashRecoveryResult(
                records.size(),
                analysis.committed,
                analysis.incomplete,
                counters.redone,
                counters.undone,
                counters.skipped,
                walManager.repairedTornTailOnOpen()
        );
    }

    private Analysis analyze(
            List<WalRecord> records
    ) throws WalCorruptionException {

        Map<Long, TransactionState> states = new HashMap<>();
        Set<Long> legacyTransactions = new HashSet<>();
        Set<Long> committed = new LinkedHashSet<>();
        Set<Long> incomplete = new LinkedHashSet<>();

        for (WalRecord record : records) {
            long transactionId = record.getTransactionId();

            if (record.getType() == WalRecordType.PAGE_UPDATE) {
                validatePageUpdate(record);

                TransactionState state = states.get(transactionId);

                if (state == TransactionState.COMMITTED
                        || state == TransactionState.ROLLED_BACK) {
                    throw corrupt(record,
                            "page update appears after transaction completion");
                }

                if (transactionId > 0 && state == null) {
                    legacyTransactions.add(transactionId);
                }
                continue;
            }

            if (legacyTransactions.contains(transactionId)) {
                throw corrupt(record,
                        "transaction boundary follows a legacy page update");
            }

            switch (record.getType()) {
                case TRANSACTION_BEGIN -> {
                    if (states.putIfAbsent(
                            transactionId,
                            TransactionState.ACTIVE
                    ) != null) {
                        throw corrupt(record,
                                "duplicate transaction begin");
                    }
                }
                case TRANSACTION_COMMIT -> {
                    requireActive(states, record);
                    states.put(transactionId,
                            TransactionState.COMMITTED);
                    committed.add(transactionId);
                }
                case TRANSACTION_ROLLBACK -> {
                    requireActive(states, record);
                    states.put(transactionId,
                            TransactionState.ROLLED_BACK);
                }
                default -> throw corrupt(record,
                        "unsupported WAL record type");
            }
        }

        for (Map.Entry<Long, TransactionState> entry
                : states.entrySet()) {
            if (entry.getValue() == TransactionState.ACTIVE) {
                incomplete.add(entry.getKey());
            }
        }

        return new Analysis(
                Set.copyOf(committed),
                Set.copyOf(incomplete),
                Set.copyOf(legacyTransactions)
        );
    }

    private void validatePageUpdate(
            WalRecord record
    ) throws WalCorruptionException {

        int beforeLength = record.getBeforeImage().length;
        int afterLength = record.getAfterImage().length;

        if ((beforeLength != 0 && beforeLength != Page.PAGE_SIZE)
                || afterLength != Page.PAGE_SIZE) {
            throw corrupt(record,
                    "page images do not match the physical page size");
        }
    }

    private void requireActive(
            Map<Long, TransactionState> states,
            WalRecord record
    ) throws WalCorruptionException {

        if (states.get(record.getTransactionId())
                != TransactionState.ACTIVE) {
            throw corrupt(record,
                    "transaction completion has no active begin");
        }
    }

    private void applyFinalPageImages(
            DataFile dataFile,
            List<WalRecord> records,
            Analysis analysis,
            Counters counters
    ) throws IOException {

        Map<Integer, PagePlan> plans = new LinkedHashMap<>();

        for (WalRecord record : records) {
            if (record.getType() != WalRecordType.PAGE_UPDATE) {
                continue;
            }

            PagePlan plan = plans.computeIfAbsent(
                    record.getPageId(),
                    ignored -> new PagePlan(
                            record,
                            record.getBeforeImage(),
                            RecoveryAction.UNDO
                    )
            );

            if (analysis.shouldRedo(record)) {
                plan.source = record;
                plan.image = record.getAfterImage();
                plan.action = RecoveryAction.REDO;
            } else if (analysis.incomplete.contains(
                    record.getTransactionId()
            )) {
                plan.source = record;
                plan.action = RecoveryAction.UNDO;
            }
        }

        List<PagePlan> removals = plans.values()
                .stream()
                .filter(plan -> plan.image.length == 0)
                .sorted((left, right) -> Integer.compare(
                        right.source.getPageId(),
                        left.source.getPageId()
                ))
                .toList();

        for (PagePlan plan : plans.values()) {
            if (plan.image.length != 0) {
                applyPageImage(dataFile, plan, counters);
            }
        }

        for (PagePlan plan : removals) {
            applyPageImage(dataFile, plan, counters);
        }
    }

    private void applyPageImage(
            DataFile dataFile,
            PagePlan plan,
            Counters counters
    ) throws IOException {

        WalRecord record = plan.source;
        long offset = pageOffset(record.getPageId());
        byte[] image = plan.image;
        long size = dataFile.size();

        if (image.length == 0) {
            if (size == offset) {
                counters.skipped++;
                return;
            }
            if (size != offset + Page.PAGE_SIZE) {
                throw corrupt(record,
                        "new page undo is not at the data file tail");
            }
            dataFile.resize(offset);
            counters.undone++;
            return;
        }

        if (size < offset) {
            throw corrupt(record,
                    "recovery would create a gap in the data file");
        }

        if (size >= offset + Page.PAGE_SIZE) {
            byte[] current = dataFile.read(offset, Page.PAGE_SIZE);
            if (Arrays.equals(current, image)) {
                counters.skipped++;
                return;
            }
        }

        dataFile.write(offset, image);
        if (plan.action == RecoveryAction.REDO) {
            counters.redone++;
        } else {
            counters.undone++;
        }
    }

    private void repairHeaderPageCount(
            DataFile dataFile,
            Counters counters
    ) throws IOException {

        long pageArea = dataFile.size() - DatabaseHeader.HEADER_SIZE;

        if (pageArea < 0 || pageArea % Page.PAGE_SIZE != 0) {
            throw new WalCorruptionException(
                    "Recovery produced an unaligned data file."
            );
        }

        int physicalPages = Math.toIntExact(
                pageArea / Page.PAGE_SIZE
        );
        DatabaseHeader header = DatabaseHeader.fromBytes(
                dataFile.read(0, DatabaseHeader.HEADER_SIZE)
        );

        if (header.getTotalPages() == physicalPages) {
            return;
        }

        DatabaseHeader repaired = new DatabaseHeader(
                header.getMagicNumber(),
                header.getVersion(),
                header.getPageSize(),
                physicalPages,
                header.getCreationTime(),
                header.getLastCheckpoint()
        );
        dataFile.write(0, repaired.toBytes());
    }

    private long pageOffset(int pageId) {
        return DatabaseHeader.HEADER_SIZE
                + Math.multiplyExact(
                (long) pageId,
                Page.PAGE_SIZE
        );
    }

    private WalCorruptionException corrupt(
            WalRecord record,
            String reason
    ) {
        return new WalCorruptionException(
                "Invalid WAL sequence at LSN "
                        + record.getLsn()
                        + " for transaction "
                        + record.getTransactionId()
                        + ": "
                        + reason
                        + "."
        );
    }

    private enum TransactionState {
        ACTIVE,
        COMMITTED,
        ROLLED_BACK
    }

    private record Analysis(
            Set<Long> committed,
            Set<Long> incomplete,
            Set<Long> legacyTransactions
    ) {
        private boolean shouldRedo(WalRecord record) {
            long transactionId = record.getTransactionId();
            return transactionId == 0
                    || legacyTransactions.contains(transactionId)
                    || committed.contains(transactionId);
        }
    }

    private static final class Counters {
        private int redone;
        private int undone;
        private int skipped;
    }

    private enum RecoveryAction {
        REDO,
        UNDO
    }

    private static final class PagePlan {
        private WalRecord source;
        private byte[] image;
        private RecoveryAction action;

        private PagePlan(
                WalRecord source,
                byte[] image,
                RecoveryAction action
        ) {
            this.source = source;
            this.image = image;
            this.action = action;
        }
    }
}
