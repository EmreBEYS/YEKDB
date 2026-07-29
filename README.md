# YEKDB
### Yet Another Embedded Key Database

> A lightweight relational database management system written entirely from scratch in Java.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Version](https://img.shields.io/badge/Version-v0.0.6-blueviolet)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Active%20Development-yellow)
![Tests](https://img.shields.io/badge/JUnit-81%20Tests%20Passed-brightgreen)

---

# 📖 About

YEKDB (Yet Another Embedded Key Database) is a relational database management system (RDBMS) implemented entirely from scratch in Java.

Unlike PostgreSQL, MySQL, or SQLite, this project is **not based on any existing database source code**. Every subsystem is independently designed and implemented to understand the internal architecture of modern database systems.

The long-term objective is to build a complete page-oriented database engine featuring transactions, indexing, SQL parsing, concurrency control, and client-server support.

---

# ✨ Current Features

## Core Infrastructure

- ✅ Configuration System
- ✅ Logging Infrastructure
- ✅ YEKDB Core Engine
- ✅ Cross-platform File Operations

## Physical Storage Engine

- ✅ Storage Engine
- ✅ DataFile Manager
- ✅ Database Header
- ✅ Physical Page Architecture
- ✅ Page Header
- ✅ Page Serializer
- ✅ Page Manager
- ✅ Persistent Database File (`.ydb`)
- ✅ Binary File Format
- ✅ Random Page Access

## Database Management Layer

- ✅ Runtime `Database` Model
- ✅ Persistent `DatabaseMetadata`
- ✅ `CREATE DATABASE`
- ✅ `USE DATABASE`
- ✅ `DROP DATABASE`
- ✅ `LIST DATABASES`
- ✅ Database Existence Control
- ✅ Active Database Tracking
- ✅ Database Name Validation
- ✅ Metadata Serialization and Loading
- ✅ Custom Database Exceptions
- ✅ Failed-Creation Cleanup
- ✅ Database Lifecycle Demo

## Quality Assurance

- ✅ JUnit 5 Test Suite
- ✅ 81 Automated Tests Passed

---

# 🏗 System Architecture

```text
                    Application
                         │
                         ▼
                    YekdbEngine
                         │
             ┌───────────┴───────────┐
             ▼                       ▼
      DatabaseManager          StorageEngine
             │                       │
      ┌──────┴──────┐          ┌─────┴──────────┐
      ▼             ▼          ▼                ▼
   Database   DatabaseMetadata PageManager  DatabaseHeader
                                      │
                                      ▼
                                PageSerializer
                                      │
                                      ▼
                                   DataFile
                                      │
                                      ▼
                                  yekdb.ydb
```

The `DatabaseManager` controls the logical database lifecycle, while the `StorageEngine` manages page-oriented physical persistence.


---

# 🗄 Database Management

YEKDB v0.0.6 introduces the first complete database management layer.

## Supported Operations

```java
DatabaseManager manager = new DatabaseManager(Path.of("data"));

manager.createDatabase("SchoolDB");
manager.useDatabase("SchoolDB");

List<String> databases = manager.listDatabases();

boolean exists = manager.exists("SchoolDB");

manager.dropDatabase("SchoolDB");
```

## Database Lifecycle

```text
CREATE DATABASE
        │
        ▼
Validate Database Name
        │
        ▼
Check Existing Database
        │
        ▼
Create Database Directory
        │
        ▼
Create Database Metadata
        │
        ▼
Write database.meta
        │
        ▼
Database Ready
        │
        ▼
USE DATABASE
        │
        ▼
Active Database
        │
        ▼
DROP DATABASE
        │
        ▼
Recursive Physical Deletion
```

---

# 🧾 Database Metadata

Every database contains a `database.meta` file.

Example:

```text
YEKDB DATABASE
Version=0.0.6
Database=SchoolDB
Created=2026-07-29T09:35:42.126
LastModified=2026-07-29T09:35:42.126
Encoding=UTF-8
PageSize=4096
```

A directory is considered a valid YEKDB database only when the database directory and its `database.meta` file both exist.

---

# 💾 Physical Database Layout

```
+-----------------------------+
| Database Header (128 Bytes) |
+-----------------------------+
| Page 0 (4096 Bytes)         |
+-----------------------------+
| Page 1 (4096 Bytes)         |
+-----------------------------+
| Page 2 (4096 Bytes)         |
+-----------------------------+
```

---

# 📂 Project Structure

```text
YEKDB
│
├── config/
├── data/
├── docs/
├── logs/
│
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/yekdb/
│   │           ├── config/
│   │           ├── core/
│   │           ├── database/
│   │           │   ├── exception/
│   │           │   │   ├── DatabaseAlreadyExistsException.java
│   │           │   │   ├── DatabaseNotFoundException.java
│   │           │   │   └── DatabaseOperationException.java
│   │           │   ├── Database.java
│   │           │   ├── DatabaseManager.java
│   │           │   └── DatabaseMetadata.java
│   │           ├── logging/
│   │           └── storage/
│   │               ├── file/
│   │               ├── page/
│   │               └── record/
│   │
│   └── test/
│       └── java/
│           └── com/yekdb/
│               ├── database/
│               │   └── DatabaseManagerTest.java
│               └── storage/
│
└── pom.xml
```

---

# 🧪 Testing

Current test results:

| Component | Tests |
|-----------|------:|
| DataFile | 7 / 7 |
| DatabaseHeader | 11 / 11 |
| PageSerializer | 11 / 11 |
| PageManager | 13 / 13 |
| StorageEngine | 11 / 11 |
| YekdbEngine | 11 / 11 |
| DatabaseManager | 17 / 17 |

**Total:** **81 / 81 Tests Passed**

Run all tests with:

```bash
mvn clean test
```

---

# 🚀 Roadmap

## Completed

- ✅ Sprint 00-01 — Project Architecture and Environment Setup
- ✅ Sprint 00-02 — Core Engine and Storage Engine Architecture
- ✅ Sprint 00-03 — Configuration, Logging and Page Architecture
- ✅ Sprint 00-04 — Record and Data File Foundations
- ✅ Sprint 00-05 — Physical Storage Engine
- ✅ Sprint 00-06 — Database Management Layer

## Next Sprint

### Sprint 00-07 — Table Management Layer

- ⏳ `Table`
- ⏳ `TableMetadata`
- ⏳ `TableManager`
- ⏳ Column Definitions
- ⏳ Data Type System
- ⏳ `CREATE TABLE`
- ⏳ `DROP TABLE`
- ⏳ `LIST TABLES`
- ⏳ Table Catalog
- ⏳ Physical `.tbl` File Structure
- ⏳ JUnit Tests
- ⏳ Demo Application

## Long-Term Plan

- ⏳ B+ Tree Indexes
- ⏳ SQL Parser
- ⏳ Query Executor
- ⏳ Transaction Manager
- ⏳ Write Ahead Logging (WAL)
- ⏳ Buffer Pool
- ⏳ Multi-Version Concurrency Control (MVCC)
- ⏳ Client / Server Architecture

---

# 📄 Documentation

Each development sprint is documented with detailed technical documentation and developer notes.

Current Sprint 00-06 documents:

```text
docs/
├── YEKDB_Developer_Notes_00-06.docx
└── YEKDB_00-06_Database_Management.docx
```

The documentation set includes:

- Developer Notes
- Technical Architecture Documents
- Design Decisions
- Code Examples
- Physical File Layouts
- Test Reports
- Demo Outputs

---

# 🛠 Technologies

- Java 21
- Maven
- JUnit 5
- IntelliJ IDEA
- Git
- GitHub

---

# 📜 License

This project is licensed under the MIT License.

---

# 👨‍💻 Author

**Yunus Emre KUL**

Computer Engineering Student

Developing a relational database management system from scratch for educational and research purposes.