<div align="center">

# YEKDB

### A relational database management system built from scratch in Java for education and research

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Maven](https://img.shields.io/badge/Maven-Build-blue?logo=apachemaven)
![Tests](https://img.shields.io/badge/Tests-1084%2F1084-success)
![Status](https://img.shields.io/badge/Status-Active%20Development-brightgreen)

</div>

---

## About the Project

**YEKDB** is a database management system developed from scratch in Java to explore and implement the internal architecture of a relational DBMS.

The project goes beyond executing SQL-like statements. It covers physical file management, page-based storage, record serialization, table metadata, indexing, query parsing, expression evaluation, JOIN processing, aggregation, and query optimization.

Development follows a sprint-based workflow. The current milestone is **00-21 — RecordManager Refactor / Physical Record-Page Integration**, which is now complete.

---

## Current Status

- **Sprint:** `00-21`
- **Sprint Name:** `RecordManager Refactor / Physical Record-Page Integration`
- **Build:** Successful
- **Compile:** Successful
- **Package:** Successful
- **Tests:** **1084 / 1084 passing**
- **Status:** Active Development

---

## 00-21 Sprint Summary

Sprint 00-21 refactored the relationship between the record layer and the physical page layer. `RecordManager` can now work with explicit physical record addresses while preserving the existing logical record API.

### Completed Work

- Added `RecordId(pageId, slotId)` as the physical record identifier.
- Added `RecordLocation` to represent page, slot, offset, serialized size, and record information.
- Preserved logical `long recordId` APIs while introducing physical `RecordId` APIs.
- Added physical insert support.
- Added physical read support.
- Added physical update support.
- Added physical delete support.
- Implemented tombstone-based deletion.
- Preserved physical slot stability after deletes.
- Centralized physical page and slot validation.
- Added validation for null IDs, missing pages, missing slots, and deleted records.
- Added wrong-`PageType` validation.
- Hardened page/slot boundary checks.
- Removed redundant page reads from physical lookup paths.
- Cleaned up `RecordId` and hardened `RecordLocation` validation.
- Integrated the index layer with the physical `RecordId` model.
- Preserved `RecordPointer` as a backward-compatible compatibility layer.
- Kept the dependency direction as Index → Storage.
- Completed the full regression suite successfully.

---

## Record Addressing Model

YEKDB distinguishes between logical and physical record identities.

```text
Logical Record ID
────────────────────────
long recordId

Stable logical identity of a record.


Physical Record ID
────────────────────────
RecordId(pageId, slotId)

Physical location of a record inside storage.
```

Example:

```java
RecordId physicalId =
        recordManager.findPhysicalRecordId(15);

Record record =
        recordManager.readRecord(physicalId);
```

When a physical `RecordId` is available, `RecordManager` can access the target page directly instead of scanning every page in storage.

---

## Tombstone Delete

Physical records are not immediately removed from the page payload when deleted. Instead, YEKDB uses a tombstone flag.

```text
Before

Page 4
┌────────┬────────┬────────┬────────┐
│ Slot 0 │ Slot 1 │ Slot 2 │ Slot 3 │
│   A    │   B    │   C    │   D    │
└────────┴────────┴────────┴────────┘


After deleting B

Page 4
┌────────┬─────────────┬────────┬────────┐
│ Slot 0 │   Slot 1    │ Slot 2 │ Slot 3 │
│   A    │ B [DELETED] │   C    │   D    │
└────────┴─────────────┴────────┴────────┘
```

Therefore:

```text
C → RecordId(4, 2)
D → RecordId(4, 3)
```

remain unchanged.

This prevents physical identifiers of following records from shifting after a delete.

---

## RecordManager API

### Logical API

```java
Record insert(Row row);

Record getRecord(long recordId);

Row getRow(long recordId);

void update(
        long recordId,
        Row newRow
);

void delete(long recordId);
```

### Physical API

```java
RecordId insertWithLocation(Row row);

RecordLocation insertAndLocate(Row row);

Record readRecord(RecordId recordId);

RecordLocation locateRecord(RecordId recordId);

RecordId findPhysicalRecordId(long recordId);

void update(
        RecordId recordId,
        Row newRow
);

void delete(RecordId recordId);
```

---

## Index / Record Integration

The index layer is now compatible with the physical `RecordId` addressing model.

```text
Index Key
    │
    ▼
RecordId(pageId, slotId)
    │
    ▼
RecordManager
    │
    ▼
Physical Page
    │
    ▼
Record
```

The existing `RecordPointer` abstraction is preserved for backward compatibility with existing APIs, tests, and code paths.

The intended dependency direction remains:

```text
Index
  │
  ▼
Storage / RecordId
```

`RecordManager` does not depend on the index package.

---

## Storage Architecture

```text
Row
 │
 ▼
RowSerializer
 │
 ▼
Record
 │
 ▼
RecordSerializer
 │
 ▼
RecordManager
 │
 ├──────────────► RecordId
 │                    │
 │                    ├─ pageId
 │                    └─ slotId
 │
 ├──────────────► RecordLocation
 │                    │
 │                    ├─ Page
 │                    ├─ offset
 │                    ├─ serializedSize
 │                    └─ slotId
 │
 ▼
Page
 │
 ▼
PageManager
 │
 ▼
DataFile
 │
 ▼
Disk
```

---

## Page Structure

YEKDB stores physical records inside fixed-size pages.

The page header tracks information such as:

```text
Page ID
Page Type
Record Count
Used Bytes
Next Page ID
```

`RecordManager` walks serialized records inside the page payload to resolve physical slot locations.

---

## Storage Layer

The storage layer currently contains components such as:

```text
com.yekdb.storage
├── StorageEngine
│
├── file
│   ├── DataFile
│   └── DatabaseHeader
│
├── record
│   ├── Record
│   ├── RecordId
│   ├── RecordLocation
│   ├── RecordManager
│   ├── RecordSerializer
│   ├── Row
│   ├── RowSerializer
│   │
│   └── page
│       ├── Page
│       ├── PageHeader
│       ├── PageManager
│       ├── PageSerializer
│       └── PageType
│
└── table
    ├── Column
    ├── DataType
    ├── Table
    ├── TableCatalog
    ├── TableManager
    ├── TableMetadata
    │
    └── header
        ├── TableHeader
        ├── TableHeaderConstants
        ├── TableHeaderFile
        ├── TableHeaderIO
        ├── TableHeaderSerializer
        ├── TableHeaderUpdater
        ├── TableHeaderValidator
        └── TableIdAllocator
```

---

## Query Engine

The query layer separates SQL-like parsing, expression evaluation, execution, and optimization into dedicated components.

### Core Statements

- `CREATE DATABASE`
- `DROP DATABASE`
- `USE`
- `CREATE TABLE`
- `DROP TABLE`
- `INSERT`
- `SELECT`
- `UPDATE`
- `DELETE`

### Expression Support

- Comparison expressions
- Logical expressions
- `NOT`
- `BETWEEN`
- `IN`
- `LIKE`
- Qualified column resolution

### SELECT Features

- `WHERE`
- `ORDER BY`
- `LIMIT`
- `FETCH`
- `GROUP BY`
- `HAVING`
- Aggregate expressions

### JOIN Support

- `INNER JOIN`
- `LEFT JOIN`
- `RIGHT JOIN`
- `FULL JOIN`
- Multiple JOIN chains
- JOIN + WHERE
- JOIN + GROUP BY
- JOIN + HAVING
- JOIN + aggregate expressions
- JOIN optimization

---

## Query Execution Architecture

```text
SQL
 │
 ▼
SqlTokenizer
 │
 ▼
SqlParser
 │
 ▼
Statement
 │
 ▼
StatementCommandMapper
 │
 ▼
Command
 │
 ▼
QueryExecutor
 │
 ├── SelectExecutor
 ├── InsertExecutor
 ├── UpdateExecutor
 ├── DeleteExecutor
 ├── JoinExecutor
 ├── MultiJoinExecutor
 ├── GroupByExecutor
 ├── AggregateExecutor
 ├── OrderByExecutor
 └── LimitExecutor
 │
 ▼
Storage / Table Layer
```

---

## Query Optimization

YEKDB contains dedicated components for query and JOIN planning.

```text
query.optimizer
├── JoinExecutionContext
├── JoinOptimizationResult
├── JoinOptimizationRule
├── JoinOptimizer
├── QueryOptimizer
├── QueryPlan
└── QueryPlanType
```

This keeps execution behavior separate from optimization decisions.

---

## Index Layer

The index subsystem includes the following core components:

```text
com.yekdb.index
├── Index
├── IndexEntry
├── IndexIdentifierValidator
├── IndexManager
├── IndexMetadata
├── IndexType
├── RecordPointer
│
└── exception
    ├── DuplicateIndexException
    ├── DuplicateIndexKeyException
    ├── IndexNotFoundException
    └── InvalidIndexException
```

As of sprint 00-21, the index addressing model is compatible with storage-layer `RecordId` values while retaining `RecordPointer` compatibility.

---

## Physical Validation

Physical record access is validated before an operation is executed.

```text
RecordId
   │
   ▼
Page exists?
   │
   ▼
Correct PageType?
   │
   ▼
Slot within bounds?
   │
   ▼
Record location valid?
   │
   ▼
Operation
```

Covered edge cases include:

- Null `RecordId`
- Missing physical page
- Missing physical slot
- Slot equal to `recordCount`
- Slot greater than `recordCount`
- Wrong page type
- Reading a deleted physical record
- Updating a deleted physical record
- Deleting an already deleted physical record
- Null row during physical update

---

## Test Status

Current full regression result:

```text
Tests run: 1084
Failures : 0
Errors   : 0
Skipped  : 0

BUILD SUCCESS
```

Major test areas include:

- Record serialization
- Row serialization
- Page management
- Page serialization
- Physical RecordId addressing
- Physical insert
- Physical read
- Physical update
- Tombstone delete
- Slot stability
- Page boundary validation
- Wrong PageType validation
- Database management
- Table management
- Index management
- SQL parsing
- Expression evaluation
- SELECT execution
- CRUD persistence
- JOIN execution
- Multiple JOIN chains
- GROUP BY / HAVING
- Aggregation
- Query optimization
- Integration and regression behavior

---

## Build

Compile the project with Maven:

```bash
mvn clean compile
```

Run the full test suite:

```bash
mvn test
```

Build the package:

```bash
mvn clean package
```

Current sprint status:

```text
compile  ✅
test     ✅ 1084/1084
package  ✅
```

---

## Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.yekdb
│   │       ├── config
│   │       ├── core
│   │       ├── database
│   │       ├── demo
│   │       ├── exception
│   │       ├── index
│   │       ├── logs
│   │       ├── query
│   │       └── storage
│   │
│   └── resources
│       └── yekdb.properties
│
└── test
    └── java
        └── com.yekdb
```

---

## Design Principles

YEKDB development follows several architectural principles:

- Keep storage, query, and index responsibilities separated.
- Hide physical storage details from higher layers where practical.
- Preserve backward compatibility during refactors when possible.
- Prefer small, testable components.
- Keep serialization formats explicit.
- Reject invalid or corrupted physical state instead of silently accepting it.
- Run the full regression suite at the end of each sprint.
- Build a reliable physical storage foundation before adding higher-level features.
- Keep dependency directions explicit and avoid circular coupling.

---

## Sprint History

YEKDB development progresses through small, controlled implementation sprints.

Recent milestones:

```text
00-17  ✅ Query / codebase stabilization
00-18  ✅ Storage development
00-19  ✅ Binary Table Header
00-20  ✅ Table Header / physical metadata continuation
00-21  ✅ RecordManager Refactor / Physical Record-Page Integration
```

### 00-21 Result

```text
Record
   │
   ▼
RecordManager
   │
   ├── Logical Record ID
   ├── Physical RecordId
   ├── Physical Insert
   ├── Physical Read
   ├── Physical Update
   ├── Tombstone Delete
   ├── Physical Validation
   └── Index RecordId Integration
   │
   ▼
Page / Storage
```

---

## Next Steps

Future storage and engine work may include:

- Slotted-page architecture
- Stable slot directory
- Free-space management
- Record relocation
- Further index/record physical lookup optimization
- Buffer pool and cache management
- Transaction infrastructure
- Write-ahead logging and recovery
- Concurrency control
- Query planner improvements

---

<div align="center">

**YEKDB — Building a Database Management System from Scratch in Java**

`00-21 complete • 1084/1084 tests passing`

</div>
