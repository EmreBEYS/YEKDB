package com.yekdb.query.executor;

import com.yekdb.table.Column;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * SELECT yürütme akışında kullanılan kolon çözümleme yardımcılarını
 * tek bir yerde toplar.
 *
 * <p>Bu sınıf tablo kolonlarının bulunması, qualified kolon adlarının
 * normalize edilmesi ve JOIN sonuç map'lerinde case-insensitive kolon
 * erişimi gibi tekrar eden işlemlerden sorumludur.</p>
 *
 * <p>Sınıf package-private tutulur; query executor katmanının dahili
 * yardımcı bileşenidir.</p>
 */
final class SelectColumnResolver {

    private SelectColumnResolver() {
        // Utility sınıfı.
    }

    static Column findColumnOrNull(
            List<Column> columns,
            String columnName
    ) {

        Objects.requireNonNull(
                columns,
                "Columns cannot be null."
        );

        String normalized =
                normalizeColumnName(columnName);

        for (Column column : columns) {

            if (column != null
                    && column.getName()
                    .equalsIgnoreCase(normalized)) {

                return column;
            }
        }

        return null;
    }

    static Column findColumn(
            List<Column> columns,
            String columnName
    ) {

        int index =
                findColumnIndex(
                        columns,
                        columnName
                );

        return columns.get(index);
    }

    static int findColumnIndex(
            List<Column> columns,
            String columnName
    ) {

        Objects.requireNonNull(
                columns,
                "Columns cannot be null."
        );

        String normalizedColumnName =
                normalizeColumnName(columnName);

        for (int i = 0;
             i < columns.size();
             i++) {

            Column column = columns.get(i);

            if (column != null
                    && column.getName()
                    .equalsIgnoreCase(
                            normalizedColumnName
                    )) {

                return i;
            }
        }

        throw new QueryExecutionException(
                "Column not found: "
                        + columnName
        );
    }

    /**
     * Qualified kolon adını fiziksel kolon adına indirger.
     *
     * <pre>
     * e.salary  -> salary
     * salary    -> salary
     * </pre>
     */
    static String normalizeColumnName(
            String columnName
    ) {

        String normalized =
                Objects.requireNonNull(
                                columnName,
                                "Column name cannot be null."
                        )
                        .trim();

        if (normalized.isEmpty()) {
            throw new QueryExecutionException(
                    "Column name cannot be blank."
            );
        }

        int dotIndex =
                normalized.lastIndexOf('.');

        if (dotIndex >= 0) {

            normalized =
                    normalized.substring(
                            dotIndex + 1
                    );
        }

        if (normalized.isBlank()) {
            throw new QueryExecutionException(
                    "Invalid column name: "
                            + columnName
            );
        }

        return normalized.toLowerCase(
                Locale.ROOT
        );
    }

    static boolean containsKeyIgnoreCase(
            Map<String, Object> values,
            String key
    ) {

        Objects.requireNonNull(
                values,
                "Values cannot be null."
        );

        Objects.requireNonNull(
                key,
                "Key cannot be null."
        );

        for (String existingKey
                : values.keySet()) {

            if (existingKey != null
                    && existingKey.equalsIgnoreCase(key)) {

                return true;
            }
        }

        return false;
    }

    static Object getValueIgnoreCase(
            Map<String, Object> values,
            String key
    ) {

        Objects.requireNonNull(
                values,
                "Values cannot be null."
        );

        Objects.requireNonNull(
                key,
                "Key cannot be null."
        );

        for (Map.Entry<String, Object> entry
                : values.entrySet()) {

            if (entry.getKey() != null
                    && entry.getKey()
                    .equalsIgnoreCase(key)) {

                return entry.getValue();
            }
        }

        throw new QueryExecutionException(
                "Column not found in JOIN result: "
                        + key
        );
    }
}
