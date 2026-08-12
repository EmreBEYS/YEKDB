package com.yekdb.query.evaluator;

import com.yekdb.query.expression.LikeExpression;
import com.yekdb.query.expression.LikeOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionEvaluatorLikeTest {

    private ExpressionEvaluator evaluator;
    private Map<String, Object> rowValues;

    @BeforeEach
    void setUp() {

        evaluator =
                new ExpressionEvaluator();

        rowValues =
                new HashMap<>();
    }

    @Test
    void shouldMatchPrefixWithLike() {

        rowValues.put(
                "name",
                "Ali Veli"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "Ali%",
                        LikeOperator.LIKE
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldMatchSuffixWithLike() {

        rowValues.put(
                "name",
                "Yunus Emre"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "%Emre",
                        LikeOperator.LIKE
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldMatchContainsWithLike() {

        rowValues.put(
                "name",
                "Yunus Emre Kul"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "%Emre%",
                        LikeOperator.LIKE
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldMatchSingleCharacterWildcard() {

        rowValues.put(
                "name",
                "Ali"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "A_i",
                        LikeOperator.LIKE
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldReturnFalseWhenLikeDoesNotMatch() {

        rowValues.put(
                "name",
                "Mehmet"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "Ali%",
                        LikeOperator.LIKE
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldBeCaseSensitiveForLike() {

        rowValues.put(
                "name",
                "ALI"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "ali%",
                        LikeOperator.LIKE
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldBeCaseInsensitiveForILike() {

        rowValues.put(
                "name",
                "ALI VELI"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "ali%",
                        LikeOperator.ILIKE
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldEvaluateNotLike() {

        rowValues.put(
                "name",
                "Mehmet"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "Ali%",
                        LikeOperator.NOT_LIKE
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldReturnFalseWhenNotLikeMatches() {

        rowValues.put(
                "name",
                "Ali Veli"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "Ali%",
                        LikeOperator.NOT_LIKE
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldEvaluateNotILike() {

        rowValues.put(
                "name",
                "Mehmet"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "ali%",
                        LikeOperator.NOT_ILIKE
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldReturnFalseWhenNotILikeMatchesIgnoringCase() {

        rowValues.put(
                "name",
                "ALI VELI"
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "ali%",
                        LikeOperator.NOT_ILIKE
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldTreatRegexSpecialCharactersLiterally() {

        rowValues.put(
                "version",
                "v1.0-release"
        );

        LikeExpression expression =
                new LikeExpression(
                        "version",
                        "v1.0%",
                        LikeOperator.LIKE
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldReturnFalseWhenActualValueIsNull() {

        rowValues.put(
                "name",
                null
        );

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "%",
                        LikeOperator.LIKE
                );

        assertFalse(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldConvertNonStringValueToString() {

        rowValues.put(
                "code",
                12345
        );

        LikeExpression expression =
                new LikeExpression(
                        "code",
                        "123%",
                        LikeOperator.LIKE
                );

        assertTrue(
                evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenColumnDoesNotExist() {

        LikeExpression expression =
                new LikeExpression(
                        "name",
                        "Ali%",
                        LikeOperator.LIKE
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> evaluator.evaluate(
                        expression,
                        rowValues
                )
        );
    }
}