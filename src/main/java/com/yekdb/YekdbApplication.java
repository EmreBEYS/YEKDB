package com.yekdb;

import com.yekdb.core.YekdbEngine;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordSerializer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class YekdbApplication {

    public static void main(String[] args) {

        YekdbEngine engine = new YekdbEngine(
                Path.of("data", "yekdb.ydb")
        );

        try {
            engine.start();

            Record originalRecord = new Record(
                    1L,
                    "YEKDB Storage Engine"
                            .getBytes(StandardCharsets.UTF_8)
            );

            long position =
                    engine.insertRecord(originalRecord);

            int serializedLength =
                    RecordSerializer.calculateSerializedSize(
                            originalRecord
                    );

            Record loadedRecord =
                    engine.readRecord(
                            position,
                            serializedLength
                    );

            System.out.println(
                    "Engine çalışıyor mu: "
                            + engine.isRunning()
            );

            System.out.println(
                    "Kayıt konumu: " + position
            );

            System.out.println(
                    "Yazılan kayıt: " + originalRecord
            );

            System.out.println(
                    "Okunan kayıt: " + loadedRecord
            );

            System.out.println(
                    "Okunan veri: "
                            + new String(
                            loadedRecord.getData(),
                            StandardCharsets.UTF_8
                    )
            );

            System.out.println(
                    "Kayıtlar eşit mi: "
                            + originalRecord.equals(loadedRecord)
            );

            System.out.println(
                    "Veri dosyası boyutu: "
                            + engine.getDataFileSize()
                            + " byte"
            );

        } catch (Exception exception) {

            System.err.println(
                    "YEKDB çalışırken hata oluştu: "
                            + exception.getMessage()
            );

            exception.printStackTrace();

        } finally {

            try {
                engine.shutdown();
            } catch (Exception exception) {
                System.err.println(
                        "YEKDB kapatılırken hata oluştu: "
                                + exception.getMessage()
                );
            }
        }
    }
}