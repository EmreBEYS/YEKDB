package com.yekdb.storage.table.header;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * TableHeader metadata alanlarını immutable şekilde güncellemek
 * için kullanılan yardımcı sınıftır.
 *
 * Mevcut header nesnesi değiştirilmez. Her güncelleme yeni bir
 * TableHeader örneği üretir.
 */
public final class TableHeaderUpdater {

    private TableHeaderUpdater() {
        // Utility class.
    }

    /**
     * Header'ın rowCount değerini belirtilen değer ile değiştirir.
     *
     * @param header mevcut table header
     * @param rowCount yeni row count değeri
     * @return güncellenmiş yeni TableHeader
     */
    public static TableHeader withRowCount(
            TableHeader header,
            long rowCount
    ) {

        Objects.requireNonNull(
                header,
                "Table header cannot be null."
        );

        if (rowCount < 0) {
            throw new TableHeaderUpdateException(
                    "Row count cannot be negative."
            );
        }

        TableHeader updatedHeader =
                new TableHeader(
                        header.getTableId(),
                        header.getTableName(),
                        header.getColumnCount(),
                        rowCount,
                        header.getFirstDataPageId(),
                        header.getLastDataPageId(),
                        header.getSchemaOffset(),
                        header.getFlags()
                );

        TableHeaderValidator.validate(
                updatedHeader
        );

        return updatedHeader;
    }

    /**
     * Row count değerini bir artırır.
     *
     * @param header mevcut table header
     * @return rowCount + 1 içeren yeni TableHeader
     */
    public static TableHeader incrementRowCount(
            TableHeader header
    ) {

        Objects.requireNonNull(
                header,
                "Table header cannot be null."
        );

        if (header.getRowCount() == Long.MAX_VALUE) {
            throw new TableHeaderUpdateException(
                    "Row count overflow."
            );
        }

        return withRowCount(
                header,
                header.getRowCount() + 1
        );
    }

    /**
     * Row count değerini bir azaltır.
     *
     * @param header mevcut table header
     * @return rowCount - 1 içeren yeni TableHeader
     */
    public static TableHeader decrementRowCount(
            TableHeader header
    ) {

        Objects.requireNonNull(
                header,
                "Table header cannot be null."
        );

        if (header.getRowCount() == 0) {
            throw new TableHeaderUpdateException(
                    "Row count cannot be decremented below zero."
            );
        }

        return withRowCount(
                header,
                header.getRowCount() - 1
        );
    }

    /**
     * Header'ın first ve last data page ID değerlerini
     * atomik olarak günceller.
     *
     * Boş page range:
     * firstDataPageId = -1
     * lastDataPageId  = -1
     *
     * @param header mevcut table header
     * @param firstDataPageId ilk data page ID
     * @param lastDataPageId son data page ID
     * @return güncellenmiş yeni TableHeader
     */
    public static TableHeader withDataPageRange(
            TableHeader header,
            long firstDataPageId,
            long lastDataPageId
    ) {

        Objects.requireNonNull(
                header,
                "Table header cannot be null."
        );

        boolean emptyRange =
                firstDataPageId == -1L
                        && lastDataPageId == -1L;

        boolean validRange =
                firstDataPageId >= 0L
                        && lastDataPageId >= 0L;

        if (!emptyRange && !validRange) {
            throw new TableHeaderUpdateException(
                    "First and last data page IDs must either both be -1 or both be non-negative."
            );
        }

        if (validRange
                && firstDataPageId > lastDataPageId) {

            throw new TableHeaderUpdateException(
                    "First data page ID cannot be greater than last data page ID."
            );
        }

        TableHeader updatedHeader =
                new TableHeader(
                        header.getTableId(),
                        header.getTableName(),
                        header.getColumnCount(),
                        header.getRowCount(),
                        firstDataPageId,
                        lastDataPageId,
                        header.getSchemaOffset(),
                        header.getFlags()
                );

        TableHeaderValidator.validate(
                updatedHeader
        );

        return updatedHeader;
    }

    /**
     * Mevcut lastDataPageId korunarak firstDataPageId değerini değiştirir.
     *
     * @param header mevcut table header
     * @param firstDataPageId yeni first data page ID
     * @return güncellenmiş yeni TableHeader
     */
    public static TableHeader withFirstDataPageId(
            TableHeader header,
            long firstDataPageId
    ) {

        Objects.requireNonNull(
                header,
                "Table header cannot be null."
        );

        return withDataPageRange(
                header,
                firstDataPageId,
                header.getLastDataPageId()
        );
    }

