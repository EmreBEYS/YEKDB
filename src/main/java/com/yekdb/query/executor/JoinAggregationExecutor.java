package com.yekdb.query.executor;

import com.yekdb.query.statement.GroupByClause;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JOIN sonucu üzerinde GROUP BY ve aggregate işlemlerini
 * birlikte yürütür.
 *
 * Bu sınıf JOIN sonuçlarını aggregate sonuç satırlarına
 * dönüştürür.
 */

public final  class JoinAggregationExecutor {
    private final GroupByExecutor groupByExecutor;
    private final AggregateExecutor aggregateExecutor;

    public JoinAggregationExecutor(){
        this(new GroupByExecutor(),new AggregateExecutor());
    }
    public JoinAggregationExecutor(GroupByExecutor groupByExecutor,AggregateExecutor aggregateExecutor){
        this.groupByExecutor=Objects.requireNonNull(groupByExecutor,"groupByExecutor cannot be null.");
        this.aggregateExecutor=Objects.requireNonNull(aggregateExecutor,"aggegateExecutor cannot be null");
    }

    /**
     * JOIN satırlarını GROUP BY ile gruplar.
     */
    public Map<List<Object>, List<Map<String, Object>>> group(
            List<Map<String, Object>> joinedRows,
            GroupByClause groupByClause
    ) {

        return groupByExecutor.executeJoinedRows(
                joinedRows,
                groupByClause
        );
    }

    /**
     * Tek bir grup için aggregate sonuç satırı oluşturur.
     *
     * Örnek:
     *
     * d.name = Software
     * COUNT(e.id) = 2
     * AVG(e.salary) = 45000
     */
    public Map<String, Object> createAggregateRow(
            List<Map<String, Object>> groupRows,
            String groupColumn,
            String countColumn
    ) {

        Objects.requireNonNull(
                groupRows,
                "Grup rows cannot be null."
        );

        Objects.requireNonNull(
                groupColumn,
                "GROUP BY column cannot be null."
        );

        Objects.requireNonNull(
                countColumn,
                "COUNT column cannot be null."
        );

        Map<String, Object> result =
                new LinkedHashMap<>();

        /*
         * Grup boş değilse GROUP BY değerini
         * ilk satırdan alabiliriz.
         */
        if (!groupRows.isEmpty()) {

            result.put(
                    groupColumn,
                    groupRows.get(0).get(groupColumn)
            );
        }

        Object count =
                aggregateExecutor.executeJoinedRows(
                        groupRows,
                        AggregateExecutor.AggregateFunction.COUNT,
                        countColumn
                );

        /*
         * Expression biçimini de saklıyoruz.
         */
        result.put(
                "COUNT(" + countColumn + ")",
                count
        );

        return result;
    }

    /**
     * Grupları aggregate satır listesine dönüştürür.
     */
    public List<Map<String, Object>> createCountRows(
            Map<List<Object>, List<Map<String, Object>>> groups,
            String groupColumn,
            String countColumn,
            String aggregateAlias
    ) {

        Objects.requireNonNull(
                groups,
                "Groups cannot be null."
        );

        List<Map<String, Object>> results =
                new ArrayList<>();

        for (List<Map<String, Object>> groupRows
                : groups.values()) {

            Map<String, Object> result =
                    createAggregateRow(
                            groupRows,
                            groupColumn,
                            countColumn
                    );

            /*
             * SELECT alias kullanılmışsa aggregate
             * değer alias üzerinden de erişilebilir.
             *
             * COUNT(e.id) AS employee_count
             */
            if (aggregateAlias != null
                    && !aggregateAlias.isBlank()) {

                result.put(
                        aggregateAlias,
                        result.get(
                                "COUNT(" + countColumn + ")"
                        )
                );
            }

            results.add(result);
        }

        return results;
    }
}
