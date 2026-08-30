package com.yekdb.query.executor;

import com.yekdb.query.parser.SqlParser;
import com.yekdb.query.result.QueryResult;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectVarcharProjectionMetadataPhase16Test {

    private final Table table = new Table(
            "function_demo",
            List.of(
                    new Column("id", DataType.INT),
                    new Column("name", DataType.VARCHAR, 100),
                    new Column("city", DataType.VARCHAR, 100),
                    new Column("balance", DataType.INT)
            )
    );

    private final List<Row> rows = List.of(
            new Row(List.of(1, "Emre", "MALATYA", -250)),
            new Row(List.of(2, "YEKDB", "Ankara", 500)),
            new Row(List.of(3, "Ali", "malatya", -75))
    );

    @Test
    void shouldPreserveVarcharMetadataWhenWhereUsesFunction() {
        SelectStatement statement = (SelectStatement) new SqlParser().parse(
                "SELECT id, name, city FROM function_demo WHERE LOWER(city) = 'malatya';"
        );

        QueryResult result = new SelectExecutor().executeStatement(
                table,
                rows,
                statement
        );

        assertTrue(result.isSuccess());
        assertEquals(2, result.getRows().size());
        assertEquals(DataType.VARCHAR, result.getColumns().get(1).getDataType());
        assertEquals(100, result.getColumns().get(1).getLength());
        assertEquals(DataType.VARCHAR, result.getColumns().get(2).getDataType());
        assertEquals(100, result.getColumns().get(2).getLength());
    }

    @Test
    void shouldPreserveVarcharMetadataInMixedFunctionProjection() {
        SelectStatement statement = (SelectStatement) new SqlParser().parse(
                "SELECT name, LOWER(city) AS city_lower FROM function_demo;"
        );

        QueryResult result = new SelectExecutor().executeStatement(
                table,
                rows,
                statement
        );

        assertTrue(result.isSuccess());
        assertEquals(DataType.VARCHAR, result.getColumns().get(0).getDataType());
        assertEquals(100, result.getColumns().get(0).getLength());
        assertEquals("malatya", result.getRows().get(0).getValue(1));
    }
}
