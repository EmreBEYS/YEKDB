package com.yekdb.query.command;

import com.yekdb.constraint.NotNullConstraint;
import com.yekdb.constraint.PrimaryKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateTableCommandConstraintTest {

    @Test
    void oldConstructorShouldCreateConstraintFreeCommand() {
        CreateTableCommand command = new CreateTableCommand(
                "users",
                List.of(
                        new Column("id", DataType.INT)
                )
        );

        assertFalse(command.hasConstraints());
        assertEquals(0, command.getConstraintCount());
    }

    @Test
    void shouldCarryConstraintMetadata() {
        CreateTableCommand command = new CreateTableCommand(
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
        );

        assertTrue(command.hasConstraints());
        assertEquals(3, command.getConstraintCount());
        assertEquals(3, command.getConstraints().size());
    }
}
