package com.yekdb.query.expression;

import java.util.Objects;

/**
 * SQL karşılaştırma ifadelerini temsil eder.
 *
 * Sprint 00-15 ile birlikte hem klasik
 * column-value karşılaştırmalarını hem de
 * JOIN işlemlerinde kullanılan
 * column-column karşılaştırmalarını destekler.
 *
 * Örnekler:
 *
 * age > 18
 * name = "Yunus"
 * salary >= 50000
 *
 * e.department_id = d.id
 */
public record ComparisonExpression(

        String columnName,
        ComparisonOperator operator,
        Object expectedValue

) implements Expression {

    /**
     * Ana constructor.
     *
     * Eski Sprint 00-11 / 00-14 kullanımını
     * tamamen korur.
     *
     * Örnek:
     *
     * new ComparisonExpression(
     *     "age",
     *     ComparisonOperator.GREATER_THAN,
     *     18
     * );
     */
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

    // --------------------------------------------------
    // Sprint 00-15 JOIN constructors
    // --------------------------------------------------

    /**
     * Qualified veya unqualified bir sol kolon ile
     * sabit bir değeri karşılaştırır.
     *
     * Örnek:
     *
     * e.salary > 50000
     */
    public ComparisonExpression(
            ColumnExpression leftColumn,
            ComparisonOperator operator,
            Object expectedValue
    ) {

        this(
                Objects.requireNonNull(
                        leftColumn,
                        "Left column cannot be null."
                ).getQualifiedName(),
                operator,
                expectedValue
        );
    }

    /**
     * İki kolonu karşılaştırır.
     *
     * JOIN ON ifadelerinin temel constructor'ıdır.
     *
     * Örnek:
     *
     * e.department_id = d.id
     */
    public ComparisonExpression(
            ColumnExpression leftColumn,
            ComparisonOperator operator,
            ColumnExpression rightColumn
    ) {

        this(
                Objects.requireNonNull(
                        leftColumn,
                        "Left column cannot be null."
                ).getQualifiedName(),

                operator,

                Objects.requireNonNull(
                        rightColumn,
                        "Right column cannot be null."
                )
        );
    }

    // --------------------------------------------------
    // LEFT COLUMN
    // --------------------------------------------------

    /**
     * Sol taraftaki columnName değerini
     * ColumnExpression olarak döndürür.
     *
     * "age"
     *
     * ->
     *
     * ColumnExpression(
     *     qualifier = null,
     *     columnName = "age"
     * )
     *
     *
     * "e.department_id"
     *
     * ->
     *
     * ColumnExpression(
     *     qualifier = "e",
     *     columnName = "department_id"
     * )
     */
    public ColumnExpression getLeftColumnExpression() {

        return ColumnExpression.parse(
                columnName
        );
    }

    /**
     * Sol kolon qualified mı?
     *
     * age
     * -> false
     *
     * e.age
     * -> true
     */
    public boolean hasQualifiedLeftColumn() {

        return getLeftColumnExpression()
                .isQualified();
    }

    // --------------------------------------------------
    // RIGHT OPERAND
    // --------------------------------------------------

    /**
     * Sağ taraf başka bir kolon mu?
     *
     * age > 18
     *
     * -> false
     *
     *
     * e.department_id = d.id
     *
     * -> true
     */
    public boolean isColumnToColumnComparison() {

        return expectedValue
                instanceof ColumnExpression;
    }

    /**
     * Sağ taraf ColumnExpression ise döndürür.
     *
     * JOIN executor / evaluator tarafından
     * kullanılacaktır.
     */
    public ColumnExpression getRightColumnExpression() {

        if (!isColumnToColumnComparison()) {

            return null;
        }

        return (ColumnExpression) expectedValue;
    }

    /**
     * Sağ taraf sabit bir değer mi?
     *
     * age > 18
     *
     * -> true
     *
     *
     * e.department_id = d.id
     *
     * -> false
     */
    public boolean isColumnToValueComparison() {

        return !isColumnToColumnComparison();
    }

    // --------------------------------------------------
    // QUALIFIED NAME HELPERS
    // --------------------------------------------------

    /**
     * Sol kolonun tam adını döndürür.
     *
     * age
     *
     * veya
     *
     * e.department_id
     */
    public String getLeftQualifiedName() {

        return getLeftColumnExpression()
                .getQualifiedName();
    }

    /**
     * Sağ taraf kolon ise tam kolon adını döndürür.
     *
     * Değer karşılaştırması ise null döndürür.
     */
    public String getRightQualifiedName() {

        ColumnExpression right =
                getRightColumnExpression();

        if (right == null) {

            return null;
        }

        return right.getQualifiedName();
    }

    // --------------------------------------------------
    // DEBUG
    // --------------------------------------------------

    @Override
    public String toString() {

        return columnName
                + " "
                + operator
                + " "
                + formatExpectedValue();
    }

    private String formatExpectedValue() {

        if (expectedValue
                instanceof String stringValue) {

            return "'"
                    + stringValue
                    + "'";
        }

        return String.valueOf(
                expectedValue
        );
    }
}