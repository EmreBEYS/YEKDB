package com.yekdb.query.executor;

import com.yekdb.index.Index;
import com.yekdb.index.RecordPointer;
import com.yekdb.query.evaluator.WhereEvaluator;
import com.yekdb.query.expression.BetweenExpression;
import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.result.QueryResult;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * B+ Tree destekli INDEX_SCAN execution katmanıdır.
 *
 * Index üzerinden elde edilen RecordPointer değerlerini dışarıdan
 * verilen row resolver ile gerçek Row nesnelerine dönüştürür.
 *
 * Phase 13 kapsamında desteklenen access predicate'leri:
 *
 * - =
 * - >
 * - >=
 * - <
 * - <=
 * - BETWEEN
 *
 * NOT_EQUALS ve NOT BETWEEN index access path olarak kullanılmaz.
 */
public final class IndexScanExecutor {

    /**
     * Uygun index kullanarak WHERE taraması gerçekleştirir.
     */
    public QueryResult execute(
            Table table,
            Index<?> index,
            Expression whereExpression,
            Function<RecordPointer, Row> rowResolver
    ) {

        return execute(
                table,
                index,
                whereExpression,
                whereExpression,
                rowResolver
        );
    }

    /**
     * Access predicate ile index pointer'larını çözer, final predicate ile
     * SQL semantiğini koruyacak son filtrelemeyi yapar.
     */
    public QueryResult execute(
            Table table,
            Index<?> index,
            Expression accessPredicate,
            Expression finalPredicate,
            Function<RecordPointer, Row> rowResolver
    ) {

        Objects.requireNonNull(
                table,
                "Table cannot be null."
        );

        Objects.requireNonNull(
                index,
                "Index cannot be null."
        );

        Objects.requireNonNull(
                accessPredicate,
                "Access predicate cannot be null for INDEX_SCAN."
        );

        Objects.requireNonNull(
                rowResolver,
                "Row resolver cannot be null."
        );

        long startTime =
                System.nanoTime();

        List<RecordPointer> pointers =
                resolvePointers(
                        index,
                        accessPredicate
                );

        List<Row> matchedRows =
                new ArrayList<>(
                        pointers.size()
                );

        for (RecordPointer pointer : pointers) {

            if (pointer == null
                    || !pointer.isValid()) {

                throw new IllegalStateException(
                        "Index returned an invalid RecordPointer."
                );
            }

            Row row =
                    rowResolver.apply(pointer);

            if (row == null) {

                throw new IllegalStateException(
                        "Row resolver returned null for RecordPointer: "
                                + pointer
                );
            }

            /*
             * Index doğru adayları üretse bile WHERE ifadesi
             * tekrar değerlendirilir.
             *
             * Böylece INDEX_SCAN ve FULL_TABLE_SCAN
             * aynı SQL semantiğini korur.
             */
            if (finalPredicate == null
                    || WhereEvaluator.evaluate(
                    finalPredicate,
                    row,
                    table
            )) {

                matchedRows.add(row);
            }
        }

        long executionTime =
                System.nanoTime()
                        - startTime;

        return QueryResult.selectSuccess(
                table.getColumns(),
                matchedRows,
                executionTime
        );
    }

    /**
     * Expression tipine göre kullanılacak B+ Tree
     * search metodunu belirler.
     */
    @SuppressWarnings({
            "rawtypes",
            "unchecked"
    })
    private List<RecordPointer> resolvePointers(
            Index<?> index,
            Expression whereExpression
    ) {

        Index rawIndex =
                index;

        if (whereExpression
                instanceof ComparisonExpression comparison) {

            if (!comparison
                    .isColumnToValueComparison()) {

                throw unsupportedExpression(
                        whereExpression
                );
            }

            Object expectedValue =
                    comparison.expectedValue();

            if (!(expectedValue
                    instanceof Comparable<?> comparable)) {

                throw new IllegalArgumentException(
                        "INDEX_SCAN comparison value must implement Comparable."
                );
            }

            return switch (
                    comparison.operator()
                    ) {

                case EQUALS ->
                        rawIndex.search(
                                comparable
                        );

                case GREATER_THAN ->
                        rawIndex.searchGreaterThan(
                                comparable
                        );

                case GREATER_THAN_OR_EQUALS ->
                        rawIndex.searchGreaterThanOrEqual(
                                comparable
                        );

                case LESS_THAN ->
                        rawIndex.searchLessThan(
                                comparable
                        );

                case LESS_THAN_OR_EQUALS ->
                        rawIndex.searchLessThanOrEqual(
                                comparable
                        );

                case NOT_EQUALS ->
                        throw unsupportedExpression(
                                whereExpression
                        );
            };
        }

        if (whereExpression
                instanceof BetweenExpression between) {

            if (between.isNegated()) {

                throw unsupportedExpression(
                        whereExpression
                );
            }

            Object lower =
                    between.getLowerBound();

            Object upper =
                    between.getUpperBound();

            if (!(lower
                    instanceof Comparable<?> lowerComparable)
                    || !(upper
                    instanceof Comparable<?> upperComparable)) {

                throw new IllegalArgumentException(
                        "INDEX_SCAN BETWEEN bounds must implement Comparable."
                );
            }

            return rawIndex.searchRange(
                    lowerComparable,
                    upperComparable
            );
        }

        throw unsupportedExpression(
                whereExpression
        );
    }

    private UnsupportedOperationException
    unsupportedExpression(
            Expression expression
    ) {

        return new UnsupportedOperationException(
                "Expression is not supported by INDEX_SCAN: "
                        + expression
                        .getClass()
                        .getSimpleName()
        );
    }
}
