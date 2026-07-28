package com.yekdb.storage.file;

import com.yekdb.core.YekdbConstants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * YEKDB veri dosyasının başlangıç bölümünde bulunan
 * veritabanı başlık bilgisini temsil eder.
 *
 * Header yapısı:
 *
 * Offset  Size   Field
 * ------  -----  ----------------
 * 0       4      Magic number
 * 4       4      File format version
 * 8       4      Page size
 * 12      4      Total page count
 * 16      8      Creation timestamp
 * 24      8      Last checkpoint timestamp
 * 32      96     Reserved
 *
 * Toplam header boyutu: 128 byte
 */
public final class DatabaseHeader {

    public static final int HEADER_SIZE = 128;

    public static final int MAGIC_NUMBER = 0x59454B44;

    public static final int CURRENT_VERSION = 1;

    private static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;

    private final int magicNumber;
    private final int version;
    private final int pageSize;
    private int totalPages;
    private final long creationTime;
    private long lastCheckpoint;

    /**
     * Yeni bir YEKDB veritabanı için varsayılan header oluşturur.
     */
    public DatabaseHeader() {
        this(
                MAGIC_NUMBER,
                CURRENT_VERSION,
                YekdbConstants.PAGE_SIZE,
                0,
                System.currentTimeMillis(),
                0
        );
    }

    public DatabaseHeader(
            int magicNumber,
            int version,
            int pageSize,
            int totalPages,
            long creationTime,
            long lastCheckpoint
    ) {
        validateVersion(version);
        validatePageSize(pageSize);
        validateTotalPages(totalPages);
        validateTimestamp(creationTime, "Creation time");
        validateTimestamp(lastCheckpoint, "Last checkpoint");

        this.magicNumber = magicNumber;
        this.version = version;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.creationTime = creationTime;
        this.lastCheckpoint = lastCheckpoint;
    }

    /**
     * Header nesnesini 128 byte uzunluğunda byte dizisine dönüştürür.
     */
    public byte[] toBytes() {

        ByteBuffer buffer = ByteBuffer
                .allocate(HEADER_SIZE)
                .order(BYTE_ORDER);

        buffer.putInt(magicNumber);
        buffer.putInt(version);
        buffer.putInt(pageSize);
        buffer.putInt(totalPages);
        buffer.putLong(creationTime);
        buffer.putLong(lastCheckpoint);

        /*
         * ByteBuffer kalan alanları varsayılan olarak 0 ile doldurur.
         * Bu bölüm gelecekteki dosya formatı alanları için ayrılmıştır.
         */
        return buffer.array();
    }

    /**
     * 128 byte uzunluğundaki veriden DatabaseHeader oluşturur.
     */
    public static DatabaseHeader fromBytes(byte[] bytes) {

        Objects.requireNonNull(
                bytes,
                "Database header bytes cannot be null."
        );

        if (bytes.length != HEADER_SIZE) {
            throw new IllegalArgumentException(
                    "Database header must be exactly "
                            + HEADER_SIZE
                            + " bytes."
            );
        }

        ByteBuffer buffer = ByteBuffer
                .wrap(bytes)
                .order(BYTE_ORDER);

        int magicNumber = buffer.getInt();
        int version = buffer.getInt();
        int pageSize = buffer.getInt();
        int totalPages = buffer.getInt();
        long creationTime = buffer.getLong();
        long lastCheckpoint = buffer.getLong();

        if (magicNumber != MAGIC_NUMBER) {
            throw new IllegalArgumentException(
                    "Invalid YEKDB database file magic number."
            );
        }

        return new DatabaseHeader(
                magicNumber,
                version,
                pageSize,
                totalPages,
                creationTime,
                lastCheckpoint
        );
    }

    /**
     * Toplam sayfa sayısını bir artırır.
     */
    public void incrementTotalPages() {

        if (totalPages == Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Maximum page count has been reached."
            );
        }

        totalPages++;
    }

    /**
     * Son checkpoint zamanını günceller.
     */
    public void updateLastCheckpoint(long timestamp) {

        validateTimestamp(timestamp, "Last checkpoint");

        if (timestamp < creationTime) {
            throw new IllegalArgumentException(
                    "Last checkpoint cannot be earlier than creation time."
            );
        }

        lastCheckpoint = timestamp;
    }

    public boolean hasValidMagicNumber() {
        return magicNumber == MAGIC_NUMBER;
    }

    public boolean isCurrentVersion() {
        return version == CURRENT_VERSION;
    }

    public int getMagicNumber() {
        return magicNumber;
    }

    public int getVersion() {
        return version;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastCheckpoint() {
        return lastCheckpoint;
    }

    private static void validateVersion(int version) {

        if (version <= 0) {
            throw new IllegalArgumentException(
                    "Database format version must be positive."
            );
        }
    }

    private static void validatePageSize(int pageSize) {

        if (pageSize <= 0) {
            throw new IllegalArgumentException(
                    "Page size must be positive."
            );
        }

        if (Integer.bitCount(pageSize) != 1) {
            throw new IllegalArgumentException(
                    "Page size must be a power of two."
            );
        }
    }

    private static void validateTotalPages(int totalPages) {

        if (totalPages < 0) {
            throw new IllegalArgumentException(
                    "Total page count cannot be negative."
            );
        }
    }

    private static void validateTimestamp(
            long timestamp,
            String fieldName
    ) {

        if (timestamp < 0) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be negative."
            );
        }
    }

    @Override
    public String toString() {
        return "DatabaseHeader{" +
                "magicNumber=0x" +
                Integer.toHexString(magicNumber).toUpperCase() +
                ", version=" + version +
                ", pageSize=" + pageSize +
                ", totalPages=" + totalPages +
                ", creationTime=" + creationTime +
                ", lastCheckpoint=" + lastCheckpoint +
                '}';
    }
}