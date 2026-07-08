# Notification Application Layer

Version: 1.0
Sprint: 11.7 (extends the pre-existing Notification domain and persistence foundation)
Status: Implemented
Last Updated: 2026-07-08

## Purpose

This sprint builds the complete application and REST layer around the `Notification` aggregate and its
persistence adapter, both of which already existed (`notification.domain.*`,
`infrastructure.persistence.{entity,mapper,repository,adapter}.*Notification*`) but were never previously
wired to an application or REST layer — the same situation Receipt was in ahead of Sprint 11.4. No business
logic on the pre-existing `Notification` aggregate was changed.

This sprint is a *foundation*: it makes notification creation, retrieval, listing, and delivery-status
tracking work end-to-end, synchronously, over four channels, with centralized message templates. It
explicitly does not add a message broker, background workers, real provider integrations, or RBAC.

## Architecture

```mermaid
flowchart LR
    A[NotificationController] --> M[NotificationApiMapper]
    M --> U[Use case]
    U --> S[Application service]
    S --> T[TransactionPort]
    S --> R[notification.domain.port.NotificationRepository]
    S --> TPL[NotificationTemplateCatalog / NotificationTemplateRenderer]
    S --> D[Notification aggregate]
    S --> E[DomainEventPublisherPort]
    S --> CH{Channel}
    CH -->|EMAIL| ES[EmailSender]
    CH -->|SMS| SS[SmsSender]
    CH -->|WHATSAPP| WS[WhatsappSender]
    CH -->|PUSH| IS[InAppNotificationSender]
```

Dependency direction is inward: the application package depends only on the Notification domain, shared
domain contracts, and Java — enforced by the same `APPLICATION_MUST_DEPEND_ONLY_ON_DOMAIN_AND_APPLICATION`
ArchUnit rule every other module's application layer already honors.

## Use Cases

| Use case | Command/input | Result |
| --- | --- | --- |
| `CreateNotificationUseCase` | `CreateNotificationCommand` | `NotificationResult` |
| `GetNotificationUseCase` | Tenant ID and notification ID | `NotificationResult` |
| `ListNotificationsUseCase` | Tenant ID and `NotificationPageRequest` | `NotificationPage<NotificationSummary>` |
| `MarkNotificationDeliveredUseCase` | `MarkNotificationDeliveredCommand` | `NotificationResult` |
| `MarkNotificationFailedUseCase` | `MarkNotificationFailedCommand` | `NotificationResult` |

## Notification Lifecycle

The pre-existing `Notification` aggregate's lifecycle is `QUEUED → SENDING → SENT → DELIVERED`, with `FAILED`
reachable from `SENDING` or `SENT`, and `SENDING` re-enterable from `FAILED` (retry). This sprint drives that
lifecycle as follows:

1. **`CreateNotificationApplicationService`** performs the *entire* synchronous flow in one request/transaction:
   render the template, `Notification.queue(...)`, `startDelivery(...)`, dispatch to the channel port, then
   `markSent(...)`. A created notification is therefore always returned in status `SENT` (or the request
   fails outright — see below) — never `QUEUED`, since nothing asynchronous exists to pick up a merely-queued
   notification later.
2. **`MarkNotificationDeliveredUseCase`** and **`MarkNotificationFailedUseCase`** are separate REST calls,
   representing a later confirmation (e.g. a delivery receipt or bounce notice) arriving after the create
   request already returned. They load the persisted notification, call `markDelivered`/`markFailed`, and
   save.

### Why Creation Bypasses `NotificationFactory`

`CreateNotificationApplicationService` calls `Notification.queue(...)` (the aggregate's own public static
factory method) directly, instead of the pre-existing `notification.domain.factory.NotificationFactory`.
`NotificationFactory.queue(...)` derives `queuedAt` from its own internally injected `Clock`, independent of
whatever `scheduledAt` the caller passes in. `Notification.queue`'s own guard,
`scheduledAt.isBefore(queuedAt)`, then rejects the call unless `scheduledAt` is at or after that independent,
slightly-later clock reading — which an application-layer caller cannot reliably arrange when it wants to
schedule "now" for immediate synchronous dispatch (two separate `Clock.instant()` calls almost never tie).
Calling `Notification.queue(...)` directly, with one `ClockPort.now()` reading reused for both `scheduledAt`
and `queuedAt`, sidesteps that race without changing `Notification` or `NotificationFactory`. This is
documented here because it is a deliberate, non-obvious choice, not an oversight — `NotificationFactory`
itself is untouched and remains available for a future scheduled/deferred creation path.

