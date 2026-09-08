# YEKDB 00-37 EXPLAIN ANALYZE

## Objective

Sprint 00-37 extends the plan-only `EXPLAIN SELECT` workflow with
`EXPLAIN ANALYZE SELECT`. Analyze mode executes the SELECT through YEKDB's
normal query path, keeps the optimizer plan visible, and appends actual result
row count and elapsed execution time. Existing `EXPLAIN SELECT` behavior stays
plan-only and backward compatible.

## Phase Plan

Every phase is compile-safe and has a focused verification boundary. Apply or
review the phases in order, run the listed command, and only then continue.

### Phase 1 Explain Mode Foundation

- Add the immutable `ExplainMode` model.
- Extend `ExplainStatement` and `ExplainCommand` with mode metadata.
- Keep their existing one-argument constructors as plan-only compatibility APIs.

```bash
mvn -Dtest=ExplainStatementTest test
```

### Phase 2 SQL Parsing and Mapping

- Parse `EXPLAIN ANALYZE SELECT ...` without introducing a globally reserved
  `ANALYZE` keyword.
- Carry the selected mode through `StatementCommandMapper`.
- Continue rejecting non-SELECT EXPLAIN targets.

```bash
mvn -Dtest=SqlParserExplainTest,StatementCommandMapperExplainTest test
```

### Phase 3 Runtime Analysis

- Generate the optimizer plan before query execution.
- Execute analyze targets through the existing SELECT pipeline.
- Append `ANALYZE`, `ACTUAL_ROWS`, and `EXECUTION_TIME_NANOS` plan rows.
- Return plan/measurement rows only; do not mix selected data rows into the
  EXPLAIN result.

```bash
mvn -Dtest=ExplainCommandExecutionSupportTest,QueryExecutorExplainTest test
```

### Phase 4 View and Concurrency Integration

- Preserve view source-plan reporting.
- Measure the final view query after view materialization and outer filtering.
- Reuse the ordinary SELECT path so active isolation-level read-lock rules also
  apply to analyze mode.

```bash
mvn -Dtest=QueryExecutorExplainViewTest,QueryExecutorReadLockIsolationTest test
```

### Phase 5 Regression Closure

- Re-run all EXPLAIN tests.
- Compile the complete production tree.
- Run the complete project suite before merging.

```bash
mvn -Dtest='*Explain*' test
mvn clean test
```

## SQL Contract

Plan-only inspection remains unchanged:

```sql
EXPLAIN SELECT id, name
FROM users
WHERE age >= 18;
```

Runtime analysis uses:

```sql
EXPLAIN ANALYZE SELECT id, name
FROM users
WHERE age >= 18;
```

The final plan rows include:

```text
PLAN: FULL_TABLE_SCAN
INDEX: NONE
...
ANALYZE: TRUE
ACTUAL_ROWS: 2
EXECUTION_TIME_NANOS: 123456
```

The time value is deliberately expressed as an integer nanosecond duration so
the execution contract is locale-independent. Tests assert its presence and
non-negative shape rather than a machine-dependent exact value.

## Execution Semantics

- Plain `EXPLAIN` never executes its SELECT.
- `EXPLAIN ANALYZE` executes its SELECT exactly once.
- The measured interval covers SELECT execution, including view materialization
  and the applicable read-lock acquisition/release behavior.
- SELECT failures are returned through the existing query error path; partial
  analysis output is not reported as a success.
- Only SELECT targets are supported, so analyze mode introduces no data mutation.

## Compatibility

- Existing SQL and plan row labels are unchanged.
- Existing `ExplainStatement(SelectStatement)` and
  `ExplainCommand(SelectStatement)` callers remain source-compatible.
- `ANALYZE` is recognized only immediately after `EXPLAIN`, so existing uses of
  the word as an identifier elsewhere are unaffected.
- No storage, table, row, index, transaction-log, or durability format changes
  are introduced.

## Verification Result

```text
Compile: SUCCESS
Tests:   2042 / 2042 PASSED
```

The passing suite includes the new parser, mapping, plan-only compatibility,
runtime analysis, view analysis, transaction, and concurrency regression tests.

## Known Limits

- Runtime measurements are wall-clock observations and vary by machine and run.
- Actual row count is the final SELECT result size, not per-operator cardinality.
- Per-operator timing, buffer statistics, estimated cost, and JSON plan output
  remain future enhancements.
