# Member Persistence

Version: 1.0
Sprint: 10.1 (extends persistence infrastructure shipped earlier)
Status: Implemented
Last Updated: 2026-07-07

## Purpose

This document describes how the Member application layer (Sprint 10.1) reaches the persistence
infrastructure that already existed for `MemberProfile`, and the one additive change made to support it.

## The Existing Model: A Profile Is A View Over Participation Rows

`community.group_members` (`GroupMemberJpaEntity`) has no companion `members` table. Each row represents one
user's participation in one specific group; there is no row representing a `MemberProfile` independent of
any group. `MemberRepositoryAdapter.findByUserId` reconstructs a complete `MemberProfile` by loading every
`group_members` row for a `(tenantId, userId)` pair and flat-mapping their participations onto the first
row's profile-level fields (id, member number, status, consents, audit info, version). The existing
`MemberRepositoryAdapter.save` mirrors this: it iterates `member.participations()` and writes one row per
participation, and it explicitly rejects a member with zero participations, since there is nothing else to
write.

This is why `CreateMemberProfileUseCase` (see
[Member Application Layer](../application/member-application.md#why-creation-always-carries-a-group))
always creates the aggregate and its first participation in the same transaction before calling `save`.

## What Changed

Only one method was added, additively, to `member.domain.port.MemberRepository`:

```java
Optional<MemberProfile> findById(AggregateId tenantId, AggregateId memberId);
```

`MemberRepositoryAdapter` implements it by delegating to a new derived Spring Data query,
`findByTenantIdAndIdAndDeletedFalse`, and mapping the single matched row exactly as the pre-existing
`findById(AggregateId memberId)` overload already does (it does not re-assemble every participation row —
that remains specific to `findByUserId`). No JPA entity, mapping method, or Flyway migration changed.
`GroupMemberJpaEntity`, `MemberJpaMapper`, and every pre-existing `MemberRepositoryAdapter`/
`MemberSpringDataRepository` method are unmodified.

## Adapter Placement

Sprint 10.1 explicitly could not modify ArchUnit rules. `GENERAL_INFRASTRUCTURE_MUST_NOT_DEPEND_ON_APPLICATION_OR_INTERFACES`
(in `LayerDependencyArchitectureTest`) only exempts `infrastructure.auth`, `infrastructure.group`, and the
single class `SavingsGroupRepositoryAdapter` by name. A new `infrastructure.member` package containing
adapters for `ClockPort`/`TransactionPort`/`DomainEventPublisherPort`/`MemberNumberGeneratorPort` would have
tripped that rule, and adding a fourth carve-out was out of scope.

Because all four of those ports are `@FunctionalInterface`s, their adapters are ordinary classes with no
persistence concerns, so they are composed instead under `member.interfaces.rest.adapter` and wired from
`member.interfaces.rest.config.MemberInfrastructureConfig` — a package ArchUnit already permits to depend on
Spring, the application layer, and the domain layer, mirroring how `group.interfaces.rest.config` already
composes `SavingsGroupApplicationConfig`. The repository port itself needed no new adapter: Member's
application services depend directly on the pre-existing `member.domain.port.MemberRepository`, so
`MemberRepositoryAdapter` keeps depending only on that domain port, exactly as it does today.

## Testing

- `MemberRepositoryAdapterTest` (new) covers every adapter method, including the new tenant-scoped
  `findById`, using mocked Spring Data and mapper collaborators.
- `MemberPersistencePostgreSqlIntegrationTest` (new) exercises the full stack — Flyway schema, a real
  `SavingsGroup` and two real users, `MemberProfile.create` plus `joinGroup`, `save`, and tenant-scoped
  `findById`/`findByUserId`/`findByMemberNumber` — against a Testcontainers PostgreSQL instance. It is
  skipped cleanly when Docker is unavailable, matching every other Testcontainers suite in this project.
- `RepositoryQueryDerivationTest` and `RepositoryAdapterTest` (both pre-existing, unmodified) already
  enumerate `MemberSpringDataRepository`/`MemberRepositoryAdapter` and therefore automatically validate the
  new derived query and the adapter's domain-port implementation and transaction annotations.
