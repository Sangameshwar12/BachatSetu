# Payment Persistence

Version: 1.0
Sprint: 11.1 (extends persistence infrastructure shipped earlier)
Status: Implemented
Last Updated: 2026-07-07

## Purpose

This document describes how the Payment application layer (Sprint 11.1) reaches the persistence
infrastructure that already existed for `Payment` — `PaymentJpaEntity`, `PaymentJpaMapper`,
`PaymentSpringDataRepository`, and `PaymentRepositoryAdapter`, all shipped ahead of this sprint — and the
additive changes made to support tenant-scoped lookup and pagination.

## The Existing Model

Unlike Member's `community.group_members` (one row per participation, requiring assembly across rows),
`finance.payments` maps one row per `Payment` aggregate — a conventional one-to-one entity mapping.
`PaymentJpaMapper.toDomain`/`toEntity` were already complete and are unmodified by this sprint. Both the
schema (`V1__initial_schema.sql`) and the indexes needed for tenant-scoped lookup, status filtering, and
provider-reference lookup already existed; no Flyway migration was required or added.

## What Changed

Two methods were added, additively, to `payment.domain.port.PaymentRepository`:

```java
Optional<Payment> findById(AggregateId tenantId, AggregateId paymentId);

PaymentPage<Payment> findPage(AggregateId tenantId, PaymentPageRequest pageRequest);
```

`PaymentRepositoryAdapter` implements the first by delegating to a new derived Spring Data query,
`findByTenantIdAndIdAndDeletedFalse`, mirroring the equivalent method already added to
`SavingsGroupRepositoryAdapter` (Sprint 9.x) and `MemberRepositoryAdapter` (Sprint 10.1). It implements the
second with a second new derived query, `findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable)`
— a *pure* derived method requiring no `@Query`, since (unlike Member's representative-row problem) each
payment is already exactly one row. `PaymentRepositoryAdapter` builds the `Sort`/`Pageable` from the
framework-free `PaymentPageRequest` and converts the returned Spring Data `Page<PaymentJpaEntity>` back into
`PaymentPage<Payment>`, exactly as `SavingsGroupRepositoryAdapter` and `MemberRepositoryAdapter` already do.
Sorting by `AMOUNT` maps to the `amountPaise` entity property; sorting by `CREATED_AT` maps to `createdAt`.

No JPA entity, mapping method, or Flyway migration changed. `PaymentJpaEntity`,
`PaymentJpaMapper`, and every pre-existing `PaymentRepositoryAdapter`/`PaymentSpringDataRepository` method —
including the pessimistic-write lookup reserved for future provider-callback serialization — are
unmodified.

## Adapter Placement

Sprint 11.1 explicitly could not modify ArchUnit rules, and no carve-out exists for a `payment` adapter
depending on `payment.application` (`GENERAL_INFRASTRUCTURE_MUST_NOT_DEPEND_ON_APPLICATION_OR_INTERFACES`
only exempts `infrastructure.auth`, `infrastructure.group`, and `SavingsGroupRepositoryAdapter` by name).
Following the exact resolution used for Member (Sprint 10.1), the `ClockPort`/`TransactionPort`/
`DomainEventPublisherPort` adapters — all `@FunctionalInterface`s with no persistence concerns — are
composed under `payment.interfaces.rest.adapter` and wired from
`payment.interfaces.rest.config.PaymentInfrastructureConfig`, a package ArchUnit already permits to depend
on Spring, the application layer, and the domain layer. `PaymentRepositoryAdapter` needed no new adapter or
port: Payment's application services depend directly on the pre-existing
`payment.domain.port.PaymentRepository`, so the adapter keeps depending only on that domain port, exactly as
it does today.

## Known Limitations

Two gaps in the *pre-existing* persistence mapping (both predate this sprint and are unrelated to the
additive changes above) constrain what `PATCH /api/v1/payments/{paymentId}/status` can durably record:

- **Payment attempts are not persisted.** `PaymentJpaMapper.toDomain` always returns `List.of()` for
  `Payment.attempts()` — there is no child entity or join for `PaymentAttempt` in the schema. Calling
  `startAttempt`/`verify`/`fail` still correctly mutates and persists the payment's own `status` and
  `reconciliationStatus` columns (both plain enum fields on `PaymentJpaEntity`), but the attempt record
  itself (sequence, timestamps, per-attempt provider reference or failure code) is lost on the next reload.
- **The provider reference supplied to `verify` is not persisted.** `PaymentJpaMapper.toEntity` hardcodes
  `providerName`/`providerPaymentReference` to `null` regardless of what `Payment.verify(...)` was called
  with; the domain model only stores `ProviderReference` on the (unpersisted) `PaymentAttempt`, not as a
  top-level `Payment` field.

Both gaps mirror the class of limitation Member Sprint 10.2 documented for consents: real, pre-existing,
and out of this sprint's additive scope to fix. Building attempt persistence and provider-reference storage
would mean designing a schema for gateway-callback data — exactly the kind of "Payment Gateway... external
integration" work this sprint's brief explicitly excludes. The REST API still returns correct `status`/
`reconciliationStatus` values; only the underlying attempt/provider detail does not survive a reload.

## Testing

- `PaymentRepositoryAdapterTest` (new) covers every adapter method, including the pre-existing
  `findByReference`/`findByIdempotencyKey`/`findByProviderReference`/`save` (previously untested) and the
  Sprint 11.1 additions (`findById(tenantId, paymentId)`, `findPage`), using mocked Spring Data and mapper
  collaborators.
- `PaymentPersistencePostgreSqlIntegrationTest` (new) exercises the full stack — Flyway schema, a real
  `SavingsGroup` and real users, `Payment.initiate`, `save`, tenant-scoped `findById`/`findByReference`/
  `findByIdempotencyKey`, and real database-level pagination and sorting by amount and creation time —
  against a Testcontainers PostgreSQL instance. It is skipped cleanly when Docker is unavailable, matching
  every other Testcontainers suite in this project.
- `RepositoryQueryDerivationTest` and `RepositoryAdapterTest` (both pre-existing, unmodified) already
  enumerate `PaymentSpringDataRepository`/`PaymentRepositoryAdapter` and therefore automatically validate
  the new derived queries and the adapter's domain-port implementation and transaction annotations.
