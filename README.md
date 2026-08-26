# YEKDB
### Yet Another Embedded Key Database

> A relational database management system written from scratch in Java, built sprint by sprint to explore real DBMS internals.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Active%20Development-yellow)
![Tests](https://img.shields.io/badge/JUnit-1231%20Tests%20Passed-brightgreen)
![Sprint](https://img.shields.io/badge/Sprint-00--24-blueviolet)

---

## About

YEKDB (Yet Another Embedded Key Database) is an educational and research-oriented relational database management system implemented entirely from scratch in Java.

The project is not based on PostgreSQL, MySQL, SQLite, or another database codebase. Storage, record management, metadata, SQL processing, query execution, indexing, recovery-oriented structures, and the command-line interface are developed independently to understand how modern database systems are structured internally.

The long-term goal is a complete page-oriented database engine with durable storage, relational query execution, transactions, indexing, recovery, concurrency control, and client/server support.

---

## Current Capabilities

### Core & Storage

- Configuration and logging infrastructure
- Persistent `.ydb` database files
- Physical page architecture and page serialization
- Page headers and random page access
- Table management and persistent table metadata
- Binary table header format
- Table catalog and schema recovery
- Physical record/page integration
- Record identifiers based on physical page/slot locations
- Record insert/read/update/delete workflows
- Tombstone-based delete behavior
- RecordManager refactor and physical page integration

### Query Layer

- SQL tokenization and parsing infrastructure
- Statement / command mapping
- Query execution pipeline
- Predicate expressions
- `BETWEEN / NOT BETWEEN`
- `IN / NOT IN`
- `LIKE / NOT LIKE`
- `ILIKE / NOT ILIKE`
- Qualified columns and aliases
- `ORDER BY`
- `LIMIT`
- `FETCH`
- `GROUP BY`
- `HAVING`
- `COUNT / SUM / AVG / MIN / MAX`
- `INNER / LEFT / RIGHT / FULL JOIN`
- Multiple JOIN chains
- JOIN + filtering / grouping / aggregation combinations
- Rule-based JOIN optimization foundations
- Persistent storage-backed `SELECT`
- Projection and alias preservation in terminal result output
- DDL and DML execution through the interactive terminal
- `NOT NULL` constraint enforcement
- `UNIQUE` constraint enforcement
- `PRIMARY KEY` constraint enforcement
- Composite `UNIQUE` constraints
- Composite `PRIMARY KEY` constraints
- Constraint-aware `CREATE TABLE` parsing
- Constraint persistence and recovery through table metadata

---

## Constraints — Sprint 00-24

Sprint 00-24 introduces persistent relational constraint support across the SQL, query, metadata, storage, and terminal layers.

Implemented constraint types:

- `NOT NULL`
- `UNIQUE`
- `PRIMARY KEY`
- Composite `UNIQUE`
- Composite `PRIMARY KEY`

The constraint pipeline provides:

- `NULL` values throughout the statement / command / row / serialization pipeline
- centralized validation through `ConstraintValidator`
- INSERT enforcement before physical writes
- UPDATE enforcement before physical mutation
- self-record exclusion during UPDATE duplicate checks
- SQL parsing for inline and table-level constraint declarations
- persistent constraint metadata and recovery
- terminal-visible constraint errors
- constraint information in table descriptions

Example:

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    username STRING UNIQUE,
    email STRING NOT NULL
);

INSERT INTO users (id, username, email)
VALUES (1, 'emre', 'emre@example.com');
```

Composite key example:

```sql
CREATE TABLE enrollments (
    student_id INT,
    course_id INT,
    grade INT,
    PRIMARY KEY (student_id, course_id)
);
```

The interactive terminal from Sprint 00-23 remains fully integrated with the query and storage layers.

The terminal provides:

- REPL-style command loop
- `yekdb>` prompt
- Database-aware prompt such as `yekdb[my_database]>`
- Multi-line SQL input with continuation prompt
- SQL statement buffering until `;`
- Direct integration with `QueryExecutor`
- Persistent storage-backed `SELECT`
- ASCII table rendering for query results
- DDL / DML result formatting
- Session command history
- Centralized terminal error handling
- Metadata commands for tables and schemas
- Graceful EOF and quit handling
- Recovery after SQL errors without terminating the session

Example:

```text
YEKDB Interactive SQL Terminal
Type \help for help.

yekdb> CREATE DATABASE demo;
Database created successfully: demo

yekdb> USE DATABASE demo;
Database selected successfully: demo

yekdb[demo]> CREATE TABLE users (
...> id INT PRIMARY KEY,
...> name STRING UNIQUE,
...> age INT NOT NULL
...> );
Table created successfully: users

yekdb[demo]> INSERT INTO users (id, name, age)
...> VALUES (1, 'Emre', 21);

yekdb[demo]> SELECT name, age FROM users;
+-------+-----+
| name  | age |
+-------+-----+
| Emre  | 21  |
+-------+-----+
1 row
```

---

## Terminal Meta-Commands

The interactive terminal supports the following meta-commands:

```text
\help, \h             Show terminal help
\q, \quit             Quit terminal
\clear                Clear terminal
\history              Show command history
\history clear        Clear command history
\tables               List tables in the active database
\describe <table>     Describe a table
\d <table>            Alias for \describe
```

Examples:

```text
yekdb[demo]> \tables
Tables in database 'demo':
1  users

yekdb[demo]> \describe users
Table: users
Columns:
1  id    INT     PRIMARY KEY
2  name  STRING  UNIQUE
3  age   INT     NOT NULL
```

Invalid metadata operations are reported without terminating the terminal:

```text
yekdb[demo]> \describe missing_table
ERROR: Table not found: missing_table
```

---

## Terminal Architecture

```text
                         InteractiveSqlTerminal
                                   |
            +----------------------+----------------------+
            |                      |                      |
            v                      v                      v
     TerminalCommandParser   SqlStatementBuffer    TerminalSession
            |                      |
            v                      v
      Meta-Commands            Completed SQL
            |                      |
            |                      v
            |              SqlTerminalExecutor
            |                      |
            |                      v
            |                QueryExecutor
            |                      |
            +-----------+----------+----------+
                        |                     |
                        v                     v
              TerminalMetadataService   StorageQueryDataSource
                        |                     |
                        +----------+----------+
                                   |
                                   v
                         Table / Record / Page
                                   |
                                   v
                           Persistent Storage
```

The CLI layer does not directly manage low-level page operations. SQL execution is delegated to the query engine, while metadata commands use controlled metadata services over the active database catalog.

---

## Project Structure

```text
src/
├── main/java/com/yekdb/
│   ├── cli/
│   │   ├── command/
│   │   ├── executor/
│   │   ├── metadata/
│   │   ├── output/
│   │   └── terminal/
│   ├── constraint/
│   │   └── exception/
│   ├── core/
│   ├── database/
│   ├── index/
│   ├── logging/
│   ├── query/
│   │   ├── datasource/
│   │   ├── evaluator/
│   │   ├── executor/
│   │   ├── expression/
│   │   ├── optimizer/
│   │   ├── parser/
│   │   ├── result/
│   │   └── statement/
│   └── storage/
│       ├── file/
│       ├── page/
│       ├── record/
│       └── table/
│
└── test/java/com/yekdb/
    ├── cli/
    │   ├── command/
    │   ├── integration/
    │   ├── metadata/
    │   ├── output/
    │   └── terminal/
    └── ...
```

The exact package tree continues to evolve as the engine is refactored, but subsystem boundaries remain intentionally separated.

---

## Running the Terminal

Compile the project:

```bash
mvn clean compile
```

Run the terminal through the project's configured Java entry point using:

```text
com.yekdb.cli.terminal.TerminalLauncher
```

The default data directory is:

```text
data
```

It can be overridden with the JVM property:

```bash
-Dyekdb.data.dir=/path/to/yekdb/data
```

Example:

```bash
java -Dyekdb.data.dir=/path/to/yekdb/data \
     -cp target/classes \
     com.yekdb.cli.terminal.TerminalLauncher
```

---

## Example SQL Session

```sql
CREATE DATABASE demo;
USE DATABASE demo;

CREATE TABLE users (
    id INT PRIMARY KEY,
    name STRING UNIQUE,
    age INT NOT NULL
);

INSERT INTO users (id, name, age)
VALUES (1, 'Emre', 21);

INSERT INTO users (id, name, age)
VALUES (2, 'Ahmet', 25);

SELECT * FROM users;

SELECT name, age
FROM users;

UPDATE users
SET age = 22
WHERE id = 1;

DELETE FROM users
WHERE id = 2;

SELECT * FROM users;
```

The same session can be combined with terminal commands:

```text
\tables
\describe users
\history
\help
\q
```

---

## Error Handling

Terminal errors are centralized through `TerminalErrorHandler`.

Expected user-facing errors are reported in a concise form:

```text
ERROR: Table not found: users
ERROR: Column not found: age
ERROR: SQL parsing failed: ...
ERROR: NOT NULL constraint violated for column: email
ERROR: UNIQUE constraint violated for column(s): username
ERROR: PRIMARY KEY constraint violated for column(s): id
```

A failed SQL statement does not terminate the REPL. The user can immediately execute another statement or meta-command.

Unexpected runtime failures remain distinguishable from expected query, validation, metadata, and state errors.

---

## Result Formatting

### SELECT

`SELECT` results are rendered as ASCII tables while preserving projected column names and aliases.

```text
+----------+-----+
| username | age |
+----------+-----+
| Emre     | 21  |
+----------+-----+
1 row
```

Zero-row results preserve the result columns.

### DML

Mutation results expose affected-row information for operations such as:

```text
UPDATE 1
DELETE 1
```

### DDL

Database and table lifecycle operations return descriptive messages such as:

```text
Database created successfully: demo
Database selected successfully: demo
Table created successfully: users
```

---

## History

The terminal maintains an in-memory command history for the current session.

```text
\history
```

Multi-line SQL statements are stored as a single history entry and displayed as one normalized line.

The history can be cleared with:

```text
\history clear
```

The `\history` command itself is intentionally not added to history.

---

## Testing

YEKDB uses JUnit 5 with regression testing after every development phase.

Current project status after Sprint 00-24:

```text
Compile: SUCCESS
Tests:   1231 / 1231 PASSED
```

Sprint 00-24 includes the complete terminal regression coverage from Sprint 00-23 plus:

- constraint domain model validation
- `NULL` row support
- `NULL` binary serialization
- `NOT NULL` INSERT enforcement
- `NOT NULL` UPDATE enforcement
- single-column `UNIQUE`
- composite `UNIQUE`
- multiple `NULL` values under nullable `UNIQUE`
- UPDATE self-record exclusion for `UNIQUE`
- single-column `PRIMARY KEY`
- composite `PRIMARY KEY`
- `PRIMARY KEY` NULL rejection
- duplicate `PRIMARY KEY` rejection
- UPDATE self-record exclusion for `PRIMARY KEY`
- constraint-aware `CREATE TABLE` parsing
- inline constraint syntax
- table-level composite constraint syntax
- constraint metadata persistence
- constraint metadata recovery
- backward-compatible recovery for legacy table metadata
- terminal constraint error surfacing
- terminal table description with constraint information
- end-to-end SQL constraint regression

The complete legacy regression suite remains green.

Run the complete suite with:

```bash
mvn clean test
```

Or compile and test separately:

```bash
mvn clean compile
mvn test
```

---

## Development Timeline

Recent completed sprints:

- **00-17** — Architecture Cleanup & Query Engine Refactoring
- **00-18** — Persistent Table Catalog & Schema Recovery
- **00-19** — Binary Table Header
- **00-20** — Physical Table / Storage Integration
- **00-21** — RecordManager Refactor / Physical Record-Page Integration
- **00-22** — CLI Infrastructure / Command Layer
- **00-23** — Interactive SQL Terminal
- **00-24** — Primary Key / Unique / Not Null Constraints

---

## Sprint 00-24 Summary

Sprint 00-24 added the first full relational constraint subsystem to YEKDB.

Implemented areas:

1. Constraint domain model
2. `ConstraintType`
3. `NotNullConstraint`
4. `UniqueConstraint`
5. `PrimaryKeyConstraint`
6. Centralized `ConstraintValidator`
7. Constraint-specific exception hierarchy
8. `NULL` support in `Row`
9. `NULL` support in binary row serialization
10. `NULL` propagation through statements and commands
11. `NOT NULL` INSERT / UPDATE enforcement
12. Single-column `UNIQUE`
13. Composite `UNIQUE`
14. SQL-compatible nullable `UNIQUE` behavior
15. UPDATE self-record exclusion during duplicate checks
16. Single-column `PRIMARY KEY`
17. Composite `PRIMARY KEY`
18. Primary-key NULL rejection
19. Constraint-aware `CREATE TABLE` parsing
20. Persistent constraint metadata
21. Constraint recovery after database restart
22. Backward compatibility for legacy table metadata
23. Interactive terminal constraint error handling
24. Constraint-aware table description
25. Full unit / integration / regression verification

Final verification:

```text
1231 / 1231 tests passed
Compile successful
Constraint terminal regression successful
```

---

## Roadmap

### Completed / Established

- Project architecture
- Configuration and logging
- Persistent storage engine
- Page-oriented storage
- Record management
- Table catalog and schema recovery
- Binary table metadata
- SQL parsing foundations
- Advanced SELECT execution
- JOIN execution and optimization foundations
- CLI command infrastructure
- Interactive SQL terminal
- Persistent query execution through the terminal
- Terminal metadata inspection
- Terminal unit and integration regression coverage
- `NOT NULL` constraints
- `UNIQUE` constraints
- `PRIMARY KEY` constraints
- Composite key / uniqueness constraints
- Constraint metadata persistence and recovery

### Upcoming

- Further physical storage and free-space management
- Persistent index recovery
- B+ Tree persistence improvements
- Transaction manager
- Write Ahead Logging (WAL)
- Buffer pool
- Concurrency control / MVCC
- Client/server architecture
- Query planning and cost-based optimization improvements
- Additional terminal commands and administration features

---

## Technologies

- Java 21
- Maven
- JUnit 5
- IntelliJ IDEA
- Git
- GitHub

---

## Documentation

Development is documented sprint-by-sprint with technical developer notes covering:

- Sprint objectives
- Architecture changes
- New classes and responsibilities
- Design decisions
- Testing and regression results
- Known limitations
- Follow-up work

Latest documentation:

**Developer Notes — Sprint 00-24: Primary Key / Unique / Not Null Constraints**

---

## License

This project is licensed under the MIT License.

---

## Author

**Yunus Emre KUL**  
Computer Engineering

Developing YEKDB from scratch as a long-term database systems engineering project.
