# Admin Analytics Module

## Purpose

Extends the Admin module (Sprint 13.1) with read-only, platform-wide analytics for `PLATFORM_ADMIN`
callers: aggregated business insight across users, payments, groups, notifications, and storage. No
business state is ever modified by this module. No dashboards, no scheduled aggregation, no caching —
every response is computed fresh, on request, from the same tables every other module already owns.

## Architecture

Purely additive packages inside the existing Admin module, mirroring its Sprint 13.1 layering:

```
admin
 ├── domain/analytics        OverviewAnalytics, PaymentAnalytics, GroupAnalytics, UserAnalytics,
 │                           NotificationAnalytics, StorageAnalytics, shared value types, repository ports
 ├── application/analytics   command, query results, mapper, use cases, services (one per analytics area)
 └── interfaces/rest         AnalyticsController, analytics DTOs, AnalyticsApiMapper, AnalyticsApplicationConfig
```

Persistence adapters live in the shared `infrastructure.persistence.adapter` /
`infrastructure.persistence.repository.jpa` packages, exactly like every other module's adapters and like
Sprint 13.1's own Admin adapters — there is no `admin.infrastructure` package.

```
 AnalyticsController                         (separate from AdminController; same base package,
      │  (application boundary only)          same @PreAuthorize, same AdminExceptionHandler)
      ▼
 GetOverviewAnalyticsUseCase / GetPaymentAnalyticsUseCase / GetGroupAnalyticsUseCase /
 GetUserAnalyticsUseCase / GetNotificationAnalyticsUseCase / GetStorageAnalyticsUseCase
      │
      ▼
 OverviewAnalyticsRepository / PaymentAnalyticsRepository / GroupAnalyticsRepository /
 UserAnalyticsRepository / NotificationAnalyticsRepository / StorageAnalyticsRepository
      │
      ▼
 Existing Spring Data repositories (User, SavingsGroup, Member, Draw, Payment, Receipt,
 Notification, StoredFile), each extended with a handful of new, additive aggregate-query methods
```

`AnalyticsController` is a new, separate controller class rather than an extension of the existing
`AdminController` (which already carries the 13.1 statistics/listing/enable/disable endpoints) — keeping
this sprint's change purely additive. Both controllers share the same `@RestControllerAdvice`
(`AdminExceptionHandler`, matched via `basePackages = "in.bachatsetu.backend.admin.interfaces.rest"`) and
the identical `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` pattern.

The domain layer (the six analytics records, the value types, the repository ports) imports nothing from
Spring, JPA, or another module's domain — verified by the existing `ForbiddenDependencyArchitectureTest`
and `LayerDependencyArchitectureTest` ArchUnit rules. The one new cross-module edge this sprint introduces —
the six application services depending on `audit.application.usecase.CreateAuditEntryUseCase` — is a
one-directional dependency with no reverse edge, confirmed cycle-free by
`PackageDependencyArchitectureTest`.

## Analytics Computation

Every endpoint computes its result at request time, directly from `COUNT`/`SUM`/`GROUP BY` queries against
the relevant Spring Data repository — no intermediate table, no materialized view, no scheduled job, no
in-memory cache. Six computation flows exist, one per analytics area:

1. **Overview** (`GET /api/v1/admin/analytics/overview`) — thirteen direct counts across
   User/SavingsGroup/Payment/Receipt/Notification/StoredFile repositories. `inactiveUsers` is derived as
   `totalUsers - activeUsers` rather than queried separately.
2. **Payments** (`GET /api/v1/admin/analytics/payments`) — total/verified payment volume, failed/pending
   counts (pending = `INITIATED` + `PENDING_PROVIDER`), success/failure rates (guarded against division by
   zero), average contribution per verified payment (`verifiedVolume / verifiedCount`, not a separate `AVG`
   query), and a 30-day daily trend (count + volume per day).
3. **Groups** (`GET /api/v1/admin/analytics/groups`) — total/active/completed (`CLOSED`) groups, average
   members per group (`totalMembers / totalGroups`), average configured contribution amount (a genuine
   `AVG` query, since it is not derivable from any other fetched sum), monthly new-group counts, and draw
   completion rate (`COMPLETED / totalDraws`).
4. **Users** (`GET /api/v1/admin/analytics/users`) — total/active/disabled users, monthly registrations,
   preferred-language distribution, and per-tenant user counts.
