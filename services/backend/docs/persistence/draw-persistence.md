# Draw Persistence

Version: 1.0
Sprint: 11.2 (extends persistence infrastructure shipped earlier)
Status: Implemented
Last Updated: 2026-07-07

## Purpose

This document describes how the Draw application layer (Sprint 11.2) reaches the persistence infrastructure
that already existed for `Draw` — `DrawJpaEntity`, `AuctionBidJpaEntity`, `DrawJpaMapper`,
`AuctionBidJpaMapper`, `DrawSpringDataRepository`, and `DrawRepositoryAdapter`, all shipped ahead of this
sprint — and the additive changes made to support tenant-scoped lookup and pagination.

## The Existing Model

`community.draws` maps one row per `Draw` aggregate, with a `OneToMany` child table
(`community.auction_bids`) for `AuctionBid`. `DrawJpaMapper.toDomain`/`toEntity` and
`AuctionBidJpaMapper.toDomain`/`toEntity` were already complete and are unmodified by this sprint. The
schema, unique constraint (`uk_draws_cycle` — one draw per monthly cycle), and indexes needed for
tenant-scoped and group-scoped lookup already existed; no Flyway migration was required or added.

## What Changed

Two methods were added, additively, to `draw.domain.port.DrawRepository`:

```java
Optional<Draw> findById(AggregateId tenantId, AggregateId drawId);

DrawPage<Draw> findPage(AggregateId tenantId, DrawPageRequest pageRequest);
```

`DrawRepositoryAdapter` implements the first by delegating to a new derived Spring Data query,
`findByTenantIdAndIdAndDeletedFalse`, mirroring the equivalent method already added to
`SavingsGroupRepositoryAdapter` (Sprint 9.x), `MemberRepositoryAdapter` (Sprint 10.1), and
`PaymentRepositoryAdapter` (Sprint 11.1). It implements the second with a second new derived query,
`findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable)` — a *pure* derived method requiring no
`@Query`, since each draw is already exactly one row. `DrawRepositoryAdapter` builds the `Sort`/`Pageable`
from the framework-free `DrawPageRequest` and converts the returned Spring Data `Page<DrawJpaEntity>` back
into `DrawPage<Draw>`, exactly as the other adapters already do. Sorting by `SCHEDULED_AT` maps to the
`scheduledAt` entity property; sorting by `CREATED_AT` maps to `createdAt`.

No JPA entity, mapping method, or Flyway migration changed. `DrawJpaEntity`, `AuctionBidJpaEntity`,
`DrawJpaMapper`, `AuctionBidJpaMapper`, and every pre-existing `DrawRepositoryAdapter`/
`DrawSpringDataRepository` method — including `findByGroupAndNumber`, `findByCycleId`, and `save` (which
persists both the draw row and every bid row in one call) — are unmodified.

## Adapter Placement

Sprint 11.2 explicitly could not modify ArchUnit rules, and no carve-out exists for a `draw` adapter
depending on `draw.application` (`GENERAL_INFRASTRUCTURE_MUST_NOT_DEPEND_ON_APPLICATION_OR_INTERFACES` only
exempts `infrastructure.auth`, `infrastructure.group`, and `SavingsGroupRepositoryAdapter` by name).
Following the exact resolution used for Member (Sprint 10.1) and Payment (Sprint 11.1), the
`ClockPort`/`TransactionPort`/`DomainEventPublisherPort` adapters — all `@FunctionalInterface`s with no
persistence concerns — are composed under `draw.interfaces.rest.adapter` and wired from
`draw.interfaces.rest.config.DrawInfrastructureConfig`, a package ArchUnit already permits to depend on
Spring, the application layer, and the domain layer. `DrawRepositoryAdapter` needed no new adapter or port:
Draw's application services depend directly on the pre-existing `draw.domain.port.DrawRepository`, so the
adapter keeps depending only on that domain port, exactly as it does today.

## Known Limitations

One gap in the *pre-existing* persistence mapping (predates this sprint and is unrelated to the additive
changes above) is worth documenting rather than fixing, per the sprint brief's "document, don't invent"
rule:

- **`DrawNumber` is not stored as its own column.** `DrawJpaEntity` has no `draw_number` column.
  `DrawJpaMapper.toDomain` derives it as `new DrawNumber(entity.getCycle().getCycleNumber())` — the draw
  number is always read back as the linked `MonthlyCycle`'s own `cycle_number`, not as an independently
  persisted value. `CreateDrawRequest.drawNumber` is accepted from the caller and passed through to
  `Draw.schedule(...)` unchanged, but the value that survives a reload is whatever `cycle_number` the
  referenced `MonthlyCycle` row actually has — the API contract only holds if the caller supplies a
  `drawNumber` that matches the cycle's own number. Enforcing that match, or adding an independent column,
  would mean altering the existing schema/mapping, which this additive sprint does not do.

## Testing

- `DrawRepositoryAdapterTest` (new) covers every adapter method, including the pre-existing
  `findById(drawId)`/`findByGroupAndNumber`/`findByCycleId`/`save` (previously untested at the adapter
  level) and the Sprint 11.2 additions (`findById(tenantId, drawId)`, `findPage`), using mocked Spring Data
  and mapper collaborators.
- `DrawPersistencePostgreSqlIntegrationTest` (new) exercises the full stack — Flyway schema, a real
  `SavingsGroup`, a real `MonthlyCycle` row, a real joined `MemberProfile`, `Draw.schedule`/`open`/
  `submitBid`/`complete`, `save`, tenant-scoped `findById`, and real database-level pagination and sorting
  by scheduled time and creation time — against a Testcontainers PostgreSQL instance. It is skipped cleanly
  when Docker is unavailable, matching every other Testcontainers suite in this project.
- `RepositoryQueryDerivationTest` and `RepositoryAdapterTest` (both pre-existing, unmodified) already
  enumerate `DrawSpringDataRepository`/`DrawRepositoryAdapter` and therefore automatically validate the new
  derived queries and the adapter's domain-port implementation and transaction annotations.
