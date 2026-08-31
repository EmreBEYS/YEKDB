package com.yekdb.index.bplustree;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * B+ Tree key değerlerinin binary formata dönüştürülmesini sağlar.
 *
 * Her index kendi key tipine uygun codec'i kullanabilir.
 */
public interface BPlusTreeKeyCodec<K extends Comparable<K>> {

    byte[] encode(K key);

    K decode(byte[] bytes);

    static BPlusTreeKeyCodec<Integer> integerCodec() {
        return new BPlusTreeKeyCodec<>() {

            @Override
            public byte[] encode(Integer key) {

                if (key == null) {
                    throw new IllegalArgumentException(
                            "Key cannot be null."
                    );
                }

                return ByteBuffer
                        .allocate(Integer.BYTES)
                        .putInt(key)
                        .array();
            }

            @Override
            public Integer decode(byte[] bytes) {

                requireExactLength(
                        bytes,
                        Integer.BYTES,
                        "INTEGER"
                );

                return ByteBuffer
                        .wrap(bytes)
                        .getInt();
            }
        };
    }

    static BPlusTreeKeyCodec<Long> longCodec() {
        return new BPlusTreeKeyCodec<>() {

            @Override
            public byte[] encode(Long key) {

                if (key == null) {
                    throw new IllegalArgumentException(
                            "Key cannot be null."
                    );
                }

                return ByteBuffer
                        .allocate(Long.BYTES)
                        .putLong(key)
                        .array();
            }

            @Override
            public Long decode(byte[] bytes) {

                requireExactLength(
                        bytes,
                        Long.BYTES,
                        "BIGINT"
                );

                return ByteBuffer
                        .wrap(bytes)
                        .getLong();
            }
        };
    }

    static BPlusTreeKeyCodec<Double> doubleCodec() {
        return new BPlusTreeKeyCodec<>() {

            @Override
            public byte[] encode(Double key) {

                if (key == null) {
                    throw new IllegalArgumentException(
                            "Key cannot be null."
                    );
                }

                return ByteBuffer
                        .allocate(Double.BYTES)
                        .putDouble(key)
                        .array();
            }

            @Override
            public Double decode(byte[] bytes) {

                requireExactLength(
                        bytes,
                        Double.BYTES,
                        "DOUBLE"
                );

                return ByteBuffer
                        .wrap(bytes)
                        .getDouble();
            }
        };
    }

    static BPlusTreeKeyCodec<String> stringCodec() {
        return new BPlusTreeKeyCodec<>() {

            @Override
            public byte[] encode(String key) {

                if (key == null) {
                    throw new IllegalArgumentException(
                            "Key cannot be null."
                    );
                }

                return key.getBytes(
                        StandardCharsets.UTF_8
                );
            }

            @Override
            public String decode(byte[] bytes) {

                requireBytes(bytes);

                return new String(
                        bytes,
                        StandardCharsets.UTF_8
                );
            }
        };
    }

    static BPlusTreeKeyCodec<BigDecimal> bigDecimalCodec() {
        return new BPlusTreeKeyCodec<>() {

            @Override
            public byte[] encode(BigDecimal key) {

                if (key == null) {
                    throw new IllegalArgumentException(
                            "Key cannot be null."
                    );
                }

                return key.toPlainString()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );
            }

            @Override
            public BigDecimal decode(byte[] bytes) {

                requireBytes(bytes);

                try {

                    return new BigDecimal(
                            new String(
                                    bytes,
                                    StandardCharsets.UTF_8
                            )
                    );

                } catch (NumberFormatException exception) {

                    throw new IllegalArgumentException(
                            "Invalid DECIMAL key payload.",
                            exception
                    );
                }
            }
        };
    }

    private static void requireBytes(
            byte[] bytes
    ) {

        if (bytes == null) {

            throw new IllegalArgumentException(
                    "Key payload cannot be null."
            );
        }
    }

    private static void requireExactLength(
            byte[] bytes,
            int expectedLength,
            String type
    ) {

        requireBytes(bytes);

        if (bytes.length != expectedLength) {

            throw new IllegalArgumentException(
                    "Invalid "
                            + type
                            + " key payload length: "
                            + bytes.length
            );
        }
    }
}