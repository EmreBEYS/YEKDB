# YEKDB
### Yet Another Embedded Key Database

> A lightweight relational database management system written entirely in Java.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Development-yellow)
![Tests](https://img.shields.io/badge/JUnit-106%20Tests-success)

---

# 📖 About

YEKDB (Yet Another Embedded Key Database) is an educational relational database management system (RDBMS) implemented entirely from scratch in Java.

Unlike PostgreSQL, MySQL or SQLite, YEKDB is **not built on any existing database engine**. Every subsystem is independently designed and implemented to understand the internal architecture of modern database systems.

The project aims to simulate the internal components of a real database engine including:

- Storage Engine
- Page Management
- Record Management
- Database Management
- Table Management
- Index Management
- Query Processing
- Buffer Management
- Transaction Processing
- Recovery Mechanisms

The primary goal of YEKDB is educational research and understanding how professional database systems operate internally.

---

# 🚀 Current Features

## Core System

- Configuration Manager
- Logging System
- Database Engine
- Storage Engine
- Physical File Management

---

## Storage Layer

- Database Header
- Data Pages
- Record Storage
- Page Serialization
- Record Serialization
- Physical Record Storage

---

## Database Layer

- Database Creation
- Database Selection
- Database Metadata
- Table Management
- Record Management

---

## Index Management (Sprint 00-09)

- Primary Index Support
- Unique Index Support
- Non-Unique Index Support
- Index Metadata
- Index Manager
- Generic Index API
- Record Pointer Structure
- Index Entry Structure
- Duplicate Key Protection
- Index Search
- Pointer Update
- Pointer Deletion
- Table Index Management

---

# 📂 Project Structure

```text
src
├── main
│   └── java
│       └── com.yekdb
│           ├── config
│           ├── core
│           ├── database
│           ├── index
│           ├── logging
│           ├── record
│           ├── storage
│           └── table
│
└── test
    └── java
        └── com.yekdb
```

---

# 🧱 Architecture

```
Application
        │
        ▼
 Database Engine
        │
        ▼
 Database Manager
        │
        ▼
 Table Manager
        │
        ▼
 Record Manager
        │
        ▼
 Index Manager
        │
        ▼
 Physical Storage
```

---

# 📦 Sprint Progress

| Sprint | Module | Status |
|---------|--------|--------|
| 00-01 | Core Architecture | ✅ |
| 00-02 | Storage Engine | ✅ |
| 00-03 | Configuration & Logger | ✅ |
| 00-04 | Record Architecture | ✅ |
| 00-05 | Physical Storage Engine | ✅ |
| 00-06 | Database Management | ✅ |
| 00-07 | Table Management | ✅ |
| 00-08 | Record Management | ✅ |
| 00-09 | Index Management | ✅ |

---

# 🧪 Testing

Current test statistics:

| Module | Tests |
|---------|------:|
| RecordPointer | 12 |
| IndexEntry | 11 |
| IndexMetadata | 22 |
| Index | 30 |
| IndexManager | 31 |
| **Total** | **106** |

All tests are passing successfully.

```
106 Tests Passed
Build Successful
```

---

# 🛠️ Technologies

- Java 21
- Maven
- JUnit 5
- IntelliJ IDEA

---

# 🎯 Roadmap

### Completed

- Core Engine
- Storage Engine
- Configuration Manager
- Logger
- Database Management
- Table Management
- Record Management
- Index Management

### Next

- B+ Tree Index Engine
- Buffer Manager
- SQL Parser
- Query Optimizer
- Transaction Manager
- Write Ahead Logging (WAL)
- Recovery Manager
- Cost Based Optimizer
- Multi-user Support

---

# 📄 Documentation

Each sprint contains detailed documentation including:

- Developer Notes
- Technical Documentation
- Architecture Diagrams
- Demo Outputs
- JUnit Test Reports

---

# 📈 Current Project Status

| Component | Status |
|-----------|--------|
| Core Engine | ✅ |
| Storage Engine | ✅ |
| Database Manager | ✅ |
| Table Manager | ✅ |
| Record Manager | ✅ |
| Index Manager | ✅ |
| Documentation | ✅ |
| Unit Tests | ✅ |

---

# 📜 License

This project is licensed under the MIT License.

---

# 👨‍💻 Developer

**Yunus Emre KUL**

Computer Engineering Student

Developed for educational and research purposes.