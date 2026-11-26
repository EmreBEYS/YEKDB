package com.yekdb.storage.wal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Binary WAL kayitlarini okur, sinirlari ve CRC32 degerlerini dogrular.
 */
public final class WalReader {

    private static final int PREFIX_SIZE =
            Integer.BYTES * 2;

    private static final int MAX_RECORD_SIZE =
            16 * 1024 * 1024;

    public List<WalRecord> readAll(Path walPath)
            throws IOException {

        try (FileChannel channel = FileChannel.open(
                walPath,
                StandardOpenOption.READ
        )) {
            return scan(channel).records();
        }
    }

    ScanResult scan(FileChannel channel)
            throws IOException {

        validateFileHeader(channel);

        List<WalRecord> records = new ArrayList<>();
        long position = WalManager.FILE_HEADER_SIZE;
        long fileSize = channel.size();
        long previousLsn = 0;

        while (position < fileSize) {

            long remaining = fileSize - position;

            if (remaining < PREFIX_SIZE) {
                return new ScanResult(
                        List.copyOf(records),
                        position,
                        true,
                        previousLsn
                );
            }

            ByteBuffer prefix = ByteBuffer.allocate(
                    PREFIX_SIZE
            );
            readFully(channel, prefix, position);
            prefix.flip();

            int magic = prefix.getInt();
            int totalLength = prefix.getInt();

            if (magic != WalWriter.RECORD_MAGIC) {
                throw corruption(
                        position,
                        "invalid record magic"
                );
            }

            if (totalLength < WalWriter.FIXED_RECORD_SIZE
                    || totalLength > MAX_RECORD_SIZE) {
                throw corruption(
                        position,
                        "invalid record length " + totalLength
                );
            }

            if (remaining < totalLength) {
                return new ScanResult(
                        List.copyOf(records),
                        position,
                        true,
                        previousLsn
                );
            }

            ByteBuffer recordBuffer = ByteBuffer.allocate(
                    totalLength
            );
            readFully(channel, recordBuffer, position);
            byte[] recordBytes = recordBuffer.array();

            WalRecord record = deserialize(
                    recordBytes,
                    position
            );

            if (record.getLsn() <= previousLsn) {
                throw corruption(
                        position,
                        "LSN is not strictly increasing"
                );
            }

            records.add(record);
            previousLsn = record.getLsn();
            position += totalLength;
        }

        return new ScanResult(
                List.copyOf(records),
                position,
                false,
                previousLsn
        );
    }

    private WalRecord deserialize(
            byte[] bytes,
            long position
    ) throws WalCorruptionException {

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        buffer.getInt();
        int totalLength = buffer.getInt();
        short version = buffer.getShort();

        if (version != WalWriter.RECORD_VERSION) {
            throw corruption(
                    position,
                    "unsupported record version " + version
            );
        }

        byte typeCode = buffer.get();
        buffer.get();
        long lsn = buffer.getLong();
        long transactionId = buffer.getLong();
        int pageId = buffer.getInt();
        int beforeLength = buffer.getInt();
        int afterLength = buffer.getInt();

        int payloadLength = totalLength
                - WalWriter.FIXED_RECORD_SIZE;

        if (beforeLength < 0
                || afterLength < 0
                || (long) beforeLength + afterLength
                != payloadLength) {
            throw corruption(
                    position,
                    "invalid image lengths"
            );
        }

        int expectedChecksum = ByteBuffer.wrap(
                bytes,
                totalLength - Integer.BYTES,
                Integer.BYTES
        ).getInt();

        int actualChecksum = WalChecksum.crc32(
                bytes,
                Integer.BYTES * 2,
                totalLength - Integer.BYTES * 3
        );

        if (actualChecksum != expectedChecksum) {
            throw corruption(
                    position,
                    "CRC32 mismatch"
            );
        }

        byte[] beforeImage = new byte[beforeLength];
        byte[] afterImage = new byte[afterLength];
        buffer.get(beforeImage);
        buffer.get(afterImage);

        try {
            return new WalRecord(
                    lsn,
                    transactionId,
                    pageId,
                    WalRecordType.fromCode(typeCode),
                    beforeImage,
                    afterImage
            );
        } catch (IllegalArgumentException exception) {
            throw new WalCorruptionException(
                    "Invalid WAL record at byte "
                            + position
                            + ".",
                    exception
            );
        }
    }

    private void validateFileHeader(FileChannel channel)
            throws IOException {

        if (channel.size() < WalManager.FILE_HEADER_SIZE) {
            throw new WalCorruptionException(
                    "Physical WAL file header is incomplete."
            );
        }

        ByteBuffer header = ByteBuffer.allocate(
                WalManager.FILE_HEADER_SIZE
        );
        readFully(channel, header, 0);
        header.flip();

        int magic = header.getInt();
        short version = header.getShort();
        short headerSize = header.getShort();

        if (magic != WalManager.FILE_MAGIC) {
            throw new WalCorruptionException(
                    "Invalid physical WAL file magic."
            );
        }

        if (version != WalManager.FILE_VERSION) {
            throw new WalCorruptionException(
                    "Unsupported physical WAL version: "
                            + version
            );
        }

        if (headerSize != WalManager.FILE_HEADER_SIZE) {
            throw new WalCorruptionException(
                    "Invalid physical WAL header size: "
                            + headerSize
            );
        }
    }

    private void readFully(
            FileChannel channel,
            ByteBuffer buffer,
            long position
    ) throws IOException {

        while (buffer.hasRemaining()) {
            int read = channel.read(buffer, position);

            if (read < 0) {
                throw new WalCorruptionException(
                        "Unexpected end of physical WAL."
                );
            }

            position += read;
        }
    }

    private WalCorruptionException corruption(
            long position,
            String reason
    ) {
        return new WalCorruptionException(
                "Corrupted WAL record at byte "
                        + position
                        + ": "
                        + reason
                        + "."
        );
    }

    record ScanResult(
            List<WalRecord> records,
            long validEnd,
            boolean tornTail,
            long lastLsn
    ) {
    }
}
