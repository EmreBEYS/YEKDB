package com.yekdb.storage.table.header;

import com.yekdb.storage.exception.InvalidTableHeaderException;


/**
 * TableHeader nesnelerinin mantıksal doğrulamasını gerçekleştirir.
 */
public final class TableHeaderValidator {

    private TableHeaderValidator() {
    }

    /**
     * Verilen TableHeader nesnesinin geçerli olup olmadığını kontrol eder.
     *
     * @param header doğrulanacak header
     * @throws InvalidTableHeaderException header geçersiz ise
     */
    public static void validate(TableHeader header) {

        if (header == null) {
            throw new InvalidTableHeaderException(
                    "Table header cannot be null."
            );
        }

        validateTableId(header.getTableId());
        validateTableName(header.getTableName());
        validateColumnCount(header.getColumnCount());
        validateRowCount(header.getRowCount());
        validatePageIds(
                header.getFirstDataPageId(),
                header.getLastDataPageId()
        );
        validateSchemaOffset(header.getSchemaOffset());
        validateFlags(header.getFlags());
    }

    private static void validateTableId(long tableId) {

        if (tableId < 0) {
            throw new InvalidTableHeaderException(
                    "Table ID cannot be negative: " + tableId
            );
        }
    }

    private static void validateTableName(String tableName) {

        if (tableName == null) {
            throw new InvalidTableHeaderException(
                    "Table name cannot be null."
            );
        }

        if (tableName.isBlank()) {
            throw new InvalidTableHeaderException(
                    "Table name cannot be blank."
            );
        }

        byte[] encodedName =
                tableName.getBytes(TableHeaderConstants.TABLE_NAME_CHARSET);

        if (encodedName.length >
                TableHeaderConstants.MAX_TABLE_NAME_LENGTH) {

            throw new InvalidTableHeaderException(
                    "Table name exceeds maximum binary length. "
                            + "Maximum="
                            + TableHeaderConstants.MAX_TABLE_NAME_LENGTH
                            + " bytes, actual="
                            + encodedName.length
            );
        }
    }

    private static void validateColumnCount(int columnCount) {

        if (columnCount < 0) {
            throw new InvalidTableHeaderException(
                    "Column count cannot be negative: "
                            + columnCount
            );
        }
    }

    private static void validateRowCount(long rowCount) {

        if (rowCount < 0) {
            throw new InvalidTableHeaderException(
                    "Row count cannot be negative: "
                            + rowCount
            );
        }
    }

    private static void validatePageIds(
            long firstDataPageId,
            long lastDataPageId
    ) {

        if (firstDataPageId < -1) {
            throw new InvalidTableHeaderException(
                    "First data page ID cannot be less than -1: "
                            + firstDataPageId
            );
        }

        if (lastDataPageId < -1) {
            throw new InvalidTableHeaderException(
                    "Last data page ID cannot be less than -1: "
                            + lastDataPageId
            );
        }

        boolean firstMissing = firstDataPageId == -1;
        boolean lastMissing = lastDataPageId == -1;

        if (firstMissing != lastMissing) {
            throw new InvalidTableHeaderException(
                    "First and last data page IDs must either both "
                            + "exist or both be -1."
            );
        }
    }

    private static void validateSchemaOffset(long schemaOffset) {

        if (schemaOffset < 0) {
            throw new InvalidTableHeaderException(
                    "Schema offset cannot be negative: "
                            + schemaOffset
            );
        }
    }

    private static void validateFlags(int flags) {

        if (flags < 0) {
            throw new InvalidTableHeaderException(
                    "Flags cannot be negative: " + flags
            );
        }
    }
}