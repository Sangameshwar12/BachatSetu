# Repository Layer

## Scope

The repository layer provides Spring Data access for all thirteen mapped entities and transactional adapters for existing domain repository ports. It contains no controllers, application services, authentication, or business workflows.

## Structure

```text
in.bachatsetu.backend.infrastructure.persistence
  repository/jpa/   Spring Data repository interfaces
  adapter/          Domain port implementations and JPA reference resolution
```

## Transaction Policy

- Repository adapter classes default to `@Transactional(readOnly = true)`.
- Every domain-port `save` method opens a read-write transaction.
- Payment exposes a pessimistic-write lookup for future provider callback serialization; ordinary reads remain unlocked.
- Adapters perform mapping and persistence only. Domain decisions remain inside aggregates.

## Persistence Behavior

- All standard reads exclude soft-deleted rows.
- Existing persistence audit state and optimistic versions are copied to replacement entity snapshots before merge.
- Constraint and optimistic-lock failures are translated to `PersistenceConflictException`.
- Associations are resolved through `JpaReferenceProvider`, backed by `EntityManager.getReference`; mappers do not execute queries.
- Draw persistence saves auction bids in the same transaction as the draw.
- Member rows are assembled into one `MemberProfile` for tenant/user lookup and split by participation on save.

## Explicit Boundaries

- `AuthAccountRepository` has no adapter because authentication is excluded and no auth JPA entity exists.
- `UserProfile` does not contain `tenantId`. `UserRepositoryAdapter` is conditionally enabled only when a `TenantScopeProvider` is supplied by a future tenant-aware application boundary.
- Role, permission, monthly-cycle, installment, and audit-log domain ports do not exist yet. Their Spring Data repositories are available, but no fake domain adapters were introduced.

## Testing

- Structural tests verify domain-port implementation and transaction annotations.
- Exception tests verify persistence failure translation.
- `RepositoryLayerPostgreSqlIntegrationTest` uses PostgreSQL Testcontainers, creates isolated schemas through Hibernate for the test only, starts every Spring Data repository, and executes a real save/query round trip.
- Production schema ownership remains with Flyway. No migration was added in this sprint.
