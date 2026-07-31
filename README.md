# YEKDB
### Yet Another Embedded Key Database

> A lightweight relational database management system written entirely in Java.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Development-yellow)

---

# 📖 About

YEKDB (Yet Another Embedded Key Database) is an educational Relational Database Management System (RDBMS) developed completely from scratch in Java.

The project is **not based on PostgreSQL, MySQL or SQLite source code**. Every subsystem is independently designed and implemented to better understand how modern database systems work internally.

The objective is to build every major database component step by step while maintaining a clean, modular and well-documented architecture.

---

# 🚀 Current Features

## Core Engine

- Database initialization
- Configuration Manager
- Logging System
- Exception handling
- Database lifecycle management

## Physical Storage Engine

- Fixed-size page architecture
- Database file format
- Database Header
- Page Header
- Page Serializer
- Page Manager
- Binary storage architecture

## Database Management

- Create database
- Drop database
- Open database
- Close database
- Database metadata
- Database catalog

## Table Management (Sprint 00-07)

- Create Table
- Drop Table
- Table Catalog
- Table Metadata
- Physical `.tbl` file creation
- Column definitions
- Supported data types
- Schema validation
- Duplicate table detection
- Duplicate column detection
- Table existence checks

## Physical Record Management (Sprint 00-08)

- Row abstraction
- Physical row storage
- Row serialization
- Record serialization
- Record Manager
- Insert records
- Read records
- Update records
- Logical delete
- Automatic Record ID generation
- Multi-page record storage
- Persistent record recovery
- Active / Deleted record management

---

# 📂 Project Structure

```text
src
└── main
    └── java
        └── com.yekdb
            ├── command
            ├── config
            ├── console
            ├── core
            ├── database
            ├── exception
            ├── execution
            ├── index
            ├── logs
            ├── model
            ├── network
            ├── optimizer
            ├── parser
            ├── security
            ├── space
            ├── storage
            │     ├── file
            │     ├── page
            │     ├── record
            │     └── StorageEngine
            └── table
```

---

# 📁 Physical Storage

```text
data/
└── demo_company/
    ├── users.tbl
    ├── products.tbl
    ├── orders.tbl

Storage Pipeline

Row
 ↓
RowSerializer
 ↓
Record
 ↓
RecordSerializer
 ↓
Page
 ↓
PageManager
 ↓
PageSerializer
 ↓
DataFile
 ↓
Physical Storage
```

Each table is stored as an independent physical file while records are managed through binary pages.

---

# 📊 Supported Data Types

| Type | Description |
|------|-------------|
| INT | Integer |
| LONG | Long Integer |
| DOUBLE | Floating Point |
| BOOLEAN | True / False |
| STRING | UTF-8 String |

---

# 📌 Table Management Example

```java
TableManager manager = new TableManager(Path.of("data/company"));

Table users = new Table(
    "users",
    List.of(
        new Column("id", DataType.INT),
        new Column("name", DataType.STRING),
        new Column("email", DataType.STRING)
    )
);

manager.createTable(users);

manager.listTableNames();

manager.dropTable("users");
```

---

# 🧪 Testing

Current test coverage

| Module | Status |
|---------|--------|
| DatabaseManager | ✅ |
| TableManager | ✅ |
| PageManager | ✅ |
| Row | ✅ |
| RowSerializer | ✅ |
| RecordSerializer | ✅ |
| RecordManager | ✅ |

Current test count:

**107+ successful JUnit 5 tests**

The Record Management layer has been validated through:

- Physical page storage
- Binary serialization
- Record insertion
- Record updates
- Logical deletion
- Multi-page storage
- Persistent recovery
- Record ID generation

---

# 📸 Screenshots

## Table Management Demo

```
docs/screenshots/table-demo.png
```

## Record Management Demo

```
docs/screenshots/record-demo.png
```

## Record Architecture

```
docs/screenshots/record-architecture.png
```

## Physical Storage

```
docs/screenshots/storage-engine.png
```

---

# 🛣 Roadmap

## ✅ Sprint 00-01

- Initial project structure
- Core architecture

## ✅ Sprint 00-02

- Core Engine
- Storage Engine

## ✅ Sprint 00-03

- Configuration Manager
- Logger
- Page Architecture

## ✅ Sprint 00-04

- Record & Page System

## ✅ Sprint 00-05

- Physical Storage Engine

## ✅ Sprint 00-06

- Database Management Layer

## ✅ Sprint 00-07

- Table Management Layer

## ✅ Sprint 00-08

- Row Management
- Physical Record Storage
- Record Serialization
- Record Manager
- Insert
- Read
- Update
- Logical Delete
- Persistent Recovery

### Next Sprint (00-09)

- Index Manager
- B+ Tree Index
- Index Pages
- Index Serializer

---

# 📚 Documentation

Each sprint includes:

- Developer Notes
- Technical Documentation
- Architecture Diagrams
- Demo Application
- JUnit Tests

---

# 🛠 Technologies

- Java 21
- Maven
- JUnit 5
- Git
- GitHub

---

# 🎯 Sprint 00-08 Highlights

This sprint introduces the first complete physical record management layer of YEKDB.

Implemented features include:

- Binary Row serialization
- Binary Record serialization
- Physical Record Manager
- Persistent record storage
- Record updates
- Logical deletion
- Automatic Record ID generation
- Multi-page record allocation
- Recovery after reopening database files
- Comprehensive JUnit test coverage

---

# 📄 License

This project is licensed under the MIT License.

---

# 👨‍💻 Author

**Yunus Emre KUL**

Computer Engineering Student

Inonu University

Malatya / Türkiye

---

⭐ This repository documents the complete development process of a relational database management system built entirely from scratch in Java.