# Admin Module

## Purpose

Introduces a foundation-only Administration module: read-only platform-wide visibility (user, group, tenant
listings; platform statistics) plus one explicit mutation (enable/disable a platform user). No module's
domain model changes to support it. No dashboards, no analytics, no reporting, no caching — every number is
computed on demand through existing repositories, extended additively.

## Architecture

Hexagonal/DDD, mirroring every other module in this codebase:

```
admin
 ├── domain          PlatformAdministration, summaries, statistics, repository ports
 ├── application     ports, command, query, mapper, use cases, services
 └── interfaces/rest adapters, DTOs, mapper, controller, exception handler, config
```

Persistence adapters live in the shared `infrastructure.persistence.adapter`/`infrastructure.persistence.repository.jpa`
packages, exactly like every other module's own repository adapters (`AuditEntryRepositoryAdapter`,
`StoredFileRepositoryAdapter`, ...) — there is no separate `admin.infrastructure` package, since this
codebase's actual convention keeps outbound adapters there and reserves `<module>.interfaces.rest.adapter`
for the module's own Clock/Transaction port implementations.

```
 AdminController
      │  (application boundary only)
      ▼
 GetPlatformStatisticsUseCase / ListPlatformUsersUseCase / ListPlatformGroupsUseCase /
 ListPlatformTenantsUseCase / EnableUserUseCase / DisableUserUseCase
      │
      ▼
 PlatformUserRepository / PlatformGroupRepository / PlatformTenantRepository / PlatformStatisticsRepository
      │
      ▼
 Existing Spring Data repositories (User, SavingsGroup, Payment, Receipt, Notification, StoredFile),
 each extended with a handful of new, additive, cross-tenant query/count methods
```

The domain layer (`PlatformAdministration`, the summary types, the repository ports) never imports Spring,
JPA, or another module's domain type — verified by the existing `ForbiddenDependencyArchitectureTest` and
`LayerDependencyArchitectureTest` ArchUnit rules. `AdminController` depends only on the application boundary
(use cases, DTOs, mapper, `CurrentUserProvider`) — never on a domain or persistence type — satisfying
`CONTROLLERS_MUST_NOT_DEPEND_ON_DOMAIN_OR_INFRASTRUCTURE`.

## Why Admin Needs Its Own Cross-Tenant Repositories

Every existing repository in this codebase (`auth.domain.port.UserRepository`,
`group.domain.port.SavingsGroupRepository`, ...) is scoped to the caller's own tenant, usually via a
request-scoped `TenantScopeProvider`. That is correct for every existing use case, but wrong for platform
administration, which is inherently cross-tenant by definition.

Rather than duplicate or redesign any existing repository, Admin introduces its **own** ports
(`PlatformUserRepository`, `PlatformGroupRepository`, `PlatformTenantRepository`,
`PlatformStatisticsRepository`) backed by adapters that reuse the **same** underlying JPA entities and Spring
Data repository interfaces every other module already uses — extended with a small number of new, additive,
tenant-unscoped query and count methods (e.g. `UserSpringDataRepository.searchAcrossTenants(...)`,
`countByAuthenticationStatusAndDeletedFalse(...)`, `findDistinctTenantIds(...)`). No existing repository
method's behavior, signature, or tenant-scoping changed.

## PlatformAdministration

The one true domain aggregate this module requires: a lightweight representation of "the platform
administrator performing this action," carrying only the acting administrator's id. It exposes
`enableUser(userId, at)`/`disableUser(userId, at)`, each returning a `PlatformUserStatusChange` — a pure,
persistence-free decision (target user, target status, acting administrator, timestamp) that the application
layer then applies through `PlatformUserRepository.updateStatus(...)`.

It deliberately never touches `auth.domain.model.User`: enabling/disabling a user here is a platform-wide,
cross-tenant action, while `auth.domain.port.UserRepository` is always scoped to the caller's own tenant.
Bypassing the `User` aggregate for this one, additive, admin-triggered mutation avoided redesigning it or its
repository — the sprint's explicit constraint.

## Statistics

`GET /api/v1/admin/statistics` returns: `totalUsers`, `activeUsers`, `disabledUsers`, `totalGroups`,
`activeGroups`, `totalPayments`, `completedPayments` (payments in `VERIFIED` status), `totalReceipts`,
`totalNotifications`, `totalFiles`. Every number is a direct `count`/`countBy...` query against an existing
module's own Spring Data repository, executed on demand — no SQL view, no materialized view, no scheduled
recomputation, no cache.

