# YEKDB
### Yet Another Embedded Key Database

> An educational relational database management system built from scratch in Java 21, with its own storage, SQL parsing, query execution, JOIN engine, aggregation pipeline, and rule-based JOIN optimization architecture.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![JUnit](https://img.shields.io/badge/JUnit-5-green)
![Tests](https://img.shields.io/badge/Tests-1013%20Passing-brightgreen)
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
> Current development milestone: **Sprint 00-19 — Binary Table Header**

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

**Current Sprint:** `00-19`  
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
- 512-byte Binary Table Headers
- Binary Table Header Serialization / Deserialization
- Binary Header Integrity Validation
- Header / Schema Cross-Validation
- Persistent Table IDs
- Restart-Safe Table ID Allocation

---

## Sprint 00-19 — Binary Table Header

Sprint `00-19` introduces a fixed binary table header format and integrates it into YEKDB's physical `.tbl` storage and recovery architecture.

Previously, table schema metadata was stored only as UTF-8 text inside physical table files.

With this sprint, every table file now starts with a **512-byte binary header** containing core physical metadata. The existing UTF-8 schema remains stored after the header and is located through the `schemaOffset` field.

### Main Features

- Fixed 512-byte binary table header
- Binary magic number and format version
- Persistent table ID
- UTF-8 table name serialization
- Column count persistence
- Row count persistence
- First / last data page metadata
- Schema offset persistence
- Header flags
- Reserved future metadata area
- Binary serialization / deserialization
- Header validation
- Corrupted header detection
- Physical header read / write support
- Binary-aware table creation
- Binary-aware catalog recovery
- Header / schema cross-validation
- Restart-safe table ID allocation
- Full regression coverage

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

---

## Create / Recovery Lifecycle

```text
CREATE TABLE
     │
     ▼
   Table
     │
     ▼
TableMetadata
     │
     ▼
TableIdAllocator
     │
     ▼
TableHeader
     │
     ▼
TableHeaderSerializer
     │
     ▼
512-byte Binary Header
     │
     ├──────────────► TableCatalog
     │
     ▼
UTF-8 Schema
     │
     ▼
   .tbl File
```

After a restart:

```text
.tbl File
    │
    ▼
TableHeaderIO
    │
    ▼
Binary Header Validation
    │
    ▼
schemaOffset
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
           │
           ▼
 TableIdAllocator Sync
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

YEKDB detects invalid physical table files and binary headers using:

```java
CorruptedTableFileException
CorruptedTableHeaderException
InvalidTableHeaderException
```

Validation includes:

- Invalid binary magic number
- Unsupported binary format version
- Invalid header size
- Truncated binary header
- Invalid table name length
- Invalid table metadata
- Invalid schema offset
- Missing metadata fields
- Invalid metadata version
- Invalid column count
- Header / schema table name mismatch
- Header / schema column count mismatch
- Invalid creation timestamp
- Invalid column definition
- Unsupported DataType
- Empty column schema
- Physical filename / table name mismatch
- Incomplete table files

---

## New Components — 00-19

```text
com.yekdb.storage.table.header
│
├── TableHeader.java
├── TableHeaderConstants.java
├── TableHeaderFile.java
├── TableHeaderIO.java
├── TableHeaderSerializer.java
├── TableHeaderValidator.java
└── TableIdAllocator.java
```

Updated components:

```text
TableManager.java
TableFileMetadataReader.java
TableFileMetadataReaderTest.java
TableManagerTest.java
```

Existing schema and catalog classes remain compatible with the binary header architecture:

```text
Table.java
TableMetadata.java
TableCatalog.java
TableRecoveryEntry.java
Column.java
DataType.java
```

No breaking changes were required to the logical table model.

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

Current `.tbl` layout:

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

The schema begins at the offset stored inside the header:

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
├── createdAt=2026-08-19T08:00:00
├── columns=
├── id:INT
├── username:STRING
└── active:BOOLEAN
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

Sprint `00-19` includes dedicated tests for:

- TableHeader model
- Header validation
- Binary serialization
- Binary deserialization
- Round-trip serialization
- UTF-8 table names
- Binary magic number validation
- Format version validation
- Header size validation
- Invalid table name length
- Reserved byte verification
- Physical header write / read
- Truncated header detection
- Corrupted header detection
- Binary header integration in `TableManager`
- Schema offset verification
- Binary-aware `TableFileMetadataReader`
- Header / schema integrity validation
- Persistent table ID allocation
- Restart-safe table ID continuation
- Catalog recovery after restart
- Full regression verification

Current full project regression:

```bash
mvn clean test
```

Result:

```text
Tests run: 1013
Failures: 0
Errors: 0
Skipped: 0

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
| **00-19** | **Binary Table Header** | **✅** |

---

## Next Steps

Future YEKDB development will continue building on the binary storage foundation introduced in Sprint `00-19`.

Possible next areas include:

- Persistent system catalog
- Advanced metadata management
- RecordManager refactoring
- Record / page integration with binary table metadata
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