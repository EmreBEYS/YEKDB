# YEKDB
### Yet Another Embedded Key Database

> An educational relational database management system built from scratch in Java 21, with its own storage, SQL parsing, query execution, JOIN engine, aggregation pipeline, and rule-based JOIN optimization architecture.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![JUnit](https://img.shields.io/badge/JUnit-5-green)
![Tests](https://img.shields.io/badge/Tests-940%20Passing-brightgreen)
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

### Sprint 00-17

Sprint 00-17 focused on **codebase review, cleanup, responsibility separation, and Query Engine refactoring**.

The sprint was completed without changing the externally expected behavior of the system.

**Current test status:**

```text
940 / 940 tests passed
Compile successful
```

---

## Architecture

```text
com.yekdb
├── config
├── core
├── database
├── demo
├── exception
├── index
├── logs
├── query
├── storage
├── table
└── YekdbApplication.java
```

### Core Layers

```text
Application
    │
    ▼
Query Engine
    │
    ├── Parser
    ├── Expressions
    ├── Query Execution
    ├── JOIN Engine
    ├── Aggregation
    └── Optimizer
    │
    ▼
Database / Table / Index Management
    │
    ▼
Storage Engine
    │
    ├── DataFile
    ├── Page Management
    ├── Record Management
    └── Serialization
```

---

# Features

## Database Management

Supported database-level operations include:

```sql
CREATE DATABASE university;
USE university;
DROP DATABASE university;
```

Database management includes:

- Database creation
- Database selection
- Database deletion
- Database listing
- Metadata persistence
- Database metadata integrity checks
- Centralized database name validation

---

## Table Management

Example:

```sql
CREATE TABLE students (
    id INTEGER,
    name STRING,
    age INTEGER
);
```

Current table infrastructure includes:

- Table creation
- Table deletion
- Column definitions
- Table catalog
- Table metadata
- Table name validation
- Column name validation
- Duplicate column detection
- Immutable table schema lists

Qualified column names are also supported internally for JOIN results:

```text
students.id
departments.name
e.id
d.name
```

---

## Storage Engine

The physical storage layer includes:

```text
StorageEngine
├── DataFile
├── DatabaseHeader
├── PageManager
├── PageSerializer
├── RecordManager
├── RecordSerializer
├── Row
└── RowSerializer
```

Implemented concepts include:

- Fixed-size database pages
- Database headers
- Page serialization/deserialization
- Record storage
- Record lookup
- Record update
- Record deletion
- Row serialization
- Persistent page count management
- Storage lifecycle management

Default page size:

```text
4096 bytes
```

---

## Index Infrastructure

YEKDB currently provides an in-memory index abstraction that prepares the architecture for future persistent tree indexes.

Supported index types include:

- PRIMARY
- UNIQUE
- NON_UNIQUE

Index functionality includes:

- Index creation
- Index deletion
- Index lookup
- RecordPointer mapping
- Multiple pointers for non-unique keys
- Duplicate-key protection for unique indexes
- Index metadata
- Database/table/column association
- Identifier normalization

Example structure:

```text
Index<K>
    └── Map<K, List<RecordPointer>>
```

A persistent B+ Tree implementation is planned for a future milestone.

---

# Query Engine

The Query Engine is one of the largest parts of YEKDB.

```text
query/
├── command
├── datasource
├── evaluator
├── executor
├── expression
├── optimizer
├── parser
├── result
└── statement
```

The engine follows a general pipeline:

```text
SQL
 │
 ▼
Tokenizer
 │
 ▼
Parser
 │
 ▼
Statement / Expression Model
 │
 ▼
QueryExecutor
 │
 ▼
Specialized Executors
 │
 ▼
Query Result
```

---

## SELECT

Basic queries:

```sql
SELECT * FROM employees;
```

```sql
SELECT name, salary
FROM employees
WHERE salary > 30000;
```

Supported SELECT functionality includes:

- Column projection
- `WHERE`
- Aliases
- Qualified column resolution
- Expression evaluation
- Result column generation

---

## Filtering Expressions

Implemented expression support includes:

```sql
WHERE age > 18
```

```sql
WHERE age BETWEEN 18 AND 30
```

```sql
WHERE city IN ('Malatya', 'Elazig')
```

```sql
WHERE name LIKE 'Em%'
```

Supported operators/features include:

- Comparison expressions
- Logical expressions
- `BETWEEN`
- `IN`
- `LIKE`
- `ILIKE`
- `NOT LIKE`
- Literal expressions
- Column expressions
- Parenthesized expression parsing

---

## ORDER BY and LIMIT

Example:

```sql
SELECT *
FROM employees
ORDER BY salary DESC
LIMIT 10;
```

The Query Engine includes support for ordering and result limiting.

---

# JOIN Engine

YEKDB currently supports:

```text
INNER JOIN
LEFT JOIN
RIGHT JOIN
FULL JOIN
```

Example:

```sql
SELECT e.name, d.name
FROM employees e
INNER JOIN departments d
    ON e.department_id = d.id;
```

Multiple JOIN chains are also supported.

Example:

```sql
SELECT e.name, d.name, c.name
FROM employees e
JOIN departments d
    ON e.department_id = d.id
JOIN companies c
    ON d.company_id = c.id;
```

Additional JOIN capabilities include:

- Qualified column resolution
- JOIN row assembly
- Outer JOIN null-row generation
- Multiple JOIN chains
- JOIN projection
- JOIN + aggregation
- JOIN + GROUP BY
- JOIN + HAVING
- JOIN optimization infrastructure

---

# GROUP BY, Aggregation and HAVING

Supported aggregate operations include infrastructure for expressions such as:

```sql
SELECT department_id, COUNT(*)
FROM employees
GROUP BY department_id;
```

```sql
SELECT department_id, AVG(salary)
FROM employees
GROUP BY department_id
HAVING AVG(salary) > 30000;
```

Aggregate execution supports:

- `COUNT`
- `SUM`
- `AVG`
- `MIN`
- `MAX`
- GROUP BY
- HAVING
- JOIN + aggregate
- Multiple JOIN + aggregate

---

# Sprint 00-17 Refactoring

Sprint 00-17 was dedicated primarily to reducing architectural debt.

## Codebase Cleanup

Removed obsolete or unused skeleton classes and packages from earlier development stages.

Examples included unused placeholders for:

- old model classes
- old command abstractions
- old catalog placeholders
- incomplete transaction placeholders
- incomplete free-space placeholders
- obsolete index placeholders
- unused storage exception types

Demo classes were consolidated under:

```text
com.yekdb.demo
```

---

## Database Refactoring

Changes included:

- Centralized database name validation
- Metadata/name consistency checks
- Reduced duplicated validation logic
- Cleaner database manager responsibilities
- Demo relocation

---

## Table Refactoring

Changes included:

- `TableNameValidator`
- `ColumnNameValidator`
- Duplicate-column handling
- Immutable table column snapshots
- Reduced repeated validation
- Cleaner `TableManager`
- Cleaner `TableCatalog`
- Unified identifier normalization

---

## Index Refactoring

Changes included:

- Centralized index identifier validation
- Case-normalized logical index names
- Improved metadata validation
- Cleaner collection handling
- Safer index/table/column matching
- Demo relocation

---

## Storage Refactoring

Changes included:

- Removed obsolete `DataFileException`
- Consolidated page-count ownership
- Reduced duplicate database-header handling
- Simplified immutable row snapshots
- Demo relocation

---

## Core / Config / Logging Cleanup

Changes included:

- Shared page-size constant usage
- Configuration path normalization
- Configuration string normalization
- Improved log-message null safety
- Removed obsolete storage-level exception
- Cleaned engine documentation

---

# Query Refactoring — Phase 1 to Phase 6

The Query Engine was refactored incrementally to protect existing behavior.

Every phase was validated against the complete test suite.

## Phase 1 — Cleanup

- Removed obsolete query mapper code
- Relocated query demo classes
- Preserved execution behavior

## Phase 2A — Column Resolution

Introduced:

```text
SelectColumnResolver
```

Responsibilities extracted from `SelectExecutor` included:

- column lookup
- case-insensitive resolution
- qualified-name normalization
- result value lookup

## Phase 2B — JOIN Projection

Introduced:

```text
SelectJoinProjectionExecutor
```

Responsibilities extracted included:

- JOIN projection
- multiple-JOIN projection
- qualified JOIN column processing

## Phase 2C — Aggregation Pipeline

Introduced:

```text
SelectAggregateExecutor
```

Responsibilities extracted included:

- aggregate SELECT flow
- GROUP BY
- HAVING
- JOIN aggregates
- multiple-JOIN aggregates
- aggregate result generation

This reduced `SelectExecutor` from more than 3700 lines to approximately 1100 lines.

---

## Phase 3 — QueryExecutor

Introduced specialized support classes:

```text
TableMutationExecutionSupport
SelectCommandExecutionSupport
ManagementCommandParser
```

This reduced the responsibilities of the main `QueryExecutor` while keeping its public execution API intact.

---

## Phase 4 — SQL Parser

Introduced:

```text
SqlTokenCursor
SqlLiteralParser
```

Responsibilities extracted included:

- token cursor management
- lookahead
- token matching
- token expectations
- literal conversion

The public parser API remained unchanged.

---

## Phase 5 — Expression and JOIN Internals

Introduced:

```text
ExpressionValueSupport
JoinRowAssembler
```

Responsibilities extracted included:

- expression value utilities
- numeric comparison support
- LIKE-pattern support
- JOIN row merging
- qualified JOIN row construction
- outer JOIN null-row construction

This significantly reduced both `ExpressionEvaluator` and `JoinExecutor`.

---

## Phase 6 — Aggregate Finalization

Introduced:

```text
AggregateValueSupport
JoinedAggregateExecutor
```

Responsibilities extracted included:

- aggregate numeric validation
- MIN/MAX comparison support
- JOIN aggregate execution
- aggregate value helpers

`AggregateExecutor` was reduced from more than 700 lines to roughly 200 lines.

---

# Refactoring Result

Several historically large classes were substantially reduced:

```text
SelectExecutor
    ~3720 lines
        ↓
    ~1165 lines

QueryExecutor
    ~1369 lines
        ↓
    ~816 lines

ExpressionEvaluator
    ~807 lines
        ↓
    ~482 lines

JoinExecutor
    ~895 lines
        ↓
    ~597 lines

AggregateExecutor
    ~723 lines
        ↓
    ~203 lines
```

The objective was not simply reducing line count. The primary goal was separating responsibilities while maintaining existing behavior.

---

# Testing

YEKDB uses JUnit-based automated tests throughout the codebase.

Current status after Sprint 00-17:

```text
Tests: 940
Passed: 940
Failed: 0
Errors: 0
```

Every major refactoring stage was verified before continuing to the next phase.

---

# Technology Stack

- Java 21
- Maven
- JUnit
- IntelliJ IDEA
- Git
- GitHub

Development environments include Windows and macOS, with Linux support considered as the project evolves.

---

# Current Development Principles

YEKDB currently follows these principles:

- Keep domain objects responsible for their own invariants
- Avoid duplicated validation logic
- Separate parsing from execution
- Separate query orchestration from specialized execution
- Keep storage responsibilities clearly owned
- Prefer immutable snapshots when returning collections
- Refactor incrementally
- Preserve behavior through automated tests

---

# Roadmap

Planned future areas include:

### SQL Engine

- Subqueries
- `EXISTS` / `NOT EXISTS`
- More advanced expression support
- Additional SQL syntax coverage
- Query planning improvements

### Storage

- Persistent table catalog
- Binary table metadata
- Improved page allocation
- Free-space management
- Persistent indexes
- B+ Tree

### Transactions

- Transaction manager
- Commit / rollback
- Isolation foundations
- Write-ahead logging concepts
- Crash recovery foundations

### Database Objects

- Views
- Triggers
- Stored procedures
- Additional constraints

### Security

- Users
- Roles
- Permissions

### Client / Server

- Server process
- Network protocol
- Multiple concurrent clients
- Remote query execution

### Tooling

- CLI
- Administrative interface
- Improved logging and diagnostics
- Backup / restore

---

# Repository Philosophy

YEKDB is primarily a learning-oriented systems project.

The focus is not to replace production database systems. Instead, the repository is intended to demonstrate how database internals can be designed and implemented step by step while maintaining an increasingly structured architecture.

---

## Author

**Yunus Emre KUL**

Computer Engineering  
İnönü University

---

## Development Status

```text
Sprint 00-17
Architecture Cleanup & Query Engine Refactoring
Status: COMPLETED

Compile: SUCCESS
Tests: 940 / 940 PASSED
```
