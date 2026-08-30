package com.yekdb.query.executor;

import com.yekdb.query.parser.SqlParser;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SelectFunctionProjectionExecutorTest {

    private final SelectFunctionProjectionExecutor executor =
            new SelectFunctionProjectionExecutor();

    private final Table table = new Table(
            "users",
            List.of(
                    new Column("id", DataType.INT),
                    new Column("name", DataType.STRING),
                    new Column("city", DataType.STRING),
                    new Column("balance", DataType.INT)
            )
    );

    @Test
    void shouldProjectLowerUpperLengthAndAbs() {
        SelectStatement statement = (SelectStatement) new SqlParser().parse(
                "SELECT LOWER(name), UPPER(city), LENGTH(name), ABS(balance) FROM users;"
        );

        SelectFunctionProjectionExecutor.Projection result = executor.project(
                table,
                List.of(new Row(List.of(1, "Emre", "malatya", -250))),
                statement
        );

        assertEquals(List.of("emre", "MALATYA", 4, 250), result.rows().get(0).getValues());
        assertEquals(4, result.columns().size());
    }

    @Test
    void shouldProjectNestedFunction() {
        SelectStatement statement = (SelectStatement) new SqlParser().parse(
                "SELECT LENGTH(TRIM(name)) AS clean_length FROM users;"
        );

        SelectFunctionProjectionExecutor.Projection result = executor.project(
                table,
                List.of(new Row(List.of(1, "   YEKDB   ", "Malatya", 10))),
                statement
        );

        assertEquals(5, result.rows().get(0).getValue(0));
        assertEquals("clean_length", result.columns().get(0).getName());
        assertEquals(DataType.INT, result.columns().get(0).getDataType());
    }

    @Test
    void shouldSupportMixedColumnAndFunctionProjection() {
        SelectStatement statement = (SelectStatement) new SqlParser().parse(
                "SELECT id, LOWER(name) AS normalized_name FROM users;"
        );

        SelectFunctionProjectionExecutor.Projection result = executor.project(
                table,
                List.of(new Row(List.of(7, "EMRE", "Malatya", -1))),
                statement
        );

        assertEquals(7, result.rows().get(0).getValue(0));
        assertEquals("emre", result.rows().get(0).getValue(1));
    }

    @Test
    void shouldSupportQualifiedColumnWithTableAlias() {
        SelectStatement statement = (SelectStatement) new SqlParser().parse(
                "SELECT LOWER(u.name) AS normalized_name FROM users u;"
        );

        SelectFunctionProjectionExecutor.Projection result = executor.project(
                table,
                List.of(new Row(List.of(1, "YEKDB", "Malatya", 0))),
                statement
        );

        assertEquals("yekdb", result.rows().get(0).getValue(0));
    }
    @Test
    void shouldProjectAbsFromNumericColumn() {
        Table numericTable = new Table(
                "products",
                List.of(
                        new Column("id", DataType.INT),
                        new Column("price", DataType.NUMERIC, null, 8, 2)
                )
        );

        SelectStatement statement = (SelectStatement) new SqlParser().parse(
                "SELECT ABS(price) AS absolute_price FROM products;"
        );

        SelectFunctionProjectionExecutor.Projection result = executor.project(
                numericTable,
                List.of(new Row(List.of(1, -999.99))),
                statement
        );

        assertEquals(999.99, ((Number) result.rows().get(0).getValue(0)).doubleValue(), 0.000001);
        assertEquals(DataType.NUMERIC, result.columns().get(0).getDataType());
    }

}
