package com.yekdb.query.statement;

import java.util.Objects;


/**
 * ORDER BY içindeki tek bir sıralama ifadesini temsil eder.
 *
 * <p>Örnek:</p>
 *
 * <pre>
 * ORDER BY name ASC
 * ORDER BY salary DESC
 * </pre>
 *
 * Sprint 00-14
 */

public final class OrderByItem {
    private final String columnName;
    private final SortDirection direction;

    public OrderByItem(String columnName,SortDirection direction){
        this.columnName=Objects.requireNonNull(columnName,"columnName cannot be null.");
        if(columnName.isBlank()){
            throw new IllegalArgumentException("columnName cannot be blank.");
        }
        this.direction=Objects.requireNonNull(direction,"Direction cannot be null.");
    }
    public String getColumnName() {
        return columnName;
    }

    public SortDirection getDirection() {
        return direction;
    }

    @Override
    public String toString() {

        return columnName
                + " "
                + direction;
    }
}
