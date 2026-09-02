package com.yekdb.query.optimizer;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryPlanExplainerWhereTraceTest {

    @Test
    void shouldIncludeOriginalAndOptimizedWhereLines() {

        QueryOptimizer optimizer =
                new QueryOptimizer();

        OptimizedQuery optimizedQuery =
                optimizer.optimizeQuery(
                        new com.yekdb.query.optimizer.OptimizationContext(
                                usersTable(),
                                new ComparisonExpression(
                                        "age",
                                        ComparisonOperator.GREATER_THAN,
                                        18
                                ),
                                List.of()
                        )
                );

        List<String> lines =
                optimizedQuery.getExplainLines();

        assertTrue(
                lines.contains(
                        "ORIGINAL_WHERE: age GREATER_THAN 18"
                )
        );

        assertTrue(
                lines.contains(
                        "OPTIMIZED_WHERE: age GREATER_THAN 18"
                )
        );
    }

    @Test
    void shouldShowNoneWhenWhereClauseDoesNotExist() {

        QueryOptimizer optimizer =
                new QueryOptimizer();

        OptimizedQuery optimizedQuery =
                optimizer.optimizeQuery(
                        new com.yekdb.query.optimizer.OptimizationContext(
                                usersTable(),
                                (Expression) null,
                                List.of()
                        )
                );

        List<String> lines =
                optimizedQuery.getExplainLines();

        assertTrue(
                lines.contains(
                        "ORIGINAL_WHERE: NONE"
                )
        );

        assertTrue(
                lines.contains(
                        "OPTIMIZED_WHERE: NONE"
                )
        );
    }

    private Table usersTable() {

        return new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("age", DataType.INT)
                )
        );
    }
}
