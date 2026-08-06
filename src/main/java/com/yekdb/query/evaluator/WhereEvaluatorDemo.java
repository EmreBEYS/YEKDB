package com.yekdb.query.evaluator;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;
import com.yekdb.query.expression.NotExpression;

import java.util.HashMap;
import java.util.Map;

public class WhereEvaluatorDemo {
    public static void main(String[] args){
        Map<String, Object> row = new HashMap<>();

        row.put("id", 1);
        row.put("name", "Yunus Emre");
        row.put("age", 21);
        row.put("city", "Malatya");
        row.put("active", true);

        Expression ageExpression = new ComparisonExpression(
                "age",
                ComparisonOperator.GREATER_THAN,
                18
        );

        Expression cityExpression = new ComparisonExpression(
                "city",
                ComparisonOperator.EQUALS,
                "Malatya"
        );

        Expression ageAndCityExpression = new LogicalExpression(
                ageExpression,
                LogicalOperator.AND,
                cityExpression
        );

        Expression activeExpression = new ComparisonExpression(
                "active",
                ComparisonOperator.EQUALS,
                true
        );

        Expression finalExpression = new LogicalExpression(
                ageAndCityExpression,
                LogicalOperator.AND,
                activeExpression
        );

        Expression notAnkaraExpression = new NotExpression(
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Ankara"
                )
        );

        boolean ageResult = WhereEvaluator.evaluate(
                ageExpression,
                row::get
        );

        boolean ageAndCityResult = WhereEvaluator.evaluate(
                ageAndCityExpression,
                row::get
        );

        boolean finalResult = WhereEvaluator.evaluate(
                finalExpression,
                row::get
        );

        boolean notAnkaraResult = WhereEvaluator.evaluate(
                notAnkaraExpression,
                row::get
        );

        System.out.println("Satır: " + row);
        System.out.println();

        System.out.println("age > 18: " + ageResult);

        System.out.println(
                "age > 18 AND city = Malatya: "
                        + ageAndCityResult
        );

        System.out.println(
                "age > 18 AND city = Malatya AND active = true: "
                        + finalResult
        );

        System.out.println(
                "NOT city = Ankara: "
                        + notAnkaraResult
        );
    }
}
