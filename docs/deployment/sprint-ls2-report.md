# Sprint LS-2 — Production Multi-Tenant Resolution: Final Report

> **Audience:** DevOps Engineers, Product Managers, Engineering Leadership
> **Sprint scope:** Remove the single launch blocker found in Sprint LS-1 — the backend's
> failure to start under any Spring profile except `local` — and nothing else. Rules in effect
> throughout: *"Do not redesign the domain. Do not modify business rules. Do not add new
> product features... Preserve all DDD, Hexagonal Architecture, CQRS and Clean Architecture
> boundaries. Every change must be additive or minimal."*

## 1. Overall Summary

Sprint LS-2 removed the launch blocker Sprint LS-1 found: the backend's tenant-resolution layer
(`TenantScopeProvider`) had exactly one implementation, deliberately gated to `@Profile("local")`
pending a real multi-tenant resolution design. Research confirmed this codebase is genuinely
single-tenant-per-deployment today (no tenant self-registration flow exists, `platform.tenants`
only tracks lifecycle status, not per-request routing), so the correct minimal fix — consistent
with the "no domain redesign" mandate — was to make the existing "one fixed tenant" strategy
**configurable and available in every profile**, rather than hardcoded and local-only. The same
research surfaced a second, structurally identical blocker not caught by LS-1's smoke test
(because the app crashed on the first blocker before reaching the second): `CurrentAuditorProvider`
(populates every entity's `created_by`/`updated_by`) had the same `@Profile("local")` gate. Both
were fixed the same sprint, since leaving the second in place would have made the very
production-mode smoke test this sprint was chartered to re-run fail on the first real database
write. A genuine `SPRING_PROFILES_ACTIVE=prod` boot — the same kind of test that found the
original blocker — now succeeds, and a full signup → OTP verify → authenticated onboarding →
logout flow was exercised end-to-end against a real Postgres/Redis, with database-level
verification that tenant and auditor resolution behave exactly as designed.

## 2. Files Created

| File | Purpose |
| --- | --- |
| `services/backend/src/main/java/.../infrastructure/persistence/adapter/TenantScopeProviderConfig.java` | Replaces `LocalTenantScopeProviderConfig` — resolves the single configured tenant (`TENANT_DEFAULT_ID`), available in every profile |
| `services/backend/src/main/java/.../security/context/SecurityContextCurrentAuditorProvider.java` | Replaces `LocalCurrentAuditorProviderConfig` — returns the real signed-in user's id for authenticated requests (read from `SecurityContextHolder`, the same principal `JwtAuthenticationFilter` already sets), falling back to a configured system-actor id (`AUDIT_SYSTEM_ACTOR_ID`) only for the handful of pre-authentication writes (signup, OTP request) |
| `services/backend/src/test/java/.../TenantScopeProviderConfigTest.java` | Unit test for the new tenant provider |
| `services/backend/src/test/java/.../SecurityContextCurrentAuditorProviderTest.java` | Unit test covering both the authenticated and pre-authentication code paths |

## 3. Files Modified

| File | Change |
| --- | --- |
| `AuthUserRepositoryAdapter.java`, `UserRepositoryAdapter.java`, `RoleRepositoryAdapter.java`, `GroupInvitationRepositoryAdapter.java`, `AuthProfileProvisioningAdapter.java`, `SignupTenantResolverAdapter.java` | Removed `@Profile("local")` — each depends (directly or transitively) on `TenantScopeProvider`, which is now available in every profile. `GroupInvitationRepositoryAdapter` turned out not to need `TenantScopeProvider` at all (it takes `tenantId` as a method parameter); its gate was inherited by convention, not an actual dependency. |
| `security/config/SecurityBeansConfiguration.java` | Registers the new `securityContextCurrentAuditorProvider` bean, marked `@Primary` and given a name distinct from `JpaAuditingConfig`'s `@ConditionalOnMissingBean` fallback (see §5, finding 2, for why both were necessary) |
| `configuration/production/ProductionEnvironmentGuard.java` + its test | Added fail-fast checks: both `TENANT_DEFAULT_ID` and `AUDIT_SYSTEM_ACTOR_ID` must be set under `prod`, and must not be the local development placeholder values — mirrors the existing pattern for `DATABASE_PASSWORD`/`AUTH_CORS_ALLOWED_ORIGINS` |
| `application.yml`, `application-dev.yml`, `application-prod.yml` | Added `bachatsetu.tenancy.default-tenant-id` and `bachatsetu.persistence.auditing.system-actor-id` — safe local defaults in the base file, no defaults (fail-fast) in `dev`/`prod` |
| `db/seed/V900__local_development_seed_data.sql` | Updated a comment that referenced the now-deleted `LocalTenantScopeProviderConfig` class by name |
| `docker-compose.prod.yml`, `.env.prod.example`, `docs/deployment/environment-variables-guide.md` | Wired `TENANT_DEFAULT_ID` and `AUDIT_SYSTEM_ACTOR_ID` through to the backend container and documented both |

## 4. Architecture Decisions

- **Configured single tenant, not header/subdomain resolution.** Confirmed via code and schema
  inspection that this deployment is genuinely single-tenant today: no tenant self-registration
  use case exists anywhere in `platformoperations` (only `Activate`/`Suspend`/`Archive`/`Get`/
  `SearchTenants`), and `platform.tenants` has no column that could drive per-request routing
  (no subdomain, no header key, nothing beyond a status enum). Building real per-request
  multi-tenant resolution would be new architecture and new request-routing surface — squarely
  out of this sprint's "do not redesign the domain" boundary. Making the existing "one fixed
  tenant" strategy configurable, rather than hardcoded, is the correct **minimal** fix: it
  changes zero business behavior (every request already resolved to one tenant; it's just no
  longer a hardcoded magic UUID restricted to one profile) and needs no new domain concepts.
- **`SecurityContextCurrentAuditorProvider` placed in `security.context`, not
  `infrastructure.persistence.audit`.** The obvious first attempt — reading the authenticated
  principal from `SecurityContextHolder` in a class living alongside the `CurrentAuditorProvider`
  interface it implements — would violate `LayerDependencyArchitectureTest`'s
  `GENERAL_INFRASTRUCTURE_MUST_NOT_DEPEND_ON_APPLICATION_OR_INTERFACES` rule (that class would
  need to import `auth.application.security.AuthenticatedUser`, and general `infrastructure.*`
  code is not allowed to depend on any module's `application` package). `security.context`
  already holds the exact same pattern (`CurrentUserService`, reading the identical
  `AuthenticatedUser` principal for a different port), is not part of the restricted
  `infrastructure.*` package tree, and introduces no new module dependency cycle (confirmed no
  existing `infrastructure → security` edge exists that would be reversed). `mvn clean verify`'s
  full ArchUnit suite (`PackageDependencyArchitectureTest`'s cycle check included) confirmed this
  placement is clean.
- **Real actor attribution over a blanket placeholder for auditing.** The original
  `LocalCurrentAuditorProviderConfig` returned one fixed UUID for every entity write, local or
  not. Since `JwtAuthenticationFilter` already places the real `AuthenticatedUser` in
  `SecurityContextHolder` for every authenticated request — pre-existing infrastructure, not
  something built for this sprint — using it in `CurrentAuditorProvider` costs nothing extra
  and is materially more correct for a financial savings-group platform's `created_by`/
  `updated_by` columns, while the configured system-actor placeholder still covers the small
  set of genuinely pre-authentication writes (signup, OTP request) where no real user exists
  yet. This is *not* the system's primary audit trail (that is `audit.audit_entries`, the
  dedicated business-event log built in earlier sprints, which already threads the real actor
  through explicit application-layer parameters independent of this class) — this only affects
  generic Spring Data JPA housekeeping columns.

## 5. Bugs Found

1. **[Critical, the sprint's target]** Backend does not start under `dev`/`prod` — root cause
   confirmed and fixed (see above).
2. **[Critical, discovered while implementing the fix]** A second, identically-shaped blocker:
   `CurrentAuditorProvider` (populates every entity's `created_by`/`updated_by`) also had exactly
   one implementation, also `@Profile("local")`-gated, with the same "no resolution strategy
   designed yet" rationale in its own javadoc. Left unfixed, the app would have started under
   `prod` (LS-1's exact failure) but crashed on the very first entity write with a `NOT NULL`
   constraint violation, since `JpaAuditingConfig`'s fallback (`Optional::empty()`) leaves
   `created_by`/`updated_by` null. Directly prevents production use even though it wouldn't have
   prevented context startup — fixed in the same pattern as finding 1, per the sprint's own
   instruction to fix additional blockers that "directly prevent production startup" (a write
   that always 500s on first use is a startup-adjacent blocker for any real deployment, not a
   separate feature).
3. **[High, self-introduced and self-caught]** The first implementation of finding 2's fix named
   its `@Bean` factory method `currentAuditorProvider` — identical to `JpaAuditingConfig`'s own
   `@ConditionalOnMissingBean` fallback method name. Both are plain `@Configuration` classes with
   no guaranteed processing order relative to each other; when packaged into the runnable jar
   (as opposed to running under Maven's test classpath, where the order happened to differ),
   Spring registered `JpaAuditingConfig`'s bean first, then failed hard with
   `BeanDefinitionOverrideException` when `SecurityBeansConfiguration` tried to register a
   second bean under the exact same name. Caught during this sprint's own prod-profile smoke
   test (`mvn clean verify`'s test-classpath ordering never triggered it) — renamed the method
   and added `@Primary`, matching the defensive pattern the original `LocalCurrentAuditorProviderConfig`
   already used and that the rewrite had dropped. Documented as a finding here rather than
   silently fixed, since it's exactly the kind of order-dependent fragility worth remembering.

## 6. Bugs Fixed

All three findings above are fixed and verified:

- Finding 1 & 2: verified via a live `SPRING_PROFILES_ACTIVE=prod` boot against a real
  PostgreSQL/Redis (see §10) — the application starts, and a full signup/verify/onboarding/logout
  flow completes with every entity's `tenant_id`/`created_by`/`updated_by` inspected directly in
  the database and confirmed correct.
- Finding 3: verified by rebuilding the packaged jar and re-running the exact same boot sequence
  — succeeded cleanly on the second attempt.

## 7. Security Review

- `ProductionEnvironmentGuard` now rejects `prod` startup if `TENANT_DEFAULT_ID` or
  `AUDIT_SYSTEM_ACTOR_ID` is blank *or* still the well-known local development placeholder value
  — an operator cannot accidentally ship the local seed tenant/actor id into a real deployment.
- `SecurityContextCurrentAuditorProvider` only ever reads the *already-authenticated* principal
  Spring Security's own filter chain placed in the context — it introduces no new
  authentication path, no new trust boundary, and cannot be spoofed by anything an unauthenticated
  caller controls.
- No JWTs, secrets, or OTPs were logged during manual verification — the established
  `psql`-set-known-bcrypt-hash technique was used for the OTP step, consistent with every prior
  sprint's practice in this project.
- No architecture, business rule, or existing security control was modified — confirmed by the
  file list in §3, which touches only DI wiring, configuration, and two small, additive classes.

## 8. Performance Review

No performance-relevant change. `TenantScopeProviderConfig` and
`SecurityContextCurrentAuditorProvider` are both trivial (a config-value lookup and a
`SecurityContextHolder` read respectively) — no new database queries, no new I/O, no change to
connection pooling, caching, or query patterns.

## 9. Documentation Review

`docs/deployment/environment-variables-guide.md` updated with both new required-in-prod
variables, `.env.prod.example` and `docker-compose.prod.yml` updated to match. No other
documentation needed changes — LS-1's report already flagged this exact gap by name and pointed
at this exact fix; no other document referenced the deleted classes.

## 10. Testing Summary

- **Automated:** `mvn clean verify` — **BUILD SUCCESS**, 1863 tests (8 new), 0 failures/errors,
  0 Checkstyle/PMD/SpotBugs violations, all JaCoCo coverage gates met, full ArchUnit suite
  (including the layering and module-cycle rules relevant to this sprint's new class placement)
  passed. `npm run lint` and `npm run build` both clean (frontend untouched this sprint; run to
  confirm no incidental regression).
- **Manual, live, against real infrastructure — the sprint's core deliverable:**
  1. Rebuilt the jar, booted it with `SPRING_PROFILES_ACTIVE=prod` against a real PostgreSQL
     (a fresh database, all 18 Flyway migrations applied cleanly) and a password-protected Redis
     instance. **`/actuator/health/liveness` and `/actuator/health/readiness` both returned
     `{"status":"UP"}`** — the application started successfully in 16.5 seconds, the first
     successful `prod`-profile boot in this project's history.
  2. `POST /api/v1/auth/signup` (pre-authentication, exercises the fixed tenant/auditor
     resolution) → `202 Accepted`. Verified in the database: the new `identity.users` row's
     `tenant_id` exactly matched the configured `TENANT_DEFAULT_ID`, and `created_by`/
     `updated_by` exactly matched the configured `AUDIT_SYSTEM_ACTOR_ID` (correct — no
     authenticated user exists yet at this point).
  3. `POST /api/v1/auth/signup/verify` with a known OTP (set via direct `psql` bcrypt hash,
     never logged) → `200 OK` with a real JWT, whose decoded payload carried the same
     `tenant_id` — confirming `SignupTenantResolverAdapter`'s login-completion path resolves the
     identical tenant.
  4. `POST /api/v1/users/me/onboarding` with that JWT (an authenticated write) → `200 OK`.
     Verified in the database: `updated_by` on the same `identity.users` row changed to the
     **user's own id** — confirming `SecurityContextCurrentAuditorProvider` correctly switches
     from the system-actor placeholder to the real signed-in user once one exists, while
     `created_by` (from the earlier pre-auth step) remained unchanged.
  5. `GET /api/v1/dashboard/member` (authenticated read) → `404 no-active-group` — the *correct*
     business response (the user hasn't joined a group), proving the read path resolves through
     JWT auth and tenant-scoped queries without any infrastructure failure.
  6. `GET /v3/api-docs` → `401` (Swagger correctly still blocked in `prod`, confirming Sprint
     LR-3's fix is undisturbed); `GET /actuator/metrics` → `200` (correctly still public).
  7. `POST /api/v1/auth/logout` → `204`, with the refresh token's `identity.refresh_tokens.status`
     confirmed `REVOKED` in the database.
- **Not performed, consistent with every prior sprint:** literal `docker build`/
  `docker compose up` execution — no Docker daemon is available in this environment. The
  `SPRING_PROFILES_ACTIVE=prod` boot above is the same honest substitute used in Sprint LS-1,
  now actually succeeding instead of failing.

## 11. Build Summary

`mvn clean verify`: **BUILD SUCCESS** (1863/1863 tests, 0 violations, all coverage gates met).
`npm run lint` / `npm run build`: clean, all 37 routes build. Both full logs retained for this
sprint's record.

## 12. Remaining Known Issues

- **No real multi-tenant *request* resolution exists.** This sprint deliberately did not build
  one (out of scope — see §4). If this platform ever needs to serve more than one real tenant
  from a single deployment, a genuine design (header, subdomain, or similar) is still required;
  today, every deployment serves exactly one tenant, configured once at startup.
- **No fail-fast validation exists for payment-gateway or storage-provider credentials** — noted
  in Sprint LS-1, unrelated to this sprint's scope, still true.
- **ElastiCache in-transit encryption still unsupported by the Redis client** — noted in Sprint
  LS-1, unrelated to this sprint's scope, still true.
- All other items from Sprint LS-1's "Remaining Known Issues" (§12 of that report) are
  unaffected by this sprint and remain as previously documented.
- **Session hygiene:** the throwaway `bachatsetu_smoketest` database/role (port 5433) used for
  this sprint's live verification, and this sprint's own smoke-test backend process, were both
  cleaned up before this report was written (Redis auth reset to its original unauthenticated
  state; the throwaway database can be dropped whenever convenient — it is not part of the
  deployed system).

## 13. Deployment Readiness Assessment

**Is the application now deployable to `dev`/`prod`?**

**Yes, for the specific blocker this sprint targeted.** A genuine `SPRING_PROFILES_ACTIVE=prod`
boot against real PostgreSQL and Redis now succeeds, and a full signup → verify → authenticated
write → read → logout cycle was exercised end-to-end with every tenant/auditor assignment
verified directly in the database. `docker-compose.prod.yml` now correctly requires and passes
through the two new environment variables this fix introduces, with `ProductionEnvironmentGuard`
guarding against the same placeholder-value mistake that would have silently reintroduced the
`local`-only behavior in disguise.

This does not, on its own, mean every finding from Sprint LS-1's infrastructure validation or
every "Remaining Known Issue" carried forward is resolved — those were explicitly out of this
sprint's narrow mandate and remain exactly as documented in §12. But the one finding that made
LS-1 conclude "not ready for a Closed Beta launch" — the backend's inability to start outside
`local` — is fixed and independently re-verified in this sprint, live, against real
infrastructure, not just by inspection.

Per the sprint's explicit closing instruction, this concludes Sprint LS-2. **Stopping here** — no
Sprint LS-3, no payment gateway, no push notifications, no other roadmap feature has been
started.
