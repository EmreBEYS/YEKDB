# YEKDB v1.0.15 — Physical Write-Ahead Log

Includes binary physical WAL, monotonic LSN, page and transaction IDs, before/after images, CRC32 validation and forced WAL-before-data writes through StorageEngine's PageManager. Existing UTF-8 transaction logging and V1 disk formats are preserved.

Packages: Windows x64 EXE and macOS Apple Silicon arm64 DMG, with bundled Java 21 runtimes and SHA256 checksums.

Scope: physical logging foundation; automatic physical redo/undo replay and full transaction crash recovery are not implemented by this increment. Legacy PageManager instances constructed without a WAL manager remain unlogged.

Validation: release publication requires core tests, Windows packaging, macOS packaging and checksum verification to succeed.
