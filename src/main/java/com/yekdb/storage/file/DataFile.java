package com.yekdb.storage.file;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * YEKDB fiziksel veri dosyasını yönetir.
 *
 * Bu sınıf;
 * - veri dosyasını oluşturur,
 * - açar ve kapatır,
 * - belirli konumlara veri yazar,
 * - belirli konumlardan veri okur,
 * - dosyanın sonuna veri ekler,
 * - verilerin fiziksel diske aktarılmasını sağlar.
 */
public final class DataFile implements AutoCloseable {

    private final Path filePath;

    private RandomAccessFile file;

    public DataFile(Path filePath) {
        this.filePath = Objects.requireNonNull(
                filePath,
                "File path cannot be null."
        );
    }

    /**
     * Veri dosyasını ve gerekli üst dizinleri oluşturur.
     *
     * Dosya zaten varsa herhangi bir işlem yapmaz.
     */
    public void create() throws IOException {

        Path parentDirectory = filePath.getParent();

        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }

        if (Files.notExists(filePath)) {
            Files.createFile(filePath);
        }
    }

    /**
     * Veri dosyasının mevcut olup olmadığını döndürür.
     */
    public boolean exists() {
        return Files.exists(filePath);
    }

    /**
     * Veri dosyasını okuma-yazma modunda açar.
     */
    public void open() throws IOException {

        if (isOpen()) {
            return;
        }

        create();

        file = new RandomAccessFile(
                filePath.toFile(),
                "rw"
        );
    }

    /**
     * Veri dosyasını kapatır.
     */
    @Override
    public void close() throws IOException {

        if (file != null) {
            file.close();
            file = null;
        }
    }

    /**
     * Dosyanın açık olup olmadığını döndürür.
     */
    public boolean isOpen() {
        return file != null;
    }

    /**
     * Dosyanın byte cinsinden boyutunu döndürür.
     */
    public long size() throws IOException {

        ensureOpen();

        return file.length();
    }

    /**
     * Veriyi dosyanın sonuna ekler.
     *
     * @param bytes yazılacak veri
     * @return verinin yazılmaya başlandığı dosya konumu
     */
    public long append(byte[] bytes) throws IOException {

        Objects.requireNonNull(
                bytes,
                "Bytes cannot be null."
        );

        ensureOpen();

        long position = file.length();

        file.seek(position);
        file.write(bytes);

        return position;
    }

    /**
     * Veriyi belirtilen dosya konumuna yazar.
     *
     * Mevcut veri varsa üzerine yazılır.
     *
     * @param position yazma başlangıç konumu
     * @param bytes    yazılacak veri
     */
    public void write(long position, byte[] bytes) throws IOException {

        validatePosition(position);

        Objects.requireNonNull(
                bytes,
                "Bytes cannot be null."
        );

        ensureOpen();

        file.seek(position);
        file.write(bytes);
    }

    /**
     * Belirtilen konumdan istenen uzunlukta veri okur.
     *
     * @param position okuma başlangıç konumu
     * @param length   okunacak byte sayısı
     * @return okunan byte dizisi
     */
    public byte[] read(long position, int length) throws IOException {

        validatePosition(position);

        if (length < 0) {
            throw new IllegalArgumentException(
                    "Read length cannot be negative."
            );
        }

        ensureOpen();

        long requestedEndPosition = position + length;

        if (requestedEndPosition > file.length()) {
            throw new EOFException(
                    "Requested range exceeds the data file size."
            );
        }

        byte[] data = new byte[length];

        file.seek(position);
        file.readFully(data);

        return data;
    }

    /**
     * Dosya boyutunu belirtilen değere ayarlar.
     *
     * Bu metot test temizliği ve dosya sıfırlama işlemlerinde kullanılabilir.
     */
    public void resize(long newLength) throws IOException {

        if (newLength < 0) {
            throw new IllegalArgumentException(
                    "File length cannot be negative."
            );
        }

        ensureOpen();

        file.setLength(newLength);
    }

    /**
     * Bellekte bekleyen değişiklikleri fiziksel diske aktarır.
     */
    public void sync() throws IOException {

        ensureOpen();

        file.getFD().sync();
    }

    public Path getFilePath() {
        return filePath;
    }

    private void ensureOpen() {

        if (!isOpen()) {
            throw new IllegalStateException(
                    "Data file is not open: " + filePath
            );
        }
    }

    private void validatePosition(long position) {

        if (position < 0) {
            throw new IllegalArgumentException(
                    "File position cannot be negative."
            );
        }
    }
}