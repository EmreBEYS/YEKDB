package com.yekdb.constraint;

import java.util.List;

public interface Constraint {

    ConstraintType type();

    List<String> columns();

    /**
     * Constraint'in explicit SQL adı.
     *
     * <p>Eski Sprint 00-24/00-25 metadata kayıtları isim taşımadığı için
     * varsayılan değer null'dır. Böylece mevcut constraint implementasyonları
     * ve eski tablo dosyaları backward-compatible kalır.</p>
     */
    default String name() {
        return null;
    }

    default boolean isNamed() {
        return name() != null;
    }
}
