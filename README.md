# YEKDB
### Yet Another Embedded Key Database

> A lightweight relational database management system written entirely in Java.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Development-yellow)

---

# 📖 About

YEKDB (Yet Another Embedded Key Database) is an educational relational database management system (RDBMS) developed completely from scratch in Java.

The project is **not based on PostgreSQL, MySQL or SQLite source code**. Every subsystem is designed independently to understand how modern database systems work internally.

The objective is to implement every major database component step by step while maintaining a clean, modular and well-documented architecture.

---

# 🚀 Current Features

## Core Engine

- Database initialization
- Configuration Manager
- Logging System
- Exception handling
- Database lifecycle management

## Physical Storage Engine

- Fixed-size pages
- Database file format
- Page serialization
- Record serialization
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

---

# 📂 Project Structure

```text
src
└── main
    └── java
        └── com.yekdb
            ├── config
            ├── core
            ├── database
            ├── logging
            ├── storage
            ├── table
            └── demo
```

---

# 📁 Physical Storage

```text
data/
└── demo_company/
    ├── users.tbl
    ├── products.tbl
    └── orders.tbl
```

Each table is represented as an individual **.tbl** file.

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

| Module | Tests |
|---------|------:|
| Column | 9 |
| Table | 13 |
| TableMetadata | 8 |
| TableCatalog | 21 |
| TableManager | 25 |
| **Total** | **76 ✅** |

All tests are written using **JUnit 5**.

---

# 📸 Screenshots

## Table Management Demo

```
docs/screenshots/table-demo.png
```

## Table Architecture

```
docs/screenshots/table-architecture.png
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

### Next Sprint (00-08)

- Row Management
- INSERT
- DELETE
- UPDATE
- Record Storage
- Page Allocation
- Row Serialization

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

⭐ This repository documents the complete development process of a database management system built entirely from scratch.