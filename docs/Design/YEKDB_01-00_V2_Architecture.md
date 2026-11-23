# YEKDB 01-00 V2 Architecture Contract

## Objective

Sprint 01-00 opens the YEKDB V2 preparation line without changing database
behavior or the V1 on-disk formats. It establishes the version boundary,
compatibility rules, module responsibilities, verification gates and the order
in which V2 infrastructure will be introduced.

## Verified V1 Baseline

- Git tag: `v1.0.12`
- Baseline main commit: `a8783e7`
- Java: 21
- V1 completion sprint: `00-40`
- Maven application version before this sprint: `1.0.12`
- Production sources: 374
- Test sources before this sprint: 240
- Tests: 2,042 passed, 0 failed, 0 errors, 0 skipped
- Database header format: version 1, 128 bytes
- Table header format: version 1, 512 bytes
- Database metadata version: `0.0.6`

The release tag and Maven version differ because the Windows release line was
tagged after the V1 Maven package had been stabilized. Post-V1 development
continues as `1.0.13`. The `2.0.0` version is reserved until the complete V2
scope and release gates are satisfied.

## Versioning Decision

Application versions and storage format versions are independent.

| Contract | Sprint 01-00 value | Policy |
|---|---:|---|
| Maven artifact | `1.0.13` | Post-V1 development version |
| CLI application | `1.0.13` | Must match the Maven version |
| Database header | `1` | Must not change without a migration |
| Table header | `1` | Must not change without a migration |
| Database metadata | `0.0.6` | Must remain readable |

Changing an on-disk version requires all of the following in the same sprint:

1. a versioned reader;
2. an explicit migration or backward-compatible decoder;
3. golden V1 fixture tests;
4. failure behavior for unsupported future versions;
5. recovery and rollback documentation.

## Module Boundaries

### SQL and query

The parser, statement models, optimizer and executors own SQL semantics. They
must not write pages, WAL records or lock files directly.

### Transaction and concurrency

Transaction state owns commit/rollback decisions. The concurrency package owns
lock acquisition, wait queues and deadlock policy. Storage code must not invent
independent transaction states.

### Storage and recovery

Page, record, table and file packages own binary persistence. Future WAL,
buffer-pool and checkpoint components must preserve write-ahead ordering and
must expose testable boundaries rather than being embedded in CLI code.

### Catalog and programmable objects

Database, table, index, view, trigger and procedure metadata must use versioned
formats. Catalog recovery must complete before query execution is available.

### Interfaces

The terminal remains the reference V2 client until the client/server sprint.
Future server and JDBC modules must call the same execution services instead of
duplicating SQL behavior.

## Compatibility Guarantees

Sprint 01-00 guarantees that:

- V1 database headers remain readable byte-for-byte;
- V1 table-header and database-metadata versions do not change;
- all 2,042 V1 regression tests remain green;
- the post-V1 application version change does not silently rewrite user data;
- unsupported storage changes require a later explicit migration sprint;
- V1 terminal SQL behavior remains the behavioral reference.

`V2BaselineCompatibilityTest` makes the critical format constants and a V1
binary database header part of the automated regression suite.

## V2 Dependency Order

The architectural dependency order is:

1. physical WAL;
2. redo/undo recovery;
3. checkpoints;
4. buffer pool;
5. free-space management;
6. persistent B+ Tree indexes;
7. statistics and cost-based planning;
8. row-level locking;
9. MVCC and snapshots;
10. client/server sessions;
11. JDBC, authentication and administration;
12. backup, benchmarks and V2 release closure.

Later stages must not bypass the guarantees of an earlier stage. In particular,
buffer-pool flushing must obey WAL ordering, persistent indexes must participate
in recovery, and MVCC cleanup must not remove versions visible to snapshots.

## Sprint Completion Gates

Every V2 sprint must include:

- the complete V1/V2 regression suite;
- focused unit tests;
- at least one cross-module integration test;
- storage compatibility evidence when persistence changes;
- an updated design note and README status;
- a terminal or public-interface demonstration;
- documented limitations and rollback behavior.

## Non-Goals for Sprint 01-00

This sprint does not implement WAL, change storage bytes, migrate user data,
introduce networking, add MVCC, or change query results. Its deliverable is a
tested and documented boundary from which those changes can safely begin.

## Verification

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot"
mvn clean test
mvn clean package
java -jar target/yekdb-1.0.13.jar
```

Expected Sprint 01-00 result: 2,045 tests pass and the runnable JAR reports
`YEKDB CLI 1.0.13` while retaining V1 storage format versions. Version `2.0.0`
is assigned only after the full V2 release plan is completed.
