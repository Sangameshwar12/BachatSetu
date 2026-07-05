# Identity Persistence

Version: 1.1
Sprint: 8.2, security amendment by Sprint 8.3
Status: Implemented

## Purpose

This layer connects the framework-independent authentication domain to the existing PostgreSQL/JPA persistence boundary. It reuses canonical identity records and adds only authentication state that was absent after Sprint 8.1. Domain behavior remains defined by [Identity Domain](identity-domain.md); shared persistence conventions remain defined by [Repository Layer](../persistence/repository-layer.md) and [Database Migrations](../persistence/database-migrations.md).

## Reused Components

| Component | Authentication use |
| --- | --- |
| `UserJpaEntity` | Canonical user/profile record extended with password hash, authentication status, and role associations |
| `RoleJpaEntity` | Canonical platform or tenant role extended with permission associations |
| `PermissionJpaEntity` | Canonical permission record; database code maps to the lowercase domain name |
| Existing Spring Data repositories | Extended with tenant-aware user lookups and global-role lookup |
| `BaseJpaEntity` | UUID identity, audit metadata, optimistic locking, and soft deletion |
| JPA auditing and MapStruct configuration | Applied without authentication-specific framework policy |

No duplicate user, role, or permission entity, repository, or table is introduced.

## New Components

- `RefreshTokenJpaEntity` persists lifecycle identity, owner, issue/expiry times, and status. It never stores token credential material.
- `OtpVerificationJpaEntity` persists only an opaque OTP hash plus verification attempts, resend count, purpose, owner, expiry, and status.
- Five authentication repository adapters implement ports owned by `auth.domain.port`.
- Authentication-specific MapStruct mappers reconstruct aggregates through event-free `rehydrate(...)` methods.
- `V3__identity_persistence.sql` adds authentication columns, two join tables, and the missing refresh-token and OTP tables.

## Entity Relationships

```mermaid
erDiagram
    USERS }o--o{ ROLES : user_roles
    ROLES }o--o{ PERMISSIONS : role_permissions
    USERS ||--o{ REFRESH_TOKENS : owns
    USERS ||--o{ OTP_VERIFICATIONS : owns
```

All associations are lazy. Cascaded removal and orphan removal are intentionally disabled. Foreign keys use `ON DELETE RESTRICT`; lifecycle deletion is explicit and compatible with the shared soft-delete model.

## Mapping Strategy

- `AuthUserJpaMapper` maps only authentication-owned columns and role identifiers. Profile fields remain untouched.
- Authentication user writes require an existing canonical `identity.users` row. The adapter rejects a missing row instead of inventing tenant, name, or language data that the authentication aggregate does not own.
- User lookups and writes are tenant-scoped through `TenantScopeProvider`, preventing cross-tenant access by contact value or UUID.
- Role lookup prefers the current tenant and falls back to a platform role; assigned role identifiers are rejected when they belong to another tenant. Role codes map directly to uppercase domain names, while permission codes map case-insensitively to lowercase domain names.
- Role and permission references are resolved before association writes. Missing or soft-deleted references fail with `PersistenceResourceNotFoundException`.
- Refresh-token and OTP adapters preserve JPA audit, soft-delete, and optimistic-lock state on updates. OTP replacement is a single adapter transaction.
- Rehydration restores persisted status, associations, audit metadata, and version with an empty domain-event queue.

## Repository Adapters

| Adapter | Port | Transaction policy |
| --- | --- | --- |
| `AuthUserRepositoryAdapter` | `auth.domain.port.UserRepository` | Read-only default; transactional authentication update |
| `RoleRepositoryAdapter` | `RoleRepository` | Read-only default; transactional association upsert |
| `PermissionRepositoryAdapter` | `PermissionRepository` | Read-only default; transactional upsert |
| `RefreshTokenRepositoryAdapter` | `RefreshTokenRepository` | Read-only default; transactional lifecycle upsert |
| `OtpVerificationRepositoryAdapter` | `OtpVerificationRepository` | Read-only default; transactional lifecycle upsert |

Adapters translate optimistic-lock and integrity failures through the existing persistence exception hierarchy. They contain no authentication workflow or authorization decisions.

## Database Migration

Migration V3 is append-only and leaves V1/V2 unchanged. It:

- Adds nullable `password_hash` and `auth_status` columns so existing profile rows remain valid until enrolled for authentication.
- Creates composite-primary-key join tables for user roles and role permissions.
- Creates lifecycle tables with UUID keys, audit columns, versions, soft-delete checks, expiry checks, enum checks, restrictive foreign keys, and partial operational indexes.
- Does not seed associations or credentials.

Sprint 8.3 adds append-only V4. It uses PostgreSQL `pgcrypto` to hash any pre-existing six-digit V3 values before dropping the plaintext column, adds attempt/resend counters and checks, adds `INVALIDATED`, and creates a unique partial index enforcing one pending OTP per user and purpose.

## Persistence Rules

- JPA entities never cross the persistence boundary.
- Domain models remain free of Spring, Hibernate, Jakarta Persistence, and MapStruct.
- Raw passwords and refresh-token credentials are never persisted.
- Plain OTP values are never persisted after V4; only the opaque `otp_hash` representation is mapped by JPA.
- Existing profile ownership and tenancy fields cannot be changed through authentication mappers.
- Every aggregate load excludes soft-deleted root records.
- Optimistic locking is provided by the inherited `@Version` field.

## Testing

- Domain tests verify event-free reconstruction and exact restoration of status, associations, audit metadata, and version.
- Mapper tests verify canonical names, role associations, auth-only user updates, and token/OTP mapping.
- Architecture and repository tests verify adapter-to-port assignment, transaction boundaries, entity placement, lazy associations, and Spring Data query derivation.
- Migration contract tests verify the append-only V3 shape and absence of duplicate canonical identity tables.
- `IdentityPersistencePostgreSqlIntegrationTest` exercises the ports against Flyway-managed PostgreSQL through Testcontainers. It skips when Docker is unavailable, matching the repository's established integration-test policy.

## Provider Boundary

Sprint 8.4 supplies a configurable BCrypt adapter for Sprint 8.3's `HashingPort`; persistence remains algorithm-agnostic and stores only the encoded result. No JWT, SecurityFilterChain, SMS provider, or REST behavior is introduced by persistence.
