# Changelog

All notable changes to YEKDB are documented in this file.

## [1.0.16] — 2026-11-28

### Added

- Startup crash recovery with explicit analysis, redo and undo phases.
- Durable `TRANSACTION_BEGIN`, `TRANSACTION_COMMIT` and recovery-generated
  `TRANSACTION_ROLLBACK` WAL boundaries.
- Redo of committed page updates and rollback of transactions left incomplete by a crash.
- Final-page-image planning so repeated recovery converges without rewriting already-correct pages.
- Recovery summaries covering winners, losers, physical changes, idempotent skips and torn-tail repair.
- Separate-JVM forced-halt tests, repeated-recovery tests, malformed-sequence tests and corruption tests.

### Changed

- `StorageEngine.initialize()` now runs physical recovery before constructing `PageManager` or
  validating the page count.
- New databases discard an orphaned same-name WAL sidecar before creating their first WAL header.
- Core and launcher development versions are aligned to `1.0.16`.

### Compatibility

- Sprint 01-01 page records without transaction boundaries remain valid and are treated as
  implicitly committed.
- The V1 data-page and database-header formats remain unchanged.
- The UTF-8 transaction decision log remains independent from the binary physical WAL.

## [1.0.15 foundation] — physical WAL (not tagged by this change)

### Added

- Versioned binary physical WAL files next to Storage Engine data files.
- Strictly increasing, restart-safe log sequence numbers (LSNs).
- Transaction ID and page ID metadata for every physical page update.
- Complete before/after page images for updates and an empty before image for newly allocated pages.
- CRC32 protection and structural validation for every WAL record.
- Torn-tail detection and truncation to the last complete durable record.
- Unit and integration coverage for persistence, corruption, partial records and WAL-before-data
  ordering.

### Changed

- Storage Engine page writes now force their WAL record before modifying the data file.
- Writes made through the Storage Engine's exposed `PageManager` receive the same WAL-before-data
  protection, preserving existing RecordManager and B+ Tree call paths.

### Compatibility

- The UTF-8 `transaction.log`, `TransactionDurabilityLog` and `TransactionRecoveryManager` remain
  unchanged.
- Existing V1 database and table formats remain unchanged.
- The complete 2,054-test V1 regression suite passes.
- The latest existing tag is `v1.0.14`, so the next compatible release target is `v1.0.15` rather
  than `v0.1.0.15`.
