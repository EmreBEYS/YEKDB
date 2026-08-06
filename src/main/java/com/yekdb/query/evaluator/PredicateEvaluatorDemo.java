package com.yekdb.query.evaluator;

import com.yekdb.query.expression.ComparisonOperator;

public class PredicateEvaluatorDemo {

    public static void main(String[] args) {

        boolean ageResult = PredicateEvaluator.evaluate(
                25,
                18,
                ComparisonOperator.GREATER_THAN
        );

        boolean cityResult = PredicateEvaluator.evaluate(
                "Malatya",
                "Malatya",
                ComparisonOperator.EQUALS
        );

        boolean salaryResult = PredicateEvaluator.evaluate(
                45_000,
                50_000,
                ComparisonOperator.GREATER_THAN_OR_EQUALS
        );

        boolean differentNumberTypes = PredicateEvaluator.evaluate(
                18,
                18.0,
                ComparisonOperator.EQUALS
        );

        boolean nullResult = PredicateEvaluator.evaluate(
                null,
                null,
                ComparisonOperator.EQUALS
        );

        System.out.println("25 > 18: " + ageResult);
        System.out.println("Malatya = Malatya: " + cityResult);
        System.out.println("45000 >= 50000: " + salaryResult);
        System.out.println("18 = 18.0: " + differentNumberTypes);
        System.out.println("null = null: " + nullResult);
    }
}