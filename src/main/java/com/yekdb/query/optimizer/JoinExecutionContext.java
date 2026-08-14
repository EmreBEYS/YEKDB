package com.yekdb.query.optimizer;

import com.yekdb.query.expression.Expression;
import com.yekdb.query.statement.JoinClause;
import com.yekdb.query.statement.TableReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * JOIN optimizer için gerekli execution metadata bilgisini taşır.
 *
 * Bu sınıf sorguyu çalıştırmaz. Yalnızca optimizer'ın karar
 * verebilmesi için gerekli yapısal bilgileri tek noktada toplar.
 */
public final class JoinExecutionContext {

    private final TableReference baseTable;
    private final List<JoinClause> joinClauses;
    private final Map<String, Long> tableRowCounts;
    private final Expression whereExpression;
    private final Set<String> projectedColumns;

    public JoinExecutionContext(
            TableReference baseTable,
            List<JoinClause> joinClauses,
            Map<String, Long> tableRowCounts,
            Expression whereExpression,
            Set<String> projectedColumns
    ) {

        this.baseTable =
                Objects.requireNonNull(
                        baseTable,
                        "Base table reference cannot be null."
                );

        Objects.requireNonNull(
                joinClauses,
                "JOIN clause list cannot be null."
        );

        Objects.requireNonNull(
                tableRowCounts,
                "Table row-count map cannot be null."
        );

        Objects.requireNonNull(
                projectedColumns,
                "Projected column set cannot be null."
        );

        this.joinClauses =
                List.copyOf(
                        new ArrayList<>(
                                joinClauses
                        )
                );

        Map<String, Long> normalizedRowCounts =
                new LinkedHashMap<>();

        for (Map.Entry<String, Long> entry
                : tableRowCounts.entrySet()) {

            String tableName =
                    normalizeIdentifier(
                            entry.getKey()
                    );

            Long rowCount =
                    Objects.requireNonNull(
                            entry.getValue(),
                            "Table row count cannot be null."
                    );

            if (rowCount < 0) {
                throw new IllegalArgumentException(
                        "Table row count cannot be negative: "
                                + entry.getKey()
                );
            }

            normalizedRowCounts.put(
                    tableName,
                    rowCount
            );
        }

        this.tableRowCounts =
                Map.copyOf(
                        normalizedRowCounts
                );

        this.whereExpression =
                whereExpression;

        Set<String> normalizedProjectedColumns =
                new LinkedHashSet<>();

        for (String projectedColumn
                : projectedColumns) {

            if (projectedColumn == null
                    || projectedColumn.isBlank()) {

                throw new IllegalArgumentException(
                        "Projected column cannot be null or blank."
                );
            }

            normalizedProjectedColumns.add(
                    projectedColumn.trim()
            );
        }

        this.projectedColumns =
                Set.copyOf(
                        normalizedProjectedColumns
                );
    }

    /**
     * WHERE veya projection bilgisi olmayan sade context oluşturur.
     */
    public JoinExecutionContext(
            TableReference baseTable,
            List<JoinClause> joinClauses,
            Map<String, Long> tableRowCounts
    ) {
        this(
                baseTable,
                joinClauses,
                tableRowCounts,
                null,
                Set.of()
        );
    }

    public TableReference getBaseTable() {
        return baseTable;
    }

    public List<JoinClause> getJoinClauses() {
        return joinClauses;
    }

    public Map<String, Long> getTableRowCounts() {
        return tableRowCounts;
    }

    public Expression getWhereExpression() {
        return whereExpression;
    }

    public boolean hasWhereExpression() {
        return whereExpression != null;
    }

    public Set<String> getProjectedColumns() {
        return projectedColumns;
    }

    /**
     * Tablo adı veya alias üzerinden satır sayısı döndürür.
     *
     * Metadata bulunmuyorsa Long.MAX_VALUE kullanılır.
     * Böylece bilinmeyen tablo optimizer tarafından yanlışlıkla
     * "küçük tablo" olarak seçilmez.
     */
    public long getRowCount(
            String tableName,
            String alias
    ) {

        if (alias != null
                && !alias.isBlank()) {

            Long aliasCount =
                    tableRowCounts.get(
                            normalizeIdentifier(
                                    alias
                            )
                    );

            if (aliasCount != null) {
                return aliasCount;
            }
        }

        Long tableCount =
                tableRowCounts.get(
                        normalizeIdentifier(
                                tableName
                        )
                );

        return tableCount == null
                ? Long.MAX_VALUE
                : tableCount;
    }

    public long getRowCount(
            JoinClause joinClause
    ) {

        Objects.requireNonNull(
                joinClause,
                "JOIN clause cannot be null."
        );

        return getRowCount(
                joinClause.getTableName(),
                joinClause.getAlias()
        );
    }

    /**
     * Base tablo adı ve alias bilgisini tanır.
     */
    public boolean matchesBaseTable(
            String qualifier
    ) {

        if (qualifier == null
                || qualifier.isBlank()) {
            return false;
        }

        String normalizedQualifier =
                normalizeIdentifier(
                        qualifier
                );

        if (normalizeIdentifier(
                baseTable.getTableName()
        ).equals(
                normalizedQualifier
        )) {
            return true;
        }

        String alias =
                baseTable.getAlias();

        return alias != null
                && !alias.isBlank()
                && normalizeIdentifier(
                alias
        ).equals(
                normalizedQualifier
        );
    }

    private String normalizeIdentifier(
            String identifier
    ) {

        String normalized =
                Objects.requireNonNull(
                                identifier,
                                "Identifier cannot be null."
                        )
                        .trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Identifier cannot be blank."
            );
        }

        return normalized.toLowerCase(
                Locale.ROOT
        );
    }
}
