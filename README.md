# YEKDB
### Yet Another Embedded Key Database

> Sprint 00-11 – Query Execution Engine

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![JUnit](https://img.shields.io/badge/JUnit-506%20Tests-success)
![Status](https://img.shields.io/badge/Status-Development-yellow)

---

# 📖 About

Sprint 00-11 introduces the first fully functional **Query Execution Engine** inside YEKDB.

This sprint adds the infrastructure required to evaluate SQL WHERE clauses, build logical expression trees, execute SELECT statements, perform table scans and return query results.

The system now supports complete execution of basic SELECT queries with filtering using AND, OR and NOT logical operators.

---

# ✨ Features

## Query Execution Engine

- SQL Query Execution
- Query Dispatcher
- Query Result Generation
- Execution Statistics

---

## Expression Engine

- ComparisonExpression
- LogicalExpression
- NotExpression

Supported Operators

- =
- !=
- >
- >=
- <
- <=

Logical Operators

- AND
- OR
- NOT

---

## WHERE Engine

- WHERE Evaluation
- Predicate Evaluation
- Boolean Expression Tree
- Nested Logical Expressions

---

## Table Scan

- Sequential Scan
- Row Filtering
- Expression Matching
- Result Collection

---

## Select Executor

Supported

```sql
SELECT * FROM users;

SELECT * FROM users
WHERE age > 18;

SELECT * FROM users
WHERE age > 18
AND city = 'Malatya';

SELECT * FROM users
WHERE city = 'Ankara'
OR city = 'Istanbul';

SELECT * FROM users
WHERE NOT active = true;
```

---

# 🧪 Testing

Sprint 00-11 includes comprehensive unit and integration tests.

### Successfully Tested

- QueryExecutor
- QueryExecutorIntegration
- PredicateEvaluator
- Expression
- WhereEvaluator
- RowValueProvider
- QueryResult
- SelectCommand
- SelectExecutor
- TableScanExecutor

Current project status

```
506 JUnit Tests Passed
```

Verified using

```bash
mvn clean test
```

---

# 📂 Package Structure

```
query
│
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

# 📸 Demo Screenshots

## Query Execution Demo

![](docs/screenshots/00-11/demo1.png)

---

## Query Execution Demo (WHERE)

![](docs/screenshots/00-11/demo2.png)

---

## Expression Demo

![](docs/screenshots/00-11/ExpressionDemo.png)

---

## Predicate Evaluator Demo

![](docs/screenshots/00-11/PredicateEvaluatorDemo.png)

---

## Row WHERE Evaluator Demo

![](docs/screenshots/00-11/RowWhereEvaluatorDemo.png)

---

## Select Executor Demo

![](docs/screenshots/00-11/SelectExecutorDemo.png)

---

## Table Scan Executor Demo

![](docs/screenshots/00-11/TableScanExecutorDemo.png)

---

# 🧪 Unit Tests

## Expression Tests

![](docs/screenshots/00-11/ExpressionTest.png)

---

## PredicateEvaluator Tests

![](docs/screenshots/00-11/PredicateEvaluatorTest.png)

---

## Query Executor Integration Tests

![](docs/screenshots/00-11/QueryExecutorIntegrationTest.png)

---

## Query Result Tests

![](docs/screenshots/00-11/QueryResultTest.png)

---

## RowValueProvider Tests

![](docs/screenshots/00-11/RowValueProviderTest.png)

---

## Select Command Tests

![](docs/screenshots/00-11/SelectCommandTest.png)

---

## Select Executor Tests

![](docs/screenshots/00-11/SelectExecutorTest.png)

---

## Table Scan Executor Tests

![](docs/screenshots/00-11/TableScanExecutorTest.png)

---

## Where Evaluator Tests

![](docs/screenshots/00-11/WhereEvaluatorTest.png)

---

## Generic Project Tests

### Maven Build

![](docs/screenshots/00-11/geneltest1.png)

---

### Maven Clean Test

![](docs/screenshots/00-11/geneltest2.png)

---

### InMemoryQueryDataSource Tests

![](docs/screenshots/00-11/InMemoryQueryDataSourceTest.png)

---

# 📚 Documentation

Sprint documentation

- YEKDB_Developer_Notes_00-11.pdf
- YEKDB_00-11_Query_Execution_Engine.pdf

---

# 🚀 Completed Modules

- ✅ Core Engine
- ✅ Physical Storage Engine
- ✅ Database Management
- ✅ Table Management
- ✅ Record Management
- ✅ Index Management
- ✅ Query Execution Engine

---

# 🔜 Next Sprint

Sprint 00-12

Planned features

- SQL Parser Improvements
- Projection (SELECT column1, column2)
- ORDER BY
- LIMIT
- Query Optimizer
- Index Scan
- Execution Plan

---

# 👨‍💻 Developer

**Yunus Emre KUL**

Computer Engineering Student

YEKDB is developed completely from scratch for educational and research purposes to understand the internal architecture of modern relational database systems.

---

# License

MIT License