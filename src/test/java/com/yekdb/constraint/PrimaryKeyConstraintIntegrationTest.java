package com.yekdb.constraint;

import com.yekdb.constraint.exception.PrimaryKeyConstraintViolationException;
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

class PrimaryKeyConstraintIntegrationTest {

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
                        "primary-key-constraint-integration.yekdb"
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
     * Tek kolonlu PRIMARY KEY normal bir değeri kabul etmelidir.
     */
    @Test
    void shouldAllowValidSingleColumnPrimaryKeyInsert()
            throws Exception {

        Table table =
                createUsersTableWithPrimaryKey();

        InsertCommand command =
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

        assertEquals(
                "emre",
                storedRow.getValue(1)
        );
    }

    /**
     * Aynı PRIMARY KEY değeri ikinci kez INSERT edilememelidir.
     */
    @Test
    void shouldRejectDuplicateSingleColumnPrimaryKey()
            throws Exception {

        Table table =
                createUsersTableWithPrimaryKey();

        insertUser(
                table,
                1,
                "emre"
        );

        InsertCommand duplicateCommand =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "username"
                        ),
                        List.of(
                                1,
                                "yunus"
                        )
                );

        PrimaryKeyConstraintViolationException exception =
                assertThrows(
                        PrimaryKeyConstraintViolationException.class,
                        () -> insertExecutor.execute(
                                table,
                                duplicateCommand,
                                recordManager
                        )
                );

        assertEquals(
                List.of("id"),
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
     * PRIMARY KEY NULL değer kabul etmemelidir.
     */
    @Test
    void shouldRejectNullSingleColumnPrimaryKey()
            throws Exception {

        Table table =
                createUsersTableWithPrimaryKey();

        List<Object> values =
                new ArrayList<>();

        values.add(null);
        values.add("emre");

        InsertCommand command =
                new InsertCommand(
                        "users",
                        List.of(
                                "id",
                                "username"
                        ),
                        values
                );

        PrimaryKeyConstraintViolationException exception =
                assertThrows(
                        PrimaryKeyConstraintViolationException.class,
                        () -> insertExecutor.execute(
                                table,
                                command,
                                recordManager
                        )
                );

        assertEquals(
                List.of("id"),
                exception.getColumns()
        );

        assertTrue(
                recordManager
                        .getActiveRecords()
                        .isEmpty()
        );
    }

    /**
     * UPDATE sırasında record kendi PRIMARY KEY değerini
     * koruyabilmelidir.
     */
    @Test
    void shouldIgnoreCurrentRecordDuringPrimaryKeyUpdate()
            throws Exception {

        Table table =
                createUsersTableWithPrimaryKey();

        Record record =
                insertUser(
                        table,
                        1,
                        "emre"
                );

        Map<String, Object> updatedValues =
                new HashMap<>();

        updatedValues.put(
                "id",
                1
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
                1,
                storedRow.getValue(0)
        );
    }

    /**
     * Bir kayıt başka bir kaydın PRIMARY KEY değerine
     * UPDATE edilememelidir.
     */
    @Test
    void shouldRejectDuplicatePrimaryKeyUpdate()
            throws Exception {

        Table table =
                createUsersTableWithPrimaryKey();

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
                "id",
                1
        );

        UpdateCommand updateCommand =
                new UpdateCommand(
                        "users",
                        updatedValues,
                        null
                );

        assertThrows(
                PrimaryKeyConstraintViolationException.class,
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
                1,
                firstStoredRow.getValue(0)
        );

        assertEquals(
                2,
                secondStoredRow.getValue(0)
        );
    }

    /**
     * Composite PRIMARY KEY farklı kombinasyonları
     * kabul etmelidir.
     */
    @Test
    void shouldAllowDifferentCompositePrimaryKeyValues()
            throws Exception {

        Table table =
                createEnrollmentTableWithCompositePrimaryKey();

        InsertCommand firstCommand =
                new InsertCommand(
                        "enrollments",
                        List.of(
                                "student_id",
                                "course_id",
                                "grade"
                        ),
                        List.of(
                                1,
                                100,
                                85
                        )
                );

        InsertCommand secondCommand =
                new InsertCommand(
                        "enrollments",
                        List.of(
                                "student_id",
                                "course_id",
                                "grade"
                        ),
                        List.of(
                                1,
                                101,
                                90
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
     * Aynı composite PRIMARY KEY kombinasyonu
     * ikinci kez INSERT edilememelidir.
     */
    @Test
    void shouldRejectDuplicateCompositePrimaryKey()
            throws Exception {

        Table table =
                createEnrollmentTableWithCompositePrimaryKey();

        InsertCommand firstCommand =
                new InsertCommand(
                        "enrollments",
                        List.of(
                                "student_id",
                                "course_id",
                                "grade"
                        ),
                        List.of(
                                1,
                                100,
                                85
                        )
                );

        insertExecutor.execute(
                table,
                firstCommand,
                recordManager
        );

        InsertCommand duplicateCommand =
                new InsertCommand(
                        "enrollments",
                        List.of(
                                "student_id",
                                "course_id",
                                "grade"
                        ),
                        List.of(
                                1,
                                100,
                                95
                        )
                );

        PrimaryKeyConstraintViolationException exception =
                assertThrows(
                        PrimaryKeyConstraintViolationException.class,
                        () -> insertExecutor.execute(
                                table,
                                duplicateCommand,
                                recordManager
                        )
                );

        assertEquals(
                List.of(
                        "student_id",
                        "course_id"
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
     * Composite PRIMARY KEY içerisindeki herhangi bir
     * bileşen NULL ise kayıt reddedilmelidir.
     */
    @Test
    void shouldRejectNullInsideCompositePrimaryKey()
            throws Exception {

        Table table =
                createEnrollmentTableWithCompositePrimaryKey();

        List<Object> values =
                new ArrayList<>();

        values.add(1);
        values.add(null);
        values.add(85);

        InsertCommand command =
                new InsertCommand(
                        "enrollments",
                        List.of(
                                "student_id",
                                "course_id",
                                "grade"
                        ),
                        values
                );

        PrimaryKeyConstraintViolationException exception =
                assertThrows(
                        PrimaryKeyConstraintViolationException.class,
                        () -> insertExecutor.execute(
                                table,
                                command,
                                recordManager
                        )
                );

        assertEquals(
                List.of(
                        "student_id",
                        "course_id"
                ),
                exception.getColumns()
        );

        assertTrue(
                recordManager
                        .getActiveRecords()
                        .isEmpty()
        );
    }

    /**
     * users(
     *     id INT PRIMARY KEY,
     *     username STRING
     * )
     */
    private Table createUsersTableWithPrimaryKey() {

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
                        new PrimaryKeyConstraint(
                                "id"
                        )
                )
        );
    }

    /**
     * enrollments(
     *     student_id INT,
     *     course_id INT,
     *     grade INT,
     *
     *     PRIMARY KEY(student_id, course_id)
     * )
     */
    private Table createEnrollmentTableWithCompositePrimaryKey() {

        return new Table(
                "enrollments",
                List.of(
                        new Column(
                                "student_id",
                                DataType.INT
                        ),
                        new Column(
                                "course_id",
                                DataType.INT
                        ),
                        new Column(
                                "grade",
                                DataType.INT
                        )
                ),
                List.of(
                        new PrimaryKeyConstraint(
                                List.of(
                                        "student_id",
                                        "course_id"
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