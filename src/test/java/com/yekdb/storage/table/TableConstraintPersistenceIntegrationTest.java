package com.yekdb.storage.table;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.NotNullConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.storage.table.header.TableHeaderConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableConstraintPersistenceIntegrationTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldPersistAndRecoverAllSingleColumnConstraints() {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("username", DataType.STRING),
                                new Column("email", DataType.STRING)
                        ),
                        List.of(
                                new PrimaryKeyConstraint("id"),
                                new UniqueConstraint("username"),
                                new NotNullConstraint("email")
                        )
                )
        );

        TableManager recoveredManager =
                new TableManager(tempDirectory);

        recoveredManager.loadCatalog();

        Table recovered =
                recoveredManager.getTable("users");

        assertEquals(3, recovered.getConstraints().size());
        assertConstraint(
                recovered,
                ConstraintType.PRIMARY_KEY,
                List.of("id")
        );
        assertConstraint(
                recovered,
                ConstraintType.UNIQUE,
                List.of("username")
        );
        assertConstraint(
                recovered,
                ConstraintType.NOT_NULL,
                List.of("email")
        );
    }

    @Test
    void shouldPersistAndRecoverCompositeConstraints() {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                new Table(
                        "enrollments",
                        List.of(
                                new Column("student_id", DataType.INT),
                                new Column("course_id", DataType.INT),
                                new Column("first_name", DataType.STRING),
                                new Column("last_name", DataType.STRING)
                        ),
                        List.of(
                                new PrimaryKeyConstraint(
                                        List.of(
                                                "student_id",
                                                "course_id"
                                        )
                                ),
                                new UniqueConstraint(
                                        List.of(
                                                "first_name",
                                                "last_name"
                                        )
                                )
                        )
                )
        );

        TableManager recoveredManager =
                new TableManager(tempDirectory);

        recoveredManager.loadCatalog();

        Table recovered =
                recoveredManager.getTable("enrollments");

        assertConstraint(
                recovered,
                ConstraintType.PRIMARY_KEY,
                List.of("student_id", "course_id")
        );
        assertConstraint(
                recovered,
                ConstraintType.UNIQUE,
                List.of("first_name", "last_name")
        );
    }

    @Test
    void shouldRecoverConstraintFreeTable() {
        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                "logs",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("message", DataType.STRING)
                )
        );

        TableManager recoveredManager =
                new TableManager(tempDirectory);

        recoveredManager.loadCatalog();

        Table recovered =
                recoveredManager.getTable("logs");

        assertTrue(recovered.getConstraints().isEmpty());
    }

    @Test
    void shouldRecoverLegacySchemaWithoutConstraintSection()
            throws Exception {

        TableManager manager = new TableManager(tempDirectory);

        manager.createTable(
                "legacy_users",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("name", DataType.STRING)
                )
        );

        Path tableFile =
                tempDirectory.resolve(
                        "legacy_users.tbl"
                );

        byte[] fileBytes =
                Files.readAllBytes(tableFile);

        byte[] headerBytes =
                Arrays.copyOfRange(
                        fileBytes,
                        0,
                        TableHeaderConstants.HEADER_SIZE
                );

        String schema =
                new String(
                        Arrays.copyOfRange(
                                fileBytes,
                                TableHeaderConstants.HEADER_SIZE,
                                fileBytes.length
                        ),
                        StandardCharsets.UTF_8
                );

        String legacySchema =
                schema.lines()
                        .filter(line ->
                                !ConstraintSchemaCodec.CONSTRAINTS_HEADER
                                        .equals(line.trim())
                        )
                        .reduce(
                                "",
                                (left, right) ->
                                        left
                                                + right
                                                + System.lineSeparator()
                        );

        byte[] legacySchemaBytes =
                legacySchema.getBytes(
                        StandardCharsets.UTF_8
                );

        byte[] legacyFileBytes =
                new byte[
                        headerBytes.length
                                + legacySchemaBytes.length
                        ];

        System.arraycopy(
                headerBytes,
                0,
                legacyFileBytes,
                0,
                headerBytes.length
        );

        System.arraycopy(
                legacySchemaBytes,
                0,
                legacyFileBytes,
                headerBytes.length,
                legacySchemaBytes.length
        );

        Files.write(
                tableFile,
                legacyFileBytes
        );

        TableManager recoveredManager =
                new TableManager(tempDirectory);

        recoveredManager.loadCatalog();

        Table recovered =
                recoveredManager.getTable(
                        "legacy_users"
                );

        assertTrue(
                recovered.getConstraints().isEmpty()
        );
    }

    private void assertConstraint(
            Table table,
            ConstraintType type,
            List<String> columns
    ) {
        Constraint found = table.getConstraints()
                .stream()
                .filter(constraint -> constraint.type() == type)
                .filter(constraint -> constraint.columns().equals(columns))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Constraint not found. type=" + type
                                + ", columns=" + columns
                                + ", actual=" + table.getConstraints()
                ));

        assertEquals(type, found.type());
        assertEquals(columns, found.columns());
    }
}
