# YEKDB
### Yet Another Embedded Key Database

> A lightweight relational database management system written entirely in Java.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Development-yellow)

---

# 📖 About

YEKDB (Yet Another Embedded Key Database) is a relational database management system developed completely from scratch in Java.

Unlike PostgreSQL, MySQL or SQLite, every subsystem of YEKDB is implemented independently to understand how a modern database system works internally.

The long-term objective is to build a fully functional embedded relational database including:

- Storage Engine
- Query Engine
- SQL Parser
- Index Manager
- Transaction Manager
- Buffer Manager
- Optimizer
- Catalog Manager

---

# ✨ Current Features

### Storage Layer

- Physical Data File
- Database Header
- Fixed-size Page Architecture
- Page Manager
- Record Manager
- Row Serialization

### Database Layer

- Database Management
- Table Management
- Table Metadata
- Catalog Management

### Query Layer

- SQL Command Architecture
- Command Abstraction
- Query Execution Skeleton
- Execute Result Model
- Query Exception Handling

---

# 📂 Project Structure

```text
src
├── core
├── database
├── index
├── network
├── optimizer
├── parser
├── query
│   ├── command
│   └── executor
├── security
├── space
├── storage
├── table
├── transaction
└── util
```

---

# 🚀 Sprint Progress

| Sprint | Status |
|---------|--------|
| 00-01 | ✅ Core Project Structure |
| 00-02 | ✅ Storage Engine |
| 00-03 | ✅ Configuration & Logger |
| 00-04 | ✅ Page Architecture |
| 00-05 | ✅ Physical Storage Engine |
| 00-06 | ✅ Database Management |
| 00-07 | ✅ Table Management |
| 00-08 | ✅ Record Management |
| 00-09 | ✅ SQL Command Models |
| 00-10 | ✅ Query Execution Foundation |

---

# 📌 Sprint 00-10

Completed

- Designed Query Execution architecture.
- Implemented ExecuteResult model.
- Implemented QueryExecutionException.
- Added QueryExecutor foundation.
- Completed SQL Command models.
- Prepared Query layer for future SQL Parser integration.

Implemented SQL Commands

- CREATE DATABASE
- USE DATABASE
- DROP DATABASE
- CREATE TABLE
- DROP TABLE
- INSERT
- SELECT
- DELETE

---

# 🔜 Next Sprint (00-11)

Planned Features

- SQL Lexer
- SQL Parser
- Token System
- Query Parsing
- QueryExecutor Integration
- RecordManager Integration
- INSERT execution
- SELECT execution
- DELETE execution

---

# 🛠 Technologies

- Java 21
- Maven
- JUnit 5

---

# 📖 Documentation

Each sprint contains detailed documentation including:

- Developer Notes
- Architecture Documents
- UML / Architecture Diagrams
- Internal Design Notes

---

# 🎯 Project Goal

YEKDB aims to become a fully educational embedded relational database management system where every subsystem is implemented manually for learning modern database internals.

---

# 📜 License

This project is licensed under the MIT License.