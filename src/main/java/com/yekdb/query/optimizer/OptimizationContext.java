package com.yekdb.query.optimizer;

import com.yekdb.index.Index;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.storage.table.Table;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Query Optimization V2 için optimizer girdilerini tek modelde toplar.
 *
 * Bu sınıf sorguyu çalıştırmaz; yalnızca planlama sırasında gereken tablo,
 * statement, WHERE ve index bilgisini taşır.
 */
public final class OptimizationContext {

    private final Table table;
    private final SelectStatement statement;
    private final Expression whereExpression;
    private final List<Index<?>> availableIndexes;

    public OptimizationContext(
            Table table,
            SelectStatement statement,
            List<Index<?>> availableIndexes
    ) {

        this(
                table,
                statement,
                statement == null
                        ? null
                        : statement.getWhereExpression(),
                availableIndexes
        );
    }

    public OptimizationContext(
            Table table,
            Expression whereExpression,
            List<Index<?>> availableIndexes
    ) {

        this(
                table,
                null,
                whereExpression,
                availableIndexes
        );
    }

    private OptimizationContext(
            Table table,
            SelectStatement statement,
            Expression whereExpression,
            List<Index<?>> availableIndexes
    ) {

        this.table =
                Objects.requireNonNull(
                        table,
                        "Table cannot be null."
                );

        this.statement =
                statement;

        this.whereExpression =
                whereExpression;

        this.availableIndexes =
                List.copyOf(
                        Objects.requireNonNull(
                                availableIndexes,
                                "Available index list cannot be null."
                        )
                );
    }

    public Table getTable() {
        return table;
    }

    public Optional<SelectStatement> getStatement() {
        return Optional.ofNullable(
                statement
        );
    }

    public Expression getWhereExpression() {
        return whereExpression;
    }

    public boolean hasWhereExpression() {
        return whereExpression != null;
    }

    public List<Index<?>> getAvailableIndexes() {
        return availableIndexes;
    }
}
