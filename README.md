# YEKDB
### Yet Another Embedded Key Database

> A lightweight relational database management system written entirely in Java.

![Java](https://img.shields.io/badge/Java-21-orange)
![Maven](https://img.shields.io/badge/Maven-3.x-blue)
![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Linux%20%7C%20macOS-green)
![Status](https://img.shields.io/badge/Status-Development-yellow)

---

# 📖 About

YEKDB is a relational database management system (RDBMS) developed completely from scratch in Java.

The project is not based on PostgreSQL, MySQL or SQLite source code. Instead, every subsystem is designed and implemented independently to better understand the internal architecture of modern database systems.

The long-term goal of YEKDB is to provide a modular, extensible and educational database engine featuring its own:

- Storage Engine
- Buffer Manager
- Catalog Manager
- SQL Parser
- Query Optimizer
- Execution Engine
- Transaction Manager
- B+Tree Index
- Recovery System
- Network Layer

---

# 🚀 Current Features

- ✅ Modular project architecture
- ✅ Core Engine
- ✅ Storage Engine
- ✅ Configuration Manager
- ✅ Custom Logger System
- ✅ Page Architecture
- ✅ Maven Build System
- ✅ JUnit Test Infrastructure

---

# 🏗 Current Architecture

```
YekdbApplication
        │
        ▼
YekdbEngine
        │
 ┌──────┴─────────┐
 │                │
 ▼                ▼
Configuration   Logger
        │
        ▼
 Storage Engine
        │
        ▼
      Pages
```

---

# 📂 Project Structure

```
src
 ├── main
 │    ├── java
 │    │     └── com.yekdb
 │    │           ├── config
 │    │           ├── core
 │    │           ├── logs
 │    │           ├── storage
 │    │           ├── parser
 │    │           ├── transaction
 │    │           └── ...
 │    └── resources
 │          └── yekdb.properties
 │
 └── test
      └── PageTest
```

---

# 📚 Documentation

Every development sprint is documented.

## Developer Notes

- 00-01 Project Architecture
- 00-02 Core Engine & Storage Engine
- 00-03 Configuration Manager, Logger & Page Architecture

## Design Documents

- 00-01 System Architecture
- 00-02 Core Engine Design
- 00-03 Configuration, Logger & Page Design

---

# 🗺 Roadmap

## Version 0.1

- [x] Core Engine
- [x] Storage Engine
- [x] Configuration Manager
- [x] Logger System
- [x] Page Architecture
- [x] JUnit Infrastructure

## Version 0.2

- [ ] Record
- [ ] Record Serializer
- [ ] DataFile
- [ ] Binary Reader
- [ ] Binary Writer

## Version 0.3

- [ ] Buffer Manager
- [ ] Catalog Manager
- [ ] B+Tree Index
- [ ] SQL Parser

## Version 0.4

- [ ] Query Optimizer
- [ ] Execution Engine
- [ ] Transaction Manager
- [ ] Recovery Manager

---

# 🧪 Running Tests

```bash
mvn test
```

---

# ⚙ Build

```bash
mvn clean install
```

---

# 💻 Requirements

- Java 21
- Maven 3.9+
- IntelliJ IDEA (Recommended)

---

# 📄 License

This project is currently developed for educational and research purposes.

---

# 👨‍💻 Developer

**Yunus Emre KUL**

Computer Engineering Student

İnönü University

---

⭐ If you find this project interesting, consider giving it a star.
