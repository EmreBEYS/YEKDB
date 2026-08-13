package com.yekdb.query.statement;

import com.yekdb.query.expression.Expression;

/**
 * Represents a JOIN clause inside a SELECT statement.
 *
 * Example:
 *
 * INNER JOIN department d
 * ON e.department_id = d.id
 */
public class JoinClause {

    private final JoinType joinType;
    private final String tableName;
    private final String alias;
    private final Expression condition;

    public JoinClause(
            JoinType joinType,
            String tableName,
            String alias,
            Expression condition
    ) {
        this.joinType = joinType;
        this.tableName = tableName;
        this.alias = alias;
        this.condition = condition;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public String getTableName() {
        return tableName;
    }

    public String getAlias() {
        return alias;
    }

    public Expression getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return "JoinClause{" +
                "joinType=" + joinType +
                ", tableName='" + tableName + '\'' +
                ", alias='" + alias + '\'' +
                ", condition=" + condition +
                '}';
    }
}