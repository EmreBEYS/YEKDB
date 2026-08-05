package com.yekdb.query.statement;

import java.util.List;
import java.util.Objects;

/**
 * Parser tarafından ayrıştırılmış INSERT sorgusunu temsil eder.
 *
 * <p>Örnek SQL:</p>
 *
 * <pre>
 * INSERT INTO users VALUES (1, 'Emre', 21);
 * </pre>
 */
public final class InsertStatement implements Statement {

    /**
     * Kaydın ekleneceği tablo adı.
     */
    private final String tableName;

    /**
     * INSERT sorgusunda belirtilen değerler.
     */
    private final List<Object> values;

    /**
     * Yeni bir InsertStatement oluşturur.
     *
     * @param tableName hedef tablo adı
     * @param values    eklenecek değerler
     */
    public InsertStatement(
            String tableName,
            List<Object> values
    ) {
        this.tableName = validateTableName(tableName);

        Objects.requireNonNull(
                values,
                "Values cannot be null."
        );

        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    "INSERT statement must contain at least one value."
            );
        }

        this.values = java.util.Collections.unmodifiableList(
                new java.util.ArrayList<>(values)
        );
    }

    /**
     * Statement türünü döndürür.
     *
     * @return INSERT
     */
    @Override
    public StatementType getType() {
        return StatementType.INSERT;
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
     * Eklenecek değerleri değiştirilemez liste olarak döndürür.
     *
     * @return değer listesi
     */
    public List<Object> getValues() {
        return values;
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

    @Override
    public String toString() {
        return "InsertStatement{" +
                "tableName='" + tableName + '\'' +
                ", values=" + values +
                '}';
    }
}