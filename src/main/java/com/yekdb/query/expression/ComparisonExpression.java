package com.yekdb.query.expression;

import java.util.Objects;

/**
 * Tek bir sütun üzerinde yapılan karşılaştırma ifadesini temsil eder.
 *
 * Örnekler:
 *
 * age > 18
 * name = "Yunus"
 * salary >= 50000
 *
 * @param columnName karşılaştırılacak sütunun adı
 * @param operator karşılaştırma operatörü
 * @param expectedValue karşılaştırmada kullanılacak değer
 */

public record ComparisonExpression(String columnName, ComparisonOperator operator, Object expectedValue) implements Expression {
    public ComparisonExpression{
        if (columnName == null || columnName.isBlank()){
            throw new IllegalArgumentException("Column names cannot be empty.");
        }
        Objects.requireNonNull(operator,"The comparison operator cannot be null.");
        columnName=columnName.trim();
    }

}

