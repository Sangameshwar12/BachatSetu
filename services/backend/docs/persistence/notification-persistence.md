# Notification Persistence

Version: 1.0
Sprint: 11.7 (extends persistence infrastructure shipped earlier)
Status: Implemented
Last Updated: 2026-07-08

## Purpose

This document describes how the Notification application layer (Sprint 11.7) reaches the persistence
infrastructure that already existed for `Notification` — `NotificationJpaEntity`, `NotificationJpaMapper`,
`NotificationSpringDataRepository`, and `NotificationRepositoryAdapter`, all shipped ahead of this sprint but
never previously exercised by an application or REST layer — and the additive changes made to support
tenant-scoped lookup, pagination, and a persistence-layer gap the new use cases exposed.

## The Existing Model

`notification.notifications` maps one row per `Notification` aggregate, with a `ManyToOne` reference to a
`UserJpaEntity` for the recipient. Columns: `tenant_id`, `user_id`, `notification_type` (maps to
`NotificationCategory`), `channel` (maps to `NotificationChannel`; check-constrained to `EMAIL`, `SMS`,
`WHATSAPP`, `PUSH`), `recipient_reference`, `subject`, `message_body`, `status`, `scheduled_at`, `sent_at`,
`priority`, plus the standard base audit columns. No Flyway migration was required or added — the schema
already supports every field this sprint's use cases need.

## What Changed

### Domain Port (Additive)

Two methods were added to `notification.domain.port.NotificationRepository`:

```java
Optional<Notification> findById(AggregateId tenantId, AggregateId notificationId);

NotificationPage<Notification> findPage(AggregateId tenantId, NotificationPageRequest pageRequest);
```

`NotificationRepositoryAdapter` implements the first via a new derived Spring Data query,
`findByTenantIdAndIdAndDeletedFalse`, and the second via `findAllByTenantIdAndDeletedFalse(UUID tenantId,
Pageable pageable)` — both added to `NotificationSpringDataRepository`, mirroring the exact pattern already
used by `ReceiptSpringDataRepository`/`PaymentSpringDataRepository`. Sorting by `SCHEDULED_AT` maps to the
`scheduledAt` entity property; sorting by `CREATED_AT` maps to `createdAt`. The pre-existing
`findById(notificationId)` (non-tenant-scoped) and `save` are unmodified.

### Mapper: Reconstructing Delivery Attempts on Reload

`NotificationJpaEntity` has no child table for `DeliveryAttempt` history — only a single `sent_at` timestamp
column. Before this sprint, `NotificationJpaMapper.toDomain` always reconstructed the `Notification` aggregate
with an **empty** attempts list, regardless of the entity's status. This was harmless while no application
layer existed to call `markDelivered`/`markFailed` on a *reloaded* notification, but both of those domain
methods call `attempts.getLast()` — which throws on an empty list. Since `MarkNotificationDeliveredUseCase`
and `MarkNotificationFailedUseCase` necessarily operate on a notification loaded fresh from the database in a
separate request, this sprint could not leave that gap in place without those two use cases being broken
every time they were actually invoked.

The fix is additive and mirrors Receipt's own pre-existing, documented reload-fidelity limitation (Receipt
synthesizes one line from its stored total on reload). `NotificationJpaMapper.toDomain` now derives **at most
one** synthetic `DeliveryAttempt` from the entity's own `status` column:

| Entity `status` | Synthesized attempt |
| --- | --- |
| `QUEUED`, `CANCELLED` | none (empty list, unchanged from before) |
| `SENDING` | one `STARTED` attempt |
| `SENT`, `DELIVERED` | one `ACCEPTED` attempt |
| `FAILED` | one `FAILED` attempt |

