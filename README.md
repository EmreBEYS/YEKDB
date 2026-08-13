# YEKDB
### Yet Another Embedded Key Database

> An educational relational database management system built from scratch in Java 21, with its own physical storage and query execution architecture.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![JUnit](https://img.shields.io/badge/JUnit-5-green)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-lightgrey)
![Status](https://img.shields.io/badge/Status-Development-yellow)

---

# 📖 About the Project

**YEKDB** is an educational relational database management system developed without relying on an existing database engine in the background.

The project is not based on the source code or storage engine of systems such as:

- PostgreSQL
- MySQL
- SQLite

The main goal is to understand how modern database systems work internally by designing and implementing their core components independently.

The project focuses on areas such as:

- Physical Storage Engine
- Page Management
- Record Management
- Table Management
- Index Management
- SQL Parser
- Expression Engine
- Query Execution Engine
- CRUD Operations
- Transaction Management
- Client / Server Architecture

YEKDB can currently execute SQL text through its parser, mapper, command, and execution layers; persist INSERT, UPDATE, and DELETE operations to its own physical `.data` files; execute advanced SELECT queries with filtering, grouping, aggregation, ordering, and result limiting; and execute single-table INNER JOIN queries end-to-end.

---

# 🚀 Sprint 00-15 Status

Sprint 00-15 introduces the first relational JOIN execution foundation of YEKDB.

## Completed JOIN Capabilities

| Feature | Status |
|---|---|
| INNER JOIN | ✅ |
| JOIN shorthand as INNER JOIN | ✅ |
| Qualified column references | ✅ |
| Table aliases in JOIN queries | ✅ |
| Column-to-column ON conditions | ✅ |
| Nested Loop Join execution | ✅ |
| JOIN + WHERE | ✅ |
| JOIN + SELECT projection | ✅ |
| JOIN + ORDER BY | ✅ |
| JOIN + LIMIT / FETCH | ✅ |
| Ambiguous column detection | ✅ |
| SQL Parser integration | ✅ |
| SelectStatement JOIN preservation | ✅ |
| QueryExecutor JOIN wiring | ✅ |
| End-to-end JOIN integration tests | ✅ |
| Maven regression verification | ✅ — 842 tests passed |

### Sprint 00-15 Scope

Sprint 00-15 intentionally focuses on a stable **single INNER JOIN foundation**.

Currently supported:

```sql
SELECT e.name, d.name
FROM employee e
INNER JOIN department d
ON e.department_id = d.id
WHERE d.name = 'IT';
```

The shorter SQL form is also supported:

```sql
SELECT e.name, d.name
FROM employee e
JOIN department d
ON e.department_id = d.id;
```

The following JOIN capabilities are intentionally deferred to later work:

- LEFT JOIN
- RIGHT JOIN
- FULL JOIN
- Multiple JOIN chains
- JOIN + GROUP BY / HAVING
- JOIN + aggregate expressions
- Advanced JOIN optimization

---

# 🧠 Current Query Architecture

SQL statements are not sent directly to an executor. The parsed statement model is preserved through the mapping and command layers so advanced SELECT and JOIN information is available when execution begins.

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
 ├── SelectMapper
 ├── UpdateMapper
 └── DeleteMapper
 │
 ▼
Command
 │
 ▼
QueryExecutor
 │
 ├── InsertExecutor
 ├── UpdateExecutor
 ├── DeleteExecutor
 └── SelectExecutor
        │
        ├── JoinExecutor
        │      └── Nested Loop INNER JOIN
        │
        ├── ExpressionEvaluator
        ├── WHERE
        ├── GROUP BY
        ├── AggregateExecutor
        ├── HAVING
        ├── OrderByExecutor
        └── LIMIT / FETCH
 │
 ▼
QueryResult / Storage Layer
```

For SELECT statements, `SelectCommand` preserves the complete `SelectStatement`. Sprint 00-15 extends that model with JOIN clauses, allowing the parser-generated JOIN definition to reach `QueryExecutor` and `SelectExecutor` without being rebuilt or lost.

For JOIN queries, `QueryExecutor` loads both the left and right tables from `QueryDataSource` and dispatches execution to the JOIN-aware `SelectExecutor` pipeline.

---

# 🗄️ Physical Storage

YEKDB separates table schema information from physical row storage.

Example:

```text
users.tbl
users.data
```

### `.tbl`

Stores table schema information.

Example:

```text
id INT
name STRING
age INT
active BOOLEAN
```

### `.data`

Stores physical table records using the page-based storage architecture.

Records are not stored using Java object serialization or an external database engine.

YEKDB uses its own:

- Page
- Record
- Row
- RecordSerializer
- PageManager
- RecordManager

infrastructure.

---

# ➕ INSERT

Sprint 00-12 introduces real physical INSERT execution.

Example:

```sql
INSERT INTO users
(id, name, age, active)
VALUES
(1, 'Emre', 21, true);
```

Execution flow:

```text
INSERT SQL
   ↓
InsertStatement
   ↓
InsertCommand
   ↓
InsertExecutor
   ↓
Row
   ↓
RecordManager.insert()
   ↓
users.data
```

INSERT execution validates:

- target table
- column names
- duplicate columns
- value types
- physical schema order

before writing the row into storage.

---

# ✏️ UPDATE

YEKDB can update persisted records directly through SQL.

Example:

```sql
UPDATE users
SET age = 22,
    active = false
WHERE id = 1;
```

Execution flow:

```text
UPDATE SQL
   ↓
UpdateStatement
   ↓
UpdateMapper
   ↓
ExpressionParser
   ↓
UpdateCommand
   ↓
UpdateExecutor
   ↓
WHERE Evaluation
   ↓
RecordManager.update()
   ↓
users.data
```

UPDATE operations are not limited to in-memory changes.

Updated rows are written back to the physical `.data` file, and the new values remain available after the Storage Engine is closed and reopened.

---

# 🗑️ DELETE

DELETE operations use a **logical delete** strategy.

Example:

```sql
DELETE FROM users
WHERE id = 2;
```

Execution flow:

```text
DELETE SQL
   ↓
DeleteStatement
   ↓
DeleteMapper
   ↓
ExpressionParser
   ↓
DeleteCommand
   ↓
DeleteExecutor
   ↓
WHERE Evaluation
   ↓
RecordManager.delete()
```

Deleted records are not immediately removed from the physical data file.

Instead, the record is marked as deleted and is no longer returned by:

```java
recordManager.getActiveRecords()
```

This approach provides a foundation for future systems such as:

- Transactions
- Rollback
- MVCC
- Vacuum / Compaction

---

# 🔍 Expression & Advanced SELECT Engine

The expression layer is shared by SELECT, UPDATE, and DELETE operations. Sprint 00-13 introduced recursive logical expressions; Sprint 00-14 extends predicate parsing and completes the advanced SELECT pipeline.

Supported logical and comparison features:

```text
=   !=   >   <   >=   <=
AND   OR   NOT
Parentheses
BETWEEN / NOT BETWEEN
IN / NOT IN
LIKE / NOT LIKE
ILIKE / NOT ILIKE
```

Advanced SELECT features:

```text
ORDER BY ... ASC | DESC
LIMIT n
FETCH FIRST n ROWS ONLY
FETCH NEXT n ROWS ONLY
GROUP BY column[, ...]
HAVING expression
COUNT(*) / COUNT(column)
SUM(column)
AVG(column)
MIN(column)
MAX(column)
```

Example:

```sql
SELECT department, COUNT(*) AS employee_count
FROM employees e
WHERE active = true
GROUP BY department
HAVING employee_count > 1
ORDER BY employee_count DESC
LIMIT 3;
```

Execution order:

```text
WHERE
  ↓
GROUP BY
  ↓
Aggregate
  ↓
HAVING
  ↓
ORDER BY
  ↓
LIMIT / FETCH
  ↓
QueryResult
```

---

# 🔗 INNER JOIN Foundation

Sprint 00-15 adds relational table combination to the query engine.

The initial implementation uses a **Nested Loop Join** strategy.

Example:

```sql
SELECT e.name, d.name
FROM employee e
INNER JOIN department d
ON e.department_id = d.id
WHERE d.name = 'IT';
```

Execution flow:

```text
SQL Text
   ↓
SqlTokenizer
   ↓
SqlParser
   ↓
SelectStatement
   ├── TableReference
   └── JoinClause
           ↓
StatementCommandMapper
   ↓
SelectMapper
   ↓
SelectCommand
   ↓
QueryExecutor
   ├── Load left table / rows
   └── Load right table / rows
           ↓
SelectExecutor
   ↓
JoinExecutor
   ↓
Nested Loop Join
   ↓
ExpressionEvaluator (ON)
   ↓
WHERE
   ↓
SELECT Projection
   ↓
ORDER BY
   ↓
LIMIT / FETCH
   ↓
QueryResult
```

### JOIN Model

Sprint 00-15 introduces and integrates:

```text
JoinType
JoinClause
ColumnExpression
ComparisonExpression column-to-column support
```

Qualified references such as:

```text
e.department_id
d.id
d.name
```

are preserved and resolved during JOIN execution.

### Ambiguous Columns

When both tables contain the same column name, unqualified references are rejected when the source cannot be determined safely.

Example:

```sql
SELECT id
FROM employee e
INNER JOIN department d
ON e.department_id = d.id;
```

Because both tables contain `id`, the query must use a qualified reference such as:

```sql
SELECT e.id
```

or:

```sql
SELECT d.id
```

### Current JOIN Execution Order

```text
INNER JOIN
   ↓
WHERE
   ↓
SELECT Projection
   ↓
ORDER BY
   ↓
LIMIT / FETCH
   ↓
QueryResult
```

---

# 🧪 CRUD Mutation Demo

At the end of Sprint 00-12, an end-to-end CRUD mutation demo was completed successfully.

Demo flow:

```text
CREATE DATABASE
       ↓
USE DATABASE
       ↓
CREATE TABLE
       ↓
INSERT × 3
       ↓
UPDATE
       ↓
DELETE
       ↓
Storage Engine Shutdown
       ↓
Storage Engine Reopen
       ↓
Physical Persistence Verification
```

The demo creates three records:

```text
Record ID: 0
Record ID: 1
Record ID: 2
```

Then executes:

```sql
UPDATE users
SET age = 22,
    active = false
WHERE id = 1;
```

Result:

```text
Updated row count: 1
```

After that:

```sql
DELETE FROM users
WHERE id = 2;
```

Result:

```text
Deleted row count: 1
```

After reopening the physical storage file:

```text
Active record count: 2
```

This confirms that INSERT, UPDATE, and DELETE operations are persisted through the physical storage layer.

---

# 🧪 Testing

YEKDB uses **JUnit 5** and Maven for component, parser, execution, integration, persistence, and regression verification.

Sprint 00-15 adds dedicated JOIN coverage at multiple levels:

```text
JoinExecutorTest                  12 / 12
SelectExecutorJoinTest             8 / 8
SqlParserJoinTest                  8 / 8
QueryExecutorJoinIntegrationTest   8 / 8
```

The JOIN integration suite verifies:

- INNER JOIN execution
- `JOIN` shorthand
- aliases
- qualified column references
- column-to-column ON conditions
- JOIN + WHERE
- SELECT * with joined tables
- exclusion of unmatched rows
- ambiguous column rejection
- missing JOIN table handling
- backward compatibility of non-JOIN SELECT execution
- complete SQL → Parser → Mapper → Command → QueryExecutor → SelectExecutor → JoinExecutor wiring

Final Maven regression verification:

```text
Tests run: 842
Failures: 0
Errors: 0
BUILD SUCCESS
```

Run the complete suite with:

```bash
mvn clean test
```

---

# 🏗️ Main Modules

The current project structure includes the following major modules:

```text
com.yekdb
│
├── core
├── database
├── table
├── storage
│   ├── page
│   └── record
│
├── index
│
└── query
    ├── command
    ├── datasource
    ├── evaluator
    ├── executor
    ├── expression
    ├── mapper
    ├── optimizer
    ├── parser
    ├── result
    └── statement
```

---

# 📦 Query Layer

The query layer is separated by responsibility.

## command

Contains executable query models.

```text
InsertCommand
UpdateCommand
DeleteCommand
SelectCommand
```

## statement

Contains SQL models produced by the parser.

```text
InsertStatement
UpdateStatement
DeleteStatement
SelectStatement
JoinClause
JoinType
TableReference
```

## parser

Processes SQL text.

```text
SqlTokenizer
SqlParser
ExpressionParser
```

## mapper

Transforms Statement objects into execution-ready Command objects.

```text
StatementCommandMapper
InsertMapper
UpdateMapper
DeleteMapper
SelectMapper
```

## executor

Contains the actual execution logic.

```text
QueryExecutor
InsertExecutor
UpdateExecutor
DeleteExecutor
SelectExecutor
JoinExecutor
TableScanExecutor
```

## expression

Contains the execution model used by WHERE conditions.

```text
Expression
ColumnExpression
ComparisonExpression
ComparisonOperator
LogicalExpression
LogicalOperator
NotExpression
```

---

# 📚 Sprint History

## Sprint 00-01

- Initial architecture
- Java 21
- Maven
- Git / GitHub
- Initial package structure

## Sprint 00-02

- Core Engine
- Storage Engine architecture

## Sprint 00-03

- Configuration
- Logger
- Page structure

## Sprint 00-04

- Page Serialization
- Page Manager
- Record foundation

## Sprint 00-05

- Physical Storage Engine
- DataFile
- DatabaseHeader
- Page persistence

## Sprint 00-06

- Database Management
- Database metadata
- Database lifecycle

## Sprint 00-07

- Table Management
- Column
- DataType
- TableCatalog
- TableManager

## Sprint 00-08

- Row
- RowSerializer
- RecordManager

## Sprint 00-09

- Index foundation
- RecordPointer
- IndexMetadata
- Index
- IndexManager

## Sprint 00-10

- Query Execution Foundation
- Command infrastructure
- ExecuteResult
- QueryExecutor foundation

## Sprint 00-11

- SELECT execution
- WHERE expression infrastructure
- Query evaluation
- Table scan
- Query optimizer foundation

## Sprint 00-12

- INSERT execution
- UPDATE execution
- DELETE execution
- SQL Parser integration
- Statement → Command mapping
- WHERE-based mutations
- Physical persistence
- Logical delete
- CRUD integration tests
- CRUD mutation demo

## Sprint 00-13

- Recursive WHERE Expression Engine
- AND / OR / NOT
- Parentheses and operator precedence
- Recursive parsing and evaluation
- SELECT / UPDATE / DELETE expression integration
- Persistence verification
- ExpressionEngineDemo

## Sprint 00-14

- BETWEEN / NOT BETWEEN
- IN / NOT IN
- LIKE / NOT LIKE / ILIKE / NOT ILIKE
- Table and column aliases
- ORDER BY ASC / DESC
- LIMIT / FETCH
- GROUP BY / HAVING
- COUNT / SUM / AVG / MIN / MAX
- Advanced SelectStatement model
- SelectMapper / SelectCommand preservation
- Final SelectExecutor pipeline
- Advanced SELECT integration tests
- 805-test Maven verification

## Sprint 00-15

- INNER JOIN foundation
- `JOIN` shorthand support
- JoinType / JoinClause model
- ColumnExpression qualified references
- Column-to-column comparison support
- Nested Loop JoinExecutor
- JOIN-aware SelectExecutor
- JOIN-aware SqlTokenizer / SqlParser
- QueryExecutor dual-table loading and dispatch
- Ambiguous column detection
- End-to-end SQL JOIN integration
- Backward compatibility verification
- 842-test Maven verification

---

# 🛣️ Roadmap

YEKDB is still under active development.

## Query Engine

- [x] SQL Tokenizer
- [x] SQL Parser
- [x] SELECT foundation
- [x] INSERT
- [x] UPDATE
- [x] DELETE
- [x] Basic WHERE
- [x] AND / OR / NOT extensions
- [x] BETWEEN / IN / LIKE / ILIKE
- [x] ORDER BY
- [x] LIMIT / FETCH
- [x] GROUP BY / HAVING
- [x] Aggregate Functions
- [x] INNER JOIN foundation
- [ ] LEFT / RIGHT / FULL JOIN
- [ ] Multiple JOIN chains

## Storage Engine

- [x] Page-based storage
- [x] Record persistence
- [x] Physical INSERT
- [x] Physical UPDATE
- [x] Logical DELETE
- [ ] Free Space Manager
- [ ] Deleted record compaction
- [ ] Buffer Pool

## Index

- [x] Index foundation
- [x] RecordPointer
- [x] Index metadata
- [ ] B+ Tree
- [ ] Index-assisted SELECT
- [ ] Index-assisted UPDATE / DELETE

## Transaction System

- [ ] BEGIN
- [ ] COMMIT
- [ ] ROLLBACK
- [ ] Write Ahead Logging
- [ ] Crash Recovery
- [ ] Isolation
- [ ] MVCC

## Database Features

- [ ] Constraints
- [ ] Primary Key
- [ ] Unique
- [ ] Foreign Key
- [ ] Views
- [ ] Triggers
- [ ] Stored Procedures
- [ ] Backup / Restore

## Client / Server

- [ ] TCP Server
- [ ] Multi-user architecture
- [ ] Authentication
- [ ] Role management
- [ ] Desktop client
- [ ] Remote connections

---

# ⚙️ Requirements

Recommended environment:

```text
JDK 21+
Apache Maven 3.x
Git
```

---

# 🔨 Build

Clone the repository:

```bash
git clone https://github.com/EmreBEYS/YEKDB.git
```

Enter the project directory:

```bash
cd YEKDB
```

Run tests:

```bash
mvn clean test
```

Build the project:

```bash
mvn clean package
```

---

# 🎯 Project Goal

The goal of YEKDB is not to use an existing database engine, but to implement and understand the complete database execution path:

```text
SQL
↓
Parser
↓
Execution Engine
↓
Record Manager
↓
Page Manager
↓
Storage Engine
↓
Disk
```

The project therefore focuses heavily on:

- data structures
- file systems
- algorithms
- query execution
- disk management
- indexing
- transaction systems

---

# 📄 License

This project is developed for educational and research purposes.

See:

```text
LICENSE
```

for license information.

---

# 👨‍💻 Developer

**Yunus Emre KUL**

Computer Engineering

GitHub:

**EmreBEYS**

---

> YEKDB is built from scratch to understand how database management systems work internally.
