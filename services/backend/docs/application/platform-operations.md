# Platform Operations Module

## Overview

`platformoperations` is a new, additive top-level module for the BachatSetu **platform team** — not
organizers, not members, not tenant-scoped. It covers five features:

1. **Super Admin Dashboard** — platform-wide totals and today's activity, computed on demand.
2. **Tenant Management** — view, suspend, activate, archive tenants, with per-tenant statistics.
3. **Broadcast Notifications** — fan out a notification to a target audience, reusing the existing
   Notification module.
4. **System Health** — read-only database/storage/notification component health plus JVM/host facts.
5. Platform Announcements are also implemented here; see [announcements.md](announcements.md) for that
   feature specifically. Support tickets are a **separate** module — see [support.md](support.md).

Every endpoint requires the `PLATFORM_ADMIN` role except `GET /announcements/active` (open to any
authenticated user, so a future client can display active notices to everyone) — no new authentication
model, no new filter: this reuses the exact `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` + JWT role-claim
pattern already established by the `admin` module.

## Architecture

```
platformoperations/
  domain/
    model/   TenantStatus, Tenant, TenantStatistics, PlatformOverviewSnapshot, AnnouncementSeverity,
             Announcement, BroadcastScope, BroadcastRecipient, HealthStatus, ComponentHealth,
             SystemRuntimeInfo
    event/   TenantSuspended, TenantActivated, TenantArchived, AnnouncementPublished
    exception/  PlatformOperationsDomainException
    port/    TenantRepository, TenantStatisticsRepository, KnownTenantsRepository,
             PlatformOverviewRepository, AnnouncementRepository, BroadcastRecipientRepository,
             DatabaseHealthPort, StorageHealthPort, NotificationHealthPort, SystemRuntimeInfoPort
  application/
    port/    ClockPort, TransactionPort, DomainEventPublisherPort
    command/, query/, exception/, usecase/, service/, mapper/   (one command+usecase+service per feature)
  interfaces/rest/
    controller/  PlatformOverviewController, TenantController, AnnouncementController,
                 BroadcastController, SystemHealthController
    mapper/      PlatformOperationsApiMapper
    dto/, exception/, config/, adapter/
```

As with `support`, JPA entities, mappers, Spring Data repositories, and repository adapters live under
`infrastructure.persistence.*` (`TenantJpaEntity`, `AnnouncementJpaEntity`, and their adapters), matching
every other module's persistence convention. The module's own tree holds only the domain ports it defines.

## Feature 1: Super Admin Dashboard

`GET /api/v1/platform-operations/overview` computes, on demand (no scheduled aggregation, no cache):
total users, organizers, groups, members, payments, receipts, notifications, stored files, active tenants,
revenue (sum of verified payment amounts), and five "today" counters (signups, payments, groups,
notifications, storage uploads — `[startOfDayUTC, startOfNextDayUTC)`).

`PlatformOverviewRepositoryAdapter` composes each existing module's own Spring Data repository directly —
the exact same "no SQL view, no cross-module domain coupling" pattern `AdminOverviewAnalyticsRepositoryAdapter`
already established in Sprint 13.2. **Total Active Tenants** = every tenant known to the platform (derived
from distinct `tenant_id` values on users, the same derivation `admin.domain.port.PlatformTenantRepository`
already uses) minus every tenant with an explicit `SUSPENDED` or `ARCHIVED` lifecycle row (see Feature 2) —
a tenant with no lifecycle record is active by definition.

## Feature 2: Tenant Management

This codebase has **no pre-existing Tenant aggregate or table** — until this sprint, "a tenant" was purely
inferred from having at least one user (`admin.domain.model.PlatformTenantSummary`). Suspend/activate/archive
requires real, persisted lifecycle state, so this sprint adds a genuinely new `Tenant` aggregate
(`platform.tenants`, a new schema) — **without touching or redesigning** `PlatformTenantSummary` or
`PlatformTenantRepository`, per the sprint's explicit instruction.

