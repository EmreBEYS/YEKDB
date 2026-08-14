package com.yekdb.query.statement;

/**
 * YEKDB tarafından desteklenen JOIN türlerini temsil eder.
 */
public enum JoinType {

    /**
     * Yalnızca JOIN koşulunu sağlayan satırları döndürür.
     */
    INNER,

    /**
     * Sol tablodaki tüm satırları korur.
     * Sağ tarafta eşleşme yoksa değerler NULL olur.
     */
    LEFT,

    /**
     * Sağ tablodaki tüm satırları korur.
     * Sol tarafta eşleşme yoksa değerler NULL olur.
     */
    RIGHT,

    /**
     * Her iki tablodaki tüm satırları korur.
     * Eşleşmeyen tarafın kolonları NULL olur.
     */
    FULL
}