# event-ledger - Double-Entry Bookkeeping Engine

Domain layer for a double-entry bookkeeping system. Implements ledger event recording with constructor-level validation, account balance management with optimistic locking, a payout state machine with guarded transitions and lifecycle timestamps, and batch lock infrastructure with fencing tokens for distributed write coordination.

## What It Does

- **Double-entry bookkeeping** - every transaction creates exactly two ledger events (debit + credit) that sum to zero. The composite unique constraint `(idempotency_key, event_type)` enforces idempotency at the database level.
- **Append-only ledger** - `ledger_events` table is insert-only (`updatable = false` on all columns). Account balance is maintained as a snapshot on the `accounts` table with `@Version`-based optimistic locking.
- **Currency-safe arithmetic** - `Money` value object enforces currency-aware scale (derived from `Currency.getDefaultFractionDigits()`), rejects scale overflow, and provides type-safe operations.
- **Payout state machine** - `Payout` entity enforces guarded transitions via an explicit allowed-transitions map with domain-specific methods (`markProcessing`, `markSent`, `markConfirmed`, `fail`). Each transition records a timestamp.
- **Batch lock infrastructure** - `BatchLock` entity models UUID-keyed locks with TTL, fencing tokens, owner verification, and atomic acquisition via native SQL.
- **Manifest pipeline stages** - `ManifestStage` enum defines the 5-stage processing lifecycle: `RECEIVED -> VALIDATED -> ENRICHED -> APPLIED -> SETTLED`.

## High-Load Design

- **Optimistic locking** (`@Version`) on `Account` and `Payout` prevents lost updates under concurrent modifications.
- **Composite idempotency constraint** - unique constraint on `(idempotency_key, event_type)` allows a single business operation to produce both DEBIT and CREDIT events while preventing duplicate processing.
- **Atomic lock acquisition** - native SQL `INSERT ... WHERE NOT EXISTS` prevents race conditions during batch lock acquire.
- **Fencing tokens** - monotonic tokens on batch locks prevent stale owners from corrupting downstream state.
- **JDBC batching** - Hibernate batch size 50 with ordered inserts/updates for efficient bulk persistence.
- **Virtual threads** - Java 21 virtual threads enabled for high-concurrency request handling.

## Architecture

```
com.eventledger
  +-- config/
  |     GlobalExceptionHandler    RFC 9457 ProblemDetail with full error contract
  |     EventLedgerConfig         Type-safe @ConfigurationProperties
  +-- domain/
  |     +-- entity/               JPA entities (Account, LedgerEvent, Payout, BatchLock)
  |     +-- enums/                AccountType, EventType, PayoutStatus, ManifestStage
  |     +-- valueobject/          Money (currency-aware arithmetic)
  |     +-- exception/            Domain exceptions (9 typed exceptions)
  +-- dto/
  |     +-- request/              Jakarta-validated request records
  |     +-- response/             Response records
  +-- repository/                 Spring Data JPA repositories with pagination
```

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (records, pattern matching, virtual threads) |
| Framework | Spring Boot 3.2 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 |
| Validation | Jakarta Validation 3 |
| Testing | JUnit 5 + AssertJ |
| Build | Gradle 8 (Kotlin DSL) |

## Build & Test

```bash
# Build
./gradlew build

# Run tests only
./gradlew test

# Run application (requires PostgreSQL)
./gradlew bootRun

# Run with custom DB
DB_URL=jdbc:postgresql://localhost:5432/event_ledger ./gradlew bootRun
```

## Database Setup

```sql
CREATE DATABASE event_ledger;
CREATE USER event_ledger_user WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE event_ledger TO event_ledger_user;
```

Default `ddl-auto` is `none`. For local development, set `JPA_DDL_AUTO=update`. For production, use Flyway or Liquibase migrations.

## Key Invariants (Tested)

1. `Account.credit`/`debit` reject zero, negative, and null amounts.
2. `Account.debit` enforces non-negative balance for `MERCHANT` accounts via `NegativeBalanceException`.
3. `Account.credit`/`debit` reject currency mismatches via `CurrencyMismatchException`.
4. `Money` enforces currency-aware scale - rejects amounts with more decimal places than the currency allows (e.g., JPY rejects fractional amounts).
5. `Money` comparison (`isGreaterThan`, `isLessThan`, `compareTo`) enforces same-currency constraint.
6. Double-entry DEBIT + CREDIT events for the same transaction sum to zero.
7. `LedgerEvent` constructor rejects zero/negative amounts, blank idempotency keys, blank currencies, and null IDs.
8. Composite unique constraint `(idempotency_key, event_type)` allows one DEBIT and one CREDIT per idempotency key.
9. Payout state machine rejects invalid transitions via `InvalidStateTransitionException`.
10. Payout lifecycle methods (`markProcessing`, `markSent`, `markConfirmed`) record per-stage timestamps.
11. `Payout.fail()` requires a non-blank failure code and respects the FSM - cannot fail from terminal states.
12. `BatchLock.ping()` rejects renewal attempts from non-owners.
13. `BatchLock.isExpired()` accepts explicit `Instant` for deterministic testing.

## Error Contract

All errors follow RFC 9457 ProblemDetail format with extended properties:

| Property | Description |
|---|---|
| `type` | Stable URN (`urn:event-ledger:error:<code>`) |
| `title` | Human-readable error category |
| `status` | HTTP status code |
| `detail` | Specific error description |
| `errorCode` | Machine-readable error code |
| `timestamp` | ISO-8601 timestamp |
| `fieldErrors` | Structured validation errors (on 400) |

Handled exception types: domain exceptions (404/409/422), optimistic lock conflicts (409), data integrity violations (409), validation errors (400), malformed requests (400), and a catch-all fallback (500) that sanitizes internal details.

## Release Status

**0.1.0** - API is stabilising but not yet frozen. Minor versions may include breaking changes until `1.0.0`.

---

## License

MIT - see [LICENSE](./LICENSE)
