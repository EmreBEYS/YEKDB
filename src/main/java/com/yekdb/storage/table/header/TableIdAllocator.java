package com.yekdb.storage.table.header;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Yeni tablolar için benzersiz tablo kimlikleri üretir.
 *
 * ID allocator process içerisinde monoton artar.
 * Recovery sırasında disk üzerindeki en yüksek table ID dikkate
 * alınarak sonraki değer ileri taşınabilir; mevcut sequence hiçbir
 * zaman geriye çekilmez.
 */
public final class TableIdAllocator {

    private final AtomicLong sequence;

    public TableIdAllocator() {
        this(1L);
    }

    public TableIdAllocator(long initialValue) {

        if (initialValue < 1) {
            throw new IllegalArgumentException(
                    "Initial table ID must be greater than zero."
            );
        }

        this.sequence =
                new AtomicLong(initialValue);
    }

    /**
     * Yeni bir tablo ID değeri üretir.
     */
    public long nextId() {
        return sequence.getAndIncrement();
    }

    /**
     * Bir sonraki üretilecek ID'nin verilen değerden
     * küçük olmamasını garanti eder.
     *
     * Mevcut sequence daha ilerideyse geriye çekilmez.
     */
    public void ensureNextIdAtLeast(long nextId) {

        if (nextId < 1) {
            throw new IllegalArgumentException(
                    "Next table ID must be greater than zero."
            );
        }

        sequence.updateAndGet(
                current -> Math.max(current, nextId)
        );
    }

    /**
     * Sıradaki ID değerini değiştirmeden döndürür.
     *
     * Temel olarak test ve diagnostic amacıyla kullanılır.
     */
    public long peekNextId() {
        return sequence.get();
    }
}