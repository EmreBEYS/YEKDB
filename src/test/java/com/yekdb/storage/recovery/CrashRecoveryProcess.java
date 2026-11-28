package com.yekdb.storage.recovery;

import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.record.page.Page;
import com.yekdb.storage.record.page.PageSerializer;
import com.yekdb.storage.record.page.PageType;

import java.io.RandomAccessFile;
import java.nio.file.Path;

/** Ayrı JVM crash fixture'i; JUnit testi değildir. */
public final class CrashRecoveryProcess {

    private CrashRecoveryProcess() {
    }

    public static void main(String[] arguments)
            throws Exception {

        Path dataPath = Path.of(arguments[0]);
        String mode = arguments[1];
        StorageEngine engine = new StorageEngine(dataPath);
        engine.initialize();

        Page baseline = page((byte) 10);
        engine.writePage(baseline);

        long transactionId = engine.beginWalTransaction();
        Page changed = page(
                mode.equals("commit")
                        ? (byte) 40
                        : (byte) 30
        );
        engine.writePage(transactionId, changed);

        if (mode.equals("commit")) {
            engine.commitWalTransaction(transactionId);

            byte[] baselineBytes = new PageSerializer()
                    .serialize(baseline);
            try (RandomAccessFile file = new RandomAccessFile(
                    dataPath.toFile(),
                    "rw"
            )) {
                file.seek(DatabaseHeader.HEADER_SIZE);
                file.write(baselineBytes);
                file.getFD().sync();
            }
        }

        Runtime.getRuntime().halt(23);
    }

    private static Page page(byte marker) {
        Page page = new Page(0, PageType.DATA);
        page.getPayload()[0] = marker;
        page.getHeader().setUsedBytes(1);
        page.getHeader().setRecordCount(1);
        return page;
    }
}
