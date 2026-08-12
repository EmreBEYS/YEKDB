package com.yekdb.query.expression;

import java.util.Objects;

/**
 * Tek bir sütun üzerinde yapılan karşılaştırma ifadesini temsil eder.
 *
 * <p>Örnekler:</p>
 *
 * <pre>
 * age > 18
 * name = "Yunus"
 * salary >= 50000
 * </pre>
 *
 * @param columnName    karşılaştırılacak sütunun adı
 * @param operator      karşılaştırma operatörü
 * @param expectedValue karşılaştırmada kullanılacak değer
 */
public record ComparisonExpression(
        String columnName,
        ComparisonOperator operator,
        Object expectedValue
) implements Expression {

    public ComparisonExpression {

        if (columnName == null
                || columnName.isBlank()) {

            throw new IllegalArgumentException(
                    "Column name cannot be null or blank."
            );
        }

        Objects.requireNonNull(
                operator,
                "Comparison operator cannot be null."
        );

        columnName =
                columnName.trim();
    }
}