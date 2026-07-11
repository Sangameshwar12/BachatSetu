# Non-Functional Requirements and Production Readiness

> **Audience:** DevOps Engineers, QA Engineers, Developers, Investors
> **Prerequisite reading:** [System Architecture and Modules](system-architecture-and-modules.md), [Security and Compliance](security-and-compliance.md)

This chapter separates what is **actually enforced by the build and verified in the running application** from what [`docs/operations/production-readiness.md`](../operations/production-readiness.md) requires before a production launch — the latter is written as a pre-launch checklist and, as of this writing, remains largely unstarted at the infrastructure level (no cloud account, no deployment, no on-call runbooks exist yet).

## 1. Backend Build Quality Gates

Fully implemented and enforced on every `mvn clean verify` run (local and CI), per [`services/backend/docs/quality/build-quality.md`](../../services/backend/docs/quality/build-quality.md):

| Gate | What it checks |
| --- | --- |
| Maven Enforcer | Java 21, Maven 3.9+, dependency convergence, no duplicate declarations, banned legacy dependencies |
| Checkstyle | Source hygiene, import order, 160-character line limit, no unresolved work markers |
| PMD | Correctness, unused code, null-check, and resource-lifecycle rules |
| SpotBugs | Medium-or-higher bytecode defect findings, run at maximum analysis effort |
| JaCoCo | ≥80% line coverage for invariant-bearing core domain classes (`BaseAggregateRoot`, `Money`, `SavingsGroup`, `Payment`, `PaymentAttempt`, `Draw`, `AuctionBid` families — 14 compiled classes currently in scope) |
| ArchUnit | Layered-architecture and module-boundary rules (see [System Architecture and Modules §3](system-architecture-and-modules.md#3-backend-layered-architecture)) |

A pull request is not releasable unless all of the above pass. Some persistence integration tests use Testcontainers and are automatically skipped if Docker isn't available locally (`disabledWithoutDocker = true`) — this is expected in a Docker-less environment, not a failure.

## 2. Frontend Production-Readiness Sprint (FE-6)

A dedicated sprint hardened `services/web` across the categories below, without changing any business functionality. Every item is verified in code, not aspirational.

### Performance

- React Query Devtools is now `React.lazy()`-loaded and excluded from the production bundle entirely (previously statically imported and shipped to every user regardless of environment).
- The `recharts` charting library (used only in the Admin Portal's Analytics screen) is loaded via `next/dynamic` only when a chart-bearing tab is actually opened, rather than bundled with every visit to that page.
- React Query defaults: 30-second `staleTime`, `retry: 1`, `refetchOnWindowFocus: false` — tuned to avoid redundant refetches without going stale for long.
- Fonts are self-hosted via `next/font/google` (Geist, Geist Mono) — no render-blocking external font request.

### Error Handling

Every data-fetching screen implements four states: loading (skeleton), retry-capable error, honestly-labeled empty state, and success (see [Frontend Experience §7](frontend-experience.md#7-the-honest-empty-state-pattern)). A 401 response triggers an explicit "session expired" toast before redirecting to `/login` (see [Security and Compliance §2](security-and-compliance.md#2-session-handling-frontend)). Every state-changing action (enable/disable a user, suspend/activate/archive a tenant, update configuration, publish an announcement, send a broadcast) now shows an explicit success or failure toast — this was audited and made consistent across the Admin Portal during FE-6, where several mutations previously had no user-facing feedback at all on failure.

### Accessibility

A skip-to-main-content link was added to the root layout (previously absent, including on the `(auth)` route group, which had no `<main>` landmark at all). Every navigation region (`sidebar`, marketing header, mobile drawer) now has a distinguishing `aria-label`. The active sidebar item carries `aria-current="page"`. Focus rings and keyboard navigation are inherited from the shared shadcn/ui component library throughout.

### SEO

Per-page metadata, OpenGraph/Twitter cards, canonical URLs, and JSON-LD `Organization` structured data on the landing page; `app/sitemap.ts` and `app/robots.ts` (dashboard routes explicitly disallowed, since they require authentication and have no public content to index). `/privacy` and `/terms` pages were created during this sprint because the site footer already linked to both, but neither page existed — a real 404 that FE-6 fixed.

### PWA

A web app manifest (`app/manifest.ts`) with an SVG app icon and theme color, plus a minimal offline-fallback service worker (`public/sw.js`) that does not cache or serve stale data — it only shows a friendly `/offline` page if a navigation request fails outright while the network is unreachable. There is no custom "Add to Home Screen" prompt or update-available UI; the platform relies on each browser's native install prompt.

### Monitoring Scaffolding

`src/lib/logger.ts` is a vendor-neutral logging abstraction — every error boundary and a new global `unhandledrejection`/`window.onerror` listener route through it instead of calling `console.*` directly, so a real provider (Sentry, Datadog, etc.) can be wired in later by changing only that one file, with no call-site changes anywhere else in the codebase. No such provider is wired in today.

### Security (see [Security and Compliance](security-and-compliance.md) for full detail)

The Admin Portal role-gating gap (any authenticated user could navigate into `/dashboard/admin/*`) was found and fixed during this sprint. Client-side file-type/size validation was added to the profile-photo upload.

## 3. What "Production Ready" Still Requires

[`docs/operations/production-readiness.md`](../operations/production-readiness.md) defines the full pre-launch checklist. Status of each category, as of this writing:

| Category | Status |
| --- | --- |
| Production AWS account structure | ⛔ Not started |
| Network boundaries | ⛔ Not started |
| Secrets managed outside source control | 🟡 Local/dev uses environment variables only; no secrets manager configured |
| Database backups active | ⛔ Not started — no production database exists |
| Restore drill completed | ⛔ Not started |
| Centralized logging | 🟡 Frontend has a logging abstraction ready to wire up (see above); backend logging conventions are defined in [`docs/standards/logging-standards.md`](../standards/logging-standards.md) but no centralized log aggregation is deployed |
| Metrics and alerts | ⛔ Not started |
| Error tracking | 🟡 Frontend seam exists (`logger.ts`); no provider wired on either service |
| Security scans | 🟡 Dependabot and Dependency Review are active at the repository level (see [`SECURITY.md`](../../SECURITY.md)); no SAST, secret-scanning-in-CI, container scanning, or penetration test confirmed |
| Incident response process documented | ✅ [`docs/governance/security-process.md`](../governance/security-process.md) exists |
| Support escalation process documented | 🟡 A `support` module and ticket lifecycle exist in the product (see [Business Processes — Support Ticket Lifecycle](business-processes.md#support-ticket-lifecycle)); no human on-call/escalation runbook exists |
| Observability dashboards (API latency, error rate, payment success/failure, webhook failures, DB/Redis health, background job failures) | ⛔ Not started — Spring Boot Actuator is enabled (`/actuator/health`) but no metrics pipeline or dashboarding tool is connected |
| Runbooks (deployment, rollback, DB restore, provider outage, payment mismatch, admin compromise, secret rotation, incident communication) | ⛔ Not started |

## 4. Testing Strategy

`services/backend` follows [`docs/workflow/testing-strategy.md`](../workflow/testing-strategy.md); every module has unit tests for domain/application logic and controller-level tests for the REST layer (confirmed by the presence of a `*Test.java`/`*ControllerTest.java` file for every controller listed in [Backend Module and API Reference](backend-module-and-api-reference.md)), plus persistence integration tests using Testcontainers where applicable. `services/web` has **no automated test suite** as of this writing — correctness is currently verified through `npm run lint`, `npm run build` (which includes the TypeScript compiler), and manual browser-preview verification per sprint. This is the single largest testing gap and is the top item under frontend work in [Roadmap and Future Work](roadmap-and-future-work.md).

## Next Chapter

[Roadmap and Future Work](roadmap-and-future-work.md) consolidates every gap flagged across this entire documentation set into one prioritized list.
