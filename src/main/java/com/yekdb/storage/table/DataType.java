package com.yekdb.storage.table;

/**
 * YEKDB'de desteklenen veri tipleri.
 *
 * Sprint 00-28 Phase 9:
 * CHAR, VARCHAR, TEXT, BOOLEAN.
 *
 * Sprint 00-28 Phase 10:
 * NUMERIC(p,s) exact-decimal schema semantics and FLOAT(n)
 * declaration metadata.
 *
 * Sprint 00-28 Phase 11:
 * UUID, DATE, TIME, TIMESTAMP and INTERVAL logical SQL types.
 *
 * Sprint 00-28 Phase 12:
 * One-dimensional ARRAY and JSON logical SQL types.
 *
 * Sprint 00-28 Phase 13:
 * PostgreSQL-style HSTORE logical SQL type.
 *
 * Sprint 00-28 Phase 14:
 * User-defined type (UDT) schema foundation.
 */
public enum DataType {

    INT,
    LONG,
    DOUBLE,
    NUMERIC,
    BOOLEAN,
    STRING,
    CHAR,
    VARCHAR,
    TEXT,
    UUID,
    DATE,
    TIME,
    TIMESTAMP,
    INTERVAL,
    ARRAY,
    JSON,
    HSTORE,
    UDT

}
