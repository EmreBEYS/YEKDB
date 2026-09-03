package com.yekdb.transaction;

/**
 * Transaction rollback sirasinda ters sirayla calistirilacak
 * geri alma operasyonudur.
 */
@FunctionalInterface
public interface TransactionUndoAction {

    void rollback();
}
