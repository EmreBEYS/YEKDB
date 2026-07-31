package com.yekdb.storage.record;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Row nesnelerini byte dizisine,
 * byte dizilerini tekrar Row nesnesine dönüştürür.
 *
 * Binary format:
 *
 * [Value Count: 4 byte]
 *
 * Her sütun için:
 *
 * [Type: 1 byte]
 * [Value: N byte]
 *
 * STRING değerleri için:
 *
 * [Type: 1 byte]
 * [String Length: 4 byte]
 * [UTF-8 String Data: N byte]
 */
public final class RowSerializer {

    private static final byte TYPE_INTEGER = 1;
    private static final byte TYPE_LONG = 2;
    private static final byte TYPE_DOUBLE = 3;
    private static final byte TYPE_BOOLEAN = 4;
    private static final byte TYPE_STRING = 5;

    private static final int VALUE_COUNT_SIZE = Integer.BYTES;
    private static final int TYPE_SIZE = Byte.BYTES;
    private static final int STRING_LENGTH_SIZE = Integer.BYTES;

    private RowSerializer() {
        // Utility class olduğu için nesne oluşturulamaz.
    }

    /**
     * Row nesnesini byte dizisine dönüştürür.
     *
     * @param row Serialize edilecek Row
     * @return Row verisinin binary karşılığı
     */
    public static byte[] serialize(Row row) {
        if (row == null) {
            throw new IllegalArgumentException(
                    "Serialize edilecek Row null olamaz."
            );
        }

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        writeInt(outputStream, row.size());

        for (Object value : row.getValues()) {
            writeValue(outputStream, value);
        }

        return outputStream.toByteArray();
    }

    /**
     * Byte dizisini tekrar Row nesnesine dönüştürür.
     *
     * @param bytes Deserialize edilecek binary veri
     * @return Oluşturulan Row nesnesi
     */
    public static Row deserialize(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException(
                    "Deserialize edilecek byte dizisi null olamaz."
            );
        }

        if (bytes.length < VALUE_COUNT_SIZE) {
            throw new IllegalArgumentException(
                    "Byte dizisi geçerli bir Row için çok küçük."
            );
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int valueCount = buffer.getInt();

        if (valueCount < 0) {
            throw new IllegalArgumentException(
                    "Row değer sayısı negatif olamaz."
            );
        }

        List<Object> values = new ArrayList<>(valueCount);

        for (int index = 0; index < valueCount; index++) {
            values.add(readValue(buffer, index));
        }

        if (buffer.hasRemaining()) {
            throw new IllegalArgumentException(
                    "Row verisi okunduktan sonra beklenmeyen " +
                            buffer.remaining() +
                            " byte bulundu."
            );
        }

        return new Row(values);
    }

    /**
     * Bir Row nesnesinin serialize edildiğinde kaplayacağı
     * toplam byte miktarını hesaplar.
     *
     * @param row Boyutu hesaplanacak Row
     * @return Serialized Row boyutu
     */
    public static int calculateSerializedSize(Row row) {
        if (row == null) {
            throw new IllegalArgumentException(
                    "Row null olamaz."
            );
        }

        int totalSize = VALUE_COUNT_SIZE;

        for (Object value : row.getValues()) {
            totalSize += calculateValueSize(value);
        }

        return totalSize;
    }

    private static void writeValue(
            ByteArrayOutputStream outputStream,
            Object value
    ) {
        if (value instanceof Integer integerValue) {
            outputStream.write(TYPE_INTEGER);
            writeInt(outputStream, integerValue);
            return;
        }

        if (value instanceof Long longValue) {
            outputStream.write(TYPE_LONG);
            writeLong(outputStream, longValue);
            return;
        }

        if (value instanceof Double doubleValue) {
            outputStream.write(TYPE_DOUBLE);
            writeDouble(outputStream, doubleValue);
            return;
        }

        if (value instanceof Boolean booleanValue) {
            outputStream.write(TYPE_BOOLEAN);
            outputStream.write(booleanValue ? 1 : 0);
            return;
        }

        if (value instanceof String stringValue) {
            byte[] stringBytes =
                    stringValue.getBytes(StandardCharsets.UTF_8);

            outputStream.write(TYPE_STRING);
            writeInt(outputStream, stringBytes.length);
            outputStream.writeBytes(stringBytes);
            return;
        }

        throw new IllegalArgumentException(
                "Desteklenmeyen Row değeri tipi: " +
                        value.getClass().getName()
        );
    }

