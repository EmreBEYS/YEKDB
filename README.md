# YEKDB
### Yet Another Embedded Key Database

> A lightweight, modular and educational relational database management system implemented entirely in Java from scratch.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Development-yellow)
![Tests](https://img.shields.io/badge/Tests-31_Passing-brightgreen)

---

# 📖 About

YEKDB (Yet Another Embedded Key Database) is a relational database management system (RDBMS) developed entirely from scratch in Java.

Unlike PostgreSQL, MySQL or SQLite, YEKDB does not reuse any existing database engine or source code. Every subsystem is independently designed and implemented to better understand the internal architecture of modern database systems.

The project focuses on creating a modular, extensible and educational database engine while documenting every development sprint and architectural decision.

The long-term goal of YEKDB is to implement its own:

- Storage Engine
- Buffer Manager
- Catalog Manager
- SQL Parser
- Query Optimizer
- Execution Engine
- Transaction Manager
- Recovery Manager
- B+Tree Index
- Network Layer

---

# 🚀 Current Features

- ✅ Modular Project Architecture
- ✅ Core Engine
- ✅ Yekdb Engine
- ✅ Storage Engine
- ✅ Configuration Manager
- ✅ Custom Logger System
- ✅ Page Architecture
- ✅ Record Model
- ✅ Record Serialization
- ✅ DataFile Management
- ✅ Persistent `.ydb` Storage
- ✅ JUnit Test Suite (31 Tests)
- ✅ Maven Build System

---

# 🏗 Current Architecture

```text
                    YekdbApplication
                           │
                           ▼
                      YekdbEngine
                           │
                           ▼
                     StorageEngine
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
      RecordSerializer             DataFile
              │                         │
              └────────────┬────────────┘
                           ▼
                        yekdb.ydb
```

---

# 📂 Project Structure

```text
src
├── main
│   ├── java
│   │   └── com.yekdb
│   │       ├── config
│   │       ├── core
│   │       ├── logging
│   │       ├── storage
│   │       │   ├── file
│   │       │   ├── page
│   │       │   ├── record
│   │       │   └── serializer
│   │       ├── parser
│   │       ├── transaction
│   │       └── ...
│   └── resources
│       └── yekdb.properties
│
└── test
    └── java
        └── com.yekdb
```

---

# 📚 Documentation

Every development sprint is documented with both **Developer Notes** and **Architecture Documents**.

## 📒 Developer Notes

- ✅ 00-01 Project Architecture
- ✅ 00-02 Core Engine & Storage Engine
- ✅ 00-03 Configuration Manager, Logger & Page Architecture
- ✅ 00-04 Record, Serialization & Storage Engine

## 🏛 Architecture Documents

- ✅ 00-01 System Architecture
- ✅ 00-02 Core Engine Design
- ✅ 00-03 Configuration, Logger & Page Design
- ✅ 00-04 Record & Storage Architecture

---

# 🧪 Test Coverage

| Module | Status |
|---------|:------:|
| Page | ✅ |
| Record | ✅ |
| Record Serializer | ✅ |
| DataFile | ✅ |
| Storage Engine | ✅ |
| Yekdb Engine | ✅ |

**31 / 31 Tests Passing**

---

# 🗺 Development Roadmap

## Version 0.1

- [x] Project Architecture
- [x] Core Engine
- [x] Storage Engine Skeleton
- [x] Configuration Manager
- [x] Custom Logger
- [x] Page Architecture
- [x] JUnit Infrastructure

---

## Version 0.2

- [x] Record Model
- [x] Record Serialization
- [x] DataFile
- [x] Persistent `.ydb` Storage
- [x] Storage Engine Integration
- [x] JUnit Test Suite

---

## Version 0.3

- [ ] Database Header
- [ ] Page Manager
- [ ] Catalog Manager
- [ ] Table Metadata
- [ ] CREATE TABLE
- [ ] INSERT
- [ ] SELECT

---

## Version 0.4

- [ ] SQL Parser
- [ ] Query Optimizer
- [ ] Execution Engine
- [ ] Buffer Manager
- [ ] Transaction Manager
- [ ] Recovery Manager

---

## Version 0.5

- [ ] B+Tree Index
- [ ] Network Layer
- [ ] Multi-user Support
- [ ] Client-Server Architecture
- [ ] Backup & Restore

---

# ⚙ Build

```bash
mvn clean install
```

---

# 🧪 Run Tests

```bash
mvn test
```

---

# 💻 Requirements

- Java 21
- Maven 3.9+
- IntelliJ IDEA (Recommended)

---

# 📈 Current Project Status

| Component | Status |
|-----------|:------:|
| Core Engine | ✅ |
| Storage Layer | ✅ |
| Persistence | ✅ |
| Testing | ✅ |
| SQL Engine | ⏳ |
| Query Engine | ⏳ |
| Transaction Manager | ⏳ |

---

# 📄 License

This project is currently developed for educational and research purposes.

A dedicated open-source license will be selected in a future release.

---

# 👨‍💻 Developer

**Yunus Emre KUL**

Computer Engineering Student

İnönü University

GitHub: **EmreBEYS**

---

⭐ **If you find this project interesting, consider giving it a star!**