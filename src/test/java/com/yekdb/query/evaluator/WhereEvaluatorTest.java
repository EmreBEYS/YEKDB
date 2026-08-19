package com.yekdb.query.evaluator;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.expression.NotExpression;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WhereEvaluator sınıfının birim testleri.
 */
class WhereEvaluatorTest {

    private Table usersTable;
    private Row yunusRow;
    private Row aliRow;

    @BeforeEach
    void setUp() {
        usersTable = new Table(
                "users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING),
                        new Column("age", DataType.INT),
                        new Column("city", DataType.STRING),
                        new Column("active", DataType.BOOLEAN)
                )
        );

        yunusRow = new Row(
                List.of(
                        1,
                        "Yunus Emre",
                        21,
                        "Malatya",
                        true
                )
        );

        aliRow = new Row(
                List.of(
                        2,
                        "Ali",
                        16,
                        "Ankara",
                        false
                )
        );
    }

    @Test
    void comparisonExpression_shouldReturnTrueWhenConditionMatches() {
        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        boolean result = WhereEvaluator.evaluate(
                expression,
                yunusRow,
                usersTable
        );

        assertTrue(result);
    }

    @Test
    void comparisonExpression_shouldReturnFalseWhenConditionDoesNotMatch() {
        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        boolean result = WhereEvaluator.evaluate(
                expression,
                aliRow,
                usersTable
        );

        assertFalse(result);
    }

    @Test
    void andExpression_shouldReturnTrueWhenBothConditionsMatch() {
        Expression expression =
                new LogicalExpression(
                        new ComparisonExpression(
                                "age",
                                ComparisonOperator.GREATER_THAN,
                                18
                        ),
                        LogicalOperator.AND,
                        new ComparisonExpression(
                                "city",
                                ComparisonOperator.EQUALS,
                                "Malatya"
                        )
                );

        boolean result = WhereEvaluator.evaluate(
                expression,
                yunusRow,
                usersTable
        );

        assertTrue(result);
    }

    @Test
    void andExpression_shouldReturnFalseWhenOneConditionDoesNotMatch() {
        Expression expression =
                new LogicalExpression(
                        new ComparisonExpression(
                                "age",
                                ComparisonOperator.GREATER_THAN,
                                18
                        ),
                        LogicalOperator.AND,
                        new ComparisonExpression(
                                "city",
                                ComparisonOperator.EQUALS,
                                "Malatya"
                        )
                );

        boolean result = WhereEvaluator.evaluate(
                expression,
                aliRow,
                usersTable
        );

        assertFalse(result);
    }

    @Test
    void orExpression_shouldReturnTrueWhenOneConditionMatches() {
        Expression expression =
                new LogicalExpression(
                        new ComparisonExpression(
                                "city",
                                ComparisonOperator.EQUALS,
                                "Ankara"
                        ),
                        LogicalOperator.OR,
                        new ComparisonExpression(
                                "age",
                                ComparisonOperator.GREATER_THAN,
                                30
                        )
                );

        boolean result = WhereEvaluator.evaluate(
                expression,
                aliRow,
                usersTable
        );

        assertTrue(result);
    }

    @Test
    void orExpression_shouldReturnFalseWhenNoConditionMatches() {
        Expression expression =
                new LogicalExpression(
                        new ComparisonExpression(
                                "city",
                                ComparisonOperator.EQUALS,
                                "İstanbul"
                        ),
                        LogicalOperator.OR,
                        new ComparisonExpression(
                                "age",
                                ComparisonOperator.GREATER_THAN,
                                30
                        )
                );

        boolean result = WhereEvaluator.evaluate(
                expression,
                aliRow,
                usersTable
        );

        assertFalse(result);
    }

    @Test
    void notExpression_shouldReverseExpressionResult() {
        Expression expression =
                new NotExpression(
                        new ComparisonExpression(
                                "city",
                                ComparisonOperator.EQUALS,
                                "Ankara"
                        )
                );

        boolean result = WhereEvaluator.evaluate(
                expression,
                yunusRow,
                usersTable
        );

        assertTrue(result);
    }

    @Test
    void nestedLogicalExpression_shouldBeEvaluatedRecursively() {
        Expression ageAndCity =
                new LogicalExpression(
                        new ComparisonExpression(
                                "age",
                                ComparisonOperator.GREATER_THAN,
                                18
                        ),
                        LogicalOperator.AND,
                        new ComparisonExpression(
                                "city",
                                ComparisonOperator.EQUALS,
                                "Malatya"
                        )
                );

        Expression finalExpression =
                new LogicalExpression(
                        ageAndCity,
                        LogicalOperator.AND,
                        new ComparisonExpression(
                                "active",
                                ComparisonOperator.EQUALS,
                                true
                        )
                );

        boolean result = WhereEvaluator.evaluate(
                finalExpression,
                yunusRow,
                usersTable
        );

        assertTrue(result);
    }

    @Test
    void mapValueProvider_shouldEvaluateExpressionSuccessfully() {
        Map<String, Object> values = Map.of(
                "name", "Yunus Emre",
                "age", 21,
                "city", "Malatya"
        );

        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN_OR_EQUALS,
                        21
                );

        boolean result = WhereEvaluator.evaluate(
                expression,
                values::get
        );

        assertTrue(result);
    }

    @Test
    void columnName_shouldBeCaseInsensitiveForRowEvaluation() {
        Expression expression =
                new ComparisonExpression(
                        "CITY",
                        ComparisonOperator.EQUALS,
                        "Malatya"
                );

        boolean result = WhereEvaluator.evaluate(
                expression,
                yunusRow,
                usersTable
        );

        assertTrue(result);
    }

    @Test
    void unknownColumn_shouldThrowException() {
        Expression expression =
                new ComparisonExpression(
                        "salary",
                        ComparisonOperator.GREATER_THAN,
                        10_000
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WhereEvaluator.evaluate(
                                expression,
                                yunusRow,
                                usersTable
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void rowWithInvalidValueCount_shouldThrowException() {
        Row invalidRow = new Row(
                List.of(
                        1,
                        "Eksik Satır"
                )
        );

        Expression expression =
                new ComparisonExpression(
                        "id",
                        ComparisonOperator.EQUALS,
                        1
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> WhereEvaluator.evaluate(
                                expression,
                                invalidRow,
                                usersTable
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void nullExpression_shouldThrowException() {
        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> WhereEvaluator.evaluate(
                                null,
                                yunusRow,
                                usersTable
                        )
                );

        assertMessageExists(exception);
    }

    @Test
    void nullRow_shouldThrowException() {
        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> WhereEvaluator.evaluate(
                                expression,
                                null,
                                usersTable
                        )
                );

        assertMessageExists(exception);

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("row")
        );
    }

    @Test
    void nullTable_shouldThrowException() {
        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> WhereEvaluator.evaluate(
                                expression,
                                yunusRow,
                                null
                        )
                );

        assertMessageExists(exception);

        assertTrue(
                exception.getMessage()
                        .toLowerCase()
                        .contains("table")
        );
    }

    @Test
    void nullValueProvider_shouldThrowException() {
        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        NullPointerException exception =
                assertThrows(
                        NullPointerException.class,
                        () -> WhereEvaluator.evaluate(
                                expression,
                                (Function<String, Object>) null
                        )
                );

        assertMessageExists(exception);
    }

    /**
     * Exception mesajının null veya boş olmadığını doğrular.
     *
     * Noktalama, dil veya fazladan boşluk değişikliklerine
     * testleri gereksiz yere bağımlı yapmaz.
     */
    private static void assertMessageExists(
            Throwable exception
    ) {
        assertNotNull(exception);
        assertNotNull(exception.getMessage());

        assertFalse(
                exception.getMessage()
                        .isBlank()
        );
    }
}