- **Lazy creation**: a `Tenant` row does not exist until the first suspend/archive action against that
  tenant ID; `SuspendTenantApplicationService`/`ArchiveTenantApplicationService` synthesize a fresh
  `Tenant.createActive(...)` in memory when no row is found, then immediately transition and save it.
  `activate()` requires an existing `SUSPENDED` row (there is nothing to "activate" for an implicitly-active
  tenant with no record).
- **Enumeration**: `GET /tenants` reuses the same distinct-tenant-id derivation the `admin` module already
  relies on — via a new, independent `KnownTenantsRepository` (not `admin.domain.port.PlatformTenantRepository`
  directly; see [Module Cycle Avoidance](#module-cycle-avoidance) below) — and overlays each tenant's
  lifecycle status and full statistics.
- **Statistics**: `TenantStatisticsRepositoryAdapter` composes existing repositories (users, groups,
  payments + revenue, storage files + bytes, notifications) plus one proxy: `lastActivityAt` is the most
  recently updated user record in that tenant (there is no cross-module "last activity" concept in this
  codebase to derive from directly — see Known Limitations).
- **No enforcement**: suspending a tenant records its lifecycle state and audits the action, but does
  **not** block that tenant's users from logging in or transacting — see Known Limitations.

## Feature 3: Broadcast Notifications

`POST /api/v1/platform-operations/broadcast` sends a notification to one of four audiences without
duplicating any notification logic:

| Scope | Recipient resolution |
|---|---|
| `ALL_USERS` | Every non-deleted user, platform-wide |
| `TENANT` (requires `tenantId`) | Every non-deleted user in that tenant |
| `ORGANIZERS` | Every distinct user holding `ORGANIZER` or `CO_ORGANIZER` in any group |
| `MEMBERS` | Every distinct user holding `MEMBER` in any group |

`SendBroadcastNotificationApplicationService` resolves the audience via `BroadcastRecipientRepository`, then
calls the existing, **unmodified** `notification.application.usecase.CreateNotificationUseCase` once per
recipient (channel `PUSH`, a new pass-through category `PLATFORM_ANNOUNCEMENT` added to
`NotificationCategory` exactly like `PAYMENT`/`RECEIPT`/`GROUP`/`MEMBER` before it — additive, not a
redesign). Each recipient is attempted independently: one failure does not abort the rest of the broadcast,
and the response reports `recipientCount`/`sentCount`/`failedCount`.

## Feature 4: System Health

`GET /api/v1/platform-operations/health` is read-only and composes with, rather than replaces, the
pre-existing unauthenticated `/actuator/health` (untouched — no actuator redesign):

| Component | How it's checked |
|---|---|
| Database | `Connection.isValid(2)` against the configured `DataSource` |
| Storage | `bachatsetu.storage.enabled` + configured provider (the cloud provider adapters in this codebase are simulated, so this is a configuration check, not a live probe, for anything but `LOCAL` — see their own class Javadocs) |
| Notification | A lightweight `count()` query against the notification table |
| Runtime facts | `ManagementFactory.getRuntimeMXBean()` uptime, `System.getProperty("java.version")`, `Runtime.getRuntime()` memory, and disk usage via `File.getUsableSpace()`/`getTotalSpace()` |

`buildTimestamp` is always `null`: this project has no `spring-boot-maven-plugin` `build-info` execution
configured, so there is no real timestamp to report — honestly reporting absence rather than fabricating one.

## Module Cycle Avoidance

Two design decisions in this module exist purely to satisfy `PackageDependencyArchitectureTest`'s
`TOP_LEVEL_MODULES_MUST_BE_FREE_OF_CYCLES` rule, once Broadcast Notifications made `platformoperations`
depend on `notification` (which already depends on `audit` for its own `NOTIFICATION_SENT` audit entry):

1. **`shared.domain.Page` / `PageQuery` / `SortDirection`** (new, additive classes) replace what would
   otherwise be a natural reuse of `admin.domain.port.PlatformPage`/`PlatformPageRequest`/`SortDirection`.
   Reusing admin's types would make `support` and `platformoperations` depend on `admin`; since `admin`
   already depends on `audit` (its analytics/config services record audit entries), and `audit` needs to
   depend on `support`/`platformoperations` (to react to their domain events), that would close a cycle:
   `admin → audit → platformoperations → admin`. Local, module-agnostic pagination types in `shared` (a
   pure leaf package every module already depends on) avoid this entirely.
2. **`KnownTenantsRepository`** (new port + `KnownTenantsRepositoryAdapter`) independently re-derives the
   same distinct-tenant-id query `AdminTenantRepositoryAdapter` already performs, rather than
   `SearchTenantsApplicationService` depending on `admin.domain.port.PlatformTenantRepository` directly —
   for the same cycle-avoidance reason.
3. **Tenant/Announcement/Broadcast audit entries are recorded directly** from their application services
   (`CreateAuditEntryUseCase` injected and called in a `try`/`catch`, best-effort — the same pattern
   `admin`'s analytics services already use) rather than through an Audit event listener. Support tickets
   *do* use an Audit event listener (`SupportAuditListener`), because `support` has no dependency on
   `notification`/`audit` of its own, so no cycle risk exists there.

## API Summary

| Method | Path | Access |
|---|---|---|
| GET | `/api/v1/platform-operations/overview` | `PLATFORM_ADMIN` |
| GET | `/api/v1/platform-operations/tenants` | `PLATFORM_ADMIN` |
| GET | `/api/v1/platform-operations/tenants/{tenantId}` | `PLATFORM_ADMIN` |
| POST | `/api/v1/platform-operations/tenants/{tenantId}/suspend` | `PLATFORM_ADMIN` |
| POST | `/api/v1/platform-operations/tenants/{tenantId}/activate` | `PLATFORM_ADMIN` |
| POST | `/api/v1/platform-operations/tenants/{tenantId}/archive` | `PLATFORM_ADMIN` |
| POST | `/api/v1/platform-operations/announcements` | `PLATFORM_ADMIN` |
| GET | `/api/v1/platform-operations/announcements` | `PLATFORM_ADMIN` |
| GET | `/api/v1/platform-operations/announcements/active` | Any authenticated user |
| POST | `/api/v1/platform-operations/broadcast` | `PLATFORM_ADMIN` |
| GET | `/api/v1/platform-operations/health` | `PLATFORM_ADMIN` |

## Audit Integration

`TENANT_SUSPENDED`, `TENANT_ACTIVATED`, `TENANT_ARCHIVED`, `ANNOUNCEMENT_PUBLISHED`, and
`BROADCAST_NOTIFICATION_SENT` are all recorded best-effort (see
[Module Cycle Avoidance](#module-cycle-avoidance)): a failure to record never rolls back or fails the
already-applied action.

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `bachatsetu.platform-operations.rest.enabled` | `true` | Enables every controller in this module |

## Errors

| Condition | HTTP status | Problem code |
|---|---|---|
| Tenant has no lifecycle record to activate | 404 | `tenant-not-found` |
| Invalid lifecycle transition (e.g. re-suspending) or `endAt` before `startAt` | 422 | `platform-operations-validation-failed` |
| Request validation failure | 400 | `validation-error` |
| Not authenticated | 401 | `authentication-required` |
| Missing `PLATFORM_ADMIN` role | 403 | `platform-administrator-required` |

## Known Limitations (documented scoping decisions, not omissions)

- **Suspending a tenant does not enforce anything.** It records lifecycle state and audits the action, but
  no filter blocks a suspended tenant's users from authenticating or transacting. Wiring that enforcement
  would touch the authentication/authorization path of every other module, which this strictly-additive
  sprint does not do.
- **`lastActivityAt` is a proxy** (most recently updated user in the tenant), since no cross-module
  "activity" concept exists in this codebase to derive a true last-activity timestamp from.
- **Storage health is a configuration check, not a live probe**, for every provider except `LOCAL` — the
  cloud provider adapters in this codebase are already simulated (no live SDK calls), so there is nothing
  live to probe.
- **`buildTimestamp` is always `null`** — no `build-info` Maven execution is configured in this project.
- **Broadcast notifications are synchronous, at request time**, one `CreateNotificationUseCase` call per
  recipient, with no queue. For very large audiences this is a real latency/throughput constraint; adding
  asynchronous fan-out is future work, not required by this sprint's scope.
