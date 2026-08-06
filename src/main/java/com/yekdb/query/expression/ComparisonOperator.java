package com.yekdb.query.expression;

/**
 * WHERE ifadelerinde kullanılabilecek karşılaştırma operatörlerini temsil eder.
 */
public enum ComparisonOperator {

    EQUALS("="),

    NOT_EQUALS("!="),

    GREATER_THAN(">"),

    LESS_THAN("<"),

    GREATER_THAN_OR_EQUALS(">="),

    LESS_THAN_OR_EQUALS("<=");

    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    /**
     * SQL operatör sembolünü enum değerine dönüştürür.
     *
     * @param symbol SQL operatör sembolü
     * @return karşılık gelen ComparisonOperator
     */
    public static ComparisonOperator fromSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException(
                    "Karşılaştırma operatörü boş olamaz."
            );
        }

        for (ComparisonOperator operator : values()) {
            if (operator.symbol.equals(symbol.trim())) {
                return operator;
            }
        }

        throw new IllegalArgumentException(
                "Desteklenmeyen karşılaştırma operatörü: " + symbol
        );
    }
}