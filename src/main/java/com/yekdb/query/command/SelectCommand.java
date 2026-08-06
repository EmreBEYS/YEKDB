package com.yekdb.query.command;

import com.yekdb.query.expression.Expression;

import java.util.List;
import java.util.Objects;

/**
 * SELECT SQL komutunu temsil eder.
 *
 * Desteklenen kullanımlar:
 *
 * SELECT * FROM table
 * SELECT * FROM table WHERE ...
 * SELECT column1, column2 FROM table
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

    /**
     * WHERE koşulunu temsil eden expression ağacı.
     *
     * WHERE bulunmuyorsa null değerindedir.
     */
    private final Expression whereExpression;

    private SelectCommand(
            String tableName,
            List<String> selectedColumns,
            boolean selectAll,
            Expression whereExpression
    ) {
        this.tableName = validateTableName(tableName);
        this.selectAll = selectAll;
        this.whereExpression = whereExpression;

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
     */
    public static SelectCommand allFrom(String tableName) {
        return new SelectCommand(
                tableName,
                List.of(),
                true,
                null
        );
    }

    /**
     * SELECT * FROM table WHERE ... komutu oluşturur.
     */
    public static SelectCommand allFromWhere(
            String tableName,
            Expression whereExpression
    ) {
        return new SelectCommand(
                tableName,
                List.of(),
                true,
                Objects.requireNonNull(
                        whereExpression,
                        "WHERE expression cannot be null."
                )
        );
    }

    /**
     * Belirli sütunları seçen SELECT komutu oluşturur.
     */
    public static SelectCommand columnsFrom(
            String tableName,
            List<String> selectedColumns
    ) {
        return new SelectCommand(
                tableName,
                selectedColumns,
                false,
                null
        );
    }

    /**
     * Belirli sütunları WHERE koşuluyla seçen komut oluşturur.
     */
    public static SelectCommand columnsFromWhere(
            String tableName,
            List<String> selectedColumns,
            Expression whereExpression
    ) {
        return new SelectCommand(
                tableName,
                selectedColumns,
                false,
                Objects.requireNonNull(
                        whereExpression,
                        "WHERE expression cannot be null."
                )
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

    public Expression getWhereExpression() {
        return whereExpression;
    }

    public boolean hasWhereExpression() {
        return whereExpression != null;
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

        return normalizedName;
    }

    @Override
    public String toString() {
        return "SelectCommand{" +
                "tableName='" + tableName + '\'' +
                ", selectedColumns=" + selectedColumns +
                ", selectAll=" + selectAll +
                ", whereExpression=" + whereExpression +
                '}';
    }
}