    private static Object readValue(
            ByteBuffer buffer,
            int columnIndex
    ) {
        requireRemaining(
                buffer,
                TYPE_SIZE,
                "Sütun tip bilgisi",
                columnIndex
        );

        byte type = buffer.get();

        return switch (type) {
            case TYPE_INTEGER -> readInteger(buffer, columnIndex);
            case TYPE_LONG -> readLong(buffer, columnIndex);
            case TYPE_DOUBLE -> readDouble(buffer, columnIndex);
            case TYPE_BOOLEAN -> readBoolean(buffer, columnIndex);
            case TYPE_STRING -> readString(buffer, columnIndex);

            default -> throw new IllegalArgumentException(
                    "Geçersiz Row veri tipi kodu: " + type +
                            ". Sütun indeksi: " + columnIndex
            );
        };
    }

    private static int readInteger(
            ByteBuffer buffer,
            int columnIndex
    ) {
        requireRemaining(
                buffer,
                Integer.BYTES,
                "Integer değeri",
                columnIndex
        );

        return buffer.getInt();
    }

    private static long readLong(
            ByteBuffer buffer,
            int columnIndex
    ) {
        requireRemaining(
                buffer,
                Long.BYTES,
                "Long değeri",
                columnIndex
        );

        return buffer.getLong();
    }

    private static double readDouble(
            ByteBuffer buffer,
            int columnIndex
    ) {
        requireRemaining(
                buffer,
                Double.BYTES,
                "Double değeri",
                columnIndex
        );

        return buffer.getDouble();
    }

    private static boolean readBoolean(
            ByteBuffer buffer,
            int columnIndex
    ) {
        requireRemaining(
                buffer,
                Byte.BYTES,
                "Boolean değeri",
                columnIndex
        );

        byte booleanValue = buffer.get();

        if (booleanValue == 0) {
            return false;
        }

        if (booleanValue == 1) {
            return true;
        }

        throw new IllegalArgumentException(
                "Geçersiz boolean değeri: " + booleanValue +
                        ". Sütun indeksi: " + columnIndex
        );
    }

    private static String readString(
            ByteBuffer buffer,
            int columnIndex
    ) {
        requireRemaining(
                buffer,
                STRING_LENGTH_SIZE,
                "String uzunluk bilgisi",
                columnIndex
        );

        int stringLength = buffer.getInt();

        if (stringLength < 0) {
            throw new IllegalArgumentException(
                    "String uzunluğu negatif olamaz. " +
                            "Sütun indeksi: " + columnIndex
            );
        }

        requireRemaining(
                buffer,
                stringLength,
                "String verisi",
                columnIndex
        );

        byte[] stringBytes = new byte[stringLength];
        buffer.get(stringBytes);

        return new String(
                stringBytes,
                StandardCharsets.UTF_8
        );
    }

    private static int calculateValueSize(Object value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Row değeri null olamaz."
            );
        }

        if (value instanceof Integer) {
            return TYPE_SIZE + Integer.BYTES;
        }

        if (value instanceof Long) {
            return TYPE_SIZE + Long.BYTES;
        }

        if (value instanceof Double) {
            return TYPE_SIZE + Double.BYTES;
        }

        if (value instanceof Boolean) {
            return TYPE_SIZE + Byte.BYTES;
        }

        if (value instanceof String stringValue) {
            int stringByteLength =
                    stringValue.getBytes(StandardCharsets.UTF_8).length;

            return TYPE_SIZE +
                    STRING_LENGTH_SIZE +
                    stringByteLength;
        }

        throw new IllegalArgumentException(
                "Desteklenmeyen Row değeri tipi: " +
                        value.getClass().getName()
        );
    }

    private static void requireRemaining(
            ByteBuffer buffer,
            int requiredBytes,
            String fieldName,
            int columnIndex
    ) {
        if (buffer.remaining() < requiredBytes) {
            throw new IllegalArgumentException(
                    fieldName +
                            " için yeterli byte bulunamadı. " +
                            "Sütun indeksi: " + columnIndex +
                            ", gerekli: " + requiredBytes +
                            ", kalan: " + buffer.remaining()
            );
        }
    }

    private static void writeInt(
            ByteArrayOutputStream outputStream,
            int value
    ) {
        outputStream.writeBytes(
                ByteBuffer.allocate(Integer.BYTES)
                        .putInt(value)
                        .array()
        );
    }

    private static void writeLong(
            ByteArrayOutputStream outputStream,
            long value
    ) {
        outputStream.writeBytes(
                ByteBuffer.allocate(Long.BYTES)
                        .putLong(value)
                        .array()
        );
    }

    private static void writeDouble(
            ByteArrayOutputStream outputStream,
            double value
    ) {
        outputStream.writeBytes(
                ByteBuffer.allocate(Double.BYTES)
                        .putDouble(value)
                        .array()
        );
    }
}