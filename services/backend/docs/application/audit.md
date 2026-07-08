# Audit Module

## Purpose

Introduces a completely generic, cross-module audit trail. Any module records a business action by
depending on `CreateAuditEntryUseCase` — no module writes an audit record directly through its own
repository, and no module reads another module's audit records other than through the Audit module's own
use cases. The Audit module knows nothing about the business meaning of the modules that call it: it stores
whatever `moduleName`, `eventType`, `action`, and free-form JSON `metadata` a caller supplies, and never
parses or validates that metadata's contents.

## Architecture

Hexagonal/DDD, mirroring every other module in this codebase exactly:

```
audit
 ├── domain          AuditEntry aggregate, AuditEventType, AuditRepository port, search/paging types
 ├── application     ports, command, query, mapper, use cases, services
 └── interfaces/rest adapters, DTOs, mapper, controller, exception handler, event listener, config
```

```
 Any module's application service
          │  (after its own transaction commits)
          ▼
 CreateAuditEntryUseCase  ◄───────────────────────────┐
          │                                            │
          ▼                                            │
 CreateAuditEntryApplicationService                    │
          │           │                                │
          ▼           ▼                                │
   AuditRepository  AuditPublisherPort            LoginAuditListener
   (save AuditEntry) (Spring application event)   (reacts to auth's OtpVerified)
          │
          ▼
   audit.audit_entries (PostgreSQL, JSONB metadata)
```

The domain layer (`AuditEntry`, `AuditEventType`, `AuditSearchCriteria`, `AuditPage`, `AuditRepository`)
never imports Spring, JPA, or any other module's package — verified by the existing
`ForbiddenDependencyArchitectureTest` and `LayerDependencyArchitectureTest` ArchUnit rules, which apply here
exactly as they do to every other module. `AuditController` depends only on the application boundary (use
cases, DTOs, mapper, `CurrentUserProvider`) — never on `AuditEntry` or any persistence type — satisfying
`CONTROLLERS_MUST_NOT_DEPEND_ON_DOMAIN_OR_INFRASTRUCTURE`.

## Avoiding a Module Cycle with Auth

`AuditController` depends on `auth.application.security.CurrentUserProvider`, like every other module's
controller. That makes `audit` depend on `auth`. Recording a `LOGIN` audit entry therefore cannot be done by
injecting `CreateAuditEntryUseCase` directly into `auth`'s `VerifyOtpApplicationService` — that would close
the cycle (`audit → auth → audit`), which `PackageDependencyArchitectureTest` forbids and which was
confirmed experimentally while building this integration.

Instead, the dependency stays one-directional, following the same event-driven pattern already used between
`payment` and `notification` in Sprint 11.9:

