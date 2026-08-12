package com.yekdb.query.mapper;

import com.yekdb.query.command.SelectCommand;
import com.yekdb.query.statement.SelectStatement;

import java.util.Objects;

/**
 * SelectStatement nesnesini SelectCommand
 * nesnesine dönüştürür.
 *
 * Sprint 00-14:
 *
 * Gelişmiş SELECT modelinin tamamı kayıpsız şekilde
 * execution katmanına taşınır.
 *
 * Korunan bilgiler:
 *
 * - SELECT item listesi
 * - Column alias
 * - Table alias
 * - WHERE
 * - GROUP BY
 * - HAVING
 * - ORDER BY
 * - LIMIT
 * - FETCH
 * - Aggregate expressions
 *
 * Bu mapper SQL parse etmez ve sorgu çalıştırmaz.
 * Yalnızca:
 *
 * SelectStatement -> SelectCommand
 *
 * dönüşümünden sorumludur.
 */
public final class SelectMapper {

    /**
     * SelectStatement'ı execution-ready
     * SelectCommand modeline dönüştürür.
     */
    public SelectCommand map(
            SelectStatement statement
    ) {

        Objects.requireNonNull(
                statement,
                "SelectStatement cannot be null."
        );

        return SelectCommand.fromStatement(
                statement
        );
    }
}