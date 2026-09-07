# YEKDB 00-36 Isolation and Deadlock Hardening

## Objective

Sprint 00-36 turns transaction isolation metadata into an explicit runtime
policy and makes detected deadlocks observable after their wait-for graph edges
have been cleaned. The implementation extends the table-level concurrency
architecture introduced in Sprint 00-35 without changing storage formats or
the public transaction defaults.

## Phase Plan

Each phase is compile-safe and has a focused test boundary. Run the listed
tests after the phase, then run the full suite before moving to the next phase.

### Phase 1 Isolation Policy Foundation

- Add `READ_UNCOMMITTED` to `TransactionIsolationLevel`.
- Define read-lock requirement and lock lifetime on the enum.
- Keep `READ_COMMITTED` as the default for existing callers.

```bash
mvn -Dtest=TransactionIsolationLevelPolicyTest test
```

### Phase 2 SQL Parsing

- Parse `ISOLATION LEVEL READ UNCOMMITTED`.
- Preserve all existing transaction option orderings and levels.

```bash
mvn -Dtest=SqlParserTransactionTest test
```

### Phase 3 Query Isolation Integration

- Let `READ UNCOMMITTED` SELECT statements bypass shared read locks.
- Keep `READ COMMITTED` locks statement-scoped.
- Keep `REPEATABLE READ` and `SERIALIZABLE` locks transaction-scoped.
- Reuse the isolation policy instead of duplicating enum comparisons.

```bash
mvn -Dtest=QueryExecutorReadLockIsolationTest test
```

### Phase 4 Deadlock Diagnostics

- Count deadlocks detected by an in-memory lock manager.
- Retain the latest 16 deadlock incidents in insertion order.
- Record victim owner, waiting resource, requested mode, cycle and sequence.
- Return detached immutable history through `LockManagerSnapshot`.
- Preserve the Sprint 00-35 two-argument snapshot constructor.

```bash
mvn -Dtest=InMemoryLockManagerTest,LockManagerSnapshotDeadlockTest test
```

### Phase 5 Regression Closure

- Run parser, isolation, transaction and concurrency tests together.
- Run the full project suite.
- Confirm lock queues and wait-for dependencies are still cleaned.

```bash
mvn -Dtest='*Transaction*,*LockManager*,*Deadlock*' test
mvn clean test
```

## Isolation Matrix

| Isolation level | SELECT read lock | Release boundary | Dirty read |
| --- | --- | --- | --- |
| READ UNCOMMITTED | No | Not applicable | Allowed |
| READ COMMITTED | Yes | Statement end | Prevented |
| REPEATABLE READ | Yes | Transaction end | Prevented |
| SERIALIZABLE | Yes | Transaction end | Prevented |

The guarantees are implemented with table-level locks. `REPEATABLE READ` and
`SERIALIZABLE` therefore intentionally share the same lock lifetime until
row/page locking or MVCC is introduced.

## Deadlock Diagnostic Contract

`LockManagerSnapshot` exposes a monotonic detected-deadlock count and a bounded
recent incident list. Each incident is immutable and remains available after
the victim request has left the wait queue. Live resources and wait-for
dependencies continue to describe only the current lock-manager state.

## Compatibility

- Default transactions remain `READ COMMITTED`.
- Existing three isolation levels retain their Sprint 00-35 behavior.
- Transaction log parsing accepts the new enum value without a format change.
- Existing `LockManagerSnapshot(resources, dependencies)` callers continue to
  compile and receive an empty deadlock history.
- No table, row, index or durability file format changes are introduced.

## Verification Result

```text
Compile: SUCCESS
Tests:   2034 / 2034 PASSED
```

## Known Limits

- Locking remains JVM-local and table-granular.
- `READ UNCOMMITTED` may observe data later removed by rollback.
- Deadlock history is in-memory, bounded to 16 incidents and not persisted.
- Row/page locking, MVCC and snapshot isolation remain future work.
