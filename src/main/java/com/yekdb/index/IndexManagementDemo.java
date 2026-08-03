package com.yekdb.index;

import com.yekdb.index.exception.DuplicateIndexException;
import com.yekdb.index.exception.DuplicateIndexKeyException;
import com.yekdb.index.exception.IndexNotFoundException;

import java.util.List;

/**
 * Sprint 00-09 Index Management altyapısını
 * konsol üzerinden test eden demo sınıfıdır.
 */
public class IndexManagementDemo {

    public static void main(String[] args) {

        printHeader();

        IndexManager indexManager = new IndexManager();

        createIndexes(indexManager);
        insertPrimaryIndexEntries(indexManager);
        insertNonUniqueIndexEntries(indexManager);
        searchIndexes(indexManager);
        updateIndexEntry(indexManager);
        deleteIndexEntries(indexManager);
        listIndexes(indexManager);
        testExpectedExceptions(indexManager);
        dropIndexes(indexManager);

        printFooter();
    }

    private static void createIndexes(IndexManager indexManager) {

        printSection("1. INDEX OLUŞTURMA");

        Index<Integer> studentIdIndex = indexManager.createIndex(
                "idx_students_id",
                "school_db",
                "students",
                "student_id",
                IndexType.PRIMARY
        );

        Index<String> cityIndex = indexManager.createIndex(
                "idx_students_city",
                "school_db",
                "students",
                "city",
                IndexType.NON_UNIQUE
        );

        Index<String> emailIndex = indexManager.createIndex(
                "idx_students_email",
                "school_db",
                "students",
                "email",
                IndexType.UNIQUE
        );

        System.out.println("Oluşturulan index: "
                + studentIdIndex.getMetadata().getIndexName());

        System.out.println("Oluşturulan index: "
                + cityIndex.getMetadata().getIndexName());

        System.out.println("Oluşturulan index: "
                + emailIndex.getMetadata().getIndexName());

        System.out.println("Toplam index sayısı: "
                + indexManager.size());
    }

    private static void insertPrimaryIndexEntries(
            IndexManager indexManager
    ) {

        printSection("2. PRIMARY INDEX KAYIT EKLEME");

        indexManager.insertEntry(
                "idx_students_id",
                1001,
                new RecordPointer(1, 0)
        );

        indexManager.insertEntry(
                "idx_students_id",
                1002,
                new RecordPointer(1, 1)
        );

        indexManager.insertEntry(
                "idx_students_id",
                1003,
                new RecordPointer(1, 2)
        );

        Index<Integer> index =
                indexManager.getTypedIndex("idx_students_id");

        System.out.println("PRIMARY index key sayısı: "
                + index.size());

        System.out.println("PRIMARY index pointer sayısı: "
                + index.pointerCount());

        System.out.println("PRIMARY index girdileri:");

        for (IndexEntry<Integer> entry : index.getEntryList()) {
            System.out.println("  " + entry);
        }
    }

    private static void insertNonUniqueIndexEntries(
            IndexManager indexManager
    ) {

        printSection("3. NON_UNIQUE INDEX KAYIT EKLEME");

        indexManager.insertEntry(
                "idx_students_city",
                "Malatya",
                new RecordPointer(1, 0)
        );

        indexManager.insertEntry(
                "idx_students_city",
                "Elazığ",
                new RecordPointer(1, 1)
        );

        indexManager.insertEntry(
                "idx_students_city",
                "Malatya",
                new RecordPointer(1, 2)
        );

        indexManager.insertEntry(
                "idx_students_city",
                "Malatya",
                new RecordPointer(2, 0)
        );

        Index<String> cityIndex =
                indexManager.getTypedIndex("idx_students_city");

        System.out.println("NON_UNIQUE farklı key sayısı: "
                + cityIndex.size());

        System.out.println("NON_UNIQUE toplam pointer sayısı: "
                + cityIndex.pointerCount());

        System.out.println("Malatya kayıt adresleri:");

        List<RecordPointer> malatyaRecords =
                indexManager.search(
                        "idx_students_city",
                        "Malatya"
                );

        for (RecordPointer pointer : malatyaRecords) {
            System.out.println("  " + pointer);
        }
    }

    private static void searchIndexes(IndexManager indexManager) {

        printSection("4. INDEX ARAMA");

        List<RecordPointer> studentResult =
                indexManager.search(
                        "idx_students_id",
                        1002
                );

        System.out.println("student_id = 1002 sonucu:");

        if (studentResult.isEmpty()) {
            System.out.println("  Kayıt bulunamadı.");
        } else {
            studentResult.forEach(
                    pointer -> System.out.println("  " + pointer)
            );
        }

        List<RecordPointer> missingResult =
                indexManager.search(
                        "idx_students_id",
                        9999
                );

        System.out.println("student_id = 9999 sonucu: "
                + missingResult);
    }

