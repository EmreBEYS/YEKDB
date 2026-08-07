package com.yekdb.query.statement;

/**
 * Parser tarafından ayrıştırılmış DELETE sorgusunu temsil eder.
 *
 * Örnek SQL:
 *
 * DELETE FROM users WHERE id = 1;
 * DELETE FROM users;
 *
 * WHERE koşulu Statement seviyesinde metinsel olarak tutulur.
 * DeleteMapper bu metni ExpressionParser ile execution
 * katmanındaki Expression modeline dönüştürür.
 */
public final class DeleteStatement implements Statement {

    private final String tableName;
    private final String whereClause;

    public DeleteStatement(
            String tableName,
            String whereClause
    ) {
        this.tableName =
                validateTableName(
                        tableName
                );

        this.whereClause =
                normalizeWhereClause(
                        whereClause
                );
    }

    @Override
    public StatementType getType() {
        return StatementType.DELETE;
    }

    public String getTableName() {
        return tableName;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public boolean hasWhereClause() {
        return whereClause != null;
    }

    private String validateTableName(
            String tableName
    ) {
        if (tableName == null
                || tableName.isBlank()) {
            throw new IllegalArgumentException(
                    "Table name cannot be null or blank."
            );
        }

        String normalizedName =
                tableName.trim();

        if (!normalizedName.matches(
                "[A-Za-z_][A-Za-z0-9_]*"
        )) {
            throw new IllegalArgumentException(
                    "Invalid table name: "
                            + tableName
            );
        }

        return normalizedName;
    }

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