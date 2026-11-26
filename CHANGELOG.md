# Changelog

All notable changes to YEKDB are documented in this file.

## [Unreleased] — target v1.0.15

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
