package com.yekdb.constraint;

import com.yekdb.constraint.exception.NotNullConstraintViolationException;
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
 * NOT NULL constraint integration testleri.
 *
 * Bu test sınıfı constraint mekanizmasının:
 *
 * Table
 *      ↓
 * InsertExecutor / UpdateExecutor
 *      ↓
 * ConstraintValidator
 *      ↓
 * RecordManager
 *
 * hattında doğru şekilde çalıştığını doğrular.
 */
class NotNullConstraintIntegrationTest {

    @TempDir
    Path tempDirectory;

    private StorageEngine storageEngine;

    private RecordManager recordManager;

    private InsertExecutor insertExecutor;

    private UpdateExecutor updateExecutor;

    /**
     * Her test için bağımsız fiziksel storage ortamı oluşturulur.
     */
    @BeforeEach
    void setUp() throws Exception {

        Path dataFile =
                tempDirectory.resolve(
                        "not-null-integration.yekdb"
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

    /**
     * Test sonrasında fiziksel storage güvenli şekilde kapatılır.
     */
    @AfterEach
    void tearDown() throws Exception {

        if (storageEngine != null
                && storageEngine.isInitialized()) {

            storageEngine.shutdown();
        }
    }

    /**
     * Constraint tanımlanmamış bir kolon NULL değer kabul etmelidir.
     *
     * users:
     *
     * id    INT
     * email STRING
     *
     * email üzerinde NOT NULL yoktur.
     */
    @Test
    void shouldAllowNullInsertForNullableColumn()
            throws Exception {

        Table table =
                new Table(
                        "users",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INT
                                ),
                                new Column(
                                        "email",
                                        DataType.STRING
                                )
                        )
                );

        List<Object> values =
                new ArrayList<>();

        values.add(1);
        values.add(null);

        InsertCommand command =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "email"
                        ),
                        values
                );

        Record record =
                assertDoesNotThrow(
                        () -> insertExecutor.execute(
                                table,
                                command,
                                recordManager
                        )
                );

        assertNotNull(record);

        Row storedRow =
                recordManager.getRow(
                        record.getRecordId()
                );

        assertEquals(
                1,
                storedRow.getValue(0)
        );

        assertNull(
                storedRow.getValue(1)
        );
    }

    /**
     * NOT NULL constraint bulunan bir kolona
     * INSERT sırasında NULL yazılamamalıdır.
     *
     * users:
     *
     * id    INT
     * email STRING NOT NULL
     */
    @Test
    void shouldRejectNullInsertForNotNullColumn()
            throws Exception {

        Table table =
                new Table(
                        "users",
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INT
                                ),
                                new Column(
                                        "email",
                                        DataType.STRING
                                )
                        ),
                        List.of(
                                new NotNullConstraint(
                                        "email"
                                )
                        )
                );

        List<Object> values =
                new ArrayList<>();

        values.add(1);
        values.add(null);

        InsertCommand command =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "email"
                        ),
                        values
                );

        NotNullConstraintViolationException exception =
                assertThrows(
                        NotNullConstraintViolationException.class,
                        () -> insertExecutor.execute(
                                table,
                                command,
                                recordManager
                        )
                );

        assertEquals(
                "email",
                exception.getColumnName()
        );

        /*
         * Constraint violation fiziksel INSERT işleminden
         * önce gerçekleşmelidir.
         */
        assertTrue(
                recordManager
                        .getActiveRecords()
                        .isEmpty()
        );
    }

    /**
     * Geçerli bir değer NOT NULL constraint bulunan
     * kolona yazılabilmelidir.
     */
    @Test
    void shouldAllowNonNullInsertForNotNullColumn()
            throws Exception {

        Table table =
                createUsersTableWithNotNullEmail();

        InsertCommand command =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "email"
                        ),
                        List.of(
                                1,
                                "emre@example.com"
                        )
                );

        Record record =
                assertDoesNotThrow(
                        () -> insertExecutor.execute(
                                table,
                                command,
                                recordManager
                        )
                );

        assertNotNull(record);

        Row storedRow =
                recordManager.getRow(
                        record.getRecordId()
                );

        assertEquals(
                "emre@example.com",
                storedRow.getValue(1)
        );
    }

    /**
     * NOT NULL constraint bulunan kolon UPDATE ile
     * NULL yapılmaya çalışıldığında işlem reddedilmelidir.
     */
    @Test
    void shouldRejectNullUpdateForNotNullColumn()
            throws Exception {

        Table table =
                createUsersTableWithNotNullEmail();

        /*
         * Önce geçerli bir kayıt ekliyoruz.
         */
        InsertCommand insertCommand =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "email"
                        ),
                        List.of(
                                1,
                                "emre@example.com"
                        )
                );

        Record insertedRecord =
                insertExecutor.execute(
                        table,
                        insertCommand,
                        recordManager
                );

        /*
         * Map.of(...) NULL kabul etmediğinden
         * HashMap kullanıyoruz.
         */
        Map<String, Object> updatedValues =
                new HashMap<>();

        updatedValues.put(
                "email",
                null
        );

        /*
         * WHERE null:
         *
         * mevcut UpdateExecutor davranışına göre
         * tüm aktif kayıtlar eşleşir.
         */
        UpdateCommand updateCommand =
                new UpdateCommand(
                        "users",
                        updatedValues,
                        null
                );

        NotNullConstraintViolationException exception =
                assertThrows(
                        NotNullConstraintViolationException.class,
                        () -> updateExecutor.execute(
                                table,
                                updateCommand,
                                recordManager
                        )
                );

        assertEquals(
                "email",
                exception.getColumnName()
        );

        /*
         * En kritik kontrol:
         *
         * UPDATE başarısız olduktan sonra fiziksel kayıt
         * eski değerini korumalıdır.
         */
        Row storedRow =
                recordManager.getRow(
                        insertedRecord.getRecordId()
                );

        assertEquals(
                "emre@example.com",
                storedRow.getValue(1)
        );
    }

    /**
     * NOT NULL constraint bulunan kolon geçerli bir
     * değerle UPDATE edilebilmelidir.
     */
    @Test
    void shouldAllowNonNullUpdateForNotNullColumn()
            throws Exception {

        Table table =
                createUsersTableWithNotNullEmail();

        InsertCommand insertCommand =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "email"
                        ),
                        List.of(
                                1,
                                "old@example.com"
                        )
                );

        Record insertedRecord =
                insertExecutor.execute(
                        table,
                        insertCommand,
                        recordManager
                );

        Map<String, Object> updatedValues =
                new HashMap<>();

        updatedValues.put(
                "email",
                "new@example.com"
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
                        insertedRecord.getRecordId()
                );

        assertEquals(
                "new@example.com",
                storedRow.getValue(1)
        );
    }

    /**
     * Testlerde ortak kullanılan:
     *
     * users(
     *     id INT,
     *     email STRING NOT NULL
     * )
     *
     * tablosunu oluşturur.
     */
    private Table createUsersTableWithNotNullEmail() {

        return new Table(
                "users",
                List.of(
                        new Column(
                                "id",
                                DataType.INT
                        ),
                        new Column(
                                "email",
                                DataType.STRING
                        )
                ),
                List.of(
                        new NotNullConstraint(
                                "email"
                        )
                )
        );
    }
}