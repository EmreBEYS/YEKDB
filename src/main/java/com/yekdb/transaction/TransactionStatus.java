package com.yekdb.transaction;

/**
 * Transaction yasam dongusundeki temel durumlari temsil eder.
 */
public enum TransactionStatus {

    ACTIVE,
    COMMITTED,
    ROLLED_BACK
}
