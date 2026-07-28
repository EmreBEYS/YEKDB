# YEKDB
### Yet Another Embedded Key Database

> A lightweight relational database management system written entirely from scratch in Java.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Active%20Development-yellow)
![Tests](https://img.shields.io/badge/JUnit-64%20Tests%20Passed-brightgreen)

---

# 📖 About

YEKDB (Yet Another Embedded Key Database) is a relational database management system (RDBMS) implemented entirely from scratch in Java.

Unlike PostgreSQL, MySQL, or SQLite, this project is **not based on any existing database source code**. Every subsystem is independently designed and implemented to understand the internal architecture of modern database systems.

The long-term objective is to build a complete page-oriented database engine featuring transactions, indexing, SQL parsing, concurrency control, and client-server support.

---

# ✨ Current Features

- ✅ Configuration System
- ✅ Logging Infrastructure
- ✅ Storage Engine
- ✅ DataFile Manager
- ✅ Database Header
- ✅ Physical Page Architecture
- ✅ Page Header
- ✅ Page Serializer
- ✅ Page Manager
- ✅ Persistent Database File (.ydb)
- ✅ Binary File Format
- ✅ Random Page Access
- ✅ Cross-platform Support
- ✅ JUnit Test Suite

---

# 🏗 Storage Architecture

```
                 YEKDB Engine
                       │
                       ▼
                Storage Engine
                       │
          ┌────────────┴────────────┐
          ▼                         ▼
   DatabaseHeader             PageManager
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

```
YEKDB
│
├── config/
├── data/
├── docs/
├── logs/
│
├── src
│   ├── main
│   │   └── java
│   │       └── com.yekdb
│   │           ├── core
│   │           ├── storage
│   │           │   ├── file
│   │           │   ├── page
│   │           │   └── record
│   │           └── logging
│   │
│   └── test
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

**Total:** **64 / 64 Tests Passed**

---

# 🚀 Roadmap

## Completed

- ✅ Project Architecture
- ✅ Configuration Manager
- ✅ Logger System
- ✅ Storage Engine
- ✅ Database Header
- ✅ Physical Page Architecture
- ✅ Binary Page Serialization
- ✅ Persistent Database File
- ✅ Random Page Access

## In Progress

- 🔄 Record Manager
- 🔄 Slot Directory
- 🔄 Free Space Manager
- 🔄 Record Allocation

## Planned

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

Examples:

- Developer Notes
- Storage Engine Documentation
- Architecture Documents
- Design Decisions
- Test Reports

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