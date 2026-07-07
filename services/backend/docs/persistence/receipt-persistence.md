# Receipt Persistence

Version: 1.0
Sprint: 11.4 (extends persistence infrastructure shipped earlier)
Status: Implemented
Last Updated: 2026-07-07

## Purpose

This document describes how the Receipt application layer (Sprint 11.4) reaches the persistence
infrastructure that already existed for `Receipt` — `ReceiptJpaEntity`, `ReceiptJpaMapper`,
`ReceiptSpringDataRepository`, and `ReceiptRepositoryAdapter`, all shipped ahead of this sprint but never
previously exercised by an application or REST layer — and the additive changes made to support
tenant-scoped lookup and pagination.

## The Existing Model

`finance.receipts` maps one row per `Receipt` aggregate, with a `OneToOne` reference to the `Payment` it was
generated for (`uk_receipts_payment` enforces one receipt per payment) and a `ManyToOne` reference to a
`UserJpaEntity` for the receiving member. `ReceiptJpaMapper.toDomain`/`toEntity` were already complete and
are unmodified by this sprint. The schema and the `uk_receipts_tenant_number` unique constraint needed for
tenant-scoped number lookup already existed; no Flyway migration was required or added.

## What Changed

Two methods were added, additively, to `receipt.domain.port.ReceiptRepository`:

```java
Optional<Receipt> findById(AggregateId tenantId, AggregateId receiptId);

ReceiptPage<Receipt> findPage(AggregateId tenantId, ReceiptPageRequest pageRequest);
```

`ReceiptRepositoryAdapter` implements the first by delegating to a new derived Spring Data query,
`findByTenantIdAndIdAndDeletedFalse`, mirroring the equivalent method already added to every other adapter
in this project. It implements the second with a second new derived query,
`findAllByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable)` — a *pure* derived method requiring no
`@Query`, since each receipt is already exactly one row. `ReceiptRepositoryAdapter` builds the
`Sort`/`Pageable` from the framework-free `ReceiptPageRequest` and converts the returned Spring Data
`Page<ReceiptJpaEntity>` back into `ReceiptPage<Receipt>`. Sorting by `AMOUNT` maps to the `amountPaise`
entity property; sorting by `CREATED_AT` maps to `createdAt`.

No JPA entity, mapping method, or Flyway migration changed. `ReceiptJpaEntity`, `ReceiptJpaMapper`, and
every pre-existing `ReceiptRepositoryAdapter`/`ReceiptSpringDataRepository` method — `findById(receiptId)`,
`findByNumber`, `findByPaymentId`, and `save` — are unmodified.

## Adapter Placement

Sprint 11.4 explicitly could not modify ArchUnit rules, and no carve-out exists for a `receipt` adapter
depending on `receipt.application` (`GENERAL_INFRASTRUCTURE_MUST_NOT_DEPEND_ON_APPLICATION_OR_INTERFACES`
only exempts `infrastructure.auth`, `infrastructure.group`, and `SavingsGroupRepositoryAdapter` by name).
Following the resolution used for Member, Payment, and Draw, the `TransactionPort`/`DomainEventPublisherPort`
adapters — both `@FunctionalInterface`s with no persistence concerns — are composed under
`receipt.interfaces.rest.adapter` and wired from
`receipt.interfaces.rest.config.ReceiptInfrastructureConfig`, a package ArchUnit already permits to depend
on Spring, the application layer, and the domain layer. `ReceiptRepositoryAdapter` needed no new adapter or
port: Receipt's application services depend directly on the pre-existing
`receipt.domain.port.ReceiptRepository`, so the adapter keeps depending only on that domain port.

## Known Limitations

Two gaps in the *pre-existing* domain and persistence mapping (both predate this sprint and are unrelated to
the additive changes above) are documented here rather than fixed, per the sprint brief's "document, don't
invent" and "do not redesign unrelated tables" rules:

- **Receipt has no `groupId`.** The `Receipt` aggregate carries `tenantId`, `paymentId`, and `memberId`, but
  no `groupId` field, and `ReceiptJpaEntity` has no `group_id` column. The savings group a receipt relates to
  is only reachable indirectly, by following `paymentId` to the `Payment` aggregate (which does carry a
  `groupId`). `CreateReceiptRequest` therefore does not accept, and `ReceiptResponse` does not expose, a
  `groupId` — adding one would mean altering the existing `Receipt` aggregate and `finance.receipts` schema,
  which this additive sprint does not do.
- **Only one line's worth of data survives a reload.** `ReceiptJpaEntity` has no child table for
  `ReceiptLine`s — it stores a single `amount_paise`/`currency_code` pair per row. `ReceiptJpaMapper.toDomain`
  reconstructs exactly one synthetic `ReceiptLine` on every reload (`ReceiptType.CONTRIBUTION`, description
  "Payment receipt", amount equal to the receipt's stored total) regardless of how many lines the receipt was
  originally generated with. A receipt generated with multiple lines (for example, a contribution plus a
  penalty) will still report the correct *total* after a reload — `Receipt.total()` is what
  `ReceiptJpaEntity.amountPaise` stores — but the individual line breakdown is lost, exactly mirroring the
  class of limitation Payment (11.1) documented for unpersisted payment attempts. This is verified directly
  by `ReceiptPersistencePostgreSqlIntegrationTest.persistsAndRehydratesAReceipt`, which generates a receipt
  with two lines and asserts the reloaded total is correct.

## Testing

- `ReceiptRepositoryAdapterTest` (new) covers every adapter method, including the pre-existing
  `findById(receiptId)`/`findByNumber`/`findByPaymentId`/`save` (previously untested at the adapter level)
  and the Sprint 11.4 additions (`findById(tenantId, receiptId)`, `findPage`), using mocked Spring Data and
  mapper collaborators.
- `ReceiptPersistencePostgreSqlIntegrationTest` (new) exercises the full stack — Flyway schema, a real
  `SavingsGroup`, real users, a real `Payment`, `Receipt.generate`, `save`, tenant-scoped `findById`, and
  real database-level pagination and sorting by amount and creation time — against a Testcontainers
  PostgreSQL instance. It is skipped cleanly when Docker is unavailable, matching every other Testcontainers
  suite in this project.
- `RepositoryQueryDerivationTest` and `RepositoryAdapterTest` (both pre-existing, unmodified) already
  enumerate `ReceiptSpringDataRepository`/`ReceiptRepositoryAdapter` and therefore automatically validate the
  new derived queries and the adapter's domain-port implementation and transaction annotations.
