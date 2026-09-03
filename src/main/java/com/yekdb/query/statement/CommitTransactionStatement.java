package com.yekdb.query.statement;

/**
 * COMMIT transaction statement modelidir.
 */
public final class CommitTransactionStatement implements Statement {

    @Override
    public StatementType getType() {
        return StatementType.COMMIT_TRANSACTION;
    }
}
