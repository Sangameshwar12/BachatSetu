# Persistence Foundation

The persistence foundation is an outbound adapter layer. It provides reusable JPA and MapStruct infrastructure without mapping any BachatSetu business aggregate.

## Structure

```text
in.bachatsetu.backend.infrastructure.persistence
  adapter/       Abstract repository adapter support
  audit/         JPA auditing and current-auditor port
  config/        Spring Data and entity scanning
  entity/        BaseJpaEntity only
  exception/     Technology-level persistence exception hierarchy
  flyway/        Flyway integration boundary
  mapper/        Domain mapping contracts and MapStruct policy
  repository/    Base Spring Data repository contracts
```

Tests mirror this structure under `src/test/java/in/bachatsetu/backend/infrastructure/persistence`.

## Decisions

- Domain repository interfaces remain in the domain packages. Future concrete adapters will implement those ports inside infrastructure.
- `BaseJpaEntity` is a mapped superclass, not a business entity. It supplies assigned UUID identity, UTC audit metadata, optimistic versioning, and explicit soft-delete metadata.
- UUIDs are assigned by the domain before persistence; database-generated identifiers are intentionally not used.
- `CurrentAuditorProvider` is a replaceable infrastructure port. Its default returns no actor until an authenticated actor integration is introduced in a later sprint.
- JPA auditing can be disabled with `bachatsetu.persistence.auditing.enabled=false` for isolated tests.
- Explicit Spring Data scanning can be disabled with `bachatsetu.persistence.repositories.enabled=false` when no `EntityManagerFactory` exists.
- `BaseJpaRepository` and `ReadOnlyJpaRepository` are marked `@NoRepositoryBean`, preventing Spring from instantiating incomplete generic repositories.
- Generic adapter reads exclude soft-deleted rows. Whether an aggregate may be deleted remains a module-level policy.
- `UpdatableDomainMapper` separates insert mapping from in-place update mapping so adapters can preserve JPA-managed identity and audit fields.
- MapStruct applies constructor injection and fails compilation for unmapped targets or unsafe type conversions.
- Hibernate schema generation is disabled. Flyway remains the sole schema owner, with Hibernate set to `validate`.
- `db/migration` is intentionally empty in this sprint; no schema migration has been introduced.
- PostgreSQL integration tests extend `PostgreSqlIntegrationTest`, which uses Testcontainers and skips cleanly when Docker is unavailable.

## Future Module Adapter Shape

A future module may add a concrete JPA entity, Spring Data repository, MapStruct mapper, and repository adapter beneath this persistence root. Those types must remain outside the domain packages and must not introduce persistence annotations into domain objects.

Concrete entity mapping decisions are documented in [jpa-entity-mapping.md](jpa-entity-mapping.md).

Repository implementation decisions are documented in [repository-layer.md](repository-layer.md).
