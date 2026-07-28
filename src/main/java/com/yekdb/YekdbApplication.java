package com.yekdb;

import com.yekdb.core.YekdbEngine;
import com.yekdb.storage.file.DatabaseHeader;
import com.yekdb.storage.page.Page;
import com.yekdb.storage.page.PageType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class YekdbApplication {

    private YekdbApplication() {
    }

    public static void main(String[] args) {

        YekdbEngine engine = new YekdbEngine(
                Path.of("data", "yekdb.ydb")
        );

        try {
            engine.start();

            DatabaseHeader header =
                    engine.getDatabaseHeader();

            System.out.println(
                    "Database format version: "
                            + header.getVersion()
            );

            System.out.println(
                    "Page size: "
                            + header.getPageSize()
                            + " byte"
            );

            System.out.println(
                    "Başlangıç sayfa sayısı: "
                            + engine.getPageCount()
            );

            /*
             * Uygulama birden fazla kez çalıştırıldığında
             * Page 0 yeniden oluşturulmaz.
             */
            if (!engine.pageExists(0)) {

                Page page = new Page(
                        0,
                        PageType.DATA
                );

                byte[] message =
                        "YEKDB first physical page"
                                .getBytes(
                                        StandardCharsets.UTF_8
                                );

                System.arraycopy(
                        message,
                        0,
                        page.getPayload(),
                        0,
                        message.length
                );

                page.getHeader().setRecordCount(1);
                page.getHeader().setUsedBytes(
                        message.length
                );

                engine.writePage(page);

                System.out.println(
                        "Page 0 diske yazıldı."
                );
            }

            Page loadedPage =
                    engine.readPage(0);

            int usedBytes =
                    loadedPage
                            .getHeader()
                            .getUsedBytes();

            String loadedText = new String(
                    loadedPage.getPayload(),
                    0,
                    usedBytes,
                    StandardCharsets.UTF_8
            );

            System.out.println(
                    "Engine çalışıyor mu: "
                            + engine.isRunning()
            );

            System.out.println(
                    "Okunan Page ID: "
                            + loadedPage
                            .getHeader()
                            .getPageId()
            );

            System.out.println(
                    "Page türü: "
                            + loadedPage
                            .getHeader()
                            .getPageType()
            );

            System.out.println(
                    "Kayıt sayısı: "
                            + loadedPage
                            .getHeader()
                            .getRecordCount()
            );

            System.out.println(
                    "Kullanılan alan: "
                            + usedBytes
                            + " byte"
            );

            System.out.println(
                    "Okunan veri: "
                            + loadedText
            );

            System.out.println(
                    "Toplam sayfa sayısı: "
                            + engine.getPageCount()
            );

            System.out.println(
                    "Header toplam sayfa sayısı: "
                            + engine
                            .getDatabaseHeader()
                            .getTotalPages()
            );

            System.out.println(
                    "Veri dosyası boyutu: "
                            + engine.getDataFileSize()
                            + " byte"
            );

            engine.checkpoint();

            System.out.println(
                    "Checkpoint tamamlandı."
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