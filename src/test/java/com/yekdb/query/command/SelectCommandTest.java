package com.yekdb.query.command;

import com.yekdb.query.expression.ComparisonExpression;
import com.yekdb.query.expression.ComparisonOperator;
import com.yekdb.query.expression.Expression;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SelectCommand sınıfının birim testleri.
 */
class SelectCommandTest {

    @Test
    void allFrom_shouldCreateSelectAllCommand() {
        SelectCommand command =
                SelectCommand.allFrom(
                        "users"
                );

        assertEquals(
                "users",
                command.getTableName()
        );

        assertTrue(command.isSelectAll());
        assertTrue(command.getSelectedColumns().isEmpty());

        assertFalse(command.hasWhereExpression());
        assertNull(command.getWhereExpression());
    }

    @Test
    void allFrom_shouldTrimTableName() {
        SelectCommand command =
                SelectCommand.allFrom(
                        "  users  "
                );

        assertEquals(
                "users",
                command.getTableName()
        );
    }

    @Test
    void allFromWhere_shouldCreateSelectAllCommandWithWhere() {
        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        SelectCommand command =
                SelectCommand.allFromWhere(
                        "users",
                        expression
                );

        assertEquals(
                "users",
                command.getTableName()
        );

        assertTrue(command.isSelectAll());
        assertTrue(command.getSelectedColumns().isEmpty());

        assertTrue(command.hasWhereExpression());

        assertEquals(
                expression,
                command.getWhereExpression()
        );
    }

    @Test
    void columnsFrom_shouldCreateSelectedColumnsCommand() {
        SelectCommand command =
                SelectCommand.columnsFrom(
                        "users",
                        List.of(
                                "id",
                                "name",
                                "age"
                        )
                );

        assertEquals(
                "users",
                command.getTableName()
        );

        assertFalse(command.isSelectAll());

        assertEquals(
                List.of(
                        "id",
                        "name",
                        "age"
                ),
                command.getSelectedColumns()
        );

        assertFalse(command.hasWhereExpression());
        assertNull(command.getWhereExpression());
    }

    @Test
    void columnsFrom_shouldTrimSelectedColumnNames() {
        SelectCommand command =
                SelectCommand.columnsFrom(
                        "users",
                        List.of(
                                "  id  ",
                                " name ",
                                "age"
                        )
                );

        assertEquals(
                List.of(
                        "id",
                        "name",
                        "age"
                ),
                command.getSelectedColumns()
        );
    }

    @Test
    void columnsFromWhere_shouldCreateCommandWithWhere() {
        Expression expression =
                new ComparisonExpression(
                        "city",
                        ComparisonOperator.EQUALS,
                        "Malatya"
                );

        SelectCommand command =
                SelectCommand.columnsFromWhere(
                        "users",
                        List.of(
                                "id",
                                "name"
                        ),
                        expression
                );

        assertFalse(command.isSelectAll());

        assertEquals(
                List.of(
                        "id",
                        "name"
                ),
                command.getSelectedColumns()
        );

        assertTrue(command.hasWhereExpression());

        assertEquals(
                expression,
                command.getWhereExpression()
        );
    }

    @Test
    void selectedColumns_shouldBeImmutableCopy() {
        List<String> mutableColumns =
                new ArrayList<>(
                        List.of(
                                "id",
                                "name"
                        )
                );

        SelectCommand command =
                SelectCommand.columnsFrom(
                        "users",
                        mutableColumns
                );

        mutableColumns.clear();

        assertEquals(
                2,
                command.getSelectedColumns().size()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> command
                        .getSelectedColumns()
                        .clear()
        );
    }

    @Test
    void allFrom_shouldRejectNullTableName() {
        assertThrows(
                NullPointerException.class,
                () -> SelectCommand.allFrom(
                        null
                )
        );
    }

    @Test
    void allFrom_shouldRejectBlankTableName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SelectCommand.allFrom(
                        "   "
                )
        );
    }

    @Test
    void columnsFrom_shouldRejectNullColumnList() {
        assertThrows(
                NullPointerException.class,
                () -> SelectCommand.columnsFrom(
                        "users",
                        null
                )
        );
    }

    @Test
    void columnsFrom_shouldRejectEmptyColumnList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SelectCommand.columnsFrom(
                        "users",
                        List.of()
                )
        );
    }

    @Test
    void columnsFrom_shouldRejectBlankColumnName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SelectCommand.columnsFrom(
                        "users",
                        List.of(
                                "id",
                                "   "
                        )
                )
        );
    }

    @Test
    void columnsFrom_shouldRejectNullColumnName() {
        List<String> columns =
                new ArrayList<>();

        columns.add("id");
        columns.add(null);

        assertThrows(
                NullPointerException.class,
                () -> SelectCommand.columnsFrom(
                        "users",
                        columns
                )
        );
    }

    @Test
    void allFromWhere_shouldRejectNullExpression() {
        assertThrows(
                NullPointerException.class,
                () -> SelectCommand.allFromWhere(
                        "users",
                        null
                )
        );
    }

    @Test
    void columnsFromWhere_shouldRejectNullExpression() {
        assertThrows(
                NullPointerException.class,
                () -> SelectCommand.columnsFromWhere(
                        "users",
                        List.of(
                                "id",
                                "name"
                        ),
                        null
                )
        );
    }

    @Test
    void toString_shouldContainCommandSummary() {
        Expression expression =
                new ComparisonExpression(
                        "age",
                        ComparisonOperator.GREATER_THAN,
                        18
                );

        SelectCommand command =
                SelectCommand.allFromWhere(
                        "users",
                        expression
                );

        String value = command.toString();

        assertTrue(
                value.contains("users")
        );

        assertTrue(
                value.contains("selectAll=true")
        );

        assertTrue(
                value.contains("whereExpression")
        );
    }
}