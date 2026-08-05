package com.yekdb.query.statement;

import java.util.Objects;
import java.util.List;

/**
 * Parser tarafından ayrıştırılmış SELECT sorgusunu temsil eder.
 *
 * <p>Örnek SQL:</p>
 *
 * <pre>
 * SELECT * FROM users;
 * SELECT id, name FROM users;
 * </pre>
 */

public final class SelectStatement implements Statement {
    /**
     * Verilerin okunacağı tablo adı.
     */
    private final String tableName;

    /**
     * Sorguda seçilen sütunlar.
     *
     * "*" değeri tüm sütunları temsil eder.
     */
    private final List<String> selectedColumns;

    /**
     * Yeni bir SelectStatement oluşturur.
     *
     * @param tableName       hedef tablo adı
     * @param selectedColumns seçilecek sütun adları
     */
    public SelectStatement(String tableName,List<String> selectedColumns){
        this.tableName=validateTableName(tableName);
        Objects.requireNonNull(selectedColumns,"Selected columns cannot be null.");
        if(selectedColumns.isEmpty()){
            throw new IllegalArgumentException("SELECT statement must contain at least one column.");
        }
        this.selectedColumns=selectedColumns.stream().map(this::validateColumnName).toList();
    }
    /**
     * Tüm sütunları seçen bir SelectStatement oluşturur.
     *
     * @param tableName hedef tablo adı
     * @return SELECT * statement modeli
     */
    public static SelectStatement allColumns(
            String tableName
    ) {
        return new SelectStatement(
                tableName,
                List.of("*")
        );
    }

    /**
     * Statement türünü döndürür.
     *
     * @return SELECT
     */
    @Override
    public StatementType getType() {
        return StatementType.SELECT;
    }

    /**
     * Hedef tablo adını döndürür.
     *
     * @return tablo adı
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Seçilen sütunları döndürür.
     *
     * @return değiştirilemez sütun listesi
     */
    public List<String> getSelectedColumns() {
        return selectedColumns;
    }

    /**
     * Sorgunun bütün sütunları seçip seçmediğini döndürür.
     *
     * @return SELECT * sorgusuysa true
     */
    public boolean selectsAllColumns() {
        return selectedColumns.size() == 1
                && "*".equals(selectedColumns.get(0));
    }

    /**
     * Tablo adını doğrular ve temizler.
     */
    private String validateTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }

        String normalizedName = tableName.trim();

        if (!normalizedName.matches(
                "[A-Za-z_][A-Za-z0-9_]*"
        )) {
            throw new IllegalArgumentException(
                    "Invalid table name: " + tableName
            );
        }

        return normalizedName;
    }

    /**
     * Sütun adını doğrular ve temizler.
     */
    private String validateColumnName(String columnName) {
        if (columnName == null || columnName.isBlank()) {
            throw new IllegalArgumentException(
                    "Column name cannot be null or blank."
            );
        }

        String normalizedName = columnName.trim();

        if ("*".equals(normalizedName)) {
            return normalizedName;
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
        return "SelectStatement{" +
                "tableName='" + tableName + '\'' +
                ", selectedColumns=" + selectedColumns +
                '}';
    }
}
