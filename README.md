# YEKDB
### Yet Another Embedded Key Database

> A relational database management system written from scratch in Java, built sprint by sprint to explore real DBMS internals.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Active%20Development-yellow)
![Tests](https://img.shields.io/badge/JUnit-1111%20Tests%20Passed-brightgreen)
![Sprint](https://img.shields.io/badge/Sprint-00--22-blueviolet)

---

## About

YEKDB (Yet Another Embedded Key Database) is an educational and research-oriented relational database management system implemented entirely from scratch in Java.

The project is not based on PostgreSQL, MySQL, SQLite, or another database codebase. Storage, record management, metadata, SQL processing, query execution, indexing, recovery-oriented structures, and the command-line layer are developed independently to understand how modern database systems are structured internally.

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

### CLI Infrastructure — Sprint 00-22

YEKDB now contains a dedicated CLI command layer that will serve as the foundation for the interactive SQL terminal planned for Sprint 00-23.

Built-in commands:

```text
help
version
status
exit
```

CLI architecture:

```text
Raw Input
   |
   v
CliCommandParser
   |
   v
ParsedCliCommand
   |
   v
CliCommandRegistry
   |
   v
CliCommand
   |
   v
CliCommandResult
```

The CLI layer includes:

- `CliApplication`
- `CliContext`
- `CliCommand`
- `CliCommandParser`
- `CliCommandRegistry`
- `CliCommandResult`
- `ParsedCliCommand`
- `HelpCommand`
- `VersionCommand`
- `StatusCommand`
- `ExitCommand`
- CLI-specific exceptions
- Unit and integration tests

The CLI currently handles built-in commands only. SQL input and the interactive `yekdb>` terminal loop are intentionally reserved for Sprint 00-23.

---

## Architecture

```text
                         YEKDB
                           |
          +----------------+----------------+
          |                                 |
          v                                 v
      Query Layer                       CLI Layer
          |                                 |
          v                                 v
 Parser / Mapper / Executor           Command Registry
          |                                 |
          +---------------+-----------------+
                          |
                          v
                     Core / Engine
                          |
                          v
                    Table / Catalog
                          |
                          v
                     RecordManager
                          |
                          v
                      PageManager
                          |
                          v
                       DataFile
                          |
                          v
                  Persistent Storage
```

The CLI is kept separate from physical storage internals. Commands operate through controlled application/context boundaries rather than directly depending on `PageManager` or `RecordManager`.

---

## Project Structure

```text
src/
├── main/java/com/yekdb/
│   ├── cli/
│   │   ├── command/
│   │   └── exception/
│   ├── core/
│   ├── database/
│   ├── index/
│   ├── logging/
│   ├── query/
│   ├── storage/
│   │   ├── file/
│   │   ├── page/
│   │   └── record/
│   └── table/
│
└── test/java/com/yekdb/
    ├── cli/
    └── ...
```

The exact package tree evolves as refactoring continues, but package boundaries are kept focused on subsystem responsibilities.

---

## Testing

YEKDB uses JUnit 5 and regression testing after each development phase.

Current project status after Sprint 00-22:

```text
Compile: SUCCESS
Tests:   1111 / 1111 PASSED
```

Sprint 00-22 added dedicated tests for:

- CLI command parsing
- Command registry behavior
- Built-in command execution
- CLI lifecycle state
- Unknown command handling
- Blank input handling
- Case-insensitive command lookup
- Full CLI integration lifecycle

The complete legacy regression suite also remains green.

Run the complete suite with:

```bash
mvn clean test
```

---

## Development Timeline

Recent completed sprints:

- **00-17** — Architecture Cleanup & Query Engine Refactoring
- **00-18** — Persistent Table Catalog & Schema Recovery
- **00-19** — Binary Table Header
- **00-20** — Physical table / storage integration work
- **00-21** — RecordManager Refactor / Physical Record-Page Integration
- **00-22** — CLI Infrastructure / Command Layer

### Next Sprint

**00-23 — Interactive SQL Terminal**

Planned direction:

```text
yekdb> SELECT * FROM users;
        |
        v
Interactive Terminal
        |
        v
SQL Parser / Query Layer
        |
        v
Query Executor
        |
        v
Storage / Record Layer
```

Sprint 00-23 will focus on terminal input/output, the `yekdb>` prompt, command loop behavior, and forwarding SQL statements into the existing query pipeline.

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

### Upcoming

- Interactive SQL terminal
- Further physical storage / free-space work
- Persistent index recovery
- B+ Tree persistence improvements
- Transaction manager
- Write Ahead Logging (WAL)
- Buffer pool
- Concurrency control / MVCC
- Client/server architecture
- Query planning and cost-based optimization improvements

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
- Test and regression results
- Known limitations
- Follow-up work

Latest documentation: **Developer Notes — Sprint 00-22: CLI Infrastructure / Command Layer**.

---

## License

This project is licensed under the MIT License.

---

## Author

**Yunus Emre KUL**  
Computer Engineering

Developing YEKDB from scratch as a long-term database systems engineering project.
