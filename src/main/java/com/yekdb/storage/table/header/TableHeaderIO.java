package com.yekdb.storage.table.header;

import com.yekdb.storage.exception.CorruptedTableHeaderException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * {@link TableHeader} verisinin fiziksel tablo dosyasının başlangıcına
 * yazılmasını ve aynı konumdan geri okunmasını yönetir.
 *
 * <p>Header güncellenirken dosyanın header sonrasındaki şema ve veri
 * bölümleri korunur.</p>
 */
public final class TableHeaderIO {

    private TableHeaderIO() {
    }

    public static void write(Path path, TableHeader header) throws IOException {

        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null.");
        }

        TableHeaderValidator.validate(header);

        byte[] serialized =
                TableHeaderSerializer.serialize(header);

        try (
                FileChannel channel = FileChannel.open(
                        path,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE
                )
        ) {

            ByteBuffer buffer =
                    ByteBuffer.wrap(serialized);

            long position =
                    TableHeaderFile.HEADER_OFFSET;

            while (buffer.hasRemaining()) {

                int written =
                        channel.write(
                                buffer,
                                position
                        );

                if (written <= 0) {
                    throw new IOException(
                            "Failed to write table header."
                    );
                }

                position += written;
            }

            channel.force(true);
        }
    }

    public static TableHeader read(Path path) throws IOException {

        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null.");
        }

        ByteBuffer buffer =
                ByteBuffer.allocate(
                        TableHeaderConstants.HEADER_SIZE
                );

        try (
                FileChannel channel = FileChannel.open(
                        path,
                        StandardOpenOption.READ
                )
        ) {

            long position =
                    TableHeaderFile.HEADER_OFFSET;

            while (buffer.hasRemaining()) {

                int read =
                        channel.read(
                                buffer,
                                position
                        );

                if (read == -1) {
                    break;
                }

                if (read == 0) {
                    break;
                }

                position += read;
            }
        }

        if (buffer.position()
                != TableHeaderConstants.HEADER_SIZE) {

            throw new CorruptedTableHeaderException(
                    "Table header file is truncated. Expected="
                            + TableHeaderConstants.HEADER_SIZE
                            + ", actual="
                            + buffer.position()
            );
        }

        return TableHeaderSerializer.deserialize(
                buffer.array()
        );
    }
}