    private static void updateIndexEntry(
            IndexManager indexManager
    ) {

        printSection("5. POINTER GÜNCELLEME");

        RecordPointer oldPointer = new RecordPointer(1, 1);
        RecordPointer newPointer = new RecordPointer(4, 7);

        boolean updated = indexManager.updateEntry(
                "idx_students_id",
                1002,
                oldPointer,
                newPointer
        );

        System.out.println("Pointer güncellendi mi? " + updated);

        System.out.println("1002 anahtarının yeni adresi: "
                + indexManager.search(
                "idx_students_id",
                1002
        ));
    }

    private static void deleteIndexEntries(
            IndexManager indexManager
    ) {

        printSection("6. INDEX KAYDI SİLME");

        boolean pointerDeleted =
                indexManager.deleteEntry(
                        "idx_students_city",
                        "Malatya",
                        new RecordPointer(1, 2)
                );

        System.out.println(
                "Malatya anahtarından tek pointer silindi mi? "
                        + pointerDeleted
        );

        System.out.println("Malatya kalan pointer'ları: "
                + indexManager.search(
                "idx_students_city",
                "Malatya"
        ));

        boolean keyDeleted =
                indexManager.deleteEntry(
                        "idx_students_id",
                        1003
                );

        System.out.println(
                "1003 anahtarı tamamen silindi mi? "
                        + keyDeleted
        );

        System.out.println("1003 arama sonucu: "
                + indexManager.search(
                "idx_students_id",
                1003
        ));
    }

    private static void listIndexes(IndexManager indexManager) {

        printSection("7. INDEX LİSTELEME");

        List<Index<?>> tableIndexes =
                indexManager.getIndexesForTable(
                        "school_db",
                        "students"
                );

        System.out.println(
                "students tablosuna ait index sayısı: "
                        + tableIndexes.size()
        );

        for (Index<?> index : tableIndexes) {
            IndexMetadata metadata = index.getMetadata();

            System.out.println(
                    "  Index adı: "
                            + metadata.getIndexName()
            );

            System.out.println(
                    "  Kolon: "
                            + metadata.getColumnName()
            );

            System.out.println(
                    "  Tür: "
                            + metadata.getIndexType()
            );

            System.out.println(
                    "  Root page: "
                            + metadata.getRootPageId()
            );

            System.out.println();
        }
    }

    private static void testExpectedExceptions(
            IndexManager indexManager
    ) {

        printSection("8. BEKLENEN EXCEPTION TESTLERİ");

        try {
            indexManager.createIndex(
                    "idx_students_id",
                    "school_db",
                    "students",
                    "student_id",
                    IndexType.PRIMARY
            );
        } catch (DuplicateIndexException exception) {
            System.out.println(
                    "Beklenen DuplicateIndexException yakalandı:"
            );
            System.out.println("  " + exception.getMessage());
        }

        try {
            indexManager.insertEntry(
                    "idx_students_id",
                    1001,
                    new RecordPointer(8, 9)
            );
        } catch (DuplicateIndexKeyException exception) {
            System.out.println(
                    "Beklenen DuplicateIndexKeyException yakalandı:"
            );
            System.out.println("  " + exception.getMessage());
        }

        try {
            indexManager.getIndex("idx_not_found");
        } catch (IndexNotFoundException exception) {
            System.out.println(
                    "Beklenen IndexNotFoundException yakalandı:"
            );
            System.out.println("  " + exception.getMessage());
        }
    }

    private static void dropIndexes(IndexManager indexManager) {

        printSection("9. INDEX SİLME");

        Index<?> droppedIndex =
                indexManager.dropIndex(
                        "idx_students_email"
                );

        System.out.println("Silinen index: "
                + droppedIndex.getMetadata().getIndexName());

        System.out.println("Kalan index sayısı: "
                + indexManager.size());

        int deletedTableIndexes =
                indexManager.dropIndexesForTable(
                        "school_db",
                        "students"
                );

        System.out.println(
                "Tabloya ait toplu silinen index sayısı: "
                        + deletedTableIndexes
        );

        System.out.println(
                "Manager boş mu? "
                        + indexManager.isEmpty()
        );
    }

    private static void printHeader() {

        System.out.println();
        System.out.println(
                "=============================================="
        );
        System.out.println(
                "       YEKDB SPRINT 00-09 INDEX MANAGEMENT"
        );
        System.out.println(
                "=============================================="
        );
    }

    private static void printSection(String title) {

        System.out.println();
        System.out.println("----------------------------------------------");
        System.out.println(title);
        System.out.println("----------------------------------------------");
    }

    private static void printFooter() {

        System.out.println();
        System.out.println(
                "=============================================="
        );
        System.out.println(
                "       INDEX MANAGEMENT DEMO TAMAMLANDI"
        );
        System.out.println(
                "=============================================="
        );
    }
}