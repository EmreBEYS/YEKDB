package com.yekdb.query.mapper;

import com.yekdb.query.command.BeginTransactionCommand;
import com.yekdb.query.command.CommitTransactionCommand;
import com.yekdb.query.command.Command;
import com.yekdb.query.command.ReleaseSavepointCommand;
import com.yekdb.query.command.RollbackTransactionCommand;
import com.yekdb.query.command.RollbackToSavepointCommand;
import com.yekdb.query.command.SavepointCommand;
import com.yekdb.query.parser.SqlParser;
import com.yekdb.transaction.TransactionAccessMode;
import com.yekdb.transaction.TransactionIsolationLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StatementCommandMapperTransactionTest {

    @Test
    void shouldMapBeginStatementToBeginCommand() {

        Command command =
                StatementCommandMapper.map(
                        new SqlParser()
                                .parse(
                                        "BEGIN;"
                                )
                );

        BeginTransactionCommand beginCommand =
                assertInstanceOf(
                BeginTransactionCommand.class,
                command
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                TransactionAccessMode.READ_WRITE,
                beginCommand.getAccessMode()
        );
    }

    @Test
    void shouldMapReadOnlyBeginStatementToBeginCommand() {

        Command command =
                StatementCommandMapper.map(
                        new SqlParser()
                                .parse(
                                        "START TRANSACTION READ ONLY;"
                                )
                );

        BeginTransactionCommand beginCommand =
                assertInstanceOf(
                        BeginTransactionCommand.class,
                        command
                );

        org.junit.jupiter.api.Assertions.assertEquals(
                TransactionAccessMode.READ_ONLY,
                beginCommand.getAccessMode()
        );
    }

    @Test
    void shouldMapIsolationLevelToBeginCommand() {

        Command command =
                StatementCommandMapper.map(
                        new SqlParser()
                                .parse(
                                        "START TRANSACTION ISOLATION LEVEL SERIALIZABLE;"
                                )
                );

        BeginTransactionCommand beginCommand =
                assertInstanceOf(
                        BeginTransactionCommand.class,
                        command
                );

        org.junit.jupiter.api.Assertions.assertEquals(
                TransactionIsolationLevel.SERIALIZABLE,
                beginCommand.getIsolationLevel()
        );
    }

    @Test
    void shouldMapCommitStatementToCommitCommand() {

        Command command =
                StatementCommandMapper.map(
                        new SqlParser()
                                .parse(
                                        "COMMIT;"
                                )
                );

        assertInstanceOf(
                CommitTransactionCommand.class,
                command
        );
    }

    @Test
    void shouldMapRollbackStatementToRollbackCommand() {

        Command command =
                StatementCommandMapper.map(
                        new SqlParser()
                                .parse(
                                        "ROLLBACK;"
                                )
                );

        assertInstanceOf(
                RollbackTransactionCommand.class,
                command
        );
    }

    @Test
    void shouldMapSavepointStatementToSavepointCommand() {

        Command command =
                StatementCommandMapper.map(
                        new SqlParser()
                                .parse(
                                        "SAVEPOINT sp1;"
                                )
                );

        SavepointCommand savepointCommand =
                assertInstanceOf(
                        SavepointCommand.class,
                        command
                );

        org.junit.jupiter.api.Assertions.assertEquals(
                "sp1",
                savepointCommand.getSavepointName()
        );
    }

    @Test
    void shouldMapRollbackToSavepointStatementToCommand() {

        Command command =
                StatementCommandMapper.map(
                        new SqlParser()
                                .parse(
                                        "ROLLBACK TO SAVEPOINT sp1;"
                                )
                );

        RollbackToSavepointCommand rollbackCommand =
                assertInstanceOf(
                        RollbackToSavepointCommand.class,
                        command
                );

        org.junit.jupiter.api.Assertions.assertEquals(
                "sp1",
                rollbackCommand.getSavepointName()
        );
    }

    @Test
    void shouldMapReleaseSavepointStatementToCommand() {

        Command command =
                StatementCommandMapper.map(
                        new SqlParser()
                                .parse(
                                        "RELEASE SAVEPOINT sp1;"
                                )
                );

        ReleaseSavepointCommand releaseCommand =
                assertInstanceOf(
                        ReleaseSavepointCommand.class,
                        command
                );

        org.junit.jupiter.api.Assertions.assertEquals(
                "sp1",
                releaseCommand.getSavepointName()
        );
    }
}