This restores exactly enough state for `Notification.markDelivered()`/`markFailed()` — which only ever
inspect the *most recent* attempt — to remain callable after a reload. It does **not**, and cannot, restore
the original attempt's identity, its real provider message ID, or its real failure code, since none of those
are persisted columns; the synthesized attempt's `providerMessageId`/`failureCode` are always `null`, and its
`startedAt` is approximated from `sent_at` (falling back to `scheduled_at` when `sent_at` is not set). No
schema change, JPA entity change, or Flyway migration was needed or added — this is purely a richer
reconstruction inside the existing mapper method.

## Adapter Placement

Following the resolution used for Payment, Receipt, Member, and Draw, the `TransactionPort`/
`DomainEventPublisherPort`/`ClockPort` adapters and the four placeholder channel-sender adapters — none of
which have persistence concerns — are composed under `notification.interfaces.rest.adapter` and wired from
`notification.interfaces.rest.config.NotificationInfrastructureConfig`, a package ArchUnit already permits to
depend on Spring, the application layer, and the domain layer.
`GENERAL_INFRASTRUCTURE_MUST_NOT_DEPEND_ON_APPLICATION_OR_INTERFACES` has no carve-out for a `notification`
adapter depending on `notification.application`, and this sprint forbids modifying ArchUnit.
`NotificationRepositoryAdapter` needed no new adapter or port: Notification's application services depend
directly on the pre-existing `notification.domain.port.NotificationRepository`, so the adapter keeps
depending only on that domain port, entirely inside `infrastructure.persistence.*`.

## Known Limitations

- **Delivery attempt history does not survive a reload**, as described above — only the single most recent
  attempt's *status* is recoverable, never its provider message ID or failure code once the process that
  created it exits. A future sprint could add a `notification_delivery_attempts` child table if full history
  is needed; this sprint does not, per its additive, minimal-footprint scope.
- **`sent_at` reflects the last status-changing update, not necessarily the original send time.** This is a
  pre-existing quirk of `NotificationJpaMapper.toEntity` (`sent_at` is set to `auditInfo().updatedAt()`
  whenever status is `SENT` or `DELIVERED`), unrelated to and unmodified by this sprint's changes.
- **The `PUSH` channel value is exposed to REST clients as `IN_APP`** (translated at the REST mapper, not the
  persistence layer) — see `docs/application/notification-application.md` for the full rationale. The
  database and domain model are unaffected; this is purely a REST-vocabulary translation.

## Testing

- `NotificationJpaMapperTest` (new) — covers `toDomain`'s attempt reconstruction for every
  `NotificationStatus` value, including the specific regression this sprint fixes: a `SENT` entity's
  reconstructed aggregate can still be passed through `markDelivered()` successfully. Uses
  `Mappers.getMapper(NotificationJpaMapper.class)` (a real generated MapStruct implementation, not a mock),
  matching the precedent set by `SavingsGroupJpaMapperTest`/`IdentityJpaMapperTest`.
- `NotificationRepositoryAdapterTest` (new) — covers every adapter method, including the pre-existing
  `findById(notificationId)`/`save` (previously untested at the adapter level) and this sprint's additions
  (`findById(tenantId, notificationId)`, `findPage` with both sort fields), using mocked Spring Data and
  mapper collaborators.
- `NotificationPersistencePostgreSqlIntegrationTest` (new) exercises the full stack — Flyway schema, a real
  recipient user, and a real `Notification` — through its complete `QUEUED → SENDING → SENT → DELIVERED`
  lifecycle across three separate save/reload cycles (proving the delivery-attempt reconstruction fix works
  against a real database, not just mocks), tenant isolation, and database-level pagination and sorting by
  `scheduledAt`, against a Testcontainers PostgreSQL instance. It is skipped cleanly when Docker is
  unavailable, matching every other Testcontainers suite in this project.
- `RepositoryQueryDerivationTest` and `RepositoryAdapterTest` (both pre-existing, unmodified) already
  enumerate `NotificationSpringDataRepository`/`NotificationRepositoryAdapter` and therefore automatically
  validate the new derived queries and the adapter's domain-port implementation and transaction annotations.
