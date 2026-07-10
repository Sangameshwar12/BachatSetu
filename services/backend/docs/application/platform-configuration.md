# Platform Configuration Module

## Purpose

Centralizes platform-wide behavior that previously required a code change or environment-variable
redeploy: general settings, maintenance mode, feature flags, and system limits. `PLATFORM_ADMIN` callers
read and update all of it through REST; every other module keeps running unmodified.

## Architecture

Purely additive packages inside the existing Admin module, mirroring Sprints 13.1/13.2's layering:

```
admin
 ├── domain/configuration       PlatformConfiguration aggregate, FeatureFlag, PlatformLimit, repository ports
 ├── application/configuration  commands, query results, mapper, use cases, services, hot-path query services
 └── interfaces/rest            PlatformConfigController, config DTOs, PlatformConfigApiMapper,
                                 PlatformConfigApplicationConfig
```

Persistence lives in the shared `infrastructure.persistence.adapter` / `.repository.jpa` / `.entity.config`
packages, exactly like every other module. Two new cross-cutting Servlet filters —
`security.filter.MaintenanceModeFilter` and `security.filter.FeatureFlagEnforcementFilter` — are registered
alongside the existing `JwtAuthenticationFilter` in `SecurityConfiguration`, running immediately after it so
the caller's resolved roles are available.

**Naming note:** the domain and application sub-packages are named `configuration`, not `config`, even
though every other layer in this feature uses `config` freely (`interfaces.rest.config`,
`interfaces.rest.dto.config`, `infrastructure.persistence.entity.config`). This is deliberate:
`ArchitecturePackages.CONFIGURATION = "..config.."` is an existing ArchUnit pattern meaning "the Spring
`@Configuration` wiring layer" — naming the *domain* package `config` made ArchUnit's own domain classes
match that pattern as a false-positive "outer layer" dependency. Renaming only the domain/application
packages sidesteps the collision without touching the rule itself.

## Feature Flags

Nine independently toggleable features: `AUTHENTICATION`, `PAYMENTS`, `NOTIFICATIONS`, `STORAGE`,
`RECEIPTS`, `AUCTION`, `ANALYTICS`, `AUDIT`, `SIGNUP` — seeded enabled by default in
`V11__platform_configuration.sql`.

**Enforcement design.** The sprint asked that "application services... consult FeatureFlagService before
executing business operations." Wiring that check into every business module's application services would
mean touching Payment, Notification, Storage, Auth, Receipt, Auction, and Audit's constructors and tests —
a large, invasive change explicitly out of scope ("do NOT modify existing business rules... break
Payment/Notification/Authentication/Storage"). Instead, `FeatureFlagEnforcementFilter` centralizes
enforcement at the HTTP boundary: it maps a request's path prefix to a `FeatureKey`, asks
`FeatureFlagQueryService` (a thin wrapper over `FeatureFlagRepository`) whether it's enabled, and returns
`503` before the request ever reaches a controller if not. This satisfies the requirement's intent —
disabled features genuinely stop working — without a single line changed in any other module.

Path → feature mapping:

| Path prefix | Feature |
|---|---|
| `/api/v1/admin/analytics` | `ANALYTICS` |
| `/api/v1/admin/*` (everything else) | never gated |
| `/api/v1/auth` | `AUTHENTICATION` |
| `/api/v1/payments` | `PAYMENTS` |
| `/api/v1/notifications` | `NOTIFICATIONS` |
| `/api/v1/storage` | `STORAGE` |
| `/api/v1/receipts` | `RECEIPTS` |
| `/api/v1/auctions` | `AUCTION` |
| `/api/v1/audit` | `AUDIT` |

`/api/v1/admin/**` (other than analytics) is never gated, so an administrator can always reach the
configuration endpoints — including to undo a flag they just disabled. `SIGNUP` has no dedicated REST path
(registration and login both go through the same OTP-based `/api/v1/auth/otp` endpoints in this codebase)
and is therefore not enforced by the filter; it remains a stored, editable flag for future use. Unlike
maintenance mode, feature-flag enforcement has no `PLATFORM_ADMIN` bypass — disabling a feature disables it
for everyone, admins included, except the flag's own path being excluded via the `/api/v1/admin` rule above.

A disabled feature returns `503 Service Unavailable`, RFC 7807, `code: "feature-disabled"`.

## Maintenance Flow

`PlatformConfiguration` carries `maintenanceEnabled`, `maintenanceMessage`, `maintenanceStartAt`,
`maintenanceEndAt`. `PlatformConfiguration.isMaintenanceActiveAt(Instant)` treats an unset start/end as "no
window" (maintenance is active immediately once enabled); when both are set, maintenance is only active
inside `[start, end]` inclusive — supporting scheduled maintenance windows announced in advance.

`MaintenanceModeFilter` runs on every request except:
- `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**` — health and API docs always available.
- `/api/v1/auth/**` — authentication endpoints remain available during maintenance, per the sprint's
  explicit requirement.
- `/api/v1/admin/**` — administrators must always be able to reach their own tooling, including to turn
  maintenance mode back off.
- Any request already authenticated with `ROLE_PLATFORM_ADMIN` — "platform admins continue to access
  everything," checked via the already-resolved `SecurityContextHolder` authentication (the filter runs
  after `JwtAuthenticationFilter`).

Everyone else receives `503 Service Unavailable`, RFC 7807, `code: "maintenance-mode"`, with the configured
`maintenanceMessage` as the `detail` (falling back to a generic message if none is set).

