package com.yekdb.query.function;

/**
 * SQL fonksiyonlarının genel kategorisini temsil eder.
 * Sprint 00-28 Aşama 1, başlangıçta skalar fonksiyonları tanıtmaktadır.
 * Gelecek sürümlerde ek fonksiyon aileleri eklenebilir.
 */
public enum FunctionType {
    /**
     * Bir veya daha fazla değer alan ve tek bir değer döndüren bir fonksiyon.
     *
     * Örnekler:
     * LOWER(isim)
     * ABS(bakiye)
     * LENGTH(metin)
     */
    SCALAR
}
