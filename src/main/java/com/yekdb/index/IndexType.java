package com.yekdb.index;

/**
 * YEKDB içerisinde kullanılabilecek indeks türlerini tanımlar.
 *
 * PRIMARY:
 * Birincil anahtar indeksidir. Null ve tekrar eden anahtar kabul etmez.
 *
 * UNIQUE:
 * Tekil indeks türüdür. Tekrar eden anahtar kabul etmez.
 *
 * NON_UNIQUE:
 * Aynı anahtar için birden fazla kayıt adresi tutulmasına izin verir.
 */
public enum IndexType {

    PRIMARY,

    UNIQUE,

    NON_UNIQUE;

    /**
     * İndeksin tekrar eden anahtarları kabul edip etmediğini belirtir.
     *
     * @return tekrar eden anahtarlar kabul ediliyorsa true
     */
    public boolean allowsDuplicateKeys() {
        return this == NON_UNIQUE;
    }

    /**
     * İndeksin anahtar benzersizliği sağlayıp sağlamadığını belirtir.
     *
     * @return indeks PRIMARY veya UNIQUE ise true
     */
    public boolean isUnique() {
        return this == PRIMARY || this == UNIQUE;
    }

    /**
     * İndeksin primary key indeksi olup olmadığını belirtir.
     *
     * @return indeks PRIMARY ise true
     */
    public boolean isPrimary() {
        return this == PRIMARY;
    }
}