package com.yekdb.query.statement;

/**
 * SQL LIMIT ifadesini temsil eder.
 *
 * Örnek:
 *
 * SELECT * FROM users LIMIT 10;
 *
 * Sprint 00-14
 */

public final class LimitClause {
    private final int rowCount;

    public LimitClause(int rowCount){
        if(rowCount<0){
            throw new IllegalArgumentException("Lımıt row count cannot be negative.");
        }
        this.rowCount=rowCount;
    }
    /**
     * Döndürülebilecek maksimum satır sayısı.
     */
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public String toString() {
        return "LIMIT " + rowCount;
    }
}
