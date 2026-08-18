# YEKDB
### Yet Another Embedded Key Database

> An educational relational database management system built from scratch in Java 21, with its own storage, SQL parsing, query execution, JOIN engine, aggregation pipeline, and rule-based JOIN optimization architecture.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![JUnit](https://img.shields.io/badge/JUnit-5-green)
![Tests](https://img.shields.io/badge/Tests-965%20Passing-brightgreen)
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
- Persistence and regression testing

YEKDB does **not** rely on PostgreSQL, MySQL, SQLite, or another database engine for its internal storage or query execution.
> Current development milestone: **Sprint 00-17 — Architecture Cleanup & Query Engine Refactoring**

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
- Prepare the architecture for future transactions, persistence improvements, and client/server support

---

## Current Status

**Current Sprint:** `00-18`  
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
- ORDER BY
- LIMIT
- FETCH
- BETWEEN
- IN
- LIKE / NOT LIKE / ILIKE
- GROUP BY
- HAVING
- JOIN Foundation
- INNER JOIN
- LEFT JOIN
- RIGHT JOIN
- FULL JOIN
- Multiple JOIN Chains
- JOIN + GROUP BY / HAVING
- JOIN + Aggregate Expressions
- Advanced JOIN Optimization Foundation
- Persistent Table Schema Recovery
- Atomic Table Catalog Recovery
- Corrupted Table File Detection
- Restart-Safe Table Metadata Recovery

---

## Sprint 00-18 — Persistent Table Catalog & Schema Recovery

Sprint `00-18` introduces persistent table schema recovery to YEKDB.

Previously, table definitions were registered inside the in-memory `TableCatalog`. Physical `.tbl` files remained on disk after shutdown, but a newly created `TableManager` started with an empty catalog.

With this sprint, YEKDB can reconstruct table schemas and metadata directly from physical table files.

### Main Features

- Physical `.tbl` file parsing
- Table schema reconstruction
- Column reconstruction
- DataType recovery
- TableMetadata recovery
- Creation timestamp preservation
- Metadata version preservation
- Physical filename validation
- Restart simulation support
- Multiple-table recovery
- Atomic catalog recovery
- Corrupted table file detection
- Recovery-safe DROP TABLE behavior
- Non-table file filtering
- Duplicate table protection after recovery

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

---

## Create / Recovery Lifecycle

```text
CREATE TABLE
     │
     ▼
   Table
     │
     ├──────────────► TableCatalog
     │
     ▼
   .tbl File
```

After a restart:

```text
.tbl File
    │
    ▼
TableFileMetadataReader
    │
    ▼
TableRecoveryEntry
    │
    ├── Table
    └── TableMetadata
           │
           ▼
      TableCatalog
```

---

## Atomic Catalog Recovery

Catalog recovery is performed using a temporary catalog.

```text
Disk
 │
 ▼
Temporary TableCatalog
 │
 ├── Recover table 1
 ├── Recover table 2
 └── Recover table 3
        │
        ▼
All files valid?
   │         │
  NO        YES
   │         │
   ▼         ▼
Abort     Replace
Recovery   Runtime Catalog
```

If one physical table file is corrupted, the existing runtime catalog is preserved.

This prevents partially recovered catalog states.

---

## Corruption Handling

YEKDB detects invalid physical table files using:

```java
CorruptedTableFileException
```

Validation includes:

- Invalid `YEKDB_TABLE` header
- Missing metadata fields
- Invalid metadata version
- Invalid column count
- Column count mismatch
- Invalid creation timestamp
- Invalid column definition
- Unsupported DataType
- Empty column schema
- Physical filename / table name mismatch
- Incomplete table files

---

## New Components — 00-18

```text
com.yekdb.table
│
├── TableFileMetadataReader.java
├── TableRecoveryEntry.java
└── TableManager.java              [UPDATED]

com.yekdb.table.exception
│
└── CorruptedTableFileException.java
```

Existing domain classes remained compatible with the new recovery architecture:

```text
Table.java
TableMetadata.java
TableCatalog.java
Column.java
DataType.java
```

No breaking changes were required.

---

## Recovery Example

```java
TableManager firstManager =
        new TableManager(databaseDirectory);

firstManager.createTable(
        "users",
        List.of(
                new Column("id", DataType.INT),
                new Column("username", DataType.STRING),
                new Column("active", DataType.BOOLEAN)
        )
);
```

After simulating a restart:

```java
TableManager secondManager =
        new TableManager(databaseDirectory);

secondManager.loadCatalog();

Table users =
        secondManager.getTable("users");
```

The `users` schema is reconstructed directly from:

```text
users.tbl
```

---

## Physical Table File Format

Current `.tbl` schema format:

```text
YEKDB_TABLE
version=1
tableName=users
columnCount=3
createdAt=2026-08-18T08:00:00
columns=
id:INT
username:STRING
active:BOOLEAN
```

Supported DataTypes:

```text
INT
LONG
DOUBLE
BOOLEAN
STRING
```

---

## Testing

Sprint `00-18` includes dedicated tests for:

- Valid `.tbl` file recovery
- Column recovery
- DataType recovery
- Metadata recovery
- Creation timestamp preservation
- Metadata version preservation
- Invalid magic header
- Invalid DataType
- Incorrect column count
- Filename mismatch
- Missing table file
- Restart simulation
- Multiple table recovery
- Empty database recovery
- Repeated catalog reload
- Atomic recovery
- DROP after recovery
- DROP persistence after restart
- Non-table file filtering
- Directory filtering
- Creating new tables after recovery
- Duplicate table rejection
- Metadata cleanup

Dedicated recovery test suites:

```text
TableFileMetadataReaderTest
TableManagerRecoveryTest
```

Current sprint-specific results:

```text
TableFileMetadataReaderTest  → 10 / 10 ✅
TableManagerRecoveryTest     → 15 / 15 ✅
```

Full project regression:

```bash
mvn clean test
```

Result:

```text
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
| **00-18** | **Persistent Table Catalog & Schema Recovery** | **✅** |

---

## Next Steps

Future YEKDB development will continue building on this recovery foundation.

Possible next areas include:

- Binary table headers
- Persistent system catalog
- Advanced metadata management
- RecordManager refactoring
- Persistent indexes
- Transaction management
- Write-Ahead Logging
- Query optimization
- EXPLAIN support
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