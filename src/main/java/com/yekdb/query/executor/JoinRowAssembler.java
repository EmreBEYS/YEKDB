package com.yekdb.query.executor;

import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.TableReference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JOIN satırlarının birleştirilmesi, qualified kolon üretimi ve outer JOIN
 * eşleşmeyen satırlarının oluşturulmasından sorumludur.
 */
final class JoinRowAssembler {

    Map<String, Object> createLeftUnmatchedRow(
            TableReference leftTable,
            Map<String, Object> leftRow,
            TableReference rightTable,
            List<Map<String, Object>> rightRows
    ) {

        Map<String, Object> joinedRow =
                new LinkedHashMap<>();

        /*
         * Sol tablo değerleri normal şekilde eklenir.
         */
        addQualifiedColumns(
                joinedRow,
                leftTable,
                leftRow
        );

        /*
         * Sağ tabloda en az bir satır varsa kolon yapısı
         * ilk satır üzerinden belirlenir.
         *
         * İleride bu bilgi TableMetadata üzerinden
         * alınacaktır.
         */
        if (!rightRows.isEmpty()) {

            Map<String, Object> nullRightRow =
                    new LinkedHashMap<>();

            for (String columnName
                    : rightRows.get(0).keySet()) {

                nullRightRow.put(
                        normalizeSourceColumnName(
                                columnName
                        ),
                        null
                );
            }

            addQualifiedColumns(
                    joinedRow,
                    rightTable,
                    nullRightRow
            );
        }

        return joinedRow;
    }

    Map<String, Object> createRightUnmatchedRow(
            TableReference leftTable,
            List<Map<String, Object>> leftRows,
            TableReference rightTable,
            Map<String, Object> rightRow
    ) {

        Map<String, Object> joinedRow =
                new LinkedHashMap<>();

        /*
         * Sol tabloda en az bir satır varsa kolon yapısı
         * ilk satır üzerinden belirlenir.
         *
         * İleride bu bilgi TableMetadata üzerinden
         * alınacaktır.
         */
        if (!leftRows.isEmpty()) {

            Map<String, Object> nullLeftRow =
                    new LinkedHashMap<>();

            for (String columnName
                    : leftRows.get(0).keySet()) {

                /*
                 * Kaynak satır bir önceki JOIN sonucundan
                 * gelmiş olabilir.
                 *
                 * Qualified kolon adı varsa aynen korunur.
                 */
                if (isQualifiedColumnName(columnName)) {

                    nullLeftRow.put(
                            columnName,
                            null
                    );

                } else {

                    nullLeftRow.put(
                            normalizeSourceColumnName(
                                    columnName
                            ),
                            null
                    );
                }
            }

            addQualifiedColumns(
                    joinedRow,
                    leftTable,
                    nullLeftRow
            );
        }

        /*
         * Sağ tablo değerleri normal şekilde eklenir.
         */
        addQualifiedColumns(
                joinedRow,
                rightTable,
                rightRow
        );

        return joinedRow;
    }

    Map<String, Object> mergeRows(
            TableReference leftTable,
            Map<String, Object> leftRow,
            TableReference rightTable,
            Map<String, Object> rightRow
    ) {

        Map<String, Object> joinedRow =
                new LinkedHashMap<>();

        addQualifiedColumns(
                joinedRow,
                leftTable,
                leftRow
        );

        addQualifiedColumns(
                joinedRow,
                rightTable,
                rightRow
        );

        return joinedRow;
    }

    void addQualifiedColumns(
            Map<String, Object> target,
            TableReference table,
            Map<String, Object> row
    ) {

        Objects.requireNonNull(
                target,
                "Hedef satır null olamaz."
        );

        Objects.requireNonNull(
                table,
                "Tablo referansı null olamaz."
        );

        Objects.requireNonNull(
                row,
                "Kaynak satır null olamaz."
        );

        String tableName =
                table.getTableName();

        String alias =
                table.getAlias();

        for (Map.Entry<String, Object> entry
                : row.entrySet()) {

            String sourceColumnName =
                    entry.getKey();

            Object value =
                    entry.getValue();

            /*
             * Kaynak kolon zaten qualified ise önceki
             * JOIN sonucundan gelmiş olabilir.
             *
             * Örnek:
             *
             * e.id
             * d.id
             * d.company_id
             *
             * Bu durumda qualifier kesinlikle
             * kaybedilmemelidir.
             */
            if (isQualifiedColumnName(
                    sourceColumnName
            )) {

                target.put(
                        sourceColumnName,
                        value
                );

                continue;
            }

            /*
             * Fiziksel tablo satırından gelen normal
             * kolon adı düzenlenir.
             */
            String columnName =
                    normalizeSourceColumnName(
                            sourceColumnName
                    );

            /*
             * Gerçek tablo adı.
             *
             * employee.id
             */
            target.put(
                    tableName
                            + "."
                            + columnName,
                    value
            );

            /*
             * Alias mevcutsa ayrıca alias.column
             * biçimi oluşturulur.
             *
             * e.id
             */
            if (table.hasAlias()) {

                target.put(
                        alias
                                + "."
                                + columnName,
                        value
                );
            }
        }
    }

    String normalizeSourceColumnName(
            String columnName
    ) {

        Objects.requireNonNull(
                columnName,
                "Kolon adı null olamaz."
        );

        String normalized =
                columnName.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    "Kolon adı boş olamaz."
            );
        }

        int dotIndex =
                normalized.lastIndexOf('.');

        if (dotIndex >= 0
                && dotIndex < normalized.length() - 1) {

            return normalized.substring(
                    dotIndex + 1
            );
        }

        return normalized;
    }

    boolean isQualifiedColumnName(
            String columnName
    ) {

        Objects.requireNonNull(
                columnName,
                "Kolon adı null olamaz."
        );

        String normalized =
                columnName.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    "Kolon adı boş olamaz."
            );
        }

        int dotIndex =
                normalized.indexOf('.');

        return dotIndex > 0
                && dotIndex < normalized.length() - 1;
    }

    TableReference createRightTableReference(
            JoinClause joinClause
    ) {

        return new TableReference(
                joinClause.getTableName(),
                joinClause.getAlias()
        );
    }

}
