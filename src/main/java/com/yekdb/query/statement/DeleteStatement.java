package com.yekdb.query.statement;

/**
 * Parser tarafından ayrıştırılmış DELETE sorgusunu temsil eder.
 *
 * <p>Örnek SQL:</p>
 *
 * <pre>
 * DELETE FROM users WHERE id = 1;
 * DELETE FROM users;
 * </pre>
 */

public final class DeleteStatement implements Statement {

    /**
     * Kayıtların silineceği tablo adı.
     */
    private final String tableName;

    /**
     * DELETE işleminde kullanılacak WHERE koşulu.
     *
     * <p>İlk sürümde koşul metinsel olarak saklanmaktadır.
     * İlerleyen sprintlerde ayrı bir Condition modeline
     * dönüştürülebilir.</p>
     */
    private final String whereClause;

    /**
     * Yeni bir DeleteStatement oluşturur.
     *
     * @param tableName  kayıtların silineceği tablo
     * @param whereClause WHERE koşulu; koşulsuz DELETE için null olabilir
     */
    public DeleteStatement(
            String tableName,
            String whereClause
    ) {
        this.tableName = validateTableName(tableName);
        this.whereClause = normalizeWhereClause(whereClause);
    }

    /**
     * Statement türünü döndürür.
     *
     * @return DELETE
     */
    @Override
    public StatementType getType() {
        return StatementType.DELETE;
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
     * WHERE koşulunu döndürür.
     *
     * @return koşul veya koşul bulunmuyorsa null
     */
    public String getWhereClause() {
        return whereClause;
    }

    /**
     * Statement içerisinde WHERE koşulu bulunup
     * bulunmadığını kontrol eder.
     *
     * @return WHERE koşulu varsa true
     */
    public boolean hasWhereClause() {
        return whereClause != null;
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
     * WHERE koşulunu temizler.
     *
     * Null veya yalnızca boşluk içeren koşullar
     * koşul bulunmuyor şeklinde değerlendirilir.
     */
    private String normalizeWhereClause(
            String whereClause
    ) {
        if (whereClause == null
                || whereClause.isBlank()) {
            return null;
        }

        return whereClause.trim();
    }

    @Override
    public String toString() {
        return "DeleteStatement{" +
                "tableName='" + tableName + '\'' +
                ", whereClause='" + whereClause + '\'' +
                '}';
    }
}
