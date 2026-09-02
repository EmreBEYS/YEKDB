package com.yekdb.query.statement;

/**
 * Parser tarafından oluşturulabilen SQL statement
 * türlerini temsil eder.
 */
public enum StatementType {

    INSERT,
    EXPLAIN,
    SELECT,
    UPDATE,
    DELETE
}
