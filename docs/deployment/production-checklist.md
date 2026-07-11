# Production Checklist

> **Audience:** DevOps Engineers, Product Managers, Engineering Leadership
> **Prerequisite reading:** every other chapter in this folder

A checklist to run through before pointing real users at a `docker-compose.prod.yml`
deployment. Items are grouped by what this sprint (PI-1) actually delivers versus what remains
— consolidating, for the production-infrastructure slice specifically, the same honest gap
tracking [roadmap-and-future-work.md](../product/roadmap-and-future-work.md) uses for the
product as a whole.

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
- [ ] **No payment gateway, email provider, or SMS provider is configured** — this is
      unrelated to infrastructure readiness (those integrations are business-feature work, out
      of scope for PI-1 by explicit instruction) but is worth restating here: infrastructure
      being production-ready does not mean the product is feature-complete for a real payment
      flow.
- [ ] **Legal/compliance review** of what's actually being stored (PII in Postgres, receipts
      and photos in S3/local storage) has not happened — see
      [security-and-compliance.md §9](../product/security-and-compliance.md#9-compliance-posture).

Whether each unchecked item blocks a specific launch is a product/business decision, not an
engineering one — this checklist surfaces the gap; it does not adjudicate it.
