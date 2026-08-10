package com.yekdb.query.evaluator;

import com.yekdb.query.expression.Expression;
import com.yekdb.storage.record.Row;
import com.yekdb.table.Table;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * WHERE expression değerlendirmesi için adapter katmanıdır.
 *
 * Sprint 00-13 kapsamında expression evaluation işlemi
 * ExpressionEvaluator tarafından merkezi olarak gerçekleştirilir.
 *
 * Bu sınıf iki farklı kullanım şeklini destekler:
 *
 * 1. Function<String, Object> tabanlı value provider
 * 2. Gerçek YEKDB Row + Table değerlendirmesi
 *
 * Row + Table değerlendirmesinde sütun isimleri
 * case-insensitive olarak ele alınır.
 *
 * Desteklenen expression türleri ExpressionEvaluator üzerinden:
 *
 * - ComparisonExpression
 * - LogicalExpression (AND / OR)
 * - NotExpression
 *
 * Parentheses ve operator precedence parser katmanında
 * Expression AST yapısına dönüştürülür.
 */
public final class WhereEvaluator {

    /**
     * Expression değerlendirme işlemlerinin
     * merkezi evaluator nesnesi.
     */
    private static final ExpressionEvaluator EXPRESSION_EVALUATOR =
            new ExpressionEvaluator();

    private WhereEvaluator() {
    }

    /**
     * Function tabanlı value provider kullanarak
     * WHERE expression değerlendirir.
     *
     * Bu overload eski kodlarla geriye dönük
     * uyumluluğu korumak amacıyla tutulmaktadır.
     *
     * @param expression    değerlendirilecek expression
     * @param valueProvider sütun adına göre değer sağlayan fonksiyon
     * @return expression sonucu
     */
    public static boolean evaluate(
            Expression expression,
            Function<String, Object> valueProvider
    ) {

        Objects.requireNonNull(
                expression,
                "Expression cannot be null."
        );

        Objects.requireNonNull(
                valueProvider,
                "Value provider cannot be null."
        );

        Map<String, Object> values =
                new ProviderBackedMap(
                        valueProvider
                );

        return EXPRESSION_EVALUATOR.evaluate(
                expression,
                values
        );
    }

    /**
     * WHERE expression'ını gerçek YEKDB
     * Row ve Table nesneleri üzerinde değerlendirir.
     *
     * Table içerisindeki column isimleri ile
     * Row içerisindeki değerler eşleştirilir.
     *
     * Örnek:
     *
     * Table:
     *
     * id
     * name
     * age
     * city
     *
     * Row:
     *
     * 1
     * Yunus
     * 21
     * Malatya
     *
     * Oluşan yapı:
     *
     * id   -> 1
     * name -> Yunus
     * age  -> 21
     * city -> Malatya
     *
     * @param expression değerlendirilecek WHERE expression
     * @param row        değerlendirilecek Row
     * @param table      Row'un ait olduğu Table
     * @return expression sonucu
     */
    public static boolean evaluate(
            Expression expression,
            Row row,
            Table table
    ) {

        Objects.requireNonNull(
                expression,
                "Expression cannot be null."
        );

        Objects.requireNonNull(
                row,
                "Row cannot be null."
        );

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        Map<String, Object> rowValues =
                createRowValueMap(
                        row,
                        table
                );

        return EXPRESSION_EVALUATOR.evaluate(
                expression,
                rowValues
        );
    }

    /**
     * Table sütunlarını Row değerleriyle eşleştirerek
     * ExpressionEvaluator tarafından kullanılabilecek
     * Map yapısını oluşturur.
     *
     * TreeMap + CASE_INSENSITIVE_ORDER kullanıldığı için:
     *
     * city
     * CITY
     * City
     * CiTy
     *
     * aynı sütun olarak değerlendirilir.
     */
    private static Map<String, Object> createRowValueMap(
            Row row,
            Table table
    ) {

        int rowValueCount =
                row.size();

        int tableColumnCount =
                table.getColumns().size();

        if (rowValueCount
                != tableColumnCount) {

            throw new IllegalArgumentException(
                    "Row value count does not match "
                            + "table column count. "
                            + "Row values: "
                            + rowValueCount
                            + ", table columns: "
                            + tableColumnCount
            );
        }

        Map<String, Object> values =
                new TreeMap<>(
                        String.CASE_INSENSITIVE_ORDER
                );

        for (int i = 0;
             i < tableColumnCount;
             i++) {

            String columnName =
                    table.getColumns()
                            .get(i)
                            .getName();

            Object value =
                    row.getValue(i);

            values.put(
                    columnName,
                    value
            );
        }

        return values;
    }

    /**
     * Eski Function<String, Object> tabanlı API'yi
     * ExpressionEvaluator'ın Map tabanlı API'sine
     * adapte eder.
     *
     * Bu sınıf gerçek değerleri Map içerisinde
     * fiziksel olarak saklamaz.
     *
     * get(columnName) çağrısı doğrudan
     * valueProvider.apply(columnName) çağrısına
     * yönlendirilir.
     */
    private static final class ProviderBackedMap
            extends LinkedHashMap<String, Object> {

        private final Function<String, Object> valueProvider;

        private ProviderBackedMap(
                Function<String, Object> valueProvider
        ) {

            this.valueProvider =
                    Objects.requireNonNull(
                            valueProvider,
                            "Value provider cannot be null."
                    );
        }

        /**
         * ExpressionEvaluator comparison öncesinde
         * containsKey() kontrolü yaptığı için
         * String column isimleri geçerli kabul edilir.
         *
         * Gerçek column kontrolü valueProvider
         * implementasyonunun sorumluluğundadır.
         */
        @Override
        public boolean containsKey(
                Object key
        ) {

            return key instanceof String;
        }

        /**
         * Sütun değerini valueProvider üzerinden alır.
         */
        @Override
        public Object get(
                Object key
        ) {

            if (!(key instanceof String columnName)) {

                return null;
            }

            return valueProvider.apply(
                    columnName
            );
        }
    }
}