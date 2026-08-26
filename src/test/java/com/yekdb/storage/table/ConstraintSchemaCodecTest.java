package com.yekdb.storage.table;

import com.yekdb.constraint.Constraint;
import com.yekdb.constraint.ConstraintType;
import com.yekdb.constraint.NotNullConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.storage.exception.CorruptedTableFileException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConstraintSchemaCodecTest {

    @Test
    void shouldSerializeConstraintSection() {
        List<String> lines = ConstraintSchemaCodec.serialize(
                List.of(
                        new PrimaryKeyConstraint("id"),
                        new UniqueConstraint("username"),
                        new NotNullConstraint("email")
                )
        );

        assertEquals("constraints=", lines.get(0));
        assertTrue(lines.contains("PRIMARY_KEY:id"));
        assertTrue(lines.contains("UNIQUE:username"));
        assertTrue(lines.contains("NOT_NULL:email"));
    }

    @Test
    void shouldSerializeCompositeConstraints() {
        List<String> lines = ConstraintSchemaCodec.serialize(
                List.of(
                        new PrimaryKeyConstraint(
                                List.of("student_id", "course_id")
                        ),
                        new UniqueConstraint(
                                List.of("first_name", "last_name")
                        )
                )
        );

        assertTrue(lines.contains(
                "PRIMARY_KEY:student_id,course_id"
        ));
        assertTrue(lines.contains(
                "UNIQUE:first_name,last_name"
        ));
    }

    @Test
    void shouldDeserializeConstraintSection() {
        List<Constraint> constraints = ConstraintSchemaCodec.deserialize(
                List.of(
                        "YEKDB_TABLE",
                        "version=1",
                        "tableName=users",
                        "columnCount=3",
                        "createdAt=2026-08-26T12:00:00",
                        "columns=",
                        "id:INT",
                        "username:STRING",
                        "email:STRING",
                        "constraints=",
                        "PRIMARY_KEY:id",
                        "UNIQUE:username",
                        "NOT_NULL:email"
                ),
                Path.of("users.tbl")
        );

        assertEquals(3, constraints.size());
        assertConstraint(
                constraints,
                ConstraintType.PRIMARY_KEY,
                List.of("id")
        );
        assertConstraint(
                constraints,
                ConstraintType.UNIQUE,
                List.of("username")
        );
        assertConstraint(
                constraints,
                ConstraintType.NOT_NULL,
                List.of("email")
        );
    }

    @Test
    void shouldReturnEmptyListForLegacySchemaWithoutConstraintSection() {
        List<Constraint> constraints = ConstraintSchemaCodec.deserialize(
                List.of(
                        "YEKDB_TABLE",
                        "version=1",
                        "tableName=users",
                        "columnCount=2",
                        "createdAt=2026-08-26T12:00:00",
                        "columns=",
                        "id:INT",
                        "name:STRING"
                ),
                Path.of("users.tbl")
        );

        assertTrue(constraints.isEmpty());
    }

    @Test
    void shouldRejectMalformedConstraintDefinition() {
        assertThrows(
                CorruptedTableFileException.class,
                () -> ConstraintSchemaCodec.deserialize(
                        List.of(
                                "constraints=",
                                "UNIQUE:"
                        ),
                        Path.of("users.tbl")
                )
        );
    }

    private void assertConstraint(
            List<Constraint> constraints,
            ConstraintType type,
            List<String> columns
    ) {
        Constraint found = constraints.stream()
                .filter(constraint -> constraint.type() == type)
                .filter(constraint -> constraint.columns().equals(columns))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Constraint not found. type=" + type
                                + ", columns=" + columns
                                + ", actual=" + constraints
                ));

        assertEquals(type, found.type());
        assertEquals(columns, found.columns());
    }
}
