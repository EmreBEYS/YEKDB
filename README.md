# YEKDB
### Yet Another Embedded Key Database

> An educational relational database management system built from scratch in Java 21, with its own storage, SQL parsing, query execution, JOIN engine, aggregation pipeline, rule-based JOIN optimization, persistent table recovery, and binary metadata management architecture.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![JUnit](https://img.shields.io/badge/JUnit-5-green)
![Tests](https://img.shields.io/badge/Tests-1066%20Passing-brightgreen)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-lightgrey)
![Status](https://img.shields.io/badge/Status-Development-yellow)

---

## About the Project

**YEKDB** is an educational relational database management system developed from scratch without using an existing database engine as its backend.

The project is designed to explore how relational database systems work internally by implementing the core layers independently:

- Physical storage
- Page and record management
- Table and index management
- SQL tokenization and parsing
- Expression evaluation
- Query execution
- CRUD operations
- JOIN processing
- GROUP BY / HAVING / aggregate execution
- Rule-based query optimization
- Persistent table recovery
- Binary table metadata management
- Regression testing

YEKDB does **not** rely on PostgreSQL, MySQL, SQLite, or another database engine for its internal storage or query execution.

> Current development milestone: **Sprint 00-20 — Persistent Table Metadata Mutation & TableManager Integration**

---

## Project Goals

YEKDB is designed to gradually evolve into a PostgreSQL-inspired database engine while keeping the internal architecture understandable, testable, and modular.

Main goals:

- Build a DBMS from the ground up
- Understand physical page and record storage
- Implement database and table management
- Build an SQL parsing and execution pipeline
- Support joins, filtering, grouping, aggregation, and sorting
- Develop an extensible index infrastructure
- Keep the codebase modular and test-driven
- Build restart-safe persistent metadata foundations
- Prepare the architecture for transactions, WAL, persistent indexes, and client/server support

---

## Current Status

**Current Sprint:** `00-20`  
**Status:** Completed ✅

YEKDB currently includes:

- Core Engine Architecture
- Storage Engine Foundation
- Page Management
- Record Management
- Database Management
- Table Management
- Index Management
- Query Execution Foundation
- SELECT / WHERE Execution
- Expression Evaluation
- ORDER BY / LIMIT / FETCH
- BETWEEN / IN
- LIKE / NOT LIKE / ILIKE
- GROUP BY / HAVING
- INNER / LEFT / RIGHT / FULL JOIN
- Multiple JOIN Chains
- JOIN + GROUP BY / HAVING
- JOIN + Aggregate Expressions
- Rule-Based JOIN Optimization Foundation
- Persistent Table Schema Recovery
- Atomic Table Catalog Recovery
- Corrupted Table File Detection
- Restart-Safe Table Metadata Recovery
- 512-byte Binary Table Headers
- Binary Table Header Serialization / Deserialization
- Binary Header Integrity Validation
- Header / Schema Cross-Validation
- Persistent Table IDs
- Restart-Safe Table ID Allocation
- Persistent Row Count Mutation
- Persistent First / Last Data Page Metadata Mutation
- Read-Back Header Verification
- Failure-Safe Header Updates
- TableManager Metadata Mutation API

---

# Sprint 00-20 — Persistent Table Metadata Mutation & TableManager Integration

Sprint `00-20` builds directly on the binary table-header foundation introduced in Sprint `00-19`.

The binary header already contained fields such as `rowCount`, `firstDataPageId`, and `lastDataPageId`; Sprint `00-20` turns those fields into safely mutable, persistent storage metadata and exposes them through `TableManager`.

## Main Features

- Immutable `TableHeader` mutation model
- `rowCount` update support
- `rowCount` increment / decrement support
- Overflow and negative-count protection
- First / last data page range mutation
- Atomic page-range validation
- Empty data-page range represented as `-1 / -1`
- Persistent metadata updates through `TableHeaderIO`
- Read → mutate → validate → write → read-back verification
- Schema-preserving header rewrite
- File-size preservation during header updates
- Invalid mutation leaves physical file unchanged
- Missing / truncated / corrupted file protection
- Restart-safe metadata recovery
- `TableManager` integration for storage-facing metadata operations
- Table-name normalization during metadata updates
- Full regression verification

---

## Persistent Metadata Architecture

```text
Record / Storage Layer
        │
        ▼
   TableManager
        │
        ├── getTableHeader()
        ├── updateTableRowCount()
        ├── incrementTableRowCount()
        ├── decrementTableRowCount()
        ├── updateTableDataPageRange()
        └── clearTableDataPageRange()
        │
        ▼
TableHeaderUpdater
        │
        ▼
TableHeaderValidator
        │
        ▼
TableHeaderIO
        │
        ▼
     .tbl File
```

The upper storage layers do not need to know binary field offsets or physical header layout details.

---

## Metadata Mutation Flow

```text
.tbl File
   │
   ▼
TableHeaderIO.read()
   │
   ▼
Current TableHeader
   │
   ▼
TableHeaderUpdater
   │
   ▼
Validation
   │
   ▼
TableHeaderIO.write()
   │
   ▼
TableHeaderIO.read()
   │
   ▼
Read-Back Verification
```

If validation fails, the write step is never reached and the physical file remains unchanged.

---

## TableManager Metadata API

```java
TableHeader header =
        tableManager.getTableHeader("users");

tableManager.updateTableRowCount(
        "users",
        25L
);

tableManager.incrementTableRowCount(
        "users"
);

tableManager.decrementTableRowCount(
        "users"
);

tableManager.updateTableDataPageRange(
        "users",
        100L,
        105L
);

tableManager.clearTableDataPageRange(
        "users"
);
```

This keeps header persistence details encapsulated inside the table-storage layer.

---

## Physical Table File Format

The physical layout introduced in Sprint `00-19` remains compatible:

```text
Offset     Size       Field
------------------------------------------------
0          4          Magic Number
4          2          Format Version
6          2          Header Size
8          8          Table ID
16         2          Table Name Length
18         255        Table Name
273        4          Column Count
277        8          Row Count
285        8          First Data Page ID
293        8          Last Data Page ID
301        8          Schema Offset
309        4          Flags
313        199        Reserved
------------------------------------------------
Total                 512 bytes
```

The UTF-8 schema still begins after the fixed binary header.

```text
0
│
├── Binary Table Header
│   512 bytes
│
512
│
├── YEKDB_TABLE
├── version=1
├── tableName=users
├── columnCount=3
├── createdAt=...
├── columns=
├── id:INT
├── username:STRING
└── active:BOOLEAN
```

`FORMAT_VERSION` remains `1` because Sprint `00-20` does not introduce an incompatible physical layout change.

---

## Safety & Integrity Guarantees

Sprint `00-20` verifies that:

- Row count cannot become negative
- Row count increment cannot overflow `Long.MAX_VALUE`
- Data page IDs must both be `-1` or both be valid non-negative IDs
- First data page ID cannot be greater than last data page ID
- Invalid mutation attempts do not modify the table file
- Corrupted headers are rejected
- Truncated headers are rejected
- Missing table files are rejected
- Header rewrites do not truncate the schema region
- Header rewrites do not change physical file size
- Immutable metadata fields remain unchanged during mutation
- Persisted metadata survives manager recreation and catalog reload

---

## Key Components

### Binary Header Foundation

```text
com.yekdb.storage.table.header
│
├── TableHeader.java
├── TableHeaderConstants.java
├── TableHeaderFile.java
├── TableHeaderIO.java
├── TableHeaderSerializer.java
├── TableHeaderValidator.java
├── TableIdAllocator.java
├── TableHeaderUpdater.java
└── TableHeaderUpdateException.java
```

### Updated Integration

```text
com.yekdb.storage.table.TableManager
```

`TableManager` now acts as the main storage-facing API for persistent table metadata mutation.

---

## Table Recovery Architecture

```text
Database Directory
        │
        ├── users.tbl
        ├── orders.tbl
        └── products.tbl
                │
                ▼
         TableHeaderIO
                │
                ▼
         Binary TableHeader
                │
                ▼
          schemaOffset
                │
                ▼
     TableFileMetadataReader
                │
                ▼
        TableRecoveryEntry
           │             │
           ▼             ▼
         Table      TableMetadata
           │             │
           └──────┬──────┘
                  ▼
             TableCatalog
                  │
                  ▼
             TableManager
```

Catalog recovery remains atomic: recovered tables are first loaded into a temporary catalog and the active catalog is replaced only after successful recovery.

---

## Testing

Sprint `00-20` adds coverage for:

- Row count mutation
- Immutable header behavior
- Negative row count rejection
- Row count overflow
- Data page range mutation
- Invalid page-range rejection
- Partial page-range rejection
- Header persistence
- Increment / decrement persistence
- Schema preservation
- Physical file-size preservation
- Read-back verification
- Missing table files
- Corrupted headers
- Truncated headers
- Failure-safe disk behavior
- Sequential metadata mutation
- `TableManager` metadata API
- Missing-table manager operations
- Table-name normalization
- Restart / reopen recovery

Current full project regression:

```bash
mvn clean test
```

Result:

```text
Tests run: 1066
Failures: 0
Errors: 0

BUILD SUCCESS
```

Compilation:

```bash
mvn clean compile
```

Result:

```text
BUILD SUCCESS
```

---

## Sprint History

| Sprint | Module | Status |
|---|---|---|
| 00-03 | Core / Storage Foundation | ✅ |
| 00-04 | Record & Storage Architecture | ✅ |
| 00-05 | Physical Storage Engine | ✅ |
| 00-06 | Database Management | ✅ |
| 00-07 | Table Management | ✅ |
| 00-08 | Record Management | ✅ |
| 00-09 | Index Management | ✅ |
| 00-10 | Query Execution Foundation | ✅ |
| 00-11 | SELECT / WHERE Execution | ✅ |
| 00-12 | Query Engine Expansion | ✅ |
| 00-13 | Query Processing Improvements | ✅ |
| 00-14 | Advanced SQL Operations | ✅ |
| 00-15 | JOIN Foundation | ✅ |
| 00-16 | Advanced JOIN Operations | ✅ |
| 00-17 | Query Engine Improvements | ✅ |
| 00-18 | Persistent Table Catalog & Schema Recovery | ✅ |
| 00-19 | Binary Table Header | ✅ |
| **00-20** | **Persistent Table Metadata Mutation & TableManager Integration** | **✅** |

---

## Next Steps

Future development can now build on a persistent and safely mutable table-metadata layer.

Possible next areas include:

- Physical record/page integration with persistent metadata
- Data page allocation lifecycle
- Free-space management
- RecordManager responsibility refactoring
- Persistent indexes
- Persistent system catalog
- Transaction management
- Write-Ahead Logging
- Recovery improvements
- Query optimization / EXPLAIN
- Client-server architecture

---

## Build

```bash
mvn clean compile
```

## Test

```bash
mvn clean test
```

---

**YEKDB — Built from scratch to understand how database systems work internally.**
