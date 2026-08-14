package com.yekdb.query.executor;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JoinHavingExecutorTest {

    private JoinHavingExecutor havingExecutor;

    @BeforeEach
    void setUp() {

        havingExecutor =
                new JoinHavingExecutor();
    }

    @Test
    void shouldKeepRowsMatchingHavingCondition() {

        List<Map<String, Object>> aggregateRows =
                List.of(
                        Map.of(
                                "d.name", "Software",
                                "employee_count", 2L
                        ),
                        Map.of(
                                "d.name", "Finance",
                                "employee_count", 1L
                        ),
                        Map.of(
                                "d.name", "Human Resources",
                                "employee_count", 0L
                        )
                );

        ComparisonExpression havingExpression =
                new ComparisonExpression(
                        "employee_count",
                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                        2L
                );

        List<Map<String, Object>> result =
                havingExecutor.execute(
                        aggregateRows,
                        havingExpression
                );

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                "Software",
                result.get(0).get("d.name")
        );

        assertEquals(
                2L,
                result.get(0).get("employee_count")
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoGroupMatches() {

        List<Map<String, Object>> aggregateRows =
                List.of(
                        Map.of(
                                "employee_count", 1L
                        ),
                        Map.of(
                                "employee_count", 0L
                        )
                );

        ComparisonExpression havingExpression =
                new ComparisonExpression(
                        "employee_count",
                        ComparisonOperator.GREATER_THAN,
                        5L
                );

        List<Map<String, Object>> result =
                havingExecutor.execute(
                        aggregateRows,
                        havingExpression
                );

        assertTrue(
                result.isEmpty()
        );
    }

    @Test
    void nullHavingExpressionShouldPreserveRows() {

        List<Map<String, Object>> aggregateRows =
                List.of(
                        Map.of(
                                "employee_count", 2L
                        ),
                        Map.of(
                                "employee_count", 1L
                        )
                );

        List<Map<String, Object>> result =
                havingExecutor.execute(
                        aggregateRows,
                        null
                );

        assertEquals(
                2,
                result.size()
        );
    }

    @Test
    void shouldEvaluateAggregateExpressionKey() {

        List<Map<String, Object>> aggregateRows =
                List.of(
                        Map.of(
                                "COUNT(e.id)", 3L
                        ),
                        Map.of(
                                "COUNT(e.id)", 1L
                        )
                );

        ComparisonExpression havingExpression =
                new ComparisonExpression(
                        "COUNT(e.id)",
                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                        2L
                );

        List<Map<String, Object>> result =
                havingExecutor.execute(
                        aggregateRows,
                        havingExpression
                );

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                3L,
                result.get(0).get("COUNT(e.id)")
        );
    }
}