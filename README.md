# YEKDB
### Yet Another Embedded Key Database

> A relational database management system written from scratch in Java, built sprint by sprint to explore real DBMS internals.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Active%20Development-yellow)
![Tests](https://img.shields.io/badge/JUnit-1870%20Tests%20Passed-brightgreen)
![Sprint](https://img.shields.io/badge/Sprint-00--31-blueviolet)

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
- `FOREIGN KEY` constraints
- Foreign key schema validation
- Foreign key enforcement during `INSERT`
- Foreign key enforcement during `UPDATE`
- Parent-row protection with `DELETE RESTRICT`
- Referential actions: `ON DELETE RESTRICT / CASCADE / SET NULL`
- Referential actions: `ON UPDATE RESTRICT / CASCADE / SET NULL`
- Recursive and self-referencing cascade handling
- Composite foreign key referential actions
- Pre-mutation validation for mixed referential actions
- Foreign key metadata persistence and recovery
- `ALTER TABLE` parsing and execution
- `ADD COLUMN` / `DROP COLUMN` for empty tables
- `RENAME COLUMN` and `RENAME TO` with data preservation
- `ALTER COLUMN ... SET/DROP NOT NULL`
- Named `PRIMARY KEY`, `UNIQUE`, and `FOREIGN KEY` constraints through `ALTER TABLE`
- `DROP CONSTRAINT` for named constraints
- Backward-compatible named constraint metadata persistence
- Constraint dependency protection during destructive schema changes
- Scalar SQL functions: `LOWER`, `UPPER`, `LENGTH`, `TRIM`, `ABS`
- Function calls in `SELECT` projection and `WHERE`
- Nested scalar function evaluation
- Function-aware logical predicates
- Case-insensitive built-in function registry and runtime resolution
- Character types: `CHAR(n)`, `VARCHAR(n)`, `TEXT`
- Boolean types: `BOOLEAN`, `BOOL`
- Numeric types: `INTEGER`, `FLOAT`, `FLOAT(n)`, `REAL`, `DOUBLE`, `NUMERIC`, `DECIMAL(p,s)`
- Identifier and temporal types: `UUID`, `DATE`, `TIME`, `TIMESTAMP`, `INTERVAL`
- Structured types: one-dimensional `ARRAY`, `JSON`, `HSTORE`
- User Defined Type (`UDT`) foundation and registry
- Centralized extended-type validation for `INSERT` and `UPDATE`
- Extended type metadata persistence and recovery
- B+ Tree based index core
- Ordered key lookup and range traversal
- SQL `CREATE INDEX` integration
- Existing-row index backfill
- Equality predicate index access
- Range / `BETWEEN` index access
- Automatic index maintenance on `INSERT`
- Old-key / new-key index maintenance on `UPDATE`
- Index cleanup on `DELETE`
- `DROP INDEX` execution
- Full-table-scan fallback when an index is unavailable
- SQL `CREATE VIEW` integration
- View metadata lifecycle and recovery
- Live view resolution against current base-table data
- `CREATE TRIGGER` / `DROP TRIGGER` integration
- `BEFORE` and `AFTER` trigger timing
- Trigger events for `INSERT`, `UPDATE`, and `DELETE`
- Trigger pseudo-row access through `NEW.*` and `OLD.*`
- Trigger body DML execution
- Rule-based Query Optimization V2 pipeline
- SQL `EXPLAIN SELECT ...` integration
- Query plan reporting with `FULL_TABLE_SCAN` and `INDEX_SCAN`
- Index-selection trace through `ACCESS_PREDICATE` and `RESIDUAL_PREDICATE`
- Original vs optimized WHERE expression tracing
- Optimization-rule reporting
- `EXPLAIN` support for view-backed queries
- Full-table-scan fallback after index removal

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

## Foreign Keys — Sprint 00-25

Sprint 00-25 extends the constraint subsystem introduced in Sprint 00-24 with persistent foreign key support and runtime referential integrity enforcement.

Implemented capabilities:

- `FOREIGN KEY (...) REFERENCES ...(...)` parsing in `CREATE TABLE`
- Foreign key metadata through `ForeignKeyConstraint`
- Persistence and recovery through table schema metadata
- Referenced table validation
- Referenced column validation
- Local / referenced data type compatibility checks
- Referenced `PRIMARY KEY` / `UNIQUE` validation
- Composite foreign key metadata support
- `INSERT` foreign key enforcement
- SQL-compatible nullable foreign key behavior
- `UPDATE` foreign key enforcement
- Parent-row protection through `DELETE RESTRICT`
- Terminal-visible foreign key violation messages

Example:

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    username STRING UNIQUE
);

CREATE TABLE orders (
    id INT PRIMARY KEY,
    user_id INT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

Valid child insert:

```sql
INSERT INTO users (id, username)
VALUES (1, 'emre');

INSERT INTO orders (id, user_id)
VALUES (100, 1);
```

Invalid child insert:

```sql
INSERT INTO orders (id, user_id)
VALUES (101, 999);
```

YEKDB rejects the insert because the referenced row does not exist:

```text
ERROR: FOREIGN KEY constraint violation. Columns [user_id] with values [999] reference missing row users[id]
```

The invalid row is not persisted.

Foreign key checks are also applied before updates:

```sql
UPDATE orders
SET user_id = 999
WHERE id = 100;
```

If the referenced parent row does not exist, the update is rejected before the physical mutation occurs.

Parent rows are protected with `DELETE RESTRICT`:

```sql
DELETE FROM users
WHERE id = 1;
```

The delete is rejected while a child row still references `users.id = 1`.

Current Sprint 00-25 referential action policy:

- `DELETE RESTRICT` / `NO ACTION` behavior is implemented
- `ON DELETE CASCADE` is not yet implemented
- `ON DELETE SET NULL` is not yet implemented
- `ON UPDATE CASCADE` is not yet implemented

Foreign key schema persistence uses entries such as:

```text
FOREIGN_KEY:user_id->users:id
FOREIGN_KEY:country_code,city_code->cities:country_code,city_code
```

---

## ALTER TABLE — Sprint 00-26

Sprint 00-26 introduces schema evolution through `ALTER TABLE`, extending YEKDB beyond table creation into controlled post-creation schema changes.

Implemented capabilities:

- `ALTER TABLE ... ADD COLUMN ...`
- `ALTER TABLE ... DROP COLUMN ...`
- `ALTER TABLE ... RENAME COLUMN ... TO ...`
- `ALTER TABLE ... RENAME TO ...`
- `ALTER TABLE ... ALTER COLUMN ... SET NOT NULL`
- `ALTER TABLE ... ALTER COLUMN ... DROP NOT NULL`
- `ALTER TABLE ... ADD PRIMARY KEY (...)`
- `ALTER TABLE ... ADD UNIQUE (...)`
- `ALTER TABLE ... ADD FOREIGN KEY (...) REFERENCES ...(...)`
- Named constraint syntax with `ADD CONSTRAINT <name> ...`
- `DROP CONSTRAINT <name>`
- Existing-row validation before adding `PRIMARY KEY`, `UNIQUE`, or `FOREIGN KEY` constraints
- Backward-compatible persistence for legacy unnamed constraint metadata
- Dependency checks that prevent referenced key constraints from being dropped while foreign keys depend on them
- Interactive terminal integration and end-to-end regression coverage
- Data preservation during table and column rename operations

Examples:

```sql
ALTER TABLE users RENAME COLUMN username TO display_name;

ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

ALTER TABLE orders ADD CONSTRAINT fk_orders_user
FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE users DROP CONSTRAINT uq_users_email;

ALTER TABLE users RENAME TO customers;
```

Current physical-schema limitation:

- `ADD COLUMN` and `DROP COLUMN` require an empty table because physical row rewrite is not implemented yet.
- `SET NOT NULL` on a non-empty table is conservatively rejected until safe existing-row validation/rewrite support is completed.
- These operations fail explicitly instead of risking existing record loss.
- `RENAME COLUMN`, constraint operations, and `RENAME TO` preserve existing row data.

Sprint 00-26 also added regression protection for a data-preservation issue discovered through live terminal testing: table rename now moves the associated `.data` file together with table metadata, and schema-change emptiness checks inspect real persisted table data rather than relying on stale header row counts.

---


## Referential Actions — Sprint 00-27

Sprint 00-27 extends YEKDB foreign keys with configurable referential actions for parent-row deletion and referenced-key updates.

Implemented actions:

- `ON DELETE RESTRICT`
- `ON DELETE CASCADE`
- `ON DELETE SET NULL`
- `ON UPDATE RESTRICT`
- `ON UPDATE CASCADE`
- `ON UPDATE SET NULL`

Foreign keys without an explicit referential action remain backward compatible and default to `RESTRICT`.

Example:

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    name STRING NOT NULL
);

CREATE TABLE orders (
    id INT PRIMARY KEY,
    user_id INT,
    FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);
```

Deleting a referenced parent row now follows the configured action:

```sql
DELETE FROM users
WHERE id = 1;
```

With `ON DELETE CASCADE`, dependent child rows are automatically removed. Cascades can continue recursively through multiple relationship levels.

`SET NULL` preserves the child row while clearing the foreign key:

```sql
FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE SET NULL
```

If the target foreign-key column is `NOT NULL`, the operation is rejected before any mutation occurs.

Referenced-key updates also support referential actions:

```sql
UPDATE users
SET id = 2
WHERE id = 1;
```

With `ON UPDATE CASCADE`, matching child foreign-key values are moved from the old referenced key to the new key. With `ON UPDATE SET NULL`, matching child keys become `NULL`. With `ON UPDATE RESTRICT`, the parent-key update is rejected while dependent rows exist.

Sprint 00-27 also adds:

- `ReferentialAction` metadata for foreign keys
- Parser support for `ON DELETE` and `ON UPDATE`
- Explicit and default referential action handling
- Composite foreign key action support
- Recursive delete cascade planning
- Recursive update cascade planning
- Mixed `CASCADE`, `SET NULL`, and `RESTRICT` handling
- Pre-mutation validation to prevent partial statement effects
- Self-referencing foreign key support
- Cycle-safe cascade traversal
- `NOT NULL` protection for `SET NULL`
- Terminal-visible `DELETE RESTRICT` and `UPDATE RESTRICT` failures
- Final unit, integration, regression, and live terminal verification

Live terminal validation confirmed all six action families:

```text
ON DELETE RESTRICT   PASS
ON DELETE CASCADE    PASS
ON DELETE SET NULL   PASS
ON UPDATE RESTRICT   PASS
ON UPDATE CASCADE    PASS
ON UPDATE SET NULL   PASS
```

---

## Functions & Extended Data Types — Sprint 00-28

Sprint 00-28 expands YEKDB in two major directions: scalar SQL function execution and a substantially broader SQL data type system.

### Scalar SQL Functions

Implemented built-in scalar functions:

- `LOWER(text)`
- `UPPER(text)`
- `LENGTH(text)`
- `TRIM(text)`
- `ABS(number)`

The function subsystem includes:

- `SqlFunction` contract
- `FunctionParameter`
- `FunctionRegistry`
- centralized built-in registration
- case-insensitive function lookup
- argument-count validation
- type validation
- `NULL` propagation
- nested function evaluation
- function-aware expression resolution
- function calls in `SELECT`
- function calls in `WHERE`
- function calls inside logical predicates
- terminal-visible function errors
- recovery after failed function queries without terminating the REPL

Examples:

```sql
SELECT
    LOWER(city),
    UPPER(name),
    LENGTH(name),
    TRIM(name),
    ABS(balance)
FROM users;
```

Function predicates:

```sql
SELECT id, name
FROM users
WHERE LOWER(city) = 'malatya'
AND ABS(balance) >= 75;
```

Nested functions:

```sql
SELECT id, LENGTH(TRIM(name))
FROM users;
```

Unknown functions fail cleanly:

```sql
SELECT UNKNOWN_FUNC(name)
FROM users;
```

The terminal remains usable immediately after the error.

### Extended SQL Data Types

Sprint 00-28 also introduces an expanded type system across parsing, schema metadata, validation, persistence, recovery, DML, and terminal execution.

Implemented character and boolean types:

- `CHAR(n)`
- `VARCHAR(n)`
- `TEXT`
- `BOOLEAN`
- `BOOL`

Implemented numeric types:

- `INTEGER`
- `FLOAT`
- `FLOAT(n)`
- `REAL`
- `DOUBLE`
- `NUMERIC`
- `DECIMAL`
- `NUMERIC(p)`
- `NUMERIC(p,s)`
- `DECIMAL(p,s)`

Implemented identifier and temporal types:

- `UUID`
- `DATE`
- `TIME`
- `TIMESTAMP`
- `INTERVAL`

Implemented structured and extensible types:

- one-dimensional `ARRAY`
- `JSON`
- `HSTORE`
- `UDT(name)` foundation

Example:

```sql
CREATE TABLE datatype_demo (
    id INT PRIMARY KEY,
    name VARCHAR(20),
    code CHAR(5),
    description TEXT,
    active BOOLEAN,
    price NUMERIC(8,2),
    ratio FLOAT(24)
);
```

Temporal example:

```sql
CREATE TABLE temporal_demo (
    id UUID,
    event_date DATE,
    start_time TIME,
    created_at TIMESTAMP,
    duration INTERVAL
);
```

Structured type example:

```sql
CREATE TABLE structured_demo (
    id INT PRIMARY KEY,
    tags TEXT[],
    scores INTEGER[],
    names VARCHAR(10)[],
    prices NUMERIC(6,2)[],
    metadata JSON
);
```

HSTORE example:

```sql
CREATE TABLE hstore_demo (
    id INT PRIMARY KEY,
    attributes HSTORE
);
```

### Validation & Storage

Extended-type validation is centralized so `INSERT` and `UPDATE` share the same rules.

Implemented validation includes:

- `CHAR(n)` / `VARCHAR(n)` length limits
- `NUMERIC(p,s)` precision and scale validation
- `FLOAT(n)` precision bounds
- UUID syntax validation
- calendar-valid `DATE`
- valid `TIME`
- `TIMESTAMP` validation
- textual and ISO interval validation
- JSON syntax validation
- one-dimensional ARRAY element validation
- nested element type checks for arrays
- VARCHAR / NUMERIC element constraints inside arrays
- HSTORE key/value syntax validation
- duplicate HSTORE key rejection
- UDT metadata and registry support

Extended type metadata is preserved through:

- `CREATE TABLE`
- `ALTER TABLE ADD COLUMN`
- column rename operations
- table metadata serialization
- schema recovery
- projection
- persistent row serialization / deserialization

### Terminal Validation

Live terminal verification confirmed:

```text
Scalar SELECT functions                         PASS
Nested scalar functions                         PASS
Function predicates in WHERE                    PASS
Function + logical AND                          PASS
Unknown function error recovery                 PASS
ABS(NUMERIC)                                    PASS
CHAR / VARCHAR / TEXT                           PASS
BOOLEAN                                         PASS
NUMERIC / FLOAT                                 PASS
UUID                                            PASS
DATE / TIME / TIMESTAMP / INTERVAL              PASS
ARRAY                                           PASS
JSON                                            PASS
HSTORE                                          PASS
HSTORE duplicate-key rejection                  PASS
Extended projection metadata preservation       PASS
```

Two integration issues discovered during live testing were fixed before closing the sprint:

1. `ABS(NUMERIC)` projection type inference now recognizes `NUMERIC` as a valid numeric input.
2. Projected `VARCHAR(n)` columns preserve their length metadata through SELECT execution and function-aware filtering.

Current V1 limitations:

- `INSERT INTO table VALUES (...)` without an explicit column list is not yet supported.
- multi-dimensional arrays are not yet supported.
- `JSON[]` and `HSTORE[]` are not yet supported.
- SQL JSON operators are not yet implemented.
- full `CREATE TYPE / ALTER TYPE / DROP TYPE` SQL DDL is not yet implemented.
- UDT support is currently an infrastructure / registry foundation.


---

## B+ Tree Index Integration — Sprint 00-29

Sprint 00-29 introduces a B+ Tree based index access path and integrates it with YEKDB's SQL, query execution, mutation, metadata, and terminal layers.

The sprint moves YEKDB beyond an index abstraction by making indexes participate in the complete table lifecycle: creation, backfill, lookup, range access, mutation maintenance, removal, and fallback execution.

### B+ Tree Core

Implemented B+ Tree capabilities:

- ordered key storage
- leaf-node key / record-reference entries
- internal-node routing
- child traversal
- node splitting
- split propagation
- exact-key lookup
- duplicate-safe lookup behavior
- ordered leaf traversal
- range scanning

Conceptual access flow:

```text
SQL Predicate
     |
     v
Suitable index?
   /      \
 yes      no
  |        |
  v        v
B+ Tree   Full Table Scan
  |
  +--> equality lookup
  |
  +--> range / BETWEEN scan
  |
  v
Candidate record references
  |
  v
Executor validation
  |
  v
Result Set
```

### CREATE INDEX & Backfill

`CREATE INDEX` now builds a usable B+ Tree index for both future and existing rows.

When an index is created on a table that already contains records, YEKDB scans the persisted rows and backfills the index before it becomes the active access path.

```text
CREATE INDEX
    |
    v
Create index metadata
    |
    v
Scan existing rows
    |
    v
Insert key -> record reference
    |
    v
Index ready
```

This ensures that indexes created after data insertion immediately represent the complete table state.

### Equality Lookup

Indexed equality predicates can use the B+ Tree instead of scanning the full table.

Example:

```sql
SELECT *
FROM users
WHERE age = 25;
```

When a compatible index exists, the query layer resolves matching record references through the index and continues normal executor validation.

### Range & BETWEEN Access

The ordered nature of the B+ Tree is also used for range-oriented predicates.

Examples:

```sql
SELECT *
FROM users
WHERE age >= 20 AND age <= 30;
```

```sql
SELECT *
FROM users
WHERE age BETWEEN 20 AND 30;
```

The index layer can produce ordered candidate records for compatible range conditions while preserving SQL result semantics.

### INSERT Index Maintenance

After a successful row insert, YEKDB automatically adds the corresponding index entries for indexed columns.

```text
INSERT row
    |
    v
Persist record
    |
    v
Add index entries
```

This keeps newly inserted rows immediately visible through indexed queries.

### UPDATE Index Maintenance

When an indexed column changes, YEKDB updates the index by removing the old key entry and inserting the new key entry.

```text
old key -> remove
new key -> insert
```

This prevents stale lookups after indexed-key updates.

### DELETE Index Maintenance

Deleted records are removed from the corresponding index structures.

This prevents stale index entries from resolving to records that no longer exist.

### DROP INDEX

`DROP INDEX` removes the index access path and associated metadata.

After an index is removed, compatible queries continue to work through the normal full-table-scan path.

### Full-Table-Scan Fallback

Index usage is an optimization, not a correctness requirement.

If:

- no compatible index exists
- an index was dropped
- the query is not index-eligible

YEKDB safely falls back to normal table scanning without changing query semantics.

### Terminal Validation

Live terminal verification confirmed the complete Sprint 00-29 lifecycle:

```text
CREATE INDEX on populated table                  PASS
Existing-row backfill                            PASS
Equality lookup                                  PASS
Range lookup                                     PASS
BETWEEN lookup                                   PASS
INSERT index maintenance                         PASS
UPDATE old-key / new-key maintenance             PASS
DELETE index cleanup                             PASS
DROP INDEX                                       PASS
Full-table-scan fallback after DROP INDEX        PASS
```

The observed terminal results matched the expected behavior across all scenarios.


---

## Views & Triggers — Sprint 00-30

Sprint 00-30 introduces SQL view support and the first trigger execution pipeline, extending YEKDB with derived relational projections and event-driven mutation hooks.

The sprint integrates views and triggers across parsing, metadata, query execution, mutation workflows, persistence, recovery, and terminal validation.

### Views

Implemented view capabilities:

- `CREATE VIEW ... AS SELECT ...`
- View metadata model
- View catalog registration
- View metadata persistence
- View recovery after database restart
- View resolution through normal `SELECT`
- Projection preservation from view queries
- Live base-table data visibility
- Terminal-visible view creation and querying

Example:

```sql
CREATE VIEW it_employees AS
SELECT id, name, salary
FROM employees
WHERE department = 'IT';

SELECT *
FROM it_employees;
```

Views are resolved dynamically. When the base table changes, subsequent view queries reflect the current table state rather than a stale materialized snapshot.

### Triggers

Implemented trigger capabilities:

- `CREATE TRIGGER`
- `DROP TRIGGER`
- `BEFORE INSERT`
- `BEFORE UPDATE`
- `BEFORE DELETE`
- `AFTER INSERT`
- `AFTER UPDATE`
- `AFTER DELETE`
- Trigger metadata model
- Trigger catalog registration
- Trigger metadata persistence
- Trigger recovery after database restart
- `NEW.*` pseudo-row access for insert/update workflows
- `OLD.*` pseudo-row access for update/delete workflows
- Trigger body DML execution
- Query Optimization V2 rule pipeline
- `EXPLAIN SELECT` execution-plan inspection
- Full-table-scan / index-scan plan reporting
- Access / residual predicate tracing
- View-aware EXPLAIN planning
- Index-removal fallback verification
- Terminal-visible trigger execution failures

Example:

```sql
CREATE TRIGGER employees_after_insert_log
AFTER INSERT ON employees
BEGIN
    INSERT INTO employee_log (id, employee_name, action)
    VALUES (NEW.id, NEW.name, 'INSERT')
END;
```

When a matching mutation occurs, YEKDB executes the trigger body through the normal DML pipeline.

### Terminal Validation

Live terminal verification confirmed the complete Sprint 00-30 view and trigger workflow:

```text
CREATE VIEW                                      PASS
SELECT from view                                 PASS
Live view update after base-table INSERT         PASS
Live view update after base-table UPDATE         PASS
AFTER INSERT trigger                             PASS
AFTER UPDATE trigger                             PASS
AFTER DELETE trigger                             PASS
BEFORE INSERT trigger                            PASS
BEFORE UPDATE trigger                            PASS
BEFORE DELETE trigger                            PASS
NEW.id / NEW.name access                         PASS
OLD.id / OLD.name access                         PASS
Trigger body DML                                 PASS
DROP TRIGGER                                     PASS
Trigger removal prevents future execution        PASS
```

The live workflow confirmed that views remain connected to current base-table data and that triggers can observe row state through `NEW.*` / `OLD.*` while executing DML against another table.

Current V1 limitations:

- Trigger execution is not transaction-atomic yet. If an `AFTER` trigger fails, the main mutation and earlier `BEFORE` trigger side effects may remain applied instead of being rolled back as a single atomic statement.
- `INSERT INTO table VALUES (...)` without an explicit column list is not yet supported.


---

## Query Optimization V2 & EXPLAIN — Sprint 00-31

Sprint 00-31 extends YEKDB's query engine with a second-stage rule-based optimization pipeline and terminal-visible execution-plan inspection through `EXPLAIN`.

The sprint connects SQL parsing, optimizer context construction, B+ Tree index selection, predicate classification, view resolution, query execution, and terminal output into one explainable planning workflow.

### EXPLAIN

YEKDB now supports:

```sql
EXPLAIN SELECT id, name
FROM users
WHERE age > 18;
```

A plan can expose information such as:

```text
PLAN: INDEX_SCAN
INDEX: idx_users_age
ORIGINAL_WHERE: age GREATER_THAN 18
OPTIMIZED_WHERE: age GREATER_THAN 18
ACCESS_PREDICATE: age GREATER_THAN 18
RESIDUAL_PREDICATE: NONE
RULES: EXPRESSION_OPTIMIZATION, INDEX_SELECTION
DETAIL: B+ Tree index 'idx_users_age' will be used for column 'age'.
```

When no compatible index exists, the optimizer falls back safely:

```text
PLAN: FULL_TABLE_SCAN
INDEX: NONE
ACCESS_PREDICATE: NONE
RESIDUAL_PREDICATE: age GREATER_THAN 18
RULES: EXPRESSION_OPTIMIZATION
```

### Access vs Residual Predicates

For compound predicates, YEKDB distinguishes the part used to access the index from the part that still needs row-level filtering.

Example:

```sql
EXPLAIN SELECT id, name
FROM users
WHERE id = 3 AND age > 18;
```

With an index on `id`, the observed plan is:

```text
PLAN: INDEX_SCAN
INDEX: idx_users_id
ACCESS_PREDICATE: id EQUALS 3
RESIDUAL_PREDICATE: age GREATER_THAN 18
RULES: EXPRESSION_OPTIMIZATION, INDEX_SELECTION
```

This keeps index access and post-access filtering explicit instead of hiding both inside a single WHERE representation.

### View-Aware EXPLAIN

`EXPLAIN` also resolves view-backed queries.

Example:

```sql
CREATE VIEW adult_users AS
SELECT id, name, age
FROM users
WHERE age >= 18;

EXPLAIN SELECT name
FROM adult_users;
```

The resulting plan reports the view, its source query, and the selected access path for the underlying base table.

Observed live output included:

```text
VIEW: adult_users
VIEW_SOURCE: SELECT id, name, age FROM users WHERE age >= 18
PLAN: INDEX_SCAN
INDEX: idx_users_age
ACCESS_PREDICATE: age GREATER_THAN_OR_EQUALS 18
RESIDUAL_PREDICATE: NONE
RULES: EXPRESSION_OPTIMIZATION, INDEX_SELECTION
```

### EXPLAIN Scope

Sprint 00-31 intentionally limits `EXPLAIN` to `SELECT` statements.

For example:

```sql
EXPLAIN INSERT INTO users (id, name, age)
VALUES (5, 'Test', 40);
```

is rejected during parsing with a terminal-visible error indicating that `EXPLAIN` currently supports `SELECT` statements only.

### Terminal Validation

Live terminal verification confirmed:

```text
EXPLAIN without WHERE                            PASS
FULL_TABLE_SCAN without compatible index        PASS
INDEX_SCAN after CREATE INDEX                    PASS
Equality predicate index selection              PASS
Access / residual predicate separation          PASS
View-backed EXPLAIN                              PASS
Invalid EXPLAIN INSERT rejection                 PASS
DROP INDEX -> FULL_TABLE_SCAN fallback           PASS
Query-result correctness with optimizer enabled PASS
```

The final live workflow also verified that removing `idx_users_age` immediately changes the same `age > 18` query plan from `INDEX_SCAN` back to `FULL_TABLE_SCAN` without changing query semantics.

Current V1 notes:

- `EXPLAIN` currently supports `SELECT` statements only.
- The optimizer is rule-based; cost-based planning is a later roadmap item.
- Complex logical expressions are functionally supported, while their human-readable pretty-printing can be improved further.


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
ERROR: FOREIGN KEY constraint violation. Columns [user_id] with values [999] reference missing row users[id]
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

Current project status after Sprint 00-31:

```text
Compile: SUCCESS
Tests:   1870 / 1870 PASSED
Sprint 00-31 terminal integration: PASSED
```

Sprint 00-31 keeps the complete Sprint 00-30 regression suite green and adds coverage for:

- `EXPLAIN SELECT` parsing and execution
- `FULL_TABLE_SCAN` planning without a compatible index
- `INDEX_SCAN` planning with B+ Tree indexes
- original / optimized WHERE tracing
- access-predicate selection
- residual-predicate preservation
- optimizer rule reporting
- index-selection behavior for equality and range-compatible predicates
- view-backed `EXPLAIN`
- rejection of unsupported `EXPLAIN INSERT`
- `DROP INDEX` fallback to full table scan
- query-result regression checks with optimization enabled
- edge cases with missing WHERE conditions and unavailable indexes
- live terminal verification across the complete Sprint 00-31 planning workflow

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
- **00-25** — Foreign Key Constraints
- **00-26** — ALTER TABLE / Schema Evolution
- **00-27** — Foreign Key Referential Actions: CASCADE / RESTRICT / SET NULL
- **00-28** — Scalar SQL Functions & Extended Data Types
- **00-29** — B+ Tree Index Integration
- **00-30** — Views & Triggers
- **00-31** — Query Optimization V2 & EXPLAIN

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

## Sprint 00-25 Summary

Sprint 00-25 introduced persistent foreign key constraints and referential integrity enforcement between YEKDB tables.

Implemented areas:

1. `ConstraintType.FOREIGN_KEY`
2. `ForeignKeyConstraint`
3. Single-column foreign key metadata
4. Composite foreign key metadata representation
5. Foreign key terminal formatting
6. Foreign key schema persistence
7. Foreign key schema recovery
8. `FOREIGN KEY (...) REFERENCES ...(...)` SQL parsing
9. Referenced table validation
10. Referenced column validation
11. Local / referenced column count validation
12. Data type compatibility validation
13. Referenced `PRIMARY KEY` validation
14. Referenced `UNIQUE` validation
15. INSERT referential integrity enforcement
16. SQL-compatible nullable foreign key INSERT behavior
17. UPDATE referential integrity enforcement
18. SQL-compatible nullable foreign key UPDATE behavior
19. Composite foreign key runtime validation
20. Dedicated foreign key violation exception handling
21. Parent-row protection through `DELETE RESTRICT`
22. Child-first / parent-second delete workflow
23. Foreign key integration through the mutation execution pipeline
24. Backward compatibility with existing constraint metadata
25. Interactive terminal live foreign key verification
26. Full unit / integration / regression verification

Final verification:

```text
1274 / 1274 tests passed
Compile successful
Foreign key terminal live test successful
```

Live terminal verification included:

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    username STRING UNIQUE
);

CREATE TABLE orders (
    id INT PRIMARY KEY,
    user_id INT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

INSERT INTO users (id, username)
VALUES (1, 'emre');

INSERT INTO orders (id, user_id)
VALUES (100, 1);

INSERT INTO orders (id, user_id)
VALUES (101, 999);
```

The final statement was correctly rejected because `users.id = 999` did not exist.

---

## Sprint 00-26 Summary

Sprint 00-26 introduced ALTER TABLE support and the first schema-evolution workflow in YEKDB.

Implemented areas:

1. `AlterTableCommand` and ALTER action model
2. Management command parser integration
3. `ADD COLUMN` execution for empty tables
4. `DROP COLUMN` execution for empty tables
5. `RENAME COLUMN`
6. `RENAME TABLE`
7. `ALTER COLUMN ... SET NOT NULL`
8. `ALTER COLUMN ... DROP NOT NULL`
9. `ADD PRIMARY KEY (...)`
10. `ADD UNIQUE (...)`
11. `ADD FOREIGN KEY (...) REFERENCES ...(...)`
12. Existing-row validation for newly added constraints
13. Named constraint metadata
14. `ADD CONSTRAINT <name> ...` parsing
15. Named constraint persistence and recovery
16. `DROP CONSTRAINT <name>`
17. Foreign-key dependency protection while dropping referenced constraints
18. Backward compatibility with Sprint 00-24 / 00-25 constraint metadata
19. Interactive SQL Terminal integration
20. Terminal-visible ALTER TABLE results and validation failures
21. Real persisted-data emptiness checks for schema-changing operations
22. `.data` file preservation during table rename
23. Data-preservation regression coverage
24. End-to-end terminal-style ALTER TABLE regression test
25. Full unit / integration / regression verification

Final verification:

```text
1301 / 1301 tests passed
Compile successful
ALTER TABLE terminal live test successful
Data-preservation regression successful
```

Live terminal verification covered:

```sql
CREATE TABLE users (
    id INT PRIMARY KEY,
    username STRING,
    email STRING
);

INSERT INTO users (id, username, email)
VALUES (1, 'emre', 'emre@example.com');

INSERT INTO users (id, username, email)
VALUES (2, 'ahmet', 'ahmet@example.com');

ALTER TABLE users RENAME COLUMN username TO display_name;

ALTER TABLE users ADD CONSTRAINT uq_users_email UNIQUE (email);

ALTER TABLE users DROP CONSTRAINT uq_users_email;

ALTER TABLE users RENAME TO customers;

SELECT * FROM customers;
```

The final query preserved all existing rows across rename operations. The live session also verified UNIQUE enforcement before `DROP CONSTRAINT` and successful insertion after the constraint was removed.

---

## Sprint 00-27 Summary

Sprint 00-27 completed configurable foreign key referential actions across parsing, validation, execution, recursive mutation planning, and terminal workflows.

Implemented areas:

1. `ReferentialAction`
2. Default `RESTRICT` behavior for backward compatibility
3. Foreign key `onDelete` metadata
4. Foreign key `onUpdate` metadata
5. `ON DELETE RESTRICT` parsing
6. `ON DELETE CASCADE` parsing
7. `ON DELETE SET NULL` parsing
8. `ON UPDATE RESTRICT` parsing
9. `ON UPDATE CASCADE` parsing
10. `ON UPDATE SET NULL` parsing
11. Invalid / duplicate referential action rejection
12. Runtime `ON DELETE RESTRICT`
13. Recursive `ON DELETE CASCADE`
14. Multiple-child cascade planning
15. `ON DELETE SET NULL`
16. Composite foreign key `SET NULL`
17. `NOT NULL + SET NULL` protection
18. Mixed delete referential actions
19. Pre-mutation delete validation
20. Dedicated `ON UPDATE RESTRICT` handling
21. Runtime `ON UPDATE CASCADE`
22. Recursive update cascades
23. Runtime `ON UPDATE SET NULL`
24. Composite foreign key update actions
25. Mixed update referential actions
26. Self-referencing foreign key schema validation
27. Self-referencing delete cascades
28. Self-referencing update cascades
29. Cycle-safe recursive traversal
30. Final edge-case and regression suite
31. Interactive terminal live verification
32. Full unit / integration / regression verification

Final verification:

```text
1340 / 1340 tests passed
Compile successful
Referential action terminal live tests: 6 / 6 passed
```

Live terminal verification covered:

```sql
FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE RESTRICT;

FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE;

FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE SET NULL;

FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON UPDATE RESTRICT;

FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON UPDATE CASCADE;

FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON UPDATE SET NULL;
```

Observed terminal behavior matched the configured action in all six cases. `RESTRICT` preserved both parent and child rows, `CASCADE` propagated delete/update operations to dependent rows, and `SET NULL` preserved child rows while clearing their foreign key values.


---

---

## Sprint 00-29 Summary

Sprint 00-29 completed the first fully integrated B+ Tree index workflow in YEKDB.

Implemented areas:

1. B+ Tree index core
2. Ordered key entry model
3. Leaf-node insertion and search
4. Internal-node routing
5. Child traversal
6. Node splitting
7. Split propagation
8. Exact-key lookup
9. Duplicate-safe lookup behavior
10. Ordered leaf traversal
11. Range scan infrastructure
12. Integration with YEKDB index abstractions
13. Index metadata / catalog lifecycle
14. `CREATE INDEX`
15. Existing-row index backfill
16. Equality predicate index lookup
17. Range predicate index lookup
18. `BETWEEN` index lookup
19. `INSERT` index maintenance
20. `UPDATE` old-key removal
21. `UPDATE` new-key insertion
22. `DELETE` index cleanup
23. `DROP INDEX`
24. Full-table-scan fallback
25. Query correctness preservation with and without indexes
26. Interactive terminal live verification
27. Full unit / integration / regression verification

Final verification:

```text
1723 / 1723 tests passed
Compile successful
B+ Tree index terminal live test successful
```

Live terminal verification covered:

```sql
CREATE INDEX idx_users_age
ON users(age);

SELECT *
FROM users
WHERE age = 25;

SELECT *
FROM users
WHERE age >= 20 AND age <= 30;

SELECT *
FROM users
WHERE age BETWEEN 20 AND 30;

INSERT INTO users (id, name, age)
VALUES (4, 'New User', 28);

UPDATE users
SET age = 29
WHERE id = 4;

DELETE FROM users
WHERE id = 4;

DROP INDEX idx_users_age;
```

The live workflow confirmed existing-row backfill, indexed equality and range access, automatic mutation maintenance, index removal, and correct full-table-scan fallback after the index was dropped.


---

## Sprint 00-30 Summary

Sprint 00-30 completed YEKDB's first integrated view and trigger workflow.

Implemented areas:

1. View metadata model
2. View catalog lifecycle
3. `CREATE VIEW ... AS SELECT ...`
4. View metadata persistence
5. View metadata recovery
6. View resolution through `SELECT`
7. Projection preservation for view queries
8. Live view evaluation against current base-table data
9. Trigger metadata model
10. Trigger catalog lifecycle
11. `CREATE TRIGGER`
12. `DROP TRIGGER`
13. Trigger metadata persistence
14. Trigger metadata recovery
15. `BEFORE INSERT`
16. `BEFORE UPDATE`
17. `BEFORE DELETE`
18. `AFTER INSERT`
19. `AFTER UPDATE`
20. `AFTER DELETE`
21. `NEW.*` pseudo-row support
22. `OLD.*` pseudo-row support
23. Trigger body DML execution
24. Trigger execution integration with table mutations
25. Terminal-visible trigger failure behavior
26. Interactive terminal live verification
27. Full unit / integration / regression verification

Final verification:

```text
1830 / 1830 tests passed
Compile successful
Views and triggers terminal live test successful
```

Live terminal verification covered:

```sql
CREATE VIEW it_employees AS
SELECT id, name, salary
FROM employees
WHERE department = 'IT';

CREATE TRIGGER employees_after_insert_log
AFTER INSERT ON employees
BEGIN
    INSERT INTO employee_log (id, employee_name, action)
    VALUES (NEW.id, NEW.name, 'INSERT')
END;

CREATE TRIGGER employees_before_delete_log
BEFORE DELETE ON employees
BEGIN
    INSERT INTO employee_log (id, employee_name, action)
    VALUES (OLD.id, OLD.name, 'BEFORE_DELETE')
END;

DROP TRIGGER employees_after_insert_log;
```

The live workflow confirmed `CREATE VIEW`, live view updates, `BEFORE` and `AFTER` triggers for `INSERT`, `UPDATE`, and `DELETE`, `NEW.*` and `OLD.*` pseudo-row access, trigger body DML, and `DROP TRIGGER`.

Observed V1 limitation:

- Trigger execution is not transaction-atomic yet. If an `AFTER` trigger fails, the main mutation and earlier `BEFORE` trigger side effects may remain applied instead of being rolled back as a single atomic statement.


---

## Sprint 00-31 Summary

Sprint 00-31 completed YEKDB's Query Optimization V2 and first terminal-visible query-plan explanation workflow.

Implemented areas:

1. `EXPLAIN SELECT` SQL parsing
2. EXPLAIN statement / command mapping
3. Query-plan explanation pipeline
4. Optimizer context integration
5. Original WHERE tracing
6. Optimized WHERE tracing
7. Rule-based expression optimization reporting
8. `FULL_TABLE_SCAN` plan reporting
9. `INDEX_SCAN` plan reporting
10. B+ Tree index selection reporting
11. Access-predicate extraction
12. Residual-predicate preservation
13. Equality predicate index planning
14. Range-compatible predicate index planning
15. No-WHERE plan handling
16. No-index fallback handling
17. View source resolution for EXPLAIN
18. View-backed index-plan reporting
19. Unsupported EXPLAIN statement rejection
20. DROP INDEX fallback regression
21. Query-result correctness regression
22. Edge-case and hardening tests
23. Interactive terminal live verification
24. Full unit / integration / regression verification

Final verification:

```text
1870 / 1870 tests passed
Compile successful
Query Optimization V2 terminal live tests passed
```

Live terminal verification covered:

```sql
EXPLAIN SELECT id, name
FROM users;

EXPLAIN SELECT id, name
FROM users
WHERE age > 18;

CREATE INDEX idx_users_age
ON users(age);

EXPLAIN SELECT id, name
FROM users
WHERE age > 18;

CREATE INDEX idx_users_id
ON users(id);

EXPLAIN SELECT name
FROM users
WHERE id = 3;

EXPLAIN SELECT id, name
FROM users
WHERE id = 3 AND age > 18;

CREATE VIEW adult_users AS
SELECT id, name, age
FROM users
WHERE age >= 18;

EXPLAIN SELECT name
FROM adult_users;

DROP INDEX idx_users_age;

EXPLAIN SELECT id, name
FROM users
WHERE age > 18;
```

The observed plans matched the expected access paths throughout the lifecycle: full scan without an index, B+ Tree index scan after index creation, access/residual predicate separation for compound conditions, view-aware planning, and automatic fallback after index removal.


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
- `FOREIGN KEY` constraints
- Foreign key schema validation
- Foreign key INSERT / UPDATE enforcement
- `DELETE RESTRICT` parent-row protection
- `ALTER TABLE` schema evolution foundation
- Named constraints and `DROP CONSTRAINT`
- ALTER TABLE terminal integration
- Schema rename data-preservation safeguards
- Foreign key `ON DELETE RESTRICT / CASCADE / SET NULL`
- Foreign key `ON UPDATE RESTRICT / CASCADE / SET NULL`
- Recursive / self-referencing referential action handling
- Scalar SQL function subsystem
- Function projection and function-aware WHERE predicates
- Extended character / boolean / numeric types
- UUID and temporal data types
- ARRAY / JSON / HSTORE structured types
- UDT registry foundation
- Centralized extended-type DML validation
- B+ Tree index core
- Ordered equality and range index access
- `CREATE INDEX` with existing-row backfill
- Index maintenance across `INSERT / UPDATE / DELETE`
- `DROP INDEX`
- Full-table-scan fallback after index removal
- `CREATE VIEW` and live view resolution
- View metadata persistence and recovery
- `CREATE TRIGGER` and `DROP TRIGGER`
- `BEFORE / AFTER` trigger execution
- `INSERT / UPDATE / DELETE` trigger events
- `NEW.*` and `OLD.*` pseudo-row support
- Trigger body DML execution

### Upcoming

- Positional `INSERT INTO table VALUES (...)` syntax without an explicit column list
- Full SQL `CREATE TYPE / ALTER TYPE / DROP TYPE` support
- Multi-dimensional ARRAY support and extended structured-type operators
- JSON operators and richer structured-type query support
- Physical row rewrite for `ADD COLUMN` / `DROP COLUMN` on non-empty tables
- Extended `ALTER COLUMN` operations such as type/default changes
- Further physical storage and free-space management
- Persistent index storage / recovery improvements
- Transaction manager
- Write Ahead Logging (WAL)
- Buffer pool
- Concurrency control / MVCC
- Client/server architecture
- Cost-based query planning, statistics, and optimization improvements
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

**Developer Notes — Sprint 00-31: Query Optimization V2 & EXPLAIN**

---

## License

This project is licensed under the MIT License.

---

## Author

**Yunus Emre KUL**  
Computer Engineering

Developing YEKDB from scratch as a long-term database systems engineering project.
