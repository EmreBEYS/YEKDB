package com.yekdb.storage.file;

import com.yekdb.core.YekdbConstants;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseHeaderTest {

    @Test
    void shouldCreateDefaultDatabaseHeader() {

        DatabaseHeader header = new DatabaseHeader();

        assertEquals(
                DatabaseHeader.MAGIC_NUMBER,
                header.getMagicNumber()
        );

        assertEquals(
                DatabaseHeader.CURRENT_VERSION,
                header.getVersion()
        );

        assertEquals(
                YekdbConstants.PAGE_SIZE,
                header.getPageSize()
        );

        assertEquals(0, header.getTotalPages());
        assertTrue(header.getCreationTime() > 0);
        assertEquals(0, header.getLastCheckpoint());
        assertTrue(header.hasValidMagicNumber());
        assertTrue(header.isCurrentVersion());
    }

    @Test
    void shouldSerializeHeaderToExactly128Bytes() {

        DatabaseHeader header = new DatabaseHeader();

        byte[] bytes = header.toBytes();

        assertEquals(
                DatabaseHeader.HEADER_SIZE,
                bytes.length
        );
    }

    @Test
    void shouldSerializeAndDeserializeHeader() {

        DatabaseHeader original = new DatabaseHeader();

        original.incrementTotalPages();
        original.incrementTotalPages();

        long checkpoint = System.currentTimeMillis();
        original.updateLastCheckpoint(checkpoint);

        byte[] bytes = original.toBytes();

        DatabaseHeader restored =
                DatabaseHeader.fromBytes(bytes);

        assertEquals(
                original.getMagicNumber(),
                restored.getMagicNumber()
        );

        assertEquals(
                original.getVersion(),
                restored.getVersion()
        );

        assertEquals(
                original.getPageSize(),
                restored.getPageSize()
        );

        assertEquals(
                original.getTotalPages(),
                restored.getTotalPages()
        );

        assertEquals(
                original.getCreationTime(),
                restored.getCreationTime()
        );

        assertEquals(
                original.getLastCheckpoint(),
                restored.getLastCheckpoint()
        );
    }

    @Test
    void shouldIncrementTotalPageCount() {

        DatabaseHeader header = new DatabaseHeader();

        header.incrementTotalPages();
        header.incrementTotalPages();

        assertEquals(2, header.getTotalPages());
    }

    @Test
    void shouldUpdateLastCheckpoint() {

        DatabaseHeader header = new DatabaseHeader();

        long checkpoint = header.getCreationTime() + 1000;

        header.updateLastCheckpoint(checkpoint);

        assertEquals(
                checkpoint,
                header.getLastCheckpoint()
        );
    }

    @Test
    void shouldRejectCheckpointBeforeCreationTime() {

        DatabaseHeader header = new DatabaseHeader();

        long invalidCheckpoint =
                header.getCreationTime() - 1;

        assertThrows(
                IllegalArgumentException.class,
                () -> header.updateLastCheckpoint(
                        invalidCheckpoint
                )
        );
    }

    @Test
    void shouldRejectInvalidHeaderLength() {

        byte[] invalidBytes =
                new byte[DatabaseHeader.HEADER_SIZE - 1];

        assertThrows(
                IllegalArgumentException.class,
                () -> DatabaseHeader.fromBytes(invalidBytes)
        );
    }

    @Test
    void shouldRejectInvalidMagicNumber() {

        DatabaseHeader header = new DatabaseHeader();

        byte[] bytes = header.toBytes();

        bytes[0] = 0;
        bytes[1] = 0;
        bytes[2] = 0;
        bytes[3] = 0;

        assertThrows(
                IllegalArgumentException.class,
                () -> DatabaseHeader.fromBytes(bytes)
        );
    }

    @Test
    void shouldKeepReservedAreaEmpty() {

        DatabaseHeader header = new DatabaseHeader();

        byte[] bytes = header.toBytes();

        byte[] reservedArea = Arrays.copyOfRange(
                bytes,
                32,
                DatabaseHeader.HEADER_SIZE
        );

        for (byte value : reservedArea) {
            assertEquals(0, value);
        }
    }

    @Test
    void shouldRejectNonPowerOfTwoPageSize() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new DatabaseHeader(
                        DatabaseHeader.MAGIC_NUMBER,
                        DatabaseHeader.CURRENT_VERSION,
                        3000,
                        0,
                        System.currentTimeMillis(),
                        0
                )
        );
    }

    @Test
    void shouldRejectNegativeTotalPageCount() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new DatabaseHeader(
                        DatabaseHeader.MAGIC_NUMBER,
                        DatabaseHeader.CURRENT_VERSION,
                        YekdbConstants.PAGE_SIZE,
                        -1,
                        System.currentTimeMillis(),
                        0
                )
        );
    }
}