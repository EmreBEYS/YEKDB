package com.yekdb.query.expression;

import java.util.List;
import java.util.Objects;

/**
 * SQL IN / NOT IN predicate yapısını temsil eder.
 *
 * <p>Örnek:</p>
 *
 * <pre>
 * department IN ('IT', 'HR', 'Finance')
 * department NOT IN ('IT', 'HR')
 * </pre>
 *
 * Sprint 00-14
 */

public final class InExpression implements Expression{
    private final String columnName;
    private final List<Object> values;
    private final boolean negated;

    /**
     * Normal IN expression oluşturur.
     */
    public InExpression(String columnName,List<Object> values){
        this(columnName,values,false);
    }
    /**
     * IN veya NOT IN expression oluşturur.
     */
    public InExpression(String columnName,List<Object> values, boolean negated){
        this.columnName=Objects.requireNonNull(columnName,"columnName cannot be null.");
        if(columnName.isBlank()){
            throw new IllegalArgumentException("columnName cannot be blank.");
        }
        Objects.requireNonNull(values,"Values cannot be null.");
        if(values.isEmpty()){
            throw new IllegalArgumentException("Values cannot be empyt.");
        }
        this.values = List.copyOf(values);
        this.negated=negated;
    }

    public String getColumnName(){
        return columnName;
    }
    public List<Object> getValues(){
        return values;
    }

    public boolean isNegated(){
        return negated;
    }

    @Override
    public String toString() {

        return columnName
                + (negated
                ? " NOT IN "
                : " IN ")
                + values;
    }
}
