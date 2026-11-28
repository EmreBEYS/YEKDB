package com.yekdb.storage.recovery;

import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.wal.WalCorruptionException;
import com.yekdb.storage.wal.WalManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrashRecoveryManagerIntegrationTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldUndoIncompleteTransactionAfterForcedJvmHalt()
            throws Exception {

        Path dataPath = tempDirectory.resolve("undo.ydb");
        runCrashProcess(dataPath, "incomplete");

        try (EngineHandle handle = open(dataPath)) {
            StorageEngine engine = handle.engine();
            CrashRecoveryResult result = engine.getLastRecoveryResult();

            assertEquals(10, engine.readPage(0).getPayload()[0]);
            assertEquals(1, result.incompleteTransactions().size());
            assertEquals(1, result.undonePageUpdates());
        }

        try (EngineHandle handle = open(dataPath)) {
            CrashRecoveryResult repeated = handle.engine()
                    .getLastRecoveryResult();
            assertEquals(0, repeated.redonePageUpdates());
            assertEquals(0, repeated.undonePageUpdates());
            assertEquals(10,
                    handle.engine().readPage(0).getPayload()[0]);
        }
    }

    @Test
    void shouldRedoCommittedTransactionAfterForcedJvmHalt()
            throws Exception {

        Path dataPath = tempDirectory.resolve("redo.ydb");
        runCrashProcess(dataPath, "commit");

        try (EngineHandle handle = open(dataPath)) {
            StorageEngine engine = handle.engine();
            CrashRecoveryResult result = engine.getLastRecoveryResult();

            assertEquals(40, engine.readPage(0).getPayload()[0]);
            assertEquals(1, result.committedTransactions().size());
            assertEquals(1, result.redonePageUpdates());
        }

        try (EngineHandle handle = open(dataPath)) {
            CrashRecoveryResult repeated = handle.engine()
                    .getLastRecoveryResult();
            assertEquals(0, repeated.redonePageUpdates());
            assertEquals(0, repeated.undonePageUpdates());
            assertTrue(repeated.idempotentPageSkips() >= 1);
        }
    }

    @Test
    void shouldReportCrcCorruptionWithoutChangingWal()
            throws Exception {

        Path dataPath = tempDirectory.resolve("corrupt.ydb");

        try (EngineHandle handle = open(dataPath)) {
            handle.engine().writePage(
                    CrashRecoveryTestPages.page((byte) 7)
            );
        }

        Path walPath = Path.of(dataPath + WalManager.FILE_SUFFIX);
        long originalSize = Files.size(walPath);

        try (RandomAccessFile wal = new RandomAccessFile(
                walPath.toFile(),
                "rw"
        )) {
            wal.seek(originalSize - 1);
            int lastByte = wal.read();
            wal.seek(originalSize - 1);
            wal.write(lastByte ^ 0x7F);
        }

        WalCorruptionException failure = assertThrows(
                WalCorruptionException.class,
                () -> new StorageEngine(dataPath).initialize()
        );
        assertTrue(failure.getMessage().contains("CRC32"));
        assertEquals(originalSize, Files.size(walPath));
    }

    @Test
    void shouldRepairOnlyTornWalTailAndReportIt()
            throws Exception {

        Path dataPath = tempDirectory.resolve("torn.ydb");

        try (EngineHandle handle = open(dataPath)) {
            handle.engine().writePage(
                    CrashRecoveryTestPages.page((byte) 8)
            );
        }

        Path walPath = Path.of(dataPath + WalManager.FILE_SUFFIX);
        long validSize = Files.size(walPath);
        Files.write(
                walPath,
                new byte[]{1, 2, 3, 4, 5},
                StandardOpenOption.APPEND
        );

        try (EngineHandle handle = open(dataPath)) {
            assertTrue(handle.engine()
                    .getLastRecoveryResult()
                    .tornTailRepaired());
        }

        assertEquals(validSize, Files.size(walPath));
    }

    @Test
    void shouldRejectCommitWithoutBegin()
            throws Exception {

        Path dataPath = tempDirectory.resolve("sequence.ydb");

        try (EngineHandle ignored = open(dataPath)) {
            // Create the data and WAL headers.
        }

        try (WalManager walManager = new WalManager(
                Path.of(dataPath + WalManager.FILE_SUFFIX)
        )) {
            walManager.open();
            walManager.appendTransactionCommit(77);
        }

        WalCorruptionException failure = assertThrows(
                WalCorruptionException.class,
                () -> new StorageEngine(dataPath).initialize()
        );
        assertTrue(failure.getMessage().contains("no active begin"));
    }

    private void runCrashProcess(
            Path dataPath,
            String mode
    ) throws Exception {

        String classPath = System.getProperty(
                "surefire.test.class.path",
                System.getProperty("java.class.path")
        );
        Process process = new ProcessBuilder(
                Path.of(
                        System.getProperty("java.home"),
                        "bin",
                        "java"
                ).toString(),
                "-cp",
                classPath,
                CrashRecoveryProcess.class.getName(),
                dataPath.toString(),
                mode
        ).redirectErrorStream(true).start();

        boolean finished = process.waitFor(
                Duration.ofSeconds(20).toMillis(),
                java.util.concurrent.TimeUnit.MILLISECONDS
        );
        assertTrue(finished, "Crash fixture did not terminate.");
        String processOutput = new String(
                process.getInputStream().readAllBytes()
        );
        assertEquals(23, process.exitValue(),
                processOutput);
    }

    private EngineHandle open(Path dataPath)
            throws Exception {
        StorageEngine engine = new StorageEngine(dataPath);
        engine.initialize();
        return new EngineHandle(engine);
    }

    private record EngineHandle(
            StorageEngine engine
    ) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            engine.shutdown();
        }
    }
}
