package com.yekdb.query.statement;

/**
 * ROLLBACK transaction statement modelidir.
 */
public final class RollbackTransactionStatement implements Statement {

    @Override
    public StatementType getType() {
        return StatementType.ROLLBACK_TRANSACTION;
    }
}
