package com.yekdb.query.statement;

/**
 * Parser tarafından oluşturulabilen SQL statement
 * türlerini temsil eder.
 */
public enum StatementType {

    BEGIN_TRANSACTION,
    COMMIT_TRANSACTION,
    ROLLBACK_TRANSACTION,
    SAVEPOINT,
    ROLLBACK_TO_SAVEPOINT,
    RELEASE_SAVEPOINT,
    INSERT,
    EXPLAIN,
    SELECT,
    UPDATE,
    DELETE
}