    /**
     * Mevcut firstDataPageId korunarak lastDataPageId değerini değiştirir.
     *
     * @param header mevcut table header
     * @param lastDataPageId yeni last data page ID
     * @return güncellenmiş yeni TableHeader
     */
    public static TableHeader withLastDataPageId(
            TableHeader header,
            long lastDataPageId
    ) {

        Objects.requireNonNull(
                header,
                "Table header cannot be null."
        );

        return withDataPageRange(
                header,
                header.getFirstDataPageId(),
                lastDataPageId
        );
    }

    /**
     * Disk üzerindeki table header'ın rowCount metadata
     * değerini günceller ve tekrar okuyarak doğrular.
     *
     * @param tableFile güncellenecek .tbl dosyası
     * @param rowCount yeni row count
     * @return diske yazılmış ve tekrar okunmuş header
     */
    public static TableHeader persistRowCount(
            Path tableFile,
            long rowCount
    ) {

        Objects.requireNonNull(
                tableFile,
                "Table file cannot be null."
        );

        try {

            TableHeader currentHeader =
                    TableHeaderIO.read(
                            tableFile
                    );

            TableHeader updatedHeader =
                    withRowCount(
                            currentHeader,
                            rowCount
                    );

            TableHeaderIO.write(
                    tableFile,
                    updatedHeader
            );

            return verifyPersistedHeader(
                    tableFile,
                    updatedHeader
            );

        } catch (IOException exception) {

            throw new TableHeaderUpdateException(
                    "Table header row count could not be persisted: "
                            + tableFile,
                    exception
            );
        }
    }

    /**
     * Disk üzerindeki rowCount değerini bir artırır.
     */
    public static TableHeader persistIncrementRowCount(
            Path tableFile
    ) {

        Objects.requireNonNull(
                tableFile,
                "Table file cannot be null."
        );

        try {

            TableHeader currentHeader =
                    TableHeaderIO.read(
                            tableFile
                    );

            TableHeader updatedHeader =
                    incrementRowCount(
                            currentHeader
                    );

            TableHeaderIO.write(
                    tableFile,
                    updatedHeader
            );

            return verifyPersistedHeader(
                    tableFile,
                    updatedHeader
            );

        } catch (IOException exception) {

            throw new TableHeaderUpdateException(
                    "Table header row count increment could not be persisted: "
                            + tableFile,
                    exception
            );
        }
    }

    /**
     * Disk üzerindeki rowCount değerini bir azaltır.
     */
    public static TableHeader persistDecrementRowCount(
            Path tableFile
    ) {

        Objects.requireNonNull(
                tableFile,
                "Table file cannot be null."
        );

        try {

            TableHeader currentHeader =
                    TableHeaderIO.read(
                            tableFile
                    );

            TableHeader updatedHeader =
                    decrementRowCount(
                            currentHeader
                    );

            TableHeaderIO.write(
                    tableFile,
                    updatedHeader
            );

            return verifyPersistedHeader(
                    tableFile,
                    updatedHeader
            );

        } catch (IOException exception) {

            throw new TableHeaderUpdateException(
                    "Table header row count decrement could not be persisted: "
                            + tableFile,
                    exception
            );
        }
    }

    /**
     * Disk üzerindeki first/last data page metadata
     * değerlerini atomik olarak günceller.
     */
    public static TableHeader persistDataPageRange(
            Path tableFile,
            long firstDataPageId,
            long lastDataPageId
    ) {

        Objects.requireNonNull(
                tableFile,
                "Table file cannot be null."
        );

        try {

            TableHeader currentHeader =
                    TableHeaderIO.read(
                            tableFile
                    );

            TableHeader updatedHeader =
                    withDataPageRange(
                            currentHeader,
                            firstDataPageId,
                            lastDataPageId
                    );

            TableHeaderIO.write(
                    tableFile,
                    updatedHeader
            );

            return verifyPersistedHeader(
                    tableFile,
                    updatedHeader
            );

        } catch (IOException exception) {

            throw new TableHeaderUpdateException(
                    "Table data page metadata could not be persisted: "
                            + tableFile,
                    exception
            );
        }
    }

    /**
     * Header'ın fiziksel dosyaya doğru şekilde yazıldığını
     * tekrar okuyarak doğrular.
     */
    private static TableHeader verifyPersistedHeader(
            Path tableFile,
            TableHeader expectedHeader
    ) throws IOException {

        TableHeader persistedHeader =
                TableHeaderIO.read(
                        tableFile
                );

        if (!expectedHeader.equals(
                persistedHeader
        )) {

            throw new TableHeaderUpdateException(
                    "Persisted table header does not match expected header: "
                            + tableFile
            );
        }

        return persistedHeader;
    }
}