5. **Notifications** (`GET /api/v1/admin/analytics/notifications`) — total notifications, an approximated
   "unread" count, delivery-status distribution, and notification-category distribution.
6. **Storage** (`GET /api/v1/admin/analytics/storage`) — total files, total bytes, average file size
   (`totalBytes / totalFiles`, not a separate `AVG` query), storage-provider distribution, and a 30-day
   daily upload trend.

No endpoint is paginated: every response is a fixed-shape aggregate summary with at most a small embedded
trend array (≤ 30 daily points, ≤ 12 monthly points), never an unbounded listing — the sprint's "paginated
where appropriate" requirement does not apply to any of these six shapes.

### Date-grouping strategy

The four time-series queries (monthly registrations, monthly new groups, daily payment trend, daily upload
trend) group by `EXTRACT(YEAR FROM ...)`, `EXTRACT(MONTH FROM ...)`, and (for daily trends) `EXTRACT(DAY
FROM ...)` — standard JPQL grammar, not a native-function passthrough (`function('date_trunc', ...)`).
`EXTRACT` has a well-defined, portable `Number` return type in Hibernate, so the adapter code can safely
convert each row's `Object[]` elements via `.intValue()`/`.longValue()` without depending on a
database-specific or Hibernate-version-specific type-inference decision.

## Repository Strategy

Every new query is an **additive** method on an **existing** Spring Data JPA repository interface — never a
new, competing repository for an already-owned entity, and never a change to an existing method's
signature or behavior:

| Repository | New methods added |
|---|---|
| `UserSpringDataRepository` | `countDistinctTenantIds`, `findMonthlyRegistrationCounts`, `findPreferredLanguageDistribution`, `findUserCountsByTenant` |
| `SavingsGroupSpringDataRepository` | `findAverageContributionAmountPaise`, `findMonthlyNewGroupCounts` |
| `MemberSpringDataRepository` | `countByDeletedFalse` |
| `DrawSpringDataRepository` | `countByDeletedFalse`, `countByStatusAndDeletedFalse` |
| `PaymentSpringDataRepository` | `sumAmountPaise`, `sumAmountPaiseByStatus`, `findDailyPaymentTrend` |
| `NotificationSpringDataRepository` | `findStatusDistribution`, `findCategoryDistribution` |
| `StoredFileSpringDataRepository` | `sumSize`, `findProviderDistribution`, `findDailyUploadTrend` |

Two averages considered during implementation (`findAverageAmountPaiseByStatus`,
`findAverageSize`) were deliberately **not** added: both are computed in the adapter from a sum and a
count already being fetched for other statistics, which avoids an extra query per request and guarantees
the average is always exactly consistent with the displayed sum/count (no possible drift between two
separate aggregate queries run against a changing table).

Every JPQL aggregate query is a single `GROUP BY`/`COUNT`/`SUM` statement against one entity — no join
fetches an unbounded collection, so none of the new queries introduce an N+1 pattern.

## Security

Reuses the exact mechanism introduced for `AdminController` in Sprint 13.1 — no new filter, no OAuth
change, no security redesign:

- `AnalyticsController` is annotated `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` at the class level, using
  the same pre-existing, tenant-independent `PLATFORM_ADMIN` role and the same
  `@EnableMethodSecurity(prePostEnabled = true)` infrastructure.
- A caller without the role receives `403 Forbidden` with an RFC 7807 body (code
  `platform-administrator-required`), via the same `AdminExceptionHandler.handleAuthorizationDenied(...)`
  used by `AdminController`.
- An unauthenticated caller receives `401 Unauthorized`, exactly like every other module's controller.

## Audit Integration

Every analytics endpoint records an `ADMIN_ANALYTICS_VIEWED` audit entry (a new `AuditEventType` constant,
added via the additive `V10__admin_analytics_audit_event.sql` migration, which widens the existing
`ck_audit_entries_event_type` CHECK constraint — the original `V9__audit_module.sql` migration is never
modified). Audit recording is strictly **best-effort**:

```java
Result result = transaction.execute(() -> mapper.toResult(repository.compute()));
auditAnalyticsViewed(command.administratorId());
return result;
```

