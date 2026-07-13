# Production Checklist

> **Audience:** DevOps Engineers, Product Managers, Engineering Leadership
> **Prerequisite reading:** every other chapter in this folder

A checklist to run through before pointing real users at a `docker-compose.prod.yml`
deployment. Items are grouped by what this sprint (PI-1) actually delivers versus what remains
— consolidating, for the production-infrastructure slice specifically, the same honest gap
tracking [roadmap-and-future-work.md](../product/roadmap-and-future-work.md) uses for the
product as a whole.

## LAUNCH BLOCKER (found in Sprint LS-1 production-mode smoke test)

- [ ] **The backend does not start under any Spring profile except `local`.** Confirmed by
      booting `bachatsetu-backend-*.jar` with `SPRING_PROFILES_ACTIVE=prod` against a real
      Postgres/Redis: Flyway applies all 18 migrations successfully, Hikari/Redis connect fine,
      then context startup fails with `No qualifying bean of type
      'in.bachatsetu.backend.auth.domain.port.UserRepository'`. Root cause:
      `TenantScopeProvider` — required by the pre-login persistence adapters
      (`AuthUserRepositoryAdapter`, `UserRepositoryAdapter`, `RoleRepositoryAdapter`,
      `GroupInvitationRepositoryAdapter`, `AuthProfileProvisioningAdapter`,
      `SignupTenantResolverAdapter`, and the audit module's "current auditor" resolution) — has
      exactly one implementation anywhere in the codebase
      (`LocalTenantScopeProviderConfig`), and it is `@Profile("local")`-gated by deliberate,
      documented design: "no real multi-tenant resolution strategy (header, subdomain, or
      similar) has been designed yet... must never be active outside the local profile." This
      means `docker-compose.prod.yml`'s `backend` service, exactly as configured today, would
      crash-loop indefinitely in a real deployment — signup, login, OTP, and every
      tenant-scoped read path are unusable outside `SPRING_PROFILES_ACTIVE=local`. **This is
      not an infrastructure/deployment problem and is out of scope for this sprint to fix** (it
      requires designing a real multi-tenant resolution strategy — business/architecture work
      explicitly excluded from LS-1's "production deployment work only" mandate) — but it must
      be resolved, and re-verified with the same smoke test, before any `dev`/`prod` deployment
      is attempted.

## Delivered in PI-1 — verify before go-live

- [ ] `.env` populated from `.env.prod.example` with real, non-default values for every
      required variable — see [environment-variables-guide.md](environment-variables-guide.md).
- [ ] `AUTH_JWT_SIGNING_SECRET` generated fresh for this environment (`openssl rand -base64 64`),
      not reused from `.env.prod.example`, local development, or another environment.
- [ ] `AUTH_CORS_ALLOWED_ORIGINS` set to the real production frontend origin(s), not
      `http://localhost:3000`.
- [ ] `docker compose -f docker-compose.prod.yml up -d --build` completes with every service
      reporting `healthy` — see [production-deployment-guide.md §4](production-deployment-guide.md#4-verify-the-deployment).
- [ ] TLS is terminated in front of the stack (ALB + ACM, or a self-managed alternative) — see
      [production-deployment-guide.md §5](production-deployment-guide.md#5-put-it-behind-tls).
      Do not launch with plain HTTP as the public-facing protocol.
- [ ] `NEXT_PUBLIC_API_BASE_URL` is the final public HTTPS backend URL, and the frontend image
      was built (not just run) with that value — see
      [environment-variables-guide.md §5](environment-variables-guide.md#5-frontend).
- [ ] Database and Redis credentials rotated away from any value that ever appeared in a
      Compose file, `.env.prod.example`, or a chat/ticket during setup.
- [ ] `/actuator/health/liveness` and `/actuator/health/readiness` return `200` from outside
      the host (through the load balancer, not just `curl localhost`).
- [ ] Structured JSON logs are actually being collected somewhere durable (CloudWatch Logs,
      or an equivalent log driver on the container runtime) — `docker compose logs` alone is
      not a retention strategy.
- [ ] Someone has read [runbook.md](runbook.md) and [recovery-guide.md](recovery-guide.md)
      before the first real incident, not during it.
- [ ] If `STORAGE_DEFAULT_PROVIDER=LOCAL` (the default), confirm a real file upload survives
      `docker compose up -d --build` (i.e. the `backend-prod-storage` volume is actually
      mounted, not a fresh anonymous layer) — see
      [environment-variables-guide.md §3](environment-variables-guide.md#3-backend--optional-with-a-safe-default).
      `LOCAL` does not survive scaling the backend beyond one instance; provision `AWS_S3`
      before doing so.
- [ ] If `PAYMENT_GATEWAY_ENABLED=true`, confirm `PAYMENT_GATEWAY_DEFAULT_PROVIDER` is set to the
      provider whose credentials were actually filled in — there is no fail-fast check catching
      a mismatch here, unlike SMS/Email. Leave it `false` (the default) for an MVP with no
      payment gateway yet.
- [ ] `SMS_PROVIDER_ENABLED`/`EMAIL_PROVIDER_ENABLED` reflect an intentional choice: `false`
      (the default) means OTPs/emails are logged, not sent — correct for an MVP with no real
      provider yet, but confirm this is actually the intended launch state and not an oversight
      before pointing real users at signup/login.

## Explicitly not delivered by PI-1 — decide before go-live whether each is a blocker

These are carried over from
[non-functional-and-production-readiness.md §3](../product/non-functional-and-production-readiness.md#3-what-production-ready-still-requires)
and [roadmap-and-future-work.md §3](../product/roadmap-and-future-work.md#3-consolidated-future-work-by-theme),
restated here in the context of *this specific infrastructure*:

- [ ] **No CI/CD pipeline** — every deployment in [production-deployment-guide.md](production-deployment-guide.md)
      is a manual `git pull && docker compose up -d --build` on the host. Decide whether a
      manual process is acceptable for the first launch, or whether GitHub Actions automation
      is a prerequisite.
- [ ] **No automated database backup verification / restore drill** — RDS automated backups
      exist once provisioned (§3 of [infrastructure-guide.md](infrastructure-guide.md)), but
      nobody has practiced restoring from one against this schema. See
      [recovery-guide.md](recovery-guide.md).
- [ ] **No secrets manager integration** — `.env` on the host is the only mechanism. Anyone
      with host access can read every secret in plaintext.
- [ ] **No external error-tracking or metrics dashboard wired up** — Prometheus-format metrics
      are exposed (§5 of [infrastructure-guide.md](infrastructure-guide.md)) but nothing
      scrapes or visualizes them yet. The frontend's `src/lib/logger.ts` seam is ready for a
      provider (Sentry/Datadog) but none is connected.
- [ ] **No load testing has been performed** against this Docker/Compose shape specifically.
      `DATABASE_MAX_POOL_SIZE`/`DATABASE_MIN_IDLE` defaults (30/5 under `prod`) are reasonable
      starting points, not validated against real traffic.
- [ ] **No penetration test, SAST, or dependency/container vulnerability scan** has been run
      against these images — see
      [security-and-compliance.md §9](../product/security-and-compliance.md#9-compliance-posture).
- [ ] **No payment gateway, real SMS, or real email provider is configured — by design, for the
      current MVP.** `PAYMENT_GATEWAY_ENABLED`, `SMS_PROVIDER_ENABLED`, and
      `EMAIL_PROVIDER_ENABLED` all default to `false`: OTPs and emails are logged instead of
      sent, and the payment-gateway endpoints are disabled entirely. This is a genuinely
      supported, fully-functional deployment mode, not a stopgap — see
      [docs/integrations/sms-provider.md](../integrations/sms-provider.md) and
      [docs/integrations/email-provider.md](../integrations/email-provider.md) for the real
      provider integrations (MSG91/Fast2SMS/Twilio; AWS SES/Resend/SendGrid), which activate
      with each flag plus that provider's credentials — no code change needed. Before inviting
      real users who cannot read application logs for their OTP, decide whether this MVP launch
      needs `SMS_PROVIDER_ENABLED=true` at minimum, since users otherwise cannot complete
      signup/login on their own without operator help.
- [ ] **Legal/compliance review** of what's actually being stored (PII in Postgres, receipts
      and photos in S3/local storage) has not happened — see
      [security-and-compliance.md §9](../product/security-and-compliance.md#9-compliance-posture).
- [ ] **ElastiCache Redis in-transit encryption (TLS) is not supported by the application yet**
      — the backend's Redis client has no `rediss://`/SSL configuration. Leave in-transit
      encryption off for this beta (acceptable — Redis backs no business data today, see
      [infrastructure-guide.md §4](infrastructure-guide.md#4-elasticache-redis)) or add Lettuce
      SSL support before turning it on at the ElastiCache layer.

Whether each unchecked item blocks a specific launch is a product/business decision, not an
engineering one — this checklist surfaces the gap; it does not adjudicate it.
