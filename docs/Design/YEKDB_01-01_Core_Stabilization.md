# YEKDB 01-01 Core Stabilization

## Objective

Sprint 01-01 verifies the V1 baseline on the MacBook, reconciles it with the
GitHub repository, records the actual recovery and GUI readiness level, and
applies low-risk hardening without changing SQL behavior or V1 on-disk data
formats.

## Repository and V1 Verification

Verification date: 2026-11-25 (Europe/Istanbul).

| Check | Verified result |
|---|---|
| Remote | `https://github.com/EmreBEYS/YEKDB.git` |
| Initial local branch | `main` at `7bf60d2` |
| Initial worktree | Clean |
| Initial remote state | Local `main` was 6 commits behind `origin/main` |
| Reconciliation | Fast-forward only to `b52d711` |
| Sprint branch | `sprint/01-01-core-stabilization` |
| V1 release tag | `v1.0.12` at `8a1b7a7` |
| Last local V1 packaging commit | `7bf60d2` (`Release YEKDB macOS v1.0.12`) |
| Current development version | Core/CLI and launcher `1.0.14` |
| Storage formats | Database header 1, table header 1, metadata `0.0.6` |

No local user changes were present when the fast-forward was performed. The
existing `sprint/01-00-v2-architecture` branch had already been merged into
GitHub `main` by pull request #2.

The machine default was Java 25 and Maven was not on `PATH`. Verification used
the repository-local Temurin Java 21 toolchain and an existing Maven 3.9.11
distribution. A root Maven wrapper remains a reproducibility task.

## Build and Test Evidence

The initial V1 packaging commit compiled but reported three failures among
2,042 tests. All three were stale CLI test expectations for version `1.0.0`
while the application reported `1.0.12`; they were not storage or SQL engine
failures. GitHub `main` already contained the version-alignment fix and the
Sprint 01-00 compatibility tests.

After reconciliation and the 01-01 hardening in this branch:

- core: 2,048 tests passed, 0 failures, 0 errors, 0 skipped;
- launcher on macOS: 18 tests executed, 17 passed, 1 platform-dependent test skipped;
- launcher on Windows: 18 tests executed, 14 passed, 4 macOS/platform-dependent tests skipped;
- focused recovery verification: 9 tests passed;
- Java compilation target: 21;
- production sources: 374;
- test sources: 241.

## Architecture Findings

The source tree has useful package boundaries for storage, query, transaction,
concurrency, indexes, catalogs, CLI, views, triggers, and procedures. The main
integration risk is `QueryExecutor`, which is currently 3,507 lines and owns
SQL dispatch plus database selection, transaction logging, recovery invocation,
locking, catalogs, and several session-scoped services. A GUI must not call
these responsibilities independently or duplicate terminal behavior.

The safe GUI boundary is a client-neutral session/application service that
accepts SQL and returns stable result/error DTOs. Both the terminal and a future
JavaFX administration application should use that boundary. The existing
`yekdb-launcher` is a 13-class JavaFX installer/updater/runner, now aligned to
`1.0.14`; it is not the V2 database administration GUI.

The Windows update path downloads a matching `YEKDB-Setup-*.exe` release asset
and launches its installer. The macOS update button now downloads and stages a
verified DMG and hands off bundle replacement after launcher exit. See
`YEKDB_01-01_1.0.14_Patch_Notes.md` for prerequisites and verification limits. A local
commit does not publish release assets or update installed applications.
Native Windows installation and macOS upgrade behavior have not been verified
end-to-end in this sprint.

## Recovery and WAL Findings

The current `transaction.log` is a transaction decision log. It records begin,
commit, rollback, access mode, isolation level, and timestamps. Recovery finds
transactions with a begin record but no completion and appends a rolled-back
decision.

It is not yet a physical or physiological write-ahead log:

- records have no LSN, page id, record id, before-image, after-image, or checksum;
- in-memory undo actions do not survive process termination;
- recovery does not redo committed page changes or undo uncommitted page changes;
- page flushes are not ordered behind durable WAL records;
- there is no WAL checkpoint/truncation protocol or crash-injection matrix.

Code and documentation must continue to call this component a transaction
decision log until those guarantees exist.

## Completed Safe Hardening

1. Transaction decision-log appends now take an exclusive file lock and force
   the written bytes and metadata to stable storage before returning.
2. An existing zero-byte log is repaired by writing the unchanged V1 header
   before the next record.
3. Invalid log headers are rejected on both read and append; an unknown format
   is never silently extended.
4. Database selection invokes recovery before loading the table catalog. This
   establishes the correct startup order needed by future physical recovery.
5. Three focused regression tests cover empty-log repair and invalid-header
   rejection. No V1 data, table, database, or log format was changed.

## Actionable Sprint 01-01 Backlog

### P0 — reproducible baseline

- Add a root Maven wrapper pinned to a verified Maven release.
- Add a single verification command for core and launcher tests.
- Keep application and storage format versions independently asserted.

### P0 — recovery contract before WAL implementation

- Define WAL record, LSN, flush-order, checksum, and corruption policies as
  interfaces and tests only; physical WAL implementation remains Sprint 01-03.
- Add process-crash fixtures for insert, update, delete, commit, and checkpoint
  boundaries.
- Make the distinction between decision recovery and data recovery visible in
  public diagnostics.

### P1 — GUI-ready application boundary

- Extract database-session lifecycle and SQL execution from `QueryExecutor`
  behind a client-neutral facade.
- Define stable query result, metadata, and structured error DTOs.
- Move terminal-specific rendering out of reusable execution paths.
- Add contract tests showing terminal and future GUI clients receive identical
  execution semantics.

### P1 — core decomposition

- Split transaction coordination, database selection, catalog lifecycle, and
  lock ownership out of the 3,507-line `QueryExecutor` incrementally.
- Preserve every V1 regression test during each extraction.

## Exit Criteria

Sprint 01-01 can close when the reproducible build entry point, recovery/WAL
contract tests, and client-neutral execution boundary are present; the full
2,048+ core regression suite and launcher suite remain green; and no V1 format
version changes without an explicit migration.
