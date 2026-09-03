package com.yekdb.transaction;

/**
 * Transaction izolasyon seviyesi metadata bilgisidir.
 *
 * Phase 9 bu degeri parse edip transaction context icinde tasir.
 * Gercek concurrency/MVCC semantikleri sonraki sprint fazlarinda
 * bu alan uzerine kurulacaktir.
 */
public enum TransactionIsolationLevel {

    READ_COMMITTED,
    REPEATABLE_READ,
    SERIALIZABLE
}
