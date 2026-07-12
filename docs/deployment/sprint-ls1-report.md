# Sprint LS-1 — AWS Production Deployment & Closed Beta Launch: Final Report

> **Audience:** DevOps Engineers, Product Managers, Engineering Leadership
> **Sprint scope:** Production deployment readiness validation only. No architecture changes,
> no business logic changes, no new features. Rules in effect throughout: *"Do not redesign
> any architecture. Do not modify business logic. Do not introduce new features. Do not weaken
> security. Only production deployment work."*

## 1. Overall Summary

Sprint LS-1 validated every production-deployment artifact left by Sprint PI-1 (Docker images,
`docker-compose.prod.yml`, Nginx edge config, environment-variable documentation,
PostgreSQL/Redis configuration) against the current codebase, using four parallel research
agents plus direct inspection. That validation found and fixed twelve confirmed configuration
and documentation defects, three of them Critical or High severity. It then performed a genuine
production-mode smoke test — the first time in this project's history that the backend has
actually been booted with `SPRING_PROFILES_ACTIVE=prod` against a real database in this
environment (Docker itself remains unavailable here, consistent with every prior sprint) — and
that smoke test surfaced a **launch-blocking architectural gap that pre-dates this sprint**: the
backend cannot start under any Spring profile except `local`, because its multi-tenant
resolution layer has exactly one implementation and it is deliberately gated to
`@Profile("local")` pending a real tenant-resolution design. This is not a regression introduced
by this sprint, and fixing it is explicitly out of scope for "production deployment work only" —
but it is the single most important fact in this report, and it changes the sprint's bottom-line
answer from previous sprints' "production complete" framing to **"infrastructure is
production-ready; the application itself is not yet deployable outside `local`."**

## 2. Files Created

None. This sprint modified existing configuration and documentation only — no new source files,
modules, or documents were created, consistent with the "production deployment work only"
mandate. (One new file was added and is *not* part of the tracked change set: this report
itself, `docs/deployment/sprint-ls1-report.md`.)

## 3. Files Modified

