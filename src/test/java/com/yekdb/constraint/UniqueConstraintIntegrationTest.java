package com.yekdb.constraint;

import com.yekdb.constraint.exception.UniqueConstraintViolationException;
import com.yekdb.query.command.InsertCommand;
import com.yekdb.query.command.UpdateCommand;
import com.yekdb.query.executor.InsertExecutor;
import com.yekdb.query.executor.UpdateExecutor;
import com.yekdb.storage.StorageEngine;
import com.yekdb.storage.record.Record;
import com.yekdb.storage.record.RecordManager;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.record.page.PageType;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sprint 00-24
 *
 * UNIQUE constraint integration testleri.
 *
 * Test hattı:
 *
 * Table
 *      ↓
 * InsertExecutor / UpdateExecutor
 *      ↓
 * ConstraintValidator
 *      ↓
 * RecordManager
 *
 * Test edilen senaryolar:
 *
 * - Tek kolonlu UNIQUE INSERT
 * - Duplicate INSERT rejection
 * - UNIQUE + NULL davranışı
 * - UNIQUE UPDATE
 * - UPDATE sırasında self-record ignore
 * - Composite UNIQUE
 */
class UniqueConstraintIntegrationTest {

    @TempDir
    Path tempDirectory;

    private StorageEngine storageEngine;

    private RecordManager recordManager;

    private InsertExecutor insertExecutor;

    private UpdateExecutor updateExecutor;

    @BeforeEach
    void setUp() throws Exception {

        Path dataFile =
                tempDirectory.resolve(
                        "unique-constraint-integration.yekdb"
                );

        storageEngine =
                new StorageEngine(
                        dataFile
                );

        storageEngine.initialize();

        recordManager =
                new RecordManager(
                        storageEngine.getPageManager(),
                        PageType.DATA
                );

        insertExecutor =
                new InsertExecutor();

        updateExecutor =
                new UpdateExecutor();
    }

    @AfterEach
    void tearDown() throws Exception {

        if (storageEngine != null
                && storageEngine.isInitialized()) {

            storageEngine.shutdown();
        }
    }

