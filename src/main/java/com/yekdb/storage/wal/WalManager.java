package com.yekdb.storage.wal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

/**
 * Binary fiziksel WAL dosyasinin yasam dongusunu ve kaliciligini yonetir.
 */
public final class WalManager implements AutoCloseable {

    public static final String FILE_SUFFIX = ".wal";

    static final int FILE_MAGIC = 0x59454B57;

    static final short FILE_VERSION = 1;

    static final short FILE_HEADER_SIZE = 16;

    private final Path walPath;

    private final WalWriter writer;

    private final WalReader reader;

    private FileChannel channel;

    private long nextLsn;

    public WalManager(Path walPath) {

        this.walPath = Objects.requireNonNull(
                walPath,
                "WAL path cannot be null."
        );
        this.writer = new WalWriter();
        this.reader = new WalReader();
        this.nextLsn = 1;
    }

    public synchronized void open() throws IOException {

        if (isOpen()) {
            return;
        }

        Path parent = walPath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        channel = FileChannel.open(
                walPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
        );

        try {
            if (channel.size() == 0) {
                writeFileHeader();
            }

            WalReader.ScanResult scanResult =
                    reader.scan(channel);

            repairTornTail(scanResult);
            nextLsn = scanResult.lastLsn() + 1;
        } catch (IOException | RuntimeException exception) {
            closeAfterOpenFailure();
            throw exception;
        }
    }

    public synchronized long appendPageUpdate(
            long transactionId,
            int pageId,
            byte[] beforeImage,
            byte[] afterImage
    ) throws IOException {

        ensureOpen();

        try (FileLock ignored = channel.lock()) {

            WalReader.ScanResult scanResult =
                    reader.scan(channel);

            repairTornTail(scanResult);

            long lsn = Math.max(
                    nextLsn,
                    scanResult.lastLsn() + 1
            );

            WalRecord record = new WalRecord(
                    lsn,
                    transactionId,
                    pageId,
                    WalRecordType.PAGE_UPDATE,
                    beforeImage,
                    afterImage
            );

            byte[] bytes = writer.serialize(record);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            long writePosition = channel.size();

            while (buffer.hasRemaining()) {
                writePosition += channel.write(
                        buffer,
                        writePosition
                );
            }

            channel.force(true);
            nextLsn = lsn + 1;

            return lsn;
        }
    }

    public synchronized List<WalRecord> readAll()
            throws IOException {

        ensureOpen();

        return reader.scan(channel).records();
    }

    public Path getWalPath() {
        return walPath;
    }

    public synchronized boolean isOpen() {
        return channel != null && channel.isOpen();
    }

    @Override
    public synchronized void close() throws IOException {

        if (!isOpen()) {
            return;
        }

        try {
            channel.force(true);
        } finally {
            channel.close();
            channel = null;
        }
    }

    private void writeFileHeader() throws IOException {

        ByteBuffer header = ByteBuffer.allocate(
                FILE_HEADER_SIZE
        );
        header.putInt(FILE_MAGIC);
        header.putShort(FILE_VERSION);
        header.putShort(FILE_HEADER_SIZE);
        header.putLong(0L);
        header.flip();

        long position = 0;

        while (header.hasRemaining()) {
            position += channel.write(
                    header,
                    position
            );
        }

        channel.force(true);
    }

    private void repairTornTail(
            WalReader.ScanResult scanResult
    ) throws IOException {

        if (scanResult.tornTail()) {
            channel.truncate(
                    scanResult.validEnd()
            );
            channel.force(true);
        }
    }

    private void ensureOpen() {

        if (!isOpen()) {
            throw new IllegalStateException(
                    "WalManager must be opened before use."
            );
        }
    }

    private void closeAfterOpenFailure() {

        try {
            if (channel != null) {
                channel.close();
            }
        } catch (IOException ignored) {
            // Acilis hatasini gizlememek icin yutulur.
        } finally {
            channel = null;
        }
    }
}
