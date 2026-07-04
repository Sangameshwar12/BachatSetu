# Database Standards

PostgreSQL is the primary system of record for BachatSetu. Database design must prioritize correctness, traceability, and long-term operability.

## Database Principles

- Financial data must be immutable or corrected through explicit reversal records.
- Every important state transition must be auditable.
- Database constraints should enforce invariants whenever possible.
- Migrations must be deterministic and reviewed.
- Production data must never be changed through ad hoc manual edits without an approved runbook.

## Migration Tooling

Use Flyway for schema migrations.

Migration naming:

```text
V{major}_{minor}_{patch}__{description}.sql
```

Examples:

```text
V1_0_0__create_identity_tables.sql
V1_1_0__create_bhishi_tables.sql
V1_2_0__create_ledger_tables.sql
```

Rules:

- Never modify an applied migration.
- Create a new migration for every schema change.
- Keep migrations backward-compatible during rolling deployments.
- Separate schema migrations from data correction scripts.

## Naming

- Use snake_case for tables and columns.
- Use plural table names.
- Use `_id` suffix for identifiers.
- Use `_at` suffix for timestamps.
- Use `_date` suffix for date-only columns.
- Use `_amount` or `_amount_paise` for monetary values.

## Required Columns

Most tenant-owned tables should include:

```text
id
tenant_id
created_at
updated_at
created_by
updated_by
version
```

Soft-deletable tables should include:

```text
deleted_at
deleted_by
is_deleted
```

## Identifiers

Prefer UUID or ULID for external identifiers.

Do not expose sequential database IDs in public APIs if enumeration would create a security or privacy risk.

## Money

- Store INR amounts in paise as integer values.
- Do not store money in floating-point columns.
- Store currency code when future multi-currency support is plausible.
- Use explicit rounding rules in domain logic.
- Recalculate totals server-side.

## Time

- Store timestamps in UTC.
- Use timezone-aware timestamp types.
- Convert to local display timezone only at the application edge.
- Store business dates separately when needed for collection cycles.

## Ledger Standards

The ledger should be append-only.

Ledger entries should include:

- Tenant
- Account
- Direction
- Amount
- Currency
- Source transaction
- Reference type
- Reference ID
- Entry timestamp
- Created by system actor or user

Corrections must be represented as compensating entries, not destructive updates.

## Indexing

Every foreign key used in joins should have an index unless a measured reason exists not to.

Common indexes:

- `tenant_id`
- `created_at`
- `status`
- Foreign keys
- Composite indexes for common tenant-scoped queries

Review indexes quarterly using production query metrics.

## Data Retention

Retention policies must account for:

- Financial records
- Audit logs
- Authentication logs
- User PII
- Communication history
- Exported reports

Legal and compliance counsel must review final retention timelines before production launch.

## Backups

Production must have:

- Automated backups
- Point-in-time recovery
- Restore testing
- Environment-specific retention
- Encrypted snapshots

Backup restore drills should happen before launch and at least quarterly after launch.

