package com.yekdb.query.parser;

import com.yekdb.query.statement.BeginTransactionStatement;
import com.yekdb.query.statement.CommitTransactionStatement;
import com.yekdb.query.statement.ReleaseSavepointStatement;
import com.yekdb.query.statement.RollbackToSavepointStatement;
import com.yekdb.query.statement.RollbackTransactionStatement;
import com.yekdb.query.statement.SavepointStatement;
import com.yekdb.query.statement.Statement;
import com.yekdb.query.statement.StatementType;
import com.yekdb.transaction.TransactionAccessMode;
import com.yekdb.transaction.TransactionIsolationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SqlParserTransactionTest {

    private SqlParser parser;

    @BeforeEach
    void setUp() {
        parser =
                new SqlParser();
    }

    @Test
    void shouldParseReadUncommittedIsolationLevel() {

        Statement statement =
                parser.parse(
                        "START TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;"
                );

        BeginTransactionStatement beginStatement =
                assertInstanceOf(
                        BeginTransactionStatement.class,
                        statement
                );

        assertEquals(
                TransactionIsolationLevel.READ_UNCOMMITTED,
                beginStatement.getIsolationLevel()
        );
    }

    @Test
    void shouldParseBeginTransaction() {

        Statement statement =
                parser.parse(
                        "BEGIN;"
                );

        BeginTransactionStatement beginStatement =
                assertInstanceOf(
                        BeginTransactionStatement.class,
                        statement
                );

        assertEquals(
                StatementType.BEGIN_TRANSACTION,
                beginStatement.getType()
        );
    }

    @Test
    void shouldParseBeginTransactionKeywordForm() {

        Statement statement =
                parser.parse(
                        "BEGIN TRANSACTION;"
                );

        BeginTransactionStatement beginStatement =
                assertInstanceOf(
                        BeginTransactionStatement.class,
                        statement
                );

        assertEquals(
                StatementType.BEGIN_TRANSACTION,
                beginStatement.getType()
        );
    }

    @Test
    void shouldParseStartTransaction() {

        Statement statement =
                parser.parse(
                        "START TRANSACTION;"
                );

        BeginTransactionStatement beginStatement =
                assertInstanceOf(
                        BeginTransactionStatement.class,
                        statement
                );

        assertEquals(
                StatementType.BEGIN_TRANSACTION,
                beginStatement.getType()
        );
    }

    @Test
    void shouldParseReadOnlyTransactionAccessMode() {

        Statement statement =
                parser.parse(
                        "START TRANSACTION READ ONLY;"
                );

        BeginTransactionStatement beginStatement =
                assertInstanceOf(
                        BeginTransactionStatement.class,
                        statement
                );

        assertEquals(
                TransactionAccessMode.READ_ONLY,
                beginStatement.getAccessMode()
        );
    }

    @Test
    void shouldParseReadWriteTransactionAccessMode() {

        Statement statement =
                parser.parse(
                        "BEGIN TRANSACTION READ WRITE;"
                );

        BeginTransactionStatement beginStatement =
                assertInstanceOf(
                        BeginTransactionStatement.class,
                        statement
                );

        assertEquals(
                TransactionAccessMode.READ_WRITE,
                beginStatement.getAccessMode()
        );
    }

    @Test
    void shouldParseSerializableIsolationLevel() {

        Statement statement =
                parser.parse(
                        "START TRANSACTION ISOLATION LEVEL SERIALIZABLE;"
                );

        BeginTransactionStatement beginStatement =
                assertInstanceOf(
                        BeginTransactionStatement.class,
                        statement
                );

        assertEquals(
                TransactionIsolationLevel.SERIALIZABLE,
                beginStatement.getIsolationLevel()
        );
    }

    @Test
    void shouldParseRepeatableReadIsolationLevelWithAccessMode() {

        Statement statement =
                parser.parse(
                        "BEGIN TRANSACTION READ ONLY ISOLATION LEVEL REPEATABLE READ;"
                );

        BeginTransactionStatement beginStatement =
                assertInstanceOf(
                        BeginTransactionStatement.class,
                        statement
                );

        assertEquals(
                TransactionAccessMode.READ_ONLY,
                beginStatement.getAccessMode()
        );

        assertEquals(
                TransactionIsolationLevel.REPEATABLE_READ,
                beginStatement.getIsolationLevel()
        );
    }

    @Test
    void shouldParseOptionsInIsolationThenAccessModeOrder() {

        Statement statement =
                parser.parse(
                        "START TRANSACTION ISOLATION LEVEL READ COMMITTED READ WRITE;"
                );

        BeginTransactionStatement beginStatement =
                assertInstanceOf(
                        BeginTransactionStatement.class,
                        statement
                );

        assertEquals(
                TransactionAccessMode.READ_WRITE,
                beginStatement.getAccessMode()
        );

        assertEquals(
                TransactionIsolationLevel.READ_COMMITTED,
                beginStatement.getIsolationLevel()
        );
    }

    @Test
    void shouldParseCommitTransaction() {

        Statement statement =
                parser.parse(
                        "COMMIT;"
                );

        CommitTransactionStatement commitStatement =
                assertInstanceOf(
                        CommitTransactionStatement.class,
                        statement
                );

        assertEquals(
                StatementType.COMMIT_TRANSACTION,
                commitStatement.getType()
        );
    }

    @Test
    void shouldParseCommitTransactionKeywordForm() {

        Statement statement =
                parser.parse(
                        "COMMIT TRANSACTION;"
                );

        CommitTransactionStatement commitStatement =
                assertInstanceOf(
                        CommitTransactionStatement.class,
                        statement
                );

        assertEquals(
                StatementType.COMMIT_TRANSACTION,
                commitStatement.getType()
        );
    }

    @Test
    void shouldParseRollbackTransaction() {

        Statement statement =
                parser.parse(
                        "ROLLBACK;"
                );

        RollbackTransactionStatement rollbackStatement =
                assertInstanceOf(
                        RollbackTransactionStatement.class,
                        statement
                );

        assertEquals(
                StatementType.ROLLBACK_TRANSACTION,
                rollbackStatement.getType()
        );
    }

    @Test
    void shouldParseRollbackTransactionKeywordForm() {

        Statement statement =
                parser.parse(
                        "ROLLBACK TRANSACTION;"
                );

        RollbackTransactionStatement rollbackStatement =
                assertInstanceOf(
                        RollbackTransactionStatement.class,
                        statement
                );

        assertEquals(
                StatementType.ROLLBACK_TRANSACTION,
                rollbackStatement.getType()
        );
    }

    @Test
    void shouldParseSavepoint() {

        Statement statement =
                parser.parse(
                        "SAVEPOINT before_insert;"
                );

        SavepointStatement savepointStatement =
                assertInstanceOf(
                        SavepointStatement.class,
                        statement
                );

        assertEquals(
                StatementType.SAVEPOINT,
                savepointStatement.getType()
        );

        assertEquals(
                "before_insert",
                savepointStatement.getSavepointName()
        );
    }

    @Test
    void shouldParseRollbackToSavepoint() {

        Statement statement =
                parser.parse(
                        "ROLLBACK TO SAVEPOINT before_insert;"
                );

        RollbackToSavepointStatement rollbackStatement =
                assertInstanceOf(
                        RollbackToSavepointStatement.class,
                        statement
                );

        assertEquals(
                StatementType.ROLLBACK_TO_SAVEPOINT,
                rollbackStatement.getType()
        );

        assertEquals(
                "before_insert",
                rollbackStatement.getSavepointName()
        );
    }

    @Test
    void shouldParseRollbackToSavepointWithoutSavepointKeyword() {

        Statement statement =
                parser.parse(
                        "ROLLBACK TO before_insert;"
                );

        RollbackToSavepointStatement rollbackStatement =
                assertInstanceOf(
                        RollbackToSavepointStatement.class,
                        statement
                );

        assertEquals(
                "before_insert",
                rollbackStatement.getSavepointName()
        );
    }

    @Test
    void shouldParseReleaseSavepoint() {

        Statement statement =
                parser.parse(
                        "RELEASE SAVEPOINT before_insert;"
                );

        ReleaseSavepointStatement releaseStatement =
                assertInstanceOf(
                        ReleaseSavepointStatement.class,
                        statement
                );

        assertEquals(
                StatementType.RELEASE_SAVEPOINT,
                releaseStatement.getType()
        );

        assertEquals(
                "before_insert",
                releaseStatement.getSavepointName()
        );
    }
}