1. `auth.application.service.VerifyOtpApplicationService` already builds an `OtpVerified` application event
   for its own reasons (any future consumer of an OTP outcome). It now also publishes that event through a
   new `OtpEventPublisherPort` (an `auth`-owned port, implemented by `ApplicationEventOtpPublisherAdapter`
   over Spring's `ApplicationEventPublisher`). `auth` never references `audit.*`.
2. `audit.interfaces.rest.event.LoginAuditListener` (inside the `audit` module, so this is the *existing*
   `audit → auth` direction, not a new one) listens for `OtpVerified` with a plain `@EventListener`, filters
   to `OtpPurpose.SIGN_IN`, and calls `CreateAuditEntryUseCase` to record a `LOGIN` entry.

A login has no tenant yet at this point in the flow, so the recorded entry's `tenantId` is `null` — a
tenant-less, identity-level event, exactly like the OTP challenge it reacts to.

## AuditEntry Aggregate

Fields: `auditId` (the aggregate id), `tenantId`, `actorId`, `eventType`, `moduleName`, `resourceType`,
`resourceId`, `action`, `description`, `ipAddress`, `userAgent`, `metadata` (a JSON string, opaque to this
module), `createdAt`.

`tenantId` and `actorId` are both nullable, unlike every other aggregate in this codebase:

- **`tenantId` is null** for identity-level events (login, OTP) that happen before any tenant context
  exists. `auth.domain.model.User` itself has no `tenantId` field, which is what makes this unavoidable.
- **`actorId` is null** for system/background events (a scheduled job, a startup task) that have no human
  actor. `BaseAggregateRoot`'s `AuditInfo.createdBy()` still needs a non-null value for its own bookkeeping,
  so a well-known internal placeholder (`AggregateId` wrapping the nil UUID) is used there — it is never
  confused with the real, nullable `actorId()` exposed to callers.

Once created, an `AuditEntry` is never mutated — there are no state-transition methods, unlike every other
aggregate in this codebase, because an audit record is a fact about the past, not a thing with a lifecycle.

## AuditEventType

`LOGIN`, `LOGOUT`, `OTP_SENT`, `OTP_VERIFIED`, `GROUP_CREATED`, `GROUP_UPDATED`, `GROUP_CLOSED`,
`MEMBER_ADDED`, `MEMBER_REMOVED`, `PAYMENT_CREATED`, `PAYMENT_VERIFIED`, `PAYMENT_REFUNDED`, `DRAW_CREATED`,
`DRAW_COMPLETED`, `RECEIPT_GENERATED`, `PDF_DOWNLOADED`, `NOTIFICATION_SENT`, `FILE_UPLOADED`,
`FILE_DELETED`, `GATEWAY_REFUND_INITIATED`, `GATEWAY_WEBHOOK_PROCESSED`, `SYSTEM_EVENT`.

The last two (`GATEWAY_REFUND_INITIATED`, `GATEWAY_WEBHOOK_PROCESSED`) are additions beyond the sprint's
example list, needed because the Payment Gateway integration records two distinct actions that don't fit any
of the originally listed types.

## Best-Effort Recording: Audit Never Rolls Back Business Operations

Every integration in this sprint follows the same, deliberately repeated pattern:

```java
Result result = transaction.execute(() -> businessLogic(command)); // commits first
auditSomething(result);                                            // best-effort, after commit
return result;
```

The audit call always happens **after** the enclosing business transaction has already committed — never
nested inside it — because nesting risks the audit write's own failure marking the *outer* transaction
rollback-only via Spring's exception translation, even if application code catches the exception. Each
`auditSomething(...)` helper wraps its call to `CreateAuditEntryUseCase.execute(...)` in a
`try { ... } catch (RuntimeException exception) { /* swallow */ }` block.

`CreateAuditEntryUseCase` itself does **not** swallow its own exceptions — it is the caller's responsibility
to treat it as best-effort, since only the caller knows whether its own business operation must never appear
to fail because of an audit failure.

### Why these try/catch blocks never log

Every application-layer service in this codebase is restricted by
`LayerDependencyArchitectureTest.APPLICATION_MUST_DEPEND_ONLY_ON_DOMAIN_AND_APPLICATION` to depend only on
its own `application`, `domain`, and `java.*` packages — not even `org.slf4j`. This was discovered while
building the Payment integration: adding a `Logger` field to `UpdatePaymentStatusApplicationService` failed
that ArchUnit rule. Every best-effort audit try/catch in an application-layer service therefore swallows the
exception silently, with only an explanatory code comment — this is a codebase-wide constraint, not specific
to Audit. (`LoginAuditListener`, by contrast, lives in `audit.interfaces.rest.event`, an interfaces-layer
package, so it *can* and does log the swallowed exception.)

## Integrated Call Sites

| Business Action | Event Type | Caller | Tenant / Actor Source |
|---|---|---|---|
| Sign-in OTP verified | `LOGIN` | `LoginAuditListener` (reacts to `auth`'s `OtpVerified`) | tenant: none; actor: the signed-in user |
| Payment verified | `PAYMENT_VERIFIED` | `UpdatePaymentStatusApplicationService` (only for the `VERIFIED` transition) | from the `PaymentResult` |
| Receipt generated | `RECEIPT_GENERATED` | `CreateReceiptApplicationService` (unconditionally, including the idempotent re-fetch case) | from the `ReceiptResult` |
| Receipt PDF downloaded | `PDF_DOWNLOADED` | `GetReceiptPdfApplicationService` | from the loaded `Receipt` |
| Draw completed | `DRAW_COMPLETED` | `CloseDrawApplicationService` | tenant from the draw; actor is the winning member |
| Notification sent | `NOTIFICATION_SENT` | `CreateNotificationApplicationService` | actor is the notification's recipient |
| File uploaded | `FILE_UPLOADED` | `UploadFileApplicationService` | from the `UploadFileCommand` |
| File deleted | `FILE_DELETED` | `DeleteFileApplicationService` | tenant from the command; no actor available at this port's signature |
| Gateway refund initiated | `GATEWAY_REFUND_INITIATED` | `InitiateRefundApplicationService` | from the `InitiateRefundCommand` |
| Gateway webhook processed | `GATEWAY_WEBHOOK_PROCESSED` | `ProcessPaymentWebhookApplicationService` | captured from the `GatewayOrder`/payment inside the transaction, via a local `AtomicReference` pair read after commit (the shared `PaymentStatusResult` record has no `tenantId` field, and widening it would cascade into every gateway port and simulated adapter) |

None of these call sites changed any existing business logic, request/response contract, or pre-existing
test's expected behavior — every constructor gained one new trailing `CreateAuditEntryUseCase` parameter,
composed in that module's own `*ApplicationConfig`.

## REST API

All endpoints require authentication and are always scoped to the caller's own tenant — there is no
client-supplied tenant override on search, even though the underlying `AuditSearchCriteria`/`AuditRepository`
ports are tenant-optional at the port level (to allow, in principle, tenant-less system queries). This is a
deliberate simplification to prevent any authenticated caller from ever reading another tenant's entries.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/audit` | Manually records one audit entry for the caller's own tenant/identity |
| `GET` | `/api/v1/audit/{auditId}` | Retrieves one tenant-scoped audit entry |
| `GET` | `/api/v1/audit` | Searches audit entries within the caller's tenant |

### Search Filters

All query parameters are optional except pagination, which has defaults:

- `actor` — actor id
- `module` — module name (exact match)
- `event` — `AuditEventType` name (exact match)
- `dateFrom` / `dateTo` — inclusive `Instant` range, applied to `createdAt`
- `page` (default `0`), `size` (default `20`, max `100`)
- `sort` (only `createdAt` is supported today), `direction` (`asc` or `desc`, default `desc`)

## Database

New migration `V9__audit_module.sql`, additive only, in the pre-existing `audit` schema (created by the
original `V1` migration for a different, dormant table — see Limitations). Creates `audit.audit_entries`:

- Nullable `tenant_id`, `actor_id`, `resource_id` (all `UUID`)
- `event_type VARCHAR(40) NOT NULL` with a `CHECK` constraint enumerating all 22 `AuditEventType` values
- `metadata JSONB` — stored via Hibernate's native `@JdbcTypeCode(SqlTypes.JSON)`, no new dependency, no
  parsing on the Java side; the domain-level `AuditEntry.metadata()` is a plain, pre-serialized JSON string
- `occurred_at TIMESTAMPTZ NOT NULL` — the business "when did this happen" timestamp. Named `occurred_at`
  rather than `created_at` because `BaseJpaEntity` already owns the `created_at` column for its own
  bookkeeping (mirrors `stored_files.uploaded_at` from Sprint 12.3)
- Standard audit/soft-delete columns (`created_at/by`, `updated_at/by`, `version`, `is_deleted`,
  `deleted_at/by`), matching every other table in this schema

Indexes: `idx_audit_entries_tenant_id`, `_actor_id`, `_module_name`, `_event_type`, and `_created_at` (on the
`occurred_at` column).

## Configuration

```yaml
bachatsetu:
  audit:
    rest:
      enabled: ${AUDIT_REST_ENABLED:true}
```

`AuditController` is gated on `bachatsetu.audit.rest.enabled` (default `true`); `AuditApplicationConfig` and
`AuditInfrastructureConfig` are gated on `bachatsetu.persistence.repositories.enabled` (default `true`),
matching every other module. Minimal-context tests that disable `persistence.repositories` (and therefore
have no `CreateAuditEntryUseCase` bean) also set `bachatsetu.audit.rest.enabled=false`, so `AuditController`
is not constructed without its dependencies.

## Testing

Domain (`AuditEntryTest`, `AuditSearchCriteriaTest`, `AuditPageTest`), application mapper
(`AuditApplicationMapperTest`), all three application services (create/get/search, including null-argument
and not-found cases), REST mapper (`AuditApiMapperTest`), controller (`AuditControllerTest`, including
unauthenticated and validation-error cases), infrastructure/application config wiring
(`AuditInfrastructureConfigTest`, `AuditApplicationConfigTest`), the `LoginAuditListener` (including the
`SIGN_IN`-only filter and best-effort exception swallowing), a mock-based persistence adapter test
(`AuditEntryRepositoryAdapterTest`), and a Testcontainers-based persistence integration test
(`AuditPersistencePostgreSqlIntegrationTest`) covering save/reload with metadata, tenant-less and actor-less
entries, cross-tenant isolation on both `findById` and `search`, module/event-type/date-range filtering,
pagination, and both sort directions. Every modified module's own existing application-service and
application-config tests were updated with the new `CreateAuditEntryUseCase` constructor parameter and, where
a success path exists, a `verify(createAuditEntry).execute(any())` assertion — no pre-existing test's
expected behavior was weakened or changed.

## Limitations

- Sorting only supports `createdAt`; there is no multi-field sort.
- Search has no full-text search over `description`, `ipAddress`, `userAgent`, or `metadata`.
- No client-supplied tenant override on search, by design (see REST API above) — a caller can only ever see
  their own tenant's entries, even though the domain/application layers are tenant-optional.
- `AuditEntry` is immutable and has no correction/redaction mechanism; a wrong entry can only be soft-deleted
  at the database level, not through any use case.
- A pre-existing, unrelated `AuditLogJpaEntity`/`audit.audit_logs` table and `AuditLogSpringDataRepository`
  (from the original `V1` migration) remain in the codebase, unused except by three fixed-list persistence
  "sweep" tests (`JpaEntityMappingTest`, `HibernateMetadataTest`, `RepositoryQueryDerivationTest`). They were
  deliberately left untouched, per this sprint's "do not redesign any existing module" constraint — this new
  module's `audit.audit_entries` table is a separate table in the same schema.
- No live audit feed or analytics consumer subscribes to `AuditPublisherPort` yet; it exists so one can be
  added later without changing `CreateAuditEntryApplicationService`.
