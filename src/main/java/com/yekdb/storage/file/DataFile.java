package com.yekdb.storage.file;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * YEKDB veri dosyasını temsil eder.
 *
 * Bu sınıf;
 *  - dosya oluşturur
 *  - açar
 *  - kapatır
 *  - byte yazar
 *  - byte okur
 */
public class DataFile {

    private final Path filePath;

    private RandomAccessFile file;

    public DataFile(Path filePath) {
        this.filePath = filePath;
    }

    /**
     * Dosyayı açar.
     */
    public void open() throws IOException {

        Files.createDirectories(filePath.getParent());

        file = new RandomAccessFile(
                filePath.toFile(),
                "rw"
        );
    }

    /**
     * Dosyayı kapatır.
     */
    public void close() throws IOException {

        if (file != null) {
            file.close();
        }
    }

    /**
     * Dosya boyutu.
     */
    public long size() throws IOException {
        return file.length();
    }

    /**
     * Dosyanın sonuna byte yazar.
     */
    public long append(byte[] bytes) throws IOException {

        file.seek(file.length());

        long position = file.getFilePointer();

        file.write(bytes);

        return position;
    }

    /**
     * Belirli konumdan byte okur.
     */
    public byte[] read(long position, int length)
            throws IOException {

        byte[] data = new byte[length];

        file.seek(position);

        file.readFully(data);

        return data;
    }

    /**
     * Flush
     */
    public void sync() throws IOException {

        file.getFD().sync();
    }

    public Path getFilePath() {
        return filePath;
    }
}