## Configuration Flow

`GET/PUT /api/v1/admin/config` reads/replaces the entire `PlatformConfiguration` singleton (general settings
+ maintenance state) in one call — `UpdateConfigurationCommand` is a full-replace update, not a patch.
`GetConfigurationApplicationService`/`UpdateConfigurationApplicationService` wrap the read/write in
`TransactionPort`; the update records one `PLATFORM_CONFIGURATION_UPDATED` audit entry after the write
completes.

`GET/PUT /api/v1/admin/config/feature-flags` and `GET/PUT /api/v1/admin/config/limits` are **partial**
updates: the request body is a `Map<String, Boolean>` (or `Map<String, Long>` for limits) of only the keys
being changed. Each changed key records its own audit entry (`FEATURE_FLAG_UPDATED` /
`SYSTEM_LIMIT_UPDATED`) — one call updating three flags produces three audit entries, for precise
traceability of exactly which flags changed and to what value.

## Persistence

`V11__platform_configuration.sql` creates a new `config` schema with three tables, all additive — no
existing business table is touched:

- `config.platform_configuration` — a singleton row (`id SMALLINT PRIMARY KEY DEFAULT 1`, `CHECK (id = 1)`),
  seeded with sensible defaults (English, 300s OTP expiry, LOCAL storage, RAZORPAY payments, 10 MB uploads,
  100 members/group, 20 groups/organizer, maintenance off).
- `config.feature_flags` — one row per `FeatureKey`, all seeded `enabled = TRUE`.
- `config.platform_limits` — one row per `LimitKey`, seeded with generous defaults.

The same migration widens `audit.audit_entries`' `ck_audit_entries_event_type` CHECK constraint (via `DROP
CONSTRAINT` / `ADD CONSTRAINT`, following the pattern established in `V10`) to add
`PLATFORM_CONFIGURATION_UPDATED`, `FEATURE_FLAG_UPDATED`, `SYSTEM_LIMIT_UPDATED`.

None of the three new JPA entities extend `BaseJpaEntity`: they are settings rows, not soft-deletable
business aggregates, so they carry only the columns they actually need (`version`/`updated_at`/`updated_by`
tracked as plain, domain-managed columns — not JPA `@Version` optimistic locking, to avoid the aggregate's
own manually-incremented `version` field conflicting with Hibernate's own version-management semantics).

## Security

Identical mechanism to the rest of the Admin module: `PlatformConfigController` is annotated
`@PreAuthorize("hasRole('PLATFORM_ADMIN')")` at the class level — both reads and writes require the role.
A non-administrator receives `403 Forbidden` (`platform-administrator-required`), via the same
`AdminExceptionHandler` shared with `AdminController`/`AnalyticsController`. An unauthenticated caller
receives `401 Unauthorized`.

## Limit Validation

`PlatformLimit` persists five ceilings (`MAX_GROUPS`, `MAX_MEMBERS`, `MAX_UPLOADS`, `MAX_RECEIPTS`,
`MAX_NOTIFICATIONS`) and is fully readable/writable via `GET/PUT /api/v1/admin/config/limits`. Actually
**validating** business operations against these limits (e.g. rejecting a group-creation request once
`MAX_GROUPS` is exceeded) would require modifying each business module's own creation flow — the same
blast-radius concern documented above for feature flags, and explicitly out of scope
("do NOT modify existing business rules"). This sprint ships the storage, retrieval, and audit trail for
every limit; wiring live enforcement into Group/Member/Storage/Receipt/Notification's own application
services is deliberately left as a follow-up, tracked as a known limitation below.

## Future Extensions

- Wire `PlatformLimitRepository`/limit values into the relevant business modules' own creation use cases
  (group creation, member addition, file upload, receipt generation, notification dispatch) for live
  enforcement.
- Consider a dedicated `SIGNUP` REST path (or an OTP `purpose` check in the existing `/api/v1/auth/otp`
  flow) so the `SIGNUP` feature flag can be enforced the same way the other eight are.
- Consider surfacing maintenance-mode state on the (currently unauthenticated) health endpoint, so external
  monitoring can distinguish "down" from "intentionally in maintenance."

## Testing

Domain tests for `PlatformConfiguration` (validation, `update`, and every `isMaintenanceActiveAt` window
combination), `FeatureFlag`, `PlatformLimit`, and both key enums; application-mapper tests; six
application-service tests (Get/Update × Configuration/FeatureFlags/SystemLimits, each covering success +
audit recording, a best-effort audit failure not propagating, and null-command rejection) plus dedicated
tests for the two hot-path query services (`FeatureFlagQueryService`, `MaintenanceStatusQueryService`,
including the fail-open behavior on a missing row); REST-mapper tests; a `@WebMvcTest` controller suite
covering all six endpoints plus the `PLATFORM_ADMIN`-only authorization gate; a config test verifying all
eight beans compose correctly; mock-based tests for all three persistence adapters; two filter unit-test
suites (`MaintenanceModeFilterTest`, `FeatureFlagEnforcementFilterTest`) covering the allow/reject paths,
the admin/auth bypass rules, and fail-open behavior when the underlying service is unavailable; and a
Testcontainers-based integration test exercising all three repositories against a real PostgreSQL database,
including reading the migration's seeded defaults (skipped automatically when Docker is unavailable,
consistent with every other integration test in this codebase).
