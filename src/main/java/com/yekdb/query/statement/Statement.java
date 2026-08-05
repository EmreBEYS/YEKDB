package com.yekdb.query.statement;

/**
 * Parser tarafından oluşturulan bütün SQL statement
 * modellerinin ortak sözleşmesidir.
 */

public interface Statement {
    /**
     * Statement türünü döndürür.
     *
     * @return statement türü
     */
    StatementType getType();
}
