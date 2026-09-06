package com.yekdb.concurrency;

/**
 * Lock manager tarafindan izlenebilecek kaynak seviyeleri.
 *
 * Phase 1 tablo kilitleriyle baslar. Diger seviyeler sonraki
 * concurrency phase'leri icin ayni kimlik modelini kullanir.
 */
public enum LockResourceType {

    DATABASE,
    TABLE,
    PAGE,
    ROW,
    INDEX
}
