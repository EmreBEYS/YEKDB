package com.yekdb.query.executor;

import com.yekdb.constraint.ForeignKeyConstraint;
import com.yekdb.constraint.ReferentialAction;
import com.yekdb.query.command.AddConstraintAlterAction;
import com.yekdb.query.command.AlterTableCommand;
import com.yekdb.query.command.Command;
import com.yekdb.query.command.CreateTableCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagementCommandParserReferentialActionTest {

    private final ManagementCommandParser parser =
            new ManagementCommandParser();

    @Test
    void shouldKeepRestrictAsDefaultWhenActionsAreOmitted() {
        ForeignKeyConstraint foreignKey = parseCreateTableForeignKey(
                "CREATE TABLE orders (" +
                        "id INT, user_id INT, " +
                        "FOREIGN KEY (user_id) REFERENCES users(id))"
        );

        assertEquals(ReferentialAction.RESTRICT, foreignKey.onDelete());
        assertEquals(ReferentialAction.RESTRICT, foreignKey.onUpdate());
    }

    @Test
    void shouldParseOnDeleteCascade() {
        ForeignKeyConstraint foreignKey = parseCreateTableForeignKey(
                "CREATE TABLE orders (" +
                        "id INT, user_id INT, " +
                        "FOREIGN KEY (user_id) REFERENCES users(id) " +
                        "ON DELETE CASCADE)"
        );

        assertEquals(ReferentialAction.CASCADE, foreignKey.onDelete());
        assertEquals(ReferentialAction.RESTRICT, foreignKey.onUpdate());
    }

    @Test
    void shouldParseOnDeleteSetNullAndOnUpdateCascade() {
        ForeignKeyConstraint foreignKey = parseCreateTableForeignKey(
                "CREATE TABLE orders (" +
                        "id INT, user_id INT, " +
                        "FOREIGN KEY (user_id) REFERENCES users(id) " +
                        "ON DELETE SET NULL ON UPDATE CASCADE)"
        );

        assertEquals(ReferentialAction.SET_NULL, foreignKey.onDelete());
        assertEquals(ReferentialAction.CASCADE, foreignKey.onUpdate());
    }

    @Test
    void shouldParseActionsInReverseOrder() {
        ForeignKeyConstraint foreignKey = parseCreateTableForeignKey(
                "CREATE TABLE orders (" +
                        "id INT, user_id INT, " +
                        "FOREIGN KEY (user_id) REFERENCES users(id) " +
                        "ON UPDATE SET NULL ON DELETE RESTRICT)"
        );

        assertEquals(ReferentialAction.RESTRICT, foreignKey.onDelete());
        assertEquals(ReferentialAction.SET_NULL, foreignKey.onUpdate());
    }

    @Test
    void shouldParseCompositeForeignKeyActions() {
        ForeignKeyConstraint foreignKey = parseCreateTableForeignKey(
                "CREATE TABLE child (" +
                        "a INT, b INT, " +
                        "FOREIGN KEY (a, b) REFERENCES parent(x, y) " +
                        "ON DELETE CASCADE ON UPDATE RESTRICT)"
        );

        assertEquals(List.of("a", "b"), foreignKey.columns());
        assertEquals(List.of("x", "y"), foreignKey.referencedColumnNames());
        assertEquals(ReferentialAction.CASCADE, foreignKey.onDelete());
        assertEquals(ReferentialAction.RESTRICT, foreignKey.onUpdate());
    }

    @Test
    void shouldParseNamedAlterTableForeignKeyActions() {
        Command command = parser.parse(
                "ALTER TABLE orders " +
                        "ADD CONSTRAINT fk_orders_user " +
                        "FOREIGN KEY (user_id) REFERENCES users(id) " +
                        "ON DELETE CASCADE ON UPDATE SET NULL"
        );

        AlterTableCommand alterTable =
                assertInstanceOf(AlterTableCommand.class, command);
        AddConstraintAlterAction action =
                assertInstanceOf(AddConstraintAlterAction.class, alterTable.action());
        ForeignKeyConstraint foreignKey =
                assertInstanceOf(ForeignKeyConstraint.class, action.constraint());

        assertEquals("fk_orders_user", foreignKey.name());
        assertEquals(ReferentialAction.CASCADE, foreignKey.onDelete());
        assertEquals(ReferentialAction.SET_NULL, foreignKey.onUpdate());
    }

    @Test
    void shouldRejectDuplicateOnDeleteClause() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TABLE orders (" +
                                "id INT, user_id INT, " +
                                "FOREIGN KEY (user_id) REFERENCES users(id) " +
                                "ON DELETE CASCADE ON DELETE RESTRICT)"
                )
        );
    }

    @Test
    void shouldRejectDuplicateOnUpdateClause() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TABLE orders (" +
                                "id INT, user_id INT, " +
                                "FOREIGN KEY (user_id) REFERENCES users(id) " +
                                "ON UPDATE CASCADE ON UPDATE SET NULL)"
                )
        );
    }

    @Test
    void shouldRejectUnsupportedReferentialAction() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TABLE orders (" +
                                "id INT, user_id INT, " +
                                "FOREIGN KEY (user_id) REFERENCES users(id) " +
                                "ON DELETE DO NOTHING)"
                )
        );
    }

    @Test
    void shouldRejectIncompleteSetNullSyntax() {
        assertThrows(
                QueryExecutionException.class,
                () -> parser.parse(
                        "CREATE TABLE orders (" +
                                "id INT, user_id INT, " +
                                "FOREIGN KEY (user_id) REFERENCES users(id) " +
                                "ON DELETE SET)"
                )
        );
    }

    private ForeignKeyConstraint parseCreateTableForeignKey(String sql) {
        CreateTableCommand command = assertInstanceOf(
                CreateTableCommand.class,
                parser.parse(sql)
        );

        return command.getConstraints().stream()
                .filter(ForeignKeyConstraint.class::isInstance)
                .map(ForeignKeyConstraint.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