### Channel Dispatch and Failure

`CreateNotificationApplicationService` resolves one of four outbound ports based on
`Notification.channel()` and calls it synchronously, inside the same transaction:

| `NotificationChannel` | Port | REST vocabulary |
| --- | --- | --- |
| `EMAIL` | `EmailSender` | `EMAIL` |
| `SMS` | `SmsSender` | `SMS` |
| `WHATSAPP` | `WhatsappSender` | `WHATSAPP` |
| `PUSH` | `InAppNotificationSender` | `IN_APP` |

If the sender throws, the exception is wrapped as `NotificationDeliveryFailedException` and the whole
transaction rolls back — no partial `FAILED` record is persisted, since a failed dispatch during creation
means the create request itself failed and the client should retry it, not silently receive an orphaned
failure record it never confirmed. `MarkNotificationFailedUseCase` is the mechanism for recording a failure
discovered *after* a notification was already successfully created and dispatched.

### Why the REST Vocabulary Says `IN_APP` but the Domain Says `PUSH`

The pre-existing `NotificationChannel` enum and the `notification.notifications.channel` check constraint
(`ck_notifications_channel`) already model this channel as `PUSH`, not `IN_APP`. This sprint needed an
`InAppNotificationSender` port by that exact name, and the REST contract to speak `IN_APP` per the sprint's
vocabulary — but renaming the domain enum value would mean a Flyway migration to widen a check constraint,
which the sprint's "create a migration only if absolutely necessary" instruction weighs against for a
cosmetic rename. `NotificationApiMapper` translates `"IN_APP"` ⇄ `NotificationChannel.PUSH` at the REST
boundary in both directions (`toCreateCommand` and `toResponse`/`toSummaryResponse`), so REST clients never
see the word `PUSH` and the domain/schema never see the word `IN_APP`.

## Commands

`CreateNotificationCommand` carries `tenantId`, `recipientUserId`, `destination` (the channel-specific
address — email, phone number, or device reference), `channel`, `category`, a `Map<String, String>` of
template placeholders, and `actorId`. `MarkNotificationDeliveredCommand`/`MarkNotificationFailedCommand`
carry `tenantId`, `notificationId`, (`failureCode` for the failed variant), and `actorId`. All constructors
perform null validation only.

## Query Models

- `NotificationResult` is the complete application view: identifiers, channel, category, rendered
  subject/body, status, `scheduledAt`/`createdAt`/`updatedAt`, plus two *derived* fields not stored as
  independent columns — `deliveredAt` (equal to `auditInfo().updatedAt()` only when `status == DELIVERED`)
  and `failureReason` (the most recent `DeliveryAttempt.failureCode()` only when `status == FAILED`).
- `NotificationSummary` is the compact list projection used by `ListNotificationsUseCase`.

`NotificationApplicationMapper` performs this derivation; it is the only place `deliveredAt`/`failureReason`
are computed, so no other layer duplicates this logic.

## Template Rendering

Centralizing message text was an explicit sprint goal ("do not hardcode messages across services"). Two new,
framework-free domain classes accomplish this:

- **`notification.domain.model.NotificationTemplate`** — an immutable `(category, subjectTemplate,
  bodyTemplate)` record. `bodyTemplate` is required; `subjectTemplate` is optional (SMS/WhatsApp typically
  have no subject).
- **`notification.domain.service.NotificationTemplateCatalog`** — a static, in-code registry mapping each of
  the six pre-existing `NotificationCategory` values to exactly one canned template, using the sprint's
  example placeholders (`{{memberName}}`, `{{groupName}}`, `{{amount}}`, `{{drawNumber}}`,
  `{{receiptNumber}}`). One category maps to one template — there is no separate "template code" concept,
  since the category enum already provides a natural, existing key.
