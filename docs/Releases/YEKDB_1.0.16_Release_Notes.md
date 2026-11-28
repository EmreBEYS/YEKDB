# YEKDB v1.0.16 — Crash Recovery

YEKDB 1.0.16 introduces startup crash recovery on top of the physical write-ahead log delivered by
Sprint 01-01. The engine can now distinguish committed and incomplete physical transactions, redo
durable work and restore before images after an interrupted transaction.

## Highlights

- Physical WAL startup analysis, redo and undo.
- Durable transaction begin, commit and rollback boundaries.
- Committed transaction replay and incomplete transaction rollback.
- Retry-safe recovery using final-page-image comparison and durable rollback completion.
- Strict corruption diagnostics and explicit torn-tail repair reporting.
- Automatic recovery before Storage Engine page validation.

## Reliability

Recovery computes one final image per affected page and skips pages whose bytes are already correct.
An incomplete transaction receives its durable rollback record only after restored pages have been
synced. A crash during recovery can therefore be retried without changing the result.

Complete WAL records with invalid structure, LSN ordering or CRC32 stop startup with a diagnostic
that includes the relevant LSN and transaction ID. Only a genuinely incomplete WAL tail is repaired
automatically.

## Compatibility

- Existing V1 database headers and 4096-byte page format are unchanged.
- Boundary-free Sprint 01-01 WAL page records are accepted as implicitly committed.
- Existing UTF-8 transaction decision logging remains available and unchanged.
- Core, CLI and launcher versions are aligned to `1.0.16`.

## Verification

- Core regression suite: 2,059 tests passed.
- Separate-JVM forced-halt redo and undo scenarios passed.
- Repeated recovery, CRC corruption, torn-tail and malformed-sequence tests passed.
- Launcher suite: 18 tests, 0 failures, 4 platform-specific skips.
- Core and launcher JAR packaging passed locally before publication.

## Packages

- `YEKDB-Setup-1.0.16.exe`
- `YEKDB-1.0.16-arm64.dmg`
- SHA-256 checksum files for both platforms

The macOS package uses ad-hoc signing. Windows code signing depends on the release runner's signing
configuration.