The audit call happens **after** the read completes and is wrapped in a silent catch of `RuntimeException`
— a failure to record the audit entry never fails an already-computed analytics view. Since application-layer
code in this codebase may never depend on `org.slf4j` (enforced by
`LayerDependencyArchitectureTest.APPLICATION_MUST_DEPEND_ONLY_ON_DOMAIN_AND_APPLICATION`), the catch block
is a silent swallow with an explanatory comment only, matching the pattern established for every other
best-effort audit integration in Sprint 12.4.

## Configuration

```yaml
bachatsetu:
  admin:
    enabled: ${ADMIN_ENABLED:true}
    page-size-default: ${ADMIN_PAGE_SIZE_DEFAULT:20}
    page-size-max: ${ADMIN_PAGE_SIZE_MAX:100}
    analytics:
      enabled: ${ADMIN_ANALYTICS_ENABLED:true}
```

`AnalyticsController` is gated on `bachatsetu.admin.analytics.enabled` (default `true`) via
`@ConditionalOnProperty`. No typed `AdminAnalyticsProperties` class exists — the flag is read directly on
the controller, since none of the six analytics use cases need any other configuration value.
`AnalyticsApplicationConfig` is gated on `bachatsetu.persistence.repositories.enabled`, matching every other
module's application config; the six persistence adapters are plain `@Repository`-annotated,
`@ConditionalOnPersistenceRepositories`-gated beans, auto-registered via component scan exactly like every
other module's adapters (no `AnalyticsInfrastructureConfig` was needed — `TransactionPort`/`ClockPort` beans
are reused from the existing `AdminInfrastructureConfig`).

## Performance Considerations

- Every endpoint issues a small, fixed number of aggregate queries (roughly 2–5 per analytics area) — no
  query scans an unbounded result set except the bounded 30-day trend windows.
- No caching of any kind, by explicit sprint requirement: every value reflects the current database state
  at the moment of the request. Callers needing a stable point-in-time snapshot should capture the response
  themselves; this module has no snapshotting feature.
- No scheduled aggregation job recomputes or pre-materializes any of these numbers.
- All six persistence adapters are `@Transactional(readOnly = true)`, allowing the JPA/Hibernate session and
  the underlying connection pool to treat every analytics request as a pure read.

## Known Limitations

- **"Unread notifications" is an approximation.** `NotificationStatus` has no genuine recipient-facing
  read/unread concept — it only tracks delivery lifecycle (`QUEUED`, `SENDING`, `SENT`, `DELIVERED`,
  `FAILED`, `CANCELLED`). This module approximates "unread" as "not yet `DELIVERED`"
  (`totalNotifications - deliveredCount`), computed from the already-fetched status distribution rather than
  a second query. A future notification-read-receipt feature would make this exact rather than approximate.
- **"Completed groups" maps to `GroupStatus.CLOSED`.** There is no distinct "completed" status in the
  `GroupStatus` enum (`ACTIVE`, `INACTIVE`, `SUSPENDED`, `CLOSED`); `CLOSED` is the closest semantic match.
- **"Pending payments" is `INITIATED` + `PENDING_PROVIDER`.** `PaymentStatus` has no single "pending"
  value; both pre-verification statuses are summed.
- **No pagination on any endpoint**, by design — every response is a bounded aggregate summary, not a
  listing.
- **No historical/point-in-time analytics.** Every number reflects "right now"; there is no way to ask for
  analytics "as of" a past date beyond what the bounded trend windows (30 days / 12 months) already surface.
- **Average metrics guard against zero denominators** (empty tables, no groups, no verified payments) by
  returning `0.0` rather than `NaN` or throwing.

## Testing

Domain tests for all six analytics records and the five shared value types; application-mapper tests;
six application-service tests (one per analytics area, each covering success + audit recording, a
best-effort audit failure not propagating, and null-command rejection); REST mapper tests; a
`@WebMvcTest` controller test suite covering all six endpoints plus the `PLATFORM_ADMIN`-only authorization
gate (`@WithMockUser`/`@EnableMethodSecurity`, asserting both the happy path and the 403
`platform-administrator-required` rejection); a config test verifying all six use-case beans compose
correctly; mock-based tests for all six persistence adapters (verifying rate/average computation,
zero-denominator guards, and trend-row mapping); and a Testcontainers-based integration test exercising the
six repository adapters against a real PostgreSQL database (skipped automatically when Docker is
unavailable, consistent with every other integration test in this codebase).