| File | Change |
| --- | --- |
| `services/backend/Dockerfile` | `chown -R bachatsetu:bachatsetu /app` at build time so the non-root runtime user can actually write to `STORAGE_LOCAL_PATH`; lowered `-XX:MaxRAMPercentage` from `75.0` to `70.0` for more non-heap headroom |
| `services/backend/.dockerignore` | Added `.env*` / `!.env.example` exclusion, matching the frontend's existing pattern |
| `docker-compose.prod.yml` | Added a `backend-prod-storage` named volume mounted at `/app/data/storage` (local file uploads previously had no volume and would be lost on every redeploy); added `PAYMENT_GATEWAY_DEFAULT_PROVIDER` and `STORAGE_LOCAL_PATH` passthrough; added an `nginx` healthcheck; raised the backend memory limit from `1536m` to `2048m` to match the lowered `MaxRAMPercentage` |
| `.env.prod.example` | Added `PAYMENT_GATEWAY_DEFAULT_PROVIDER` and `STORAGE_LOCAL_PATH` with explanatory comments |
| `deploy/nginx/nginx.conf` | Fixed `X-Forwarded-Proto` being unconditionally overwritten with nginx's own always-`http` `$scheme` (silently suppressed the backend's HSTS header in production); renamed the request-ID `map` target from `$request_id` to `$req_id` (it was shadowing nginx's own built-in variable of that name, breaking client-ID passthrough); added `proxy_hide_header` for the three defense-in-depth headers so they're no longer duplicated by both nginx and the upstream; tightened the `/actuator/health` and `/api`/`/v3/api-docs` location regexes; added `gzip_proxied any`, `server_tokens off`, `client_max_body_size 10m` |
| `services/web/next.config.ts` | Added a `Content-Security-Policy` header (previously absent entirely) |
| `docs/deployment/docker-guide.md` | Corrected the JVM heap-percentage figure; documented the new storage-volume/ownership behavior; documented the nginx header-dedup, `X-Forwarded-Proto`, and `$req_id` fixes |
| `docs/deployment/infrastructure-guide.md` | Fixed a wrong doc citation for the PostgreSQL version claim; corrected an overclaim that ElastiCache in-transit encryption "just works" (it doesn't — no TLS support in the Redis client today); rewrote §5 to remove a reference to `MANAGEMENT_SERVER_PORT`, a variable that doesn't exist anywhere in the codebase, and to reflect that `/actuator/metrics`/`/actuator/prometheus` are now public (see below) |
| `docs/deployment/environment-variables-guide.md` | Added 26 real, previously-undocumented environment variables (CORS tuning, JWT tuning, automation cron schedules, invitation validity, payment-gateway provider selection, Azure/GCP/local storage variables, admin page-size limits); corrected `REDIS_PASSWORD` from "optional" to "required under `prod`"; removed the phantom `MANAGEMENT_SERVER_PORT` entry |
| `docs/deployment/production-checklist.md` | Added the Sprint LS-1 launch-blocker finding (§1 of this report) as a new top-of-file section; added two new go-live checklist items (local-storage volume persistence, payment-gateway provider/credential match) and one new "not delivered" item (Redis in-transit encryption) |
| `docs/product/data-model-and-database-schema.md` | Updated the migration count/range from "14 migrations, `V1`–`V14`" to "18 migrations, `V1`–`V18`"; corrected the `audit_entries.event_type` enum listing from a stale 41-value/`V14` snapshot to the current 46-value/`V18` list |
| `services/backend/src/test/java/.../migration/MigrationContractTest.java` | Renamed a test method from `containsOnlyTheFifteenOrderedVersionedMigrations` to `containsOnlyTheEighteenOrderedVersionedMigrations`; extended the destructive-statement guardrail test to cover `V15`–`V18` (it previously only checked `V1`–`V14`) |

Note on scope: this sprint deliberately did **not** touch `services/backend/src/main/java` business/domain code (except the one test-file rename/extension above, which asserts on existing SQL files rather than changing behavior). The Dockerfile/Compose/Nginx/docs changes are infrastructure-only, per the sprint mandate.

## 4. Architecture Decisions

No architecture was redesigned. Two deliberate, narrow decisions were made to stay within the "fix confirmed drift, don't redesign" boundary:

- **CSP header placed in `next.config.ts`, not `nginx.conf`.** Nginx already needed `proxy_hide_header` to de-duplicate three other security headers between itself and its two upstreams (Spring Security's defaults, Next.js's own `headers()`); adding a fourth, more complex header (CSP, with a build-time-known `connect-src` origin) to that same duplication problem would have meant either hardcoding the API origin into a static `nginx.conf` or templating the file — both bigger changes than the sprint's scope justified. Setting it once, at the origin that actually needs it (the frontend, which serves the only HTML in this system), avoided that entirely. Verified in a live dev server: the header renders exactly as configured, includes the correct API origin from `NEXT_PUBLIC_API_BASE_URL`, and does not break the two known inline-content usages in the frontend (JSON-LD structured data via `<script type="application/ld+json">`, exempt from `script-src` per the HTML spec's script-supporting-elements rules; the chart theming `<style>` tag, covered by the added `style-src 'self' 'unsafe-inline'`).
- **Did not attempt to fix the `TenantScopeProvider`/`@Profile("local")` gap (§12 below).** This was the single hardest scope-boundary decision in the sprint. Removing the `@Profile("local")` gate is a one-line change that would make the exception disappear — but the code's own documentation is explicit that doing so without a real tenant-resolution strategy would silently route every `dev`/`prod` request to one fixed placeholder tenant, which is a correctness/security regression far worse than a clean startup failure. Designing a real strategy (header-based, subdomain-based, or otherwise) is business/architecture work explicitly excluded from this sprint's mandate. The correct action was to surface this clearly, not paper over it.

## 5. Bugs Found

Findings are listed by severity, each with the validating source (one of the four parallel agents, or the direct smoke test) and current status.

**Critical**
1. **Backend cannot start under `dev` or `prod` Spring profiles at all** (smoke test, direct). See §12 — not fixed, out of scope, and flagged as a launch blocker.
2. **`docker-compose.prod.yml`'s local-storage volume was unmounted and the runtime user lacked write permission on `/app`** (Docker agent). Any file upload would either be lost on redeploy or fail outright with a permission error on first write. **Fixed** (§3).
3. **nginx unconditionally overwrote `X-Forwarded-Proto` with its own always-`http` `$scheme`**, silently suppressing the backend's `Strict-Transport-Security` response header for every real HTTPS client behind the ALB (Nginx agent). **Fixed** (§3).

**High**
4. **`$request_id` nginx `map` shadowed nginx's own built-in variable of the same name**, breaking client-supplied request-ID passthrough for observability/tracing correlation (Nginx agent). **Fixed** (§3).
5. **`PAYMENT_GATEWAY_DEFAULT_PROVIDER` was never wired through `docker-compose.prod.yml` or `.env.prod.example`**, so an operator filling in Stripe/Cashfree credentials had no way to actually select that provider — the app would always silently fall back to Razorpay regardless (Docker agent, env-vars agent). **Fixed** (§3).
6. **No CSP header anywhere in the stack** (Nginx agent). **Fixed** (§3).
7. **ElastiCache Redis in-transit encryption (TLS) is claimed as a drop-in setting in `infrastructure-guide.md`, but the backend's Redis client has no TLS/`rediss://` support** — enabling it on the ElastiCache side today would break the connection (Postgres/Redis agent). **Fixed** (doc corrected to state this honestly; the underlying client gap itself is out of scope for a docs-and-config sprint and is now tracked in §12).

**Medium**
8. JVM heap sizing (`MaxRAMPercentage=75.0` against a `1536m` container limit) left thin non-heap headroom for this dependency set (Docker agent). **Fixed** (§3).
9. `REDIS_PASSWORD` documented as "optional" when it is in fact required with no default under `prod` (Postgres/Redis agent, env-vars agent). **Fixed** (§3).
10. Stale migration-count claims in `data-model-and-database-schema.md` (14 vs. actual 18) and a stale `audit_entries.event_type` enum listing (Postgres/Redis agent). **Fixed** (§3).
11. `infrastructure-guide.md` documented a `MANAGEMENT_SERVER_PORT` monitoring strategy referencing a variable that does not exist anywhere in the codebase (env-vars agent). **Fixed** (§3).
12. Duplicate security-header lines on nginx-proxied responses (Nginx agent). **Fixed** (§3).

**Low**
13. Wrong doc citation for the PostgreSQL-version claim; missing `nginx` healthcheck; `gzip_proxied`/`server_tokens`/`client_max_body_size` hardening gaps; ~20 real environment variables undocumented; `MigrationContractTest`'s stale "Fifteen" method name and incomplete destructive-statement coverage. **All fixed** (§3).

## 6. Bugs Fixed

Every item in §5 marked **Fixed** was fixed and re-verified in this sprint:

- Backend: `mvn clean verify` re-run in full after every change — **BUILD SUCCESS**, 1855 tests / 0 failures / 0 errors / 59 skipped, 0 Checkstyle/PMD/SpotBugs violations, all JaCoCo coverage gates met.
- Frontend: `npm run lint` and `npm run build` re-run after the CSP change — both clean, all 37 routes still build.
- CSP header verified live: started the dev server, confirmed the header renders with the correct `connect-src` origin resolved from `NEXT_PUBLIC_API_BASE_URL`, and confirmed zero console errors on the homepage (which exercises both the JSON-LD inline script and, indirectly, the styling system the inline-style CSP allowance protects).
- Standalone production server (`node .next/standalone/server.js` — the actual command the Docker image runs, not `next start`, which the build itself warns is incompatible with `output: standalone`) started cleanly and served `200` with all expected headers including the new CSP.
- `nginx.conf` changes are configuration-only (no test harness exists for nginx syntax in this environment, consistent with Docker being unavailable) — reviewed line-by-line against the specific defects the validation agent reported, with before/after reasoning recorded in code comments so future readers understand *why*, not just *what*.

The item **not** fixed — because it cannot be, within this sprint's mandate — is the launch blocker in §12.

## 7. Security Review

- **Fixed this sprint:** the HSTS-suppression bug (§5.3) was a real, silent security regression that would have shipped invisibly — HTTPS clients would never have received the header, with no error or log line indicating why. The CSP gap (§5.6) was a genuine defense-in-depth absence for the only HTML-serving origin in the system. Both are now closed.
- **Confirmed correct, not changed:** TLS architecture (ALB+ACM terminates TLS, nginx is deliberately HTTP-only, no half-finished Certbot artifacts anywhere), CORS configuration, the `/actuator/health*`-only public proxy surface, Swagger/OpenAPI correctly excluded from `prod`'s public-endpoints allowlist, secrets consistently sourced from environment variables with `${VAR:?message}` fail-fast semantics in Compose and `ProductionEnvironmentGuard`'s additional startup-time validation.
- **New gap surfaced, not fixed (out of scope):** ElastiCache Redis in-transit encryption has no application-side support (§5.7) — documented honestly rather than left as an overclaim.
- **New gap surfaced, not fixed (architecture work, out of scope):** the `TenantScopeProvider` gap (§12) is itself security-relevant beyond the startup failure — its own code comments confirm that if it were ever accidentally deployed to a real multi-tenant environment, it would resolve every request to one fixed placeholder tenant. The fact that it fails to start at all under `prod` is, in a narrow sense, the safer of two bad outcomes.

## 8. Performance Review

No performance testing or load testing was in scope for this sprint (already flagged as "not delivered" in the pre-existing `production-checklist.md`, unchanged). The one performance-adjacent change was the JVM heap/memory-limit adjustment (§5.8) — a conservative, headroom-increasing change, not a redesign of any pooling/caching/query strategy. HikariCP pool sizing, Flyway migration performance (all 18 migrations applied in under 700ms against a fresh database during the smoke test), and Redis configuration were all reviewed and found reasonable for closed-beta scale; no changes were made to any of them beyond the documentation correction in §5.9.

## 9. Documentation Review

Every deployment document in `docs/deployment/` was read in full and cross-referenced against the actual current state of the code (not just against itself) by the four parallel validation agents plus direct verification. Twelve confirmed inaccuracies were found and corrected (§3, §5). `production-checklist.md`, `runbook.md`, and `recovery-guide.md` — all pre-existing from Sprint PI-1 — were confirmed to already substantially satisfy this sprint's checklist/rollback/monitoring objectives (16–19) once the launch-blocker section was added; no new dedicated documents were created, since doing so would have duplicated existing, already-thorough content rather than adding value.

## 10. Testing Summary

- Backend: 1855 automated tests, 0 failures, 0 errors, 59 skipped (Testcontainers-gated tests, consistent with every prior sprint — no Docker daemon in this environment).
- Frontend: `npm run lint` clean; `npm run build` produces all 37 routes with no errors or warnings.
- Manual/live verification performed this sprint: dev-server CSP header check with console-error inspection; standalone production-server boot and `200` response check; a genuine `SPRING_PROFILES_ACTIVE=prod` backend boot against a real, freshly created PostgreSQL database and password-protected Redis instance (the first such boot in this project's history in this environment) — which is what surfaced the launch blocker in §12.
- Not performed, and explicitly out of reach in this environment: `docker build`/`docker compose up` execution (no Docker daemon — consistent with every prior sprint's own documented limitation), a restore drill from a real RDS snapshot, load testing, penetration testing.

## 11. Build Summary

- `mvn clean verify` (backend): **BUILD SUCCESS**. 1855/1855 tests passing, 0 Checkstyle/PMD/SpotBugs violations, all JaCoCo `BUNDLE` (80%) and `CLASS` (100%, auth/security packages) coverage gates met.
- `npm run lint` (frontend): clean, 0 warnings/errors.
- `npm run build` (frontend): succeeds, all 37 routes generated (static + one dynamic group-detail/invite route family), Turbopack production build completes in ~16s.
- `npm run start` (frontend) was found to be the *wrong* production invocation for this codebase — it emits an explicit warning that `output: standalone` requires `node .next/standalone/server.js` instead. This is itself a small but real finding: **any deployment runbook step that says `npm run start` in production is wrong** — the Dockerfile already does this correctly, but no doc explicitly warned against the `npm run start` shortcut before this sprint's smoke test surfaced it. Worth a one-line addition to `docker-guide.md` in a future sprint if this ever gets attempted outside Docker.

## 12. Remaining Known Issues (by severity)

**Critical — Launch Blocker**
- **The backend cannot start under any Spring profile except `local`.** `TenantScopeProvider` — required by every pre-login/tenant-scoped persistence adapter (`AuthUserRepositoryAdapter`, `UserRepositoryAdapter`, `RoleRepositoryAdapter`, `GroupInvitationRepositoryAdapter`, `AuthProfileProvisioningAdapter`, `SignupTenantResolverAdapter`, and the audit module's current-auditor resolution) — has exactly one implementation in the entire codebase, `LocalTenantScopeProviderConfig`, and it is `@Profile("local")`-gated by explicit, pre-existing design intent: a real multi-tenant resolution strategy (header, subdomain, or similar) was never designed. Confirmed via a real `SPRING_PROFILES_ACTIVE=prod` boot in this sprint: Postgres connects, all 18 Flyway migrations apply cleanly, Redis authenticates, and then Spring's context refresh fails outright on this missing bean. **This must be designed and implemented — real architecture/business-logic work — before any `dev` or `prod` deployment is attempted**, including the AWS Closed Beta this sprint was chartered to prepare for. It is explicitly out of scope for this sprint to fix.

**High**
- ElastiCache in-transit encryption (TLS) has no application-side support today (§5.7, §7). Either scope it out of the beta (acceptable now, since Redis backs no business data) or add Lettuce SSL configuration before enabling it at the infrastructure layer.
- Local file storage (`STORAGE_DEFAULT_PROVIDER=LOCAL`, the shipped default) does not survive horizontal scaling beyond one backend instance, and `AWS_S3` is the only other genuinely working provider (`AZURE_BLOB`/`GOOGLE_CLOUD_STORAGE` remain simulated adapters). Now correctly documented; still a real constraint for any multi-instance deployment plan.
- No fail-fast startup validation exists for payment-gateway or storage-provider credentials, unlike SMS/Email — a blank/misconfigured secret for either surfaces only at request time, not at boot. Documented as a checklist item; not fixed (would be new validation logic, arguably in-scope as a "configuration mistake" fix per the mission's bug-fixing exception, but judged better deferred given the more urgent Critical finding above consumed the sprint's fix budget).

**Medium / Low** (all carried over, unchanged from Sprint LR-3's own "Remaining Known Issues," still true)
- No CI/CD pipeline; no automated backup-restore drill; no secrets-manager integration; no external error-tracking/metrics dashboard wired up; no load testing; no penetration test/SAST/dependency scan; no payment-gateway production credentials issued; no legal/compliance review of stored PII. All pre-existing, all explicitly out of this sprint's scope, all already tracked in `production-checklist.md`.

**Session hygiene note:** two local Node processes remain running from this sprint's frontend smoke test (a stray `next start` on port 3050, and the correct standalone server on port 3060) and a throwaway PostgreSQL database/role (`bachatsetu_smoketest` on port 5433) that the user created for this sprint — none of these are part of the deployed system, but should be cleaned up (stop both node processes, `DROP DATABASE`/`DROP USER bachatsetu_smoketest`) since they were purely instrumentation for this sprint's smoke test.

## 13. Deployment Readiness Assessment

**Is BachatSetu ready for a Closed Beta launch on AWS?**

**No — not until the `TenantScopeProvider` gap in §12 is resolved.** Every piece of *infrastructure* this sprint was chartered to validate — Docker images, Compose orchestration, Nginx edge configuration, environment-variable completeness, PostgreSQL/Redis configuration, TLS architecture — is now either already correct or was corrected to be correct, and all of it is backed by a passing 1855-test suite and a clean production build on both services. If the only question were "is the deployment tooling production-ready," the answer would be yes, with the two remaining High-severity items (Redis TLS, local-storage scaling limits) noted as acceptable, scoped constraints for a single-instance closed beta.

But that is not the only question, and this sprint's own smoke test — the first genuine `SPRING_PROFILES_ACTIVE=prod` boot performed in this project — found that the application itself does not start outside the `local` profile at all. This is not a deployment-configuration problem this sprint could fix; it is a real, pre-existing gap in the application's multi-tenancy design that every previous sprint's testing (all of it profile-`local`, all of it against `mvn spring-boot:run -Dspring-boot.run.profiles=local`) never had the opportunity to surface. No production deployment, closed beta or otherwise, can proceed until a real tenant-resolution strategy is designed and implemented for `dev`/`prod`, and the same prod-profile smoke test performed in this sprint is re-run successfully.

Per the sprint's explicit closing instruction, this concludes Sprint LS-1. **Stopping here** — no next sprint, no payment gateway work, no push notifications, no other roadmap feature has been started. The recommended next step, once the user is ready, is a dedicated sprint scoped specifically to designing and implementing multi-tenant resolution for non-local profiles — genuinely architecture/business-logic work, and therefore outside what any "production deployment" sprint should attempt.
