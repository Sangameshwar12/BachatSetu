# Member Persistence

Version: 1.1
Sprint: 10.1 (extends persistence infrastructure shipped earlier), pagination amended by Sprint 10.2
Status: Implemented
Last Updated: 2026-07-07

## Purpose

This document describes how the Member application layer (Sprint 10.1, extended by Sprint 10.2) reaches the
persistence infrastructure that already existed for `MemberProfile`, and the additive changes made to
support it.

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

## Paginated Listing (Sprint 10.2)

`GET /api/v1/members` needs one page item per member, but `community.group_members` stores one row per
*participation* — a member with three group memberships has three rows. `MemberRepository.findPage` (a
second additive method) resolves this by selecting a **representative row per member** rather than paginating
raw rows directly:

```sql
SELECT groupMember FROM GroupMemberJpaEntity groupMember
 WHERE groupMember.tenantId = :tenantId
   AND groupMember.deleted = false
   AND groupMember.joinedAt = (
       SELECT MIN(earliest.joinedAt) FROM GroupMemberJpaEntity earliest
        WHERE earliest.tenantId = groupMember.tenantId
          AND earliest.user = groupMember.user
          AND earliest.deleted = false
   )
```

This is `MemberSpringDataRepository.findRepresentativeRowsByTenantId`, an explicit `@Query` (skipped by
`RepositoryQueryDerivationTest`, verified instead by `MemberPersistencePostgreSqlIntegrationTest`), following
the same "earliest `joinedAt` wins" convention `findByUserId`'s existing assembly logic already uses. Spring
Data paginates and sorts (`memberNumber` or `createdAt`, ascending or descending — the same
`Sort`/`Pageable`-building pattern `SavingsGroupRepositoryAdapter` established in Sprint 9.7) over these
representative rows only, so each member appears exactly once per page regardless of how many groups they
participate in.

For each representative row on the page, `MemberRepositoryAdapter.findPage` reuses the *existing*
`findAllByTenantIdAndUser_IdAndDeletedFalseOrderByJoinedAtAsc` query and the existing private `assemble`
helper — the same two building blocks `findByUserId` already uses — to load the member's complete set of
participations. This means every page item is a fully assembled `MemberProfile`, and no new assembly logic
was written; only the "which members are on this page" query is new.

**Cost:** this reuses `assemble()` once per page item, so a full page of size *N* costs one query for the
representative rows plus up to *N* additional queries (bounded by page size, at most 100 per the pagination
port's own limit). This mirrors the "acceptable at current scale, revisit later" posture Sprint 9.7 already
documented for Savings Group's unindexed name sort — see Operational Limitations below.

**Known edge case:** if two rows for the same user share an identical `joinedAt` instant (theoretically
possible, not observed in practice given `Instant` precision), both would match the `MIN(joinedAt)` subquery
and the member could appear twice on a page. This is not resolved with an additional tie-breaker (would
require a windowed query JPQL does not support cleanly) and is accepted as a documented limitation rather
than engineered away pre-emptively.

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

- `MemberRepositoryAdapterTest` covers every adapter method, including the tenant-scoped `findById` and the
  Sprint 10.2 `findPage` (both the representative-row assembly path and the sort/pagination wiring), using
  mocked Spring Data and mapper collaborators.
- `MemberPersistencePostgreSqlIntegrationTest` exercises the full stack — Flyway schema, a real
  `SavingsGroup` and real users, `MemberProfile.create` plus `joinGroup`, `save`, tenant-scoped
  `findById`/`findByUserId`/`findByMemberNumber`, and (Sprint 10.2) real database-level pagination and
  sorting by member number and creation time — against a Testcontainers PostgreSQL instance. It is skipped
  cleanly when Docker is unavailable, matching every other Testcontainers suite in this project.
- `RepositoryQueryDerivationTest` and `RepositoryAdapterTest` (both pre-existing, unmodified) already
  enumerate `MemberSpringDataRepository`/`MemberRepositoryAdapter` and therefore automatically validate the
  new derived query and the adapter's domain-port implementation and transaction annotations.

## Operational Limitations

Sorting members by `memberNumber` has no dedicated index; tenant-scoped member counts are expected to
remain small enough that this is acceptable until a future sprint proves otherwise — the same posture
Savings Group documented for its own unindexed name sort. The representative-row-per-member listing query
also has no dedicated index beyond what `idx_group_members_tenant_status`/the existing `group_id`/`user_id`
constraints already provide; if member listing volume grows, a dedicated `(tenant_id, user_id, joined_at)`
index would be the natural next step.