    /**
     * UNIQUE kolon farklı değerler kabul etmelidir.
     */
    @Test
    void shouldAllowDifferentUniqueValues()
            throws Exception {

        Table table =
                createUsersTableWithUniqueUsername();

        InsertCommand firstCommand =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "username"
                        ),
                        List.of(
                                1,
                                "emre"
                        )
                );

        InsertCommand secondCommand =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "username"
                        ),
                        List.of(
                                2,
                                "yunus"
                        )
                );

        Record firstRecord =
                assertDoesNotThrow(
                        () -> insertExecutor.execute(
                                table,
                                firstCommand,
                                recordManager
                        )
                );

        Record secondRecord =
                assertDoesNotThrow(
                        () -> insertExecutor.execute(
                                table,
                                secondCommand,
                                recordManager
                        )
                );

        assertNotNull(firstRecord);
        assertNotNull(secondRecord);

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    /**
     * Aynı UNIQUE değer ikinci kez INSERT edilememelidir.
     */
    @Test
    void shouldRejectDuplicateUniqueInsert()
            throws Exception {

        Table table =
                createUsersTableWithUniqueUsername();

        InsertCommand firstCommand =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "username"
                        ),
                        List.of(
                                1,
                                "emre"
                        )
                );

        insertExecutor.execute(
                table,
                firstCommand,
                recordManager
        );

        InsertCommand duplicateCommand =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "username"
                        ),
                        List.of(
                                2,
                                "emre"
                        )
                );

        UniqueConstraintViolationException exception =
                assertThrows(
                        UniqueConstraintViolationException.class,
                        () -> insertExecutor.execute(
                                table,
                                duplicateCommand,
                                recordManager
                        )
                );

        assertEquals(
                List.of("username"),
                exception.getColumns()
        );

        /*
         * Duplicate kayıt fiziksel olarak yazılmamalıdır.
         */
        assertEquals(
                1,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    /**
     * SQL UNIQUE semantiğine uygun olarak bir UNIQUE kolon
     * birden fazla NULL değer kabul edebilmelidir.
     */
    @Test
    void shouldAllowMultipleNullValuesForUniqueColumn()
            throws Exception {

        Table table =
                createUsersTableWithUniqueUsername();

        List<Object> firstValues =
                new ArrayList<>();

        firstValues.add(1);
        firstValues.add(null);

        InsertCommand firstCommand =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "username"
                        ),
                        firstValues
                );

        List<Object> secondValues =
                new ArrayList<>();

        secondValues.add(2);
        secondValues.add(null);

        InsertCommand secondCommand =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "username"
                        ),
                        secondValues
                );

        assertDoesNotThrow(
                () -> insertExecutor.execute(
                        table,
                        firstCommand,
                        recordManager
                )
        );

        assertDoesNotThrow(
                () -> insertExecutor.execute(
                        table,
                        secondCommand,
                        recordManager
                )
        );

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    /**
     * UPDATE sonucunda başka bir record'un UNIQUE değeri
     * kullanılmaya çalışılırsa işlem reddedilmelidir.
     */
    @Test
    void shouldRejectDuplicateUniqueUpdate()
            throws Exception {

        Table table =
                createUsersTableWithUniqueUsername();

        Record firstRecord =
                insertUser(
                        table,
                        1,
                        "emre"
                );

        Record secondRecord =
                insertUser(
                        table,
                        2,
                        "yunus"
                );

        Map<String, Object> updatedValues =
                new HashMap<>();

        updatedValues.put(
                "username",
                "emre"
        );

        /*
         * WHERE engine yerine bu testte tüm satırların
         * güncellenmesini istemiyoruz.
         *
         * Eğer mevcut UpdateCommand yapınız record bazlı WHERE
         * expression kabul ediyorsa burada id = 2 expression
         * kullanılmalıdır.
         *
         * Mevcut proje davranışında null WHERE tüm satırları
         * eşleştiriyorsa bu testin WHERE bölümünü mevcut
         * expression API'nize göre id = 2 şeklinde bağlayın.
         */
        UpdateCommand updateCommand =
                new UpdateCommand(
                        "users",
                        updatedValues,
                        null
                );

        /*
         * null WHERE bütün satırlara uygulanıyorsa ilk record
         * kendi "emre" değerine güncellendiğinde self-ignore
         * sayesinde geçer; ikinci record "emre" yapılmaya
         * çalışıldığında UNIQUE violation oluşmalıdır.
         */
        assertThrows(
                UniqueConstraintViolationException.class,
                () -> updateExecutor.execute(
                        table,
                        updateCommand,
                        recordManager
                )
        );

        Row firstStoredRow =
                recordManager.getRow(
                        firstRecord.getRecordId()
                );

        Row secondStoredRow =
                recordManager.getRow(
                        secondRecord.getRecordId()
                );

        assertEquals(
                "emre",
                firstStoredRow.getValue(1)
        );

        assertEquals(
                "yunus",
                secondStoredRow.getValue(1)
        );
    }

    /**
     * UPDATE sırasında record mevcut UNIQUE değerini
     * değiştirmeden bırakabilmelidir.
     *
     * Validator kendi record'unu duplicate kabul etmemelidir.
     */
    @Test
    void shouldIgnoreCurrentRecordDuringUniqueUpdate()
            throws Exception {

        Table table =
                createUsersTableWithUniqueUsername();

        Record record =
                insertUser(
                        table,
                        1,
                        "emre"
                );

        Map<String, Object> updatedValues =
                new HashMap<>();

        updatedValues.put(
                "username",
                "emre"
        );

        UpdateCommand updateCommand =
                new UpdateCommand(
                        "users",
                        updatedValues,
                        null
                );

        int updatedCount =
                assertDoesNotThrow(
                        () -> updateExecutor.execute(
                                table,
                                updateCommand,
                                recordManager
                        )
                );

        assertEquals(
                1,
                updatedCount
        );

        Row storedRow =
                recordManager.getRow(
                        record.getRecordId()
                );

        assertEquals(
                "emre",
                storedRow.getValue(1)
        );
    }

    /**
     * Composite UNIQUE farklı kombinasyonlara izin vermelidir.
     *
     * Örn:
     *
     * (Emre, Kul)
     * (Emre, Bey)
     *
     * geçerlidir.
     */
    @Test
    void shouldAllowDifferentCompositeUniqueValues()
            throws Exception {

        Table table =
                createPersonTableWithCompositeUnique();

        InsertCommand firstCommand =
                new InsertCommand(
                        "persons",
                        List.of(
                                "id",
                                "first_name",
                                "last_name"
                        ),
                        List.of(
                                1,
                                "Emre",
                                "Kul"
                        )
                );

        InsertCommand secondCommand =
                new InsertCommand(
                        "persons",
                        List.of(
                                "id",
                                "first_name",
                                "last_name"
                        ),
                        List.of(
                                2,
                                "Emre",
                                "Bey"
                        )
                );

        assertDoesNotThrow(
                () -> insertExecutor.execute(
                        table,
                        firstCommand,
                        recordManager
                )
        );

        assertDoesNotThrow(
                () -> insertExecutor.execute(
                        table,
                        secondCommand,
                        recordManager
                )
        );

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    /**
     * Aynı composite UNIQUE kombinasyonu ikinci kez
     * INSERT edilememelidir.
     *
     * (Emre, Kul)
     * (Emre, Kul)
     *
     * ikinci kayıt reddedilir.
     */
    @Test
    void shouldRejectDuplicateCompositeUniqueValues()
            throws Exception {

        Table table =
                createPersonTableWithCompositeUnique();

        InsertCommand firstCommand =
                new InsertCommand(
                        "persons",
                        List.of(
                                "id",
                                "first_name",
                                "last_name"
                        ),
                        List.of(
                                1,
                                "Emre",
                                "Kul"
                        )
                );

        insertExecutor.execute(
                table,
                firstCommand,
                recordManager
        );

        InsertCommand duplicateCommand =
                new InsertCommand(
                        "persons",
                        List.of(
                                "id",
                                "first_name",
                                "last_name"
                        ),
                        List.of(
                                2,
                                "Emre",
                                "Kul"
                        )
                );

        UniqueConstraintViolationException exception =
                assertThrows(
                        UniqueConstraintViolationException.class,
                        () -> insertExecutor.execute(
                                table,
                                duplicateCommand,
                                recordManager
                        )
                );

        assertEquals(
                List.of(
                        "first_name",
                        "last_name"
                ),
                exception.getColumns()
        );

        assertEquals(
                1,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    /**
     * Composite UNIQUE constraint içerisindeki değerlerden
     * biri NULL ise duplicate comparison atlanmalıdır.
     */
    @Test
    void shouldAllowNullInsideCompositeUniqueConstraint()
            throws Exception {

        Table table =
                createPersonTableWithCompositeUnique();

        List<Object> firstValues =
                new ArrayList<>();

        firstValues.add(1);
        firstValues.add("Emre");
        firstValues.add(null);

        List<Object> secondValues =
                new ArrayList<>();

        secondValues.add(2);
        secondValues.add("Emre");
        secondValues.add(null);

        InsertCommand firstCommand =
                new InsertCommand(
                        "persons",
                        List.of(
                                "id",
                                "first_name",
                                "last_name"
                        ),
                        firstValues
                );

        InsertCommand secondCommand =
                new InsertCommand(
                        "persons",
                        List.of(
                                "id",
                                "first_name",
                                "last_name"
                        ),
                        secondValues
                );

        assertDoesNotThrow(
                () -> insertExecutor.execute(
                        table,
                        firstCommand,
                        recordManager
                )
        );

        assertDoesNotThrow(
                () -> insertExecutor.execute(
                        table,
                        secondCommand,
                        recordManager
                )
        );

        assertEquals(
                2,
                recordManager
                        .getActiveRecords()
                        .size()
        );
    }

    /**
     * users(
     *     id INT,
     *     username STRING UNIQUE
     * )
     */
    private Table createUsersTableWithUniqueUsername() {

        return new Table(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        ),
                        new Column(
                                "username",
                                DataType.STRING
                        )
                ),
                List.of(
                        new UniqueConstraint(
                                "username"
                        )
                )
        );
    }

    /**
     * persons(
     *     id INT,
     *     first_name STRING,
     *     last_name STRING,
     *
     *     UNIQUE(first_name, last_name)
     * )
     */
    private Table createPersonTableWithCompositeUnique() {

        return new Table(
                "persons",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        ),
                        new Column(
                                "first_name",
                                DataType.STRING
                        ),
                        new Column(
                                "last_name",
                                DataType.STRING
                        )
                ),
                List.of(
                        new UniqueConstraint(
                                List.of(
                                        "first_name",
                                        "last_name"
                                )
                        )
                )
        );
    }

    private Record insertUser(
            Table table,
            int id,
            String username
    ) throws Exception {

        InsertCommand command =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "username"
                        ),
                        List.of(
                                id,
                                username
                        )
                );

        return insertExecutor.execute(
                table,
                command,
                recordManager
        );
    }
}