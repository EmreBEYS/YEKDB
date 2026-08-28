package com.yekdb.query.executor;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.UniqueConstraint;
import com.yekdb.query.command.AddConstraintAlterAction;
import com.yekdb.query.command.AlterTableCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlterTableNamedConstraintParserTest {

    @Test
    void addConstraint_shouldParseExplicitConstraintName() {
        AlterTableCommand command = (AlterTableCommand)
                new ManagementCommandParser().parse(
                        "ALTER TABLE users "
                                + "ADD CONSTRAINT uq_users_email UNIQUE (email)"
                );

        AddConstraintAlterAction action =
                (AddConstraintAlterAction) command.action();

        assertTrue(action.constraint() instanceof UniqueConstraint);
        assertEquals("uq_users_email", action.constraint().name());
    }

    @Test
    void addNamedForeignKey_shouldPreserveConstraintName() {
        AlterTableCommand command = (AlterTableCommand)
                new ManagementCommandParser().parse(
                        "ALTER TABLE orders "
                                + "ADD CONSTRAINT fk_orders_user "
                                + "FOREIGN KEY (user_id) REFERENCES users(id)"
                );

        AddConstraintAlterAction action =
                (AddConstraintAlterAction) command.action();

        assertTrue(action.constraint() instanceof ForeignKeyConstraint);
        assertEquals("fk_orders_user", action.constraint().name());
    }
}
