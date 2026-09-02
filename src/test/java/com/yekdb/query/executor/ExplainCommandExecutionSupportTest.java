package com.yekdb.query.executor;

import com.yekdb.query.command.ExplainCommand;
import com.yekdb.query.datasource.InMemoryQueryDataSource;
import com.yekdb.query.statement.SelectStatement;
import com.yekdb.storage.record.Row;
import com.yekdb.storage.table.Column;
import com.yekdb.storage.table.DataType;
import com.yekdb.storage.table.Table;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplainCommandExecutionSupportTest {

    @Test
    void shouldReturnExplainPlanRowsWithoutExecutingSelect() {

        InMemoryQueryDataSource dataSource =
                new InMemoryQueryDataSource();

        dataSource.register(
                new Table(
                        "users",
                        List.of(
                                new Column("id", DataType.INT),
                                new Column("name", DataType.STRING)
                        )
                ),
                List.of(
                        new Row(
                                List.of(1, "Emre")
                        )
                )
        );

        ExplainCommandExecutionSupport support =
                new ExplainCommandExecutionSupport();

        ExecuteResult result =
                support.execute(
                        new ExplainCommand(
                                new SelectStatement(
                                        "users",
                                        List.of("id")
                                )
                        ),
                        dataSource,
                        List.of()
                );

        assertTrue(result.isSuccess());
        assertEquals(1, result.getColumnCount());
        assertEquals(
                "plan_step",
                result.getColumns().get(0).getName()
        );
        assertTrue(
                result.getRows()
                        .stream()
                        .anyMatch(row ->
                                "PLAN: FULL_TABLE_SCAN".equals(
                                        row.getValue(0)
                                )
                        )
        );
    }
}
