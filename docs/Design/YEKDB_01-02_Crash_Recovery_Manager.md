# YEKDB 01-02 Crash Recovery Manager

## Scope

Sprint 01-02 turns the 01-01 physical WAL into a startup recovery mechanism. The recovery manager
runs after the data file and WAL are opened, but before `PageManager` is exposed or the physical
page count is validated.

## WAL transaction contract

New recovery-aware work uses this durable sequence:

1. `StorageEngine.beginWalTransaction()` forces `TRANSACTION_BEGIN` and returns a restart-safe ID.
2. `writePage(transactionId, page)` forces the complete before/after image before changing data.
3. `commitWalTransaction(transactionId)` forces `TRANSACTION_COMMIT`.

A crash before step 3 leaves a loser. A durable commit makes the transaction a winner even if its
data page is missing or stale. Recovery appends `TRANSACTION_ROLLBACK` only after loser page images
have been restored and synced. A crash during recovery is therefore safe to retry.

Positive-ID 01-01 page records that have no boundary records are treated as implicitly committed.
This compatibility rule is intentionally limited to boundary-free legacy transactions. A malformed
mix of legacy page records and later boundaries is rejected.

## Recovery phases

### Analysis

The complete valid WAL prefix is scanned. Analysis builds transaction state from begin, commit and
rollback boundaries, validates page-image sizes and rejects impossible transitions with the LSN and
transaction ID in the error. Complete records with invalid magic, length, version, LSN ordering or
CRC remain fatal. Only an incomplete physical tail is truncated.

### Redo planning

Committed, system and legacy-compatible updates contribute their after image. The manager computes
one final desired image per page instead of replaying every intermediate write. This prevents a
second recovery from rewriting a page that is already correct.

### Undo planning

Updates belonging to transactions that ended without commit do not contribute their after image.
The earliest before image remains the desired page state. A newly allocated loser page has an empty
before image and is removed only when it is still the physical tail; a non-tail removal is reported
as an invalid recovery sequence rather than creating a sparse database file.

After page images are applied, the database-header page count is reconciled with the aligned
physical file size and the data file is synced. Rollback completion records are then forced.

## Idempotency

- Already-correct final page images are byte-compared and skipped.
- A loser receives a durable rollback boundary only after its undo is stable.
- If recovery crashes before that boundary, the same before image is safely selected again.
- If recovery completes, the next startup sees no incomplete transaction.
- Torn-tail repair is reported in `CrashRecoveryResult`.

## Failure policy

Recovery refuses startup for complete WAL corruption, malformed transaction transitions, invalid
page-image sizes, missing update targets, data-file gaps and unsafe non-tail page removal. Errors use
`WalCorruptionException`; startup closes both files without masking the original failure.

## Verification

`CrashRecoveryManagerIntegrationTest` launches `CrashRecoveryProcess` in a separate JVM and ends it
with `Runtime.halt(23)`. The suite covers:

- undo after a forced halt with an uncommitted page update;
- redo of a committed update when the data page is stale;
- a second recovery with zero redo/undo writes;
- CRC corruption rejection without WAL truncation;
- safe incomplete-tail truncation and reporting;
- commit-without-begin sequence rejection;
- legacy physical-WAL and complete V1 regression compatibility.

## Deliberate limits

- Transaction IDs returned by `beginWalTransaction()` are physical-WAL IDs. Wiring every SQL
  transaction call path to these boundaries is separate from the existing UTF-8 decision log.
- WAL checkpoint truncation and page-LSN storage are not part of 01-02.
- Recovery assumes the existing lock layer prevents conflicting concurrent writers from violating
  physical page-allocation order.
