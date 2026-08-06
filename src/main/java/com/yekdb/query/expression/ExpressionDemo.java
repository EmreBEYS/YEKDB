package com.yekdb.query.expression;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import com.yekdb.query.expression.LogicalExpression;
import com.yekdb.query.expression.LogicalOperator;

public class ExpressionDemo {

    public static void main(String[] args) {

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

        Expression whereExpression = new LogicalExpression(
                ageExpression,
                LogicalOperator.AND,
                cityExpression
        );

        System.out.println(whereExpression);
    }
}