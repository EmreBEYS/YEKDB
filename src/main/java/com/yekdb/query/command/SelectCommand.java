package com.yekdb.query.command;

import java.util.List;
import java.util.Objects;

/**
 * SELECT SQL komutunu temsil eder.
 *
 * <p>Hem tüm sütunların hem de belirli sütunların
 * seçilmesini destekler.</p>
 */
public final class SelectCommand implements Command {

    /**
     * Kayıtların okunacağı tablo adı.
     */
    private final String tableName;

    /**
     * Seçilecek sütun adları.
     *
     * SELECT * kullanımında bu liste boştur.
     */
    private final List<String> selectedColumns;

    /**
     * Tüm sütunların seçilip seçilmediğini belirtir.
     */
    private final boolean selectAll;

    private SelectCommand(
            String tableName,
            List<String> selectedColumns,
            boolean selectAll
    ) {
        this.tableName = validateTableName(tableName);
        this.selectAll = selectAll;

        Objects.requireNonNull(
                selectedColumns,
                "Selected column list cannot be null."
        );

        this.selectedColumns = selectedColumns.stream()
                .map(SelectCommand::validateColumnName)
                .toList();

        if (!selectAll && this.selectedColumns.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one column must be selected."
            );
        }

        if (selectAll && !this.selectedColumns.isEmpty()) {
            throw new IllegalArgumentException(
                    "SELECT ALL command cannot contain column names."
            );
        }
    }

    /**
     * SELECT * FROM table komutu oluşturur.
     *
     * @param tableName hedef tablo adı
     * @return SELECT komutu
     */
    public static SelectCommand allFrom(String tableName) {
        return new SelectCommand(
                tableName,
                List.of(),
                true
        );
    }

    /**
     * Belirli sütunları seçen SELECT komutu oluşturur.
     *
     * @param tableName       hedef tablo adı
     * @param selectedColumns seçilecek sütunlar
     * @return SELECT komutu
     */
    public static SelectCommand columnsFrom(
            String tableName,
            List<String> selectedColumns
    ) {
        return new SelectCommand(
                tableName,
                selectedColumns,
                false
        );
    }

    public String getTableName() {
        return tableName;
    }

    public List<String> getSelectedColumns() {
        return selectedColumns;
    }

    public boolean isSelectAll() {
        return selectAll;
    }

    private static String validateTableName(String tableName) {
        String normalizedName = Objects.requireNonNull(
                tableName,
                "Table name cannot be null."
        ).trim();

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be blank."
            );
        }

        if (!normalizedName.matches(
                "[A-Za-z_][A-Za-z0-9_]*"
        )) {
            throw new IllegalArgumentException(
                    "Invalid table name: " + tableName
            );
        }

        return normalizedName;
    }

    private static String validateColumnName(String columnName) {
        String normalizedName = Objects.requireNonNull(
                columnName,
                "Column name cannot be null."
        ).trim();

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException(
                    "Column name cannot be blank."
            );
        }

        if (!normalizedName.matches(
                "[A-Za-z_][A-Za-z0-9_]*"
        )) {
            throw new IllegalArgumentException(
                    "Invalid column name: " + columnName
            );
        }

        return normalizedName;
    }

    @Override
    public String toString() {
        return "SelectCommand{" +
                "tableName='" + tableName + '\'' +
                ", selectedColumns=" + selectedColumns +
                ", selectAll=" + selectAll +
                '}';
    }
}