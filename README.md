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

---

# Sprint 00-16 — Advanced JOIN Engine & Rule-Based JOIN Optimization

Sprint 00-16 extends the relational execution layer introduced in Sprint 00-15.

The sprint focuses on:

- Outer JOIN execution
- Multiple JOIN chains
- JOIN-aware filtering and aggregation
- Qualified column preservation
- Multi-table query execution
- Safe rule-based JOIN optimization
- Full regression verification

## Completed Capabilities

| Feature | Status |
|---|---|
| INNER JOIN | ✅ |
| LEFT JOIN | ✅ |
| RIGHT JOIN | ✅ |
| FULL JOIN | ✅ |
| Multiple JOIN chains | ✅ |
| Qualified column preservation | ✅ |
| Column-to-column JOIN predicates | ✅ |
| JOIN + WHERE | ✅ |
| JOIN + GROUP BY | ✅ |
| JOIN + HAVING | ✅ |
| JOIN + COUNT | ✅ |
| JOIN + SUM | ✅ |
| JOIN + AVG | ✅ |
| JOIN + MIN | ✅ |
| JOIN + MAX | ✅ |
| JOIN + ORDER BY | ✅ |
| JOIN + LIMIT | ✅ |
| JOIN + FETCH | ✅ |
| QueryExecutor multi-JOIN routing | ✅ |
| Rule-based JOIN optimizer | ✅ |
| JOIN condition validation | ✅ |
| Cartesian JOIN prevention | ✅ |
| Predicate pushdown analysis | ✅ |
| Projection pruning analysis | ✅ |
| Safe INNER JOIN reorder | ✅ |
| Small-table-first strategy | ✅ |
| Outer JOIN reorder protection | ✅ |
| Full Maven regression | ✅ |
| 940 automated tests passing | ✅ |

---

# Query Architecture

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
SelectStatement
 │
 ├── TableReference
 ├── SelectItem list
 ├── JoinClause list
 ├── WHERE Expression
 ├── GROUP BY
 ├── HAVING
 ├── ORDER BY
 ├── LIMIT
 └── FETCH
 │
 ▼
StatementCommandMapper
 │
 ▼
SelectCommand
 │
 ▼
QueryExecutor
 │
 ▼
SelectExecutor
 │
 ├── Single-table execution
 ├── JoinExecutor
 └── MultiJoinExecutor
      │
      ▼
 JOIN result rows
      │
      ├── WHERE
      ├── GROUP BY
      ├── Aggregate
      ├── HAVING
      ├── Projection
      ├── ORDER BY
      ├── LIMIT
      └── FETCH
      │
      ▼
 QueryResult
```

---

# JOIN Model

A JOIN is represented by a `JoinClause`.

```text
JoinClause
├── JoinType
├── tableName
├── alias
└── condition
```

Supported JOIN types:

```text
INNER
LEFT
RIGHT
FULL
```

Example:

```sql
SELECT e.name, d.name
FROM employee e
INNER JOIN department d
    ON e.department_id = d.id;
```

Multiple JOIN example:

```sql
SELECT e.name, d.name, c.name
FROM employee e
INNER JOIN department d
    ON e.department_id = d.id
INNER JOIN company c
    ON d.company_id = c.id;
```

---

# Qualified Column Resolution

JOIN execution preserves qualified column information.

Example joined row keys:

```text
employee.id
employee.name
employee.department_id

e.id
e.name
e.department_id

department.id
department.name

d.id
d.name
```

This allows the query engine to distinguish columns such as:

```text
e.id
d.id
c.id
```

and detect unsafe ambiguous unqualified references.

---

# Outer JOIN Semantics

`JoinExecutor` supports null padding for unmatched JOIN sides.

Conceptually:

```text
LEFT JOIN
→ preserve all left rows
→ unmatched right-side columns are NULL

RIGHT JOIN
→ preserve all right rows
→ unmatched left-side columns are NULL

FULL JOIN
→ preserve unmatched rows from both sides
```

> Current technical debt: the low-level JOIN row map can represent null-padded columns, but the existing `Row` storage/result model does not yet fully support nullable values. NULL-aware final result materialization will be handled in a dedicated future improvement rather than changing the physical row format inside Sprint 00-16.

---

# Multiple JOIN Execution

`MultiJoinExecutor` processes JOIN clauses sequentially while preserving qualified provenance.

```text
Base table
   │
   ▼
JOIN #1
   │
   ▼
Intermediate qualified row set
   │
   ▼
JOIN #2
   │
   ▼
Intermediate qualified row set
   │
   ▼
JOIN #N
```

This supports dependent chains such as:

```text
employee → department → company
```

where the second JOIN can reference a column produced by the first JOIN.

---

# JOIN + WHERE

WHERE filtering is evaluated after JOIN execution at the logical result level.

Example:

```sql
SELECT e.name, d.name
FROM employee e
INNER JOIN department d
    ON e.department_id = d.id
