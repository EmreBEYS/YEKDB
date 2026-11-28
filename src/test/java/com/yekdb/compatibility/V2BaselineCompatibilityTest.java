package com.yekdb.compatibility;

import com.yekdb.cli.command.VersionCommand;
import com.yekdb.core.YekdbConstants;
import com.yekdb.database.DatabaseMetadata;
import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.table.header.TableHeaderConstants;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the V2 application baseline to the V1 on-disk contracts.
 *
 * <p>Sprint 01-00 changes the application version only. Database, table and
 * metadata formats must remain readable until a future sprint introduces an
 * explicit, tested migration.</p>
 */
class V2BaselineCompatibilityTest {

    @Test
    void shouldExposeThePostV1DevelopmentVersion() {
        assertEquals("1.0.16", VersionCommand.VERSION);
    }

    @Test
    void shouldKeepV1FormatVersionsStable() {
        assertEquals(1, DatabaseHeader.CURRENT_VERSION);
        assertEquals(1, TableHeaderConstants.FORMAT_VERSION);
        assertEquals("0.0.6", DatabaseMetadata.CURRENT_VERSION);
    }

    @Test
    void shouldReadAHeaderWrittenWithTheV1BinaryLayout() {
        long creationTime = 1_700_000_000_000L;
        long checkpointTime = creationTime + 5_000L;
        ByteBuffer v1Bytes = ByteBuffer
                .allocate(DatabaseHeader.HEADER_SIZE)
                .order(ByteOrder.BIG_ENDIAN);
        v1Bytes.putInt(DatabaseHeader.MAGIC_NUMBER);
        v1Bytes.putInt(1);
        v1Bytes.putInt(YekdbConstants.PAGE_SIZE);
        v1Bytes.putInt(7);
        v1Bytes.putLong(creationTime);
        v1Bytes.putLong(checkpointTime);

        DatabaseHeader restored = DatabaseHeader.fromBytes(v1Bytes.array());

        assertTrue(restored.hasValidMagicNumber());
        assertTrue(restored.isCurrentVersion());
        assertEquals(7, restored.getTotalPages());
        assertEquals(creationTime, restored.getCreationTime());
        assertEquals(checkpointTime, restored.getLastCheckpoint());
        assertArrayEquals(v1Bytes.array(), restored.toBytes());
    }
}
