# Database Migrations

## Ownership

Flyway is the only production schema owner. Hibernate uses `ddl-auto=validate` and must never create or mutate production tables.

## Migration Set

| Migration | Purpose |
| --- | --- |
| `V1__initial_schema.sql` | Creates five schemas, thirteen mapped tables, foreign keys, unique constraints, check constraints, and indexes. |
| `V2__seed_roles_permissions.sql` | Seeds deterministic platform roles and atomic permissions with stable UUIDs. |
| `V3__identity_persistence.sql` | Extends canonical identity records and adds role, permission, refresh-token, and OTP persistence. |
| `V4__secure_otp_authentication.sql` | Removes plaintext OTP storage and enforces retry, resend, status, and active-challenge constraints. |
| `V5__refresh_token_security.sql` | Adds hash-only refresh credentials, tenant/session ownership, rotation/reuse linkage, and active-session uniqueness. |

## Design Rules

- UUID values are assigned by the domain or deterministic seed data; PostgreSQL UUID generation extensions are unnecessary.
- All timestamps use `TIMESTAMPTZ` and application/database sessions operate in UTC.
- Monetary values use `BIGINT` minor units plus three-character currency codes.
- Foreign keys use restricted deletion. Financial, membership, draw, receipt, and audit history is never cascade-deleted.
- Stable statuses are stored as checked text matching `EnumType.STRING` entity mappings.
- Every mutable mapped table includes audit timestamps, audit actor IDs, optimistic versioning, and soft-delete metadata inherited from `BaseJpaEntity`.
- Indexes cover foreign keys and repository query paths. Partial indexes exclude deleted rows where useful.
- Applied versioned migrations are immutable. Corrections require a new migration.
- Migrations contain no explicit transaction control, concurrent index creation, or destructive reset statements.

## Seed Policy

Seed identifiers and timestamps are deterministic, making clean environments reproducible. Seed writes use conflict-safe updates for the same fixed IDs. Role-permission assignments are not seeded because their association entities are outside the current JPA mapping and repository scope.

## Verification

- `MigrationContractTest` validates filenames, table coverage, safety rules, constraints, indexes, and deterministic seeds without Docker.
- `FlywayMigrationPostgreSqlIntegrationTest` uses PostgreSQL Testcontainers to clean and migrate a real database, verifies a second migration is a no-op, checks seed counts, and starts the application with Hibernate schema validation.
- The PostgreSQL integration test skips when Docker is unavailable; it runs automatically in CI environments with Docker.