- **`notification.domain.service.NotificationTemplateRenderer`** — performs simple `{{placeholder}}` string
  substitution (no template engine, per the sprint's explicit scope). A placeholder absent from the caller's
  map is left literally unreplaced in the output; this is a deliberate, minimal behavior, not an oversight.

Future services (Payment, Draw, Receipt, Group) that want to notify a member call `CreateNotificationUseCase`
with a `category` and a `Map` of placeholder values — they never construct message text themselves.

## Channel Abstractions

Four `@FunctionalInterface` outbound ports live in `notification.application.port`: `EmailSender`,
`SmsSender`, `WhatsappSender`, `InAppNotificationSender`. Each has one method,
`String send(NotificationRecipient recipient, NotificationContent content)`, returning a provider-assigned
message identifier. Depending on `NotificationRecipient`/`NotificationContent` (domain model types) directly
is permitted — application code may depend on domain — and avoids inventing parallel application-layer
value types for data the domain already models.

Four placeholder adapters — `LoggingEmailSenderAdapter`, `LoggingSmsSenderAdapter`,
`LoggingWhatsappSenderAdapter`, `LoggingInAppNotificationSenderAdapter` (all under
`notification.interfaces.rest.adapter`) — log a masked destination and a dummy generated provider message ID
(e.g. `EMAIL-<uuid>`), then return it. **No SMTP, Twilio, or WhatsApp Cloud API integration exists.** These
adapters exist solely to make the synchronous dispatch flow observable and testable until real providers are
integrated in a later sprint. Per `docs/standards/logging-standards.md`'s "mask PII by default" rule, the
destination is masked before logging (`NotificationDestinationMasking`, keeping only the first/last two
characters); the in-app sender logs the recipient's opaque user ID instead, since it has no destination
string to mask.

## Ports

### notification.domain.port.NotificationRepository

Notification use cases depend directly on the pre-existing domain repository port, following the same
resolution Payment, Receipt, and Draw adopted rather than introducing a parallel
`notification.application.port.NotificationRepository`. The port gained two additive methods this sprint:
`findById(AggregateId tenantId, AggregateId notificationId)` for tenant-scoped lookup, and
`findPage(AggregateId tenantId, NotificationPageRequest pageRequest)` for pagination. The pre-existing
`findById(notificationId)` (non-tenant-scoped) and `save` are untouched.

### Additional Ports

| Port | Responsibility |
| --- | --- |
| `ClockPort` | Supplies the current instant, used uniformly for `scheduledAt`/`queuedAt`/status-change timestamps. |
| `DomainEventPublisherPort` | Publishes committed aggregate events. |
| `TransactionPort` | Executes one complete use case transaction. |

All are `@FunctionalInterface`s, structurally identical to their Payment/Receipt/Draw counterparts, and their
adapters are composed under `notification.interfaces.rest.config`/`notification.interfaces.rest.adapter`
rather than a new `infrastructure.notification` package — `GENERAL_INFRASTRUCTURE_MUST_NOT_DEPEND_ON_APPLICATION_OR_INTERFACES`
has no carve-out for a `notification` adapter depending on `notification.application`, and this sprint
forbids modifying ArchUnit.

## Pagination

`ListNotificationsUseCase` lists tenant-scoped notifications, paginated and sorted at the persistence
boundary, following the exact shape Receipt established: a page/size/totalElements carrier with derived
`totalPages()`/`hasNext()`/`hasPrevious()`, a page request record validating `page >= 0` and
`1 <= size <= 100`, and a sort-field enum (`CREATED_AT` or `SCHEDULED_AT`, since Notification has no amount
to sort by) with a direction enum (`ASC`/`DESC`). `NotificationPage`/`NotificationPageRequest`/
`NotificationSortField`/`SortDirection` live in `notification.domain.port` for the same ArchUnit reason
described above.

## REST API

Tenant-scoped and authenticated only (`CurrentUserProvider.requireCurrentUser()`), matching every other
module. RBAC is explicitly out of scope for this sprint.

| Method | Path | Use case |
| --- | --- | --- |
| `POST` | `/api/v1/notifications` | `CreateNotificationUseCase` |
| `GET` | `/api/v1/notifications/{notificationId}` | `GetNotificationUseCase` |
| `GET` | `/api/v1/notifications` | `ListNotificationsUseCase` |
| `PATCH` | `/api/v1/notifications/{notificationId}/delivered` | `MarkNotificationDeliveredUseCase` |
| `PATCH` | `/api/v1/notifications/{notificationId}/failed` | `MarkNotificationFailedUseCase` |

`NotificationController` is gated by `bachatsetu.notification.rest.enabled` (default `true`), matching every
other module's REST-toggle convention. `NotificationExceptionHandler` maps `NotificationNotFoundException` to
404, `NotificationDeliveryFailedException` to 502 (channel dispatch failure — a genuine upstream/provider
problem, distinct from a 4xx client error), `DomainException` to 422, and validation failures to 400,
mirroring `ReceiptExceptionHandler`'s structure exactly.

## Transactions

Every application service owns its transaction boundary via `TransactionPort.execute(...)`. Command
execution order for `CreateNotificationUseCase` is:

1. Begin transaction abstraction.
2. Render the category's template with the caller's placeholders.
3. `Notification.queue(...)` (direct aggregate call — see above).
4. `startDelivery(...)`.
5. Dispatch to the resolved channel port.
6. `markSent(...)`.
7. Save the aggregate.
8. Pull and publish domain events (`NotificationQueued` plus two `NotificationStatusChanged` events, for the
   QUEUED→SENDING and SENDING→SENT transitions).
9. Map and return the result.

## Testing

- `NotificationTest` (new) — the pre-existing `Notification` aggregate itself had zero test coverage before
  this sprint (the same situation `Receipt` was in before 11.4); it now covers `queue`, the full
  `startDelivery → markSent → markDelivered` success path, `markFailed` from both `SENDING` and `SENT`,
  retrying delivery after a prior failure, and every invalid-state transition.
- `NotificationTemplateRendererTest`, `NotificationTemplateCatalogTest` — placeholder substitution, missing
  placeholders left literal, and every `NotificationCategory` has a registered template.
- `NotificationPageTest`, `NotificationPageRequestTest` — pagination math and validation.
- `ApplicationContractTest`, `ApplicationTestFixture` — command null-validation, port shape, use-case and
  exception contracts, domain port method presence.
- `NotificationApplicationMapperTest` — result/summary mapping, including the derived `deliveredAt`/
  `failureReason` fields across `QUEUED`/`DELIVERED`/`FAILED` states.
- `NotificationApplicationServiceTest` — all five services: full create-and-dispatch flow, channel routing
  (including `PUSH`), wrapping a sender failure as `NotificationDeliveryFailedException` with no persisted
  side effect, tenant-scoped get/list, mark-delivered/mark-failed, and constructor/argument null validation.
- `NotificationApiMapperTest`, `NotificationControllerTest` — REST contract mapping (including the
  `IN_APP`⇄`PUSH` translation in both directions), all five endpoints, validation errors, and
  authentication/not-found error mapping.
- `NotificationInfrastructureConfigTest`, `NotificationApplicationConfigTest` — every bean wires when
  persistence repositories are enabled and none wire when disabled.
- `NotificationInfrastructureAdapterTest` — clock/transaction/event-publisher adapters and all four
  placeholder channel senders, plus the destination-masking helper.
- `NotificationJpaMapperTest`, `NotificationRepositoryAdapterTest` — see
  `docs/persistence/notification-persistence.md`.
- `NotificationPersistencePostgreSqlIntegrationTest` (Testcontainers; skips cleanly without Docker) — full
  lifecycle round-trip against a real PostgreSQL instance, tenant isolation, and database-level pagination
  and sorting.

## Current Limitations

- **No scheduled/deferred notifications.** Every notification is dispatched immediately, synchronously,
  within the create request. `NotificationFactory` (untouched) and `Notification.status() == QUEUED` /
  `SCHEDULED`-style semantics exist in the aggregate, but nothing in this sprint ever leaves a notification in
  `QUEUED` for later pickup — there is no scheduler or worker, per the sprint's explicit "no async processing"
  scope.
- **No real provider integrations.** All four channel adapters are logging placeholders. Integrating SMTP,
  an SMS gateway, the WhatsApp Cloud API, and a push/in-app delivery mechanism are separate, later concerns.
- **No RBAC.** Any authenticated user in a tenant can create, view, or transition any notification within
  that tenant; per-role restrictions are explicitly out of scope, mirroring how Draw sequenced authorization
  as its own later sprint.
- **`IN_APP`/`PUSH` naming divergence.** Documented above; a future sprint could rename the domain enum value
  (with a small Flyway migration) if the mismatch becomes confusing, but it does not affect behavior.