## REST API

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/admin/statistics` | Platform-wide totals |
| `GET` | `/api/v1/admin/users` | Cross-tenant user search, paginated |
| `GET` | `/api/v1/admin/groups` | Cross-tenant savings group search, paginated |
| `GET` | `/api/v1/admin/tenants` | Tenants known to the platform, with per-tenant totals |
| `POST` | `/api/v1/admin/users/{id}/enable` | Enables one platform user, across any tenant |
| `POST` | `/api/v1/admin/users/{id}/disable` | Disables one platform user, across any tenant |

### User search filters and sorting

- Filters (all optional): `status` (`PlatformUserStatus` name), `email` (partial, case-insensitive), `phone`
  (partial), `createdAfter`/`createdBefore` (inclusive `Instant` range)
- Sort fields: `createdAt`, `firstName`, `lastName`, `email`; direction `asc`/`desc` (default `desc`)
- `status` filters on the user's **authentication** status (`PENDING_VERIFICATION`/`ACTIVE`/`LOCKED`/
  `SUSPENDED`/`DISABLED`) — the same status `enable`/`disable` mutate — not the separate profile status
  tracked by the (unrelated, pre-existing) Profile module

### Group search filters

- Filters (all optional): `status` (`PlatformGroupStatus` name), `createdAfter`/`createdBefore`
- Sorted by `createdAt` only; direction `asc`/`desc` (default `desc`)

### Pagination

`page`/`size` are optional on every listing endpoint; when omitted, `bachatsetu.admin.page-size-default` is
used. A caller-supplied `size` greater than `bachatsetu.admin.page-size-max` is rejected with a 400, before
any query runs.

## Security

Reuses the existing authentication and permission infrastructure entirely: no new Spring Security filter, no
OAuth, no redesign.

- `SecurityConfiguration` already enables method security (`@EnableMethodSecurity(prePostEnabled = true)`)
  and `JwtAuthenticationFilter` already grants each authenticated caller a `ROLE_<role>` authority per role
  claim in their access token — both were already present, unused by any other controller until now.
- `AdminController` is annotated `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` at the class level.
  `PLATFORM_ADMIN` is a pre-existing, tenant-independent role, seeded since `V2__seed_roles_permissions.sql`
  (`role_scope = 'PLATFORM'`, `tenant_id = NULL`) but never previously checked by any endpoint.
- A caller without the `PLATFORM_ADMIN` role receives `403 Forbidden` with an RFC 7807 body
  (`platform-administrator-required`) via a dedicated `AuthorizationDeniedException` handler in
  `AdminExceptionHandler` — normally intercepted upstream by Spring Security's own filter-level
  `AccessDeniedHandler` before reaching any controller advice, but handled here too so the response is
  consistent regardless of exactly where in the chain it is caught.
- An unauthenticated caller receives `401 Unauthorized`, exactly like every other module's controller.

## Configuration

```yaml
bachatsetu:
  admin:
    enabled: ${ADMIN_ENABLED:true}
    page-size-default: ${ADMIN_PAGE_SIZE_DEFAULT:20}
    page-size-max: ${ADMIN_PAGE_SIZE_MAX:100}
```

`AdminController` is gated on `bachatsetu.admin.enabled` (default `true`); `AdminApplicationConfig` and
`AdminInfrastructureConfig` are gated on `bachatsetu.persistence.repositories.enabled` (default `true`),
matching every other module. `AdminProperties` itself is bound unconditionally (in `AdminPropertiesConfig`),
since `AdminApiMapper` — like every module's API mapper, a plain always-scanned `@Component` — depends on it
regardless of whether persistence or the controller are enabled; gating it the same way as the other beans
would leave that mapper without a required dependency in a minimal test context.

## Persistence

No new Flyway migration. Every query is additive: new methods on the existing `UserSpringDataRepository`,
`SavingsGroupSpringDataRepository`, `MemberSpringDataRepository`, `PaymentSpringDataRepository`,
`ReceiptSpringDataRepository`, `NotificationSpringDataRepository`, and `StoredFileSpringDataRepository`
interfaces — no new table, no schema change, no existing method's behavior altered. "Tenants" have no
dedicated table in this codebase; `PlatformTenantSummary` is derived from the distinct `tenant_id` values
already recorded on users.

## Testing

Domain (`PlatformAdministrationTest`, summary/statistics validation tests, search-criteria and paging
tests), application mapper and all 6 application services (statistics, list users/groups/tenants,
enable/disable — including not-found and null-argument cases), REST mapper, controller (including the
`PLATFORM_ADMIN`-only authorization gate via `@WithMockUser`/`@EnableMethodSecurity` in a `@WebMvcTest`
slice, unauthenticated handling, and not-found mapping), both config classes, mock-based tests for all four
persistence adapters, and a Testcontainers-based persistence integration test covering cross-tenant search
filtering (status/email/date range), pagination, sorting, enable/disable (including the not-found case),
tenant listing, and statistics computation. Every number in this module's tests was independently derived
from the underlying entity/query shape, not copied from the implementation.

## Limitations

- The Testcontainers integration test exercises the user- and tenant-facing repository adapters end to end
  against a real database; the group listing adapter's cross-tenant query and the statistics adapter's
  payment/receipt/notification/file counts are covered by mock-based unit tests only, not by an equivalent
  real-database integration test in this sprint.
- No full-text search, no combined multi-field sort, no export.
- Group listings sort by `createdAt` only.
- `PlatformUserSummary`'s `status` reflects the authentication status only; the separate, pre-existing
  Profile module's own status is not exposed here.
- Enable/disable is idempotent and unconditional: no domain rule prevents enabling an already-active user or
  disabling an already-disabled one, and no audit trail entry is recorded for this sprint's admin actions
  (the pre-existing Audit module is a natural, additive integration point for a later sprint).
- No rate limiting, caching, or dashboard — this sprint is the foundation only.
