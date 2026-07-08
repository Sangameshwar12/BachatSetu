# Auction Persistence

Version: 1.0
Sprint: 11.8 (extends persistence infrastructure shipped in Sprint 11.2)
Status: Implemented
Last Updated: 2026-07-08

## Purpose

This document describes how the Auction application layer (Sprint 11.8) reaches the persistence
infrastructure that already existed for `Draw` — `DrawJpaEntity`, `AuctionBidJpaEntity`, `DrawJpaMapper`,
`AuctionBidJpaMapper`, `DrawSpringDataRepository`, `AuctionBidSpringDataRepository`, and
`DrawRepositoryAdapter`, all shipped ahead of this sprint (Sprint 11.2) — and the one additive method added
to support type-filtered pagination.

## The Existing Model

`community.draws` maps one row per `Draw` aggregate (of any `DrawType`, including `AUCTION`), with a
`OneToMany` child table (`community.auction_bids`) for `AuctionBid`. Bid persistence was already fully
implemented before this sprint: `DrawRepositoryAdapter.save(Draw)` loops over `draw.bids()` and persists
each one individually through `AuctionBidSpringDataRepository`, separately from the parent entity save.
`DrawJpaMapper.toDomain`/`toEntity` and `AuctionBidJpaMapper.toDomain`/`toEntity` are unmodified by this
sprint. The schema, unique constraint (`uk_draws_cycle`), and indexes needed for tenant-scoped, type-scoped,
and group-scoped lookup already existed. No Flyway migration was required or added for Sprint 11.8.

## What Changed

One method was added, additively, to `draw.domain.port.DrawRepository`:

```java
DrawPage<Draw> findPageByType(AggregateId tenantId, DrawType type, DrawPageRequest pageRequest);
```

`DrawRepositoryAdapter` implements it by delegating to a new derived Spring Data query,
`findAllByTenantIdAndTypeAndDeletedFalse(UUID tenantId, DrawType type, Pageable pageable)` — a *pure*
derived method requiring no `@Query`, mirroring the shape of the pre-existing
`findAllByTenantIdAndDeletedFalse`. It builds the `Sort`/`Pageable` from the same framework-free
`DrawPageRequest` `findPage` already uses, and converts the returned Spring Data `Page<DrawJpaEntity>` back
into `DrawPage<Draw>` identically. `ListAuctionsApplicationService` calls this new method — not the
pre-existing `findPage` — so that `AUCTION`-only results and their pagination totals are computed at the
database, not filtered in memory after the fact (which would corrupt `totalElements`/`totalPages` for a
tenant with mixed draw types).

No JPA entity, mapping method, or Flyway migration changed. `DrawJpaEntity`, `AuctionBidJpaEntity`,
`DrawJpaMapper`, `AuctionBidJpaMapper`, and every pre-existing `DrawRepositoryAdapter`/
`DrawSpringDataRepository` method — including the bid-persistence loop inside `save()` — are unmodified.

## Adapter Placement

Auction introduces no persistence adapter of its own. Its application services depend directly on the
pre-existing `draw.domain.port.DrawRepository` and `group.application.port.SavingsGroupRepository`, so
`DrawRepositoryAdapter` remains the sole adapter implementing draw/auction persistence, exactly as it does
for Draw. Following the exact resolution used for Draw (Sprint 11.2), Notification (Sprint 11.7), and their
predecessors, Auction's own `ClockPort`/`TransactionPort`/`DomainEventPublisherPort` adapters — all
`@FunctionalInterface`s with no persistence concerns — are composed under
`auction.interfaces.rest.adapter` and wired from `auction.interfaces.rest.config.AuctionInfrastructureConfig`,
a package ArchUnit already permits to depend on Spring, the application layer, and the domain layer.

## Known Limitations

No new limitations are introduced by this sprint. The pre-existing `DrawNumber`-is-not-its-own-column
limitation documented in [draw-persistence.md](draw-persistence.md#known-limitations) applies identically to
auction-type draws, since they share the same `DrawJpaEntity`/`DrawJpaMapper`.

## Testing

- `DrawRepositoryAdapterTest` (extended) gained one new test method covering `findPageByType`, verifying the
  type parameter is passed through to `findAllByTenantIdAndTypeAndDeletedFalse` and the returned
  `DrawPage<Draw>` is built correctly. Every pre-existing test on this class is unmodified.
- `AuctionPersistencePostgreSqlIntegrationTest` (new) exercises the full stack against a Testcontainers
  PostgreSQL instance — Flyway schema, a real `SavingsGroup`, real joined `MemberProfile` bidders, multiple
  competing bids with a real leader change, `Draw.complete`/`winner()` after a full save-and-reload cycle,
  and `findPageByType` returning only `AUCTION`-type draws (with correct pagination and totals) from a tenant
  that also has `RANDOM`- and `FIXED_ROTATION`-type draws. It is skipped cleanly when Docker is unavailable,
  matching every other Testcontainers suite in this project.
- `RepositoryQueryDerivationTest` and `RepositoryAdapterTest` (both pre-existing, unmodified) already
  enumerate `DrawSpringDataRepository`/`DrawRepositoryAdapter` and therefore automatically validate the new
  derived query and the adapter's domain-port implementation and transaction annotations.