WHERE d.name = 'IT';
```

The optimizer can additionally identify predicates that are safe candidates for pushdown.

---

# JOIN + GROUP BY / Aggregate / HAVING

The JOIN result can flow directly into the aggregation pipeline.

Example:

```sql
SELECT d.name, COUNT(e.id)
FROM employee e
INNER JOIN department d
    ON e.department_id = d.id
GROUP BY d.name
HAVING COUNT(e.id) > 2;
```

Supported aggregate functions:

```text
COUNT
SUM
AVG
MIN
MAX
```

The same pipeline is supported for multiple JOIN chains.

---

# Rule-Based JOIN Optimizer

Sprint 00-16 introduces a safe rule-based optimizer.

Main classes:

```text
query.optimizer
├── JoinExecutionContext
├── JoinOptimizationRule
├── JoinOptimizationResult
└── JoinOptimizer
```

Supported rules:

```text
CONDITION_VALIDATION
CARTESIAN_PREVENTION
PREDICATE_PUSHDOWN
PROJECTION_PRUNING
INNER_JOIN_REORDER
SMALL_TABLE_FIRST
```

## Condition Validation

JOIN conditions must be valid qualified column-to-column comparisons.

Invalid conditions are rejected before execution.

Example of valid condition:

```text
e.department_id = d.id
```

Example of rejected Cartesian-style JOIN:

```sql
SELECT *
FROM employee e
JOIN department d;
```

---

## Predicate Pushdown Analysis

Qualified single-table predicates can be identified as pushdown candidates.

Safe example:

```sql
WHERE d.active = true
```

Not automatically pushed:

```sql
WHERE e.salary > d.budget
```

`OR` and `NOT` expressions are deliberately not pushed down by the Sprint 00-16 optimizer because preserving query semantics takes priority over aggressive optimization.

---

## Projection Pruning Analysis

The optimizer calculates columns required by:

- SELECT projection
- JOIN conditions
- WHERE conditions

This provides the foundation for reducing unnecessary column materialization in future execution strategies.

---

## Safe INNER JOIN Reordering

JOIN reordering is intentionally conservative.

Safe independent INNER JOIN branches:

```text
e.department_id = d.id
e.company_id    = c.id
```

can be reordered according to row-count metadata.

Dependent chain:

```text
e.department_id = d.id
d.company_id    = c.id
```

is **not** reordered because the second JOIN depends on the first JOIN result.

---

## Small-Table-First

When safe reordering is possible, row-count metadata can be used to prefer smaller JOIN inputs first.

Unknown row counts are treated conservatively and are never assumed to represent a small table.

---

## Outer JOIN Protection

The optimizer never reorders:

```text
LEFT JOIN
RIGHT JOIN
FULL JOIN
```

because changing the order of outer JOIN operations can change query semantics.

Correctness has priority over optimization.

---

# Testing

Sprint 00-16 completed with a full Maven regression pass:

```text
Tests run: 940
Failures: 0
Errors: 0
```

Test coverage includes:

- JOIN execution
- Outer JOIN behavior
- Multiple JOIN chains
- Qualified column resolution
- JOIN + WHERE
- JOIN + GROUP BY
- Aggregate functions
- HAVING
- Multi-JOIN aggregate pipelines
- QueryExecutor routing
- Optimizer core rules
- Optimizer edge cases
- Reorder safety
- Predicate pushdown safety
- Immutable optimization results
- Existing query/storage regressions

---

# Project Structure

```text
src/main/java/com/yekdb
├── buffer
├── catalog
├── command
├── configurationManager
├── console
├── core
├── database
├── exception
├── execution
├── index
├── logs
├── query
│   ├── command
│   ├── datasource
│   ├── evaluator
│   ├── executor
│   ├── expression
│   ├── mapper
│   ├── optimizer
│   ├── parser
│   ├── result
│   └── statement
├── storage
├── table
├── transaction
└── util
```

---

# Technology Stack

- Java 21
- Maven
- JUnit 5
- IntelliJ IDEA
- Git / GitHub

---

# Current Development Direction

The project is still under active development.

Planned future areas include:

- Full nullable `Row` / serialization support
- Index-assisted query execution
- Hash Join research
- Merge Join research
- Cost-based optimization foundations
- Query statistics
- Transaction improvements
- Concurrency and multi-user execution
- Client/server architecture
- Backup and recovery improvements

A dedicated architecture / project health review sprint is also planned to inspect:

- Package boundaries
- Duplicate code
- Technical debt
- NULL handling
- Exception consistency
- Test organization
- Performance bottlenecks
- Documentation alignment
- V1 readiness

---

# Educational Goal

YEKDB is primarily an educational systems project.

Its goal is not to compete with production-grade database systems, but to understand their internal architecture by implementing the major layers directly.

The project demonstrates practical work in:

- Data structures
- File I/O
- Binary storage
- SQL parsing
- Expression trees
- Relational query execution
- JOIN algorithms
- Query optimization
- Testing
- Software architecture

---

## Repository

GitHub: `EmreBEYS/YEKDB`

---

## License

See the repository license file for licensing information.
