# Production Deployment Guide

> **Audience:** DevOps Engineers, Developers
> **Prerequisite reading:** [docker-guide.md](docker-guide.md), [environment-variables-guide.md](environment-variables-guide.md), [infrastructure-guide.md](infrastructure-guide.md)

This is the step-by-step procedure for standing up BachatSetu on a single host (an EC2
instance, per [infrastructure-guide.md](infrastructure-guide.md), or any machine with Docker
installed) using `docker-compose.prod.yml`. Read
[production-checklist.md](production-checklist.md) before your first real deployment — this
guide covers *how* to run the stack, the checklist covers *whether it's safe to*.

## 1. Prerequisites on the host

- Docker Engine and the Docker Compose plugin installed (`docker compose version` succeeds).
- Network access from this host to wherever `DATABASE_URL` and `REDIS_HOST` point — either
  the co-located Postgres/Redis containers this same Compose file defines, or external
  RDS/ElastiCache endpoints per [infrastructure-guide.md](infrastructure-guide.md).
- Port 80 free for the `nginx` service (or 443, once TLS termination is configured at the
  load balancer in front of this host — see §5).
- This repository checked out, or at minimum `docker-compose.prod.yml`,
  `deploy/nginx/nginx.conf`, `services/backend/`, and `services/web/` present in the same
  relative layout (the Compose file's `build.context` paths are relative).

## 2. Configure secrets

```bash
cp .env.prod.example .env
```

Fill in every value in `.env` — see
[environment-variables-guide.md](environment-variables-guide.md) for what each one means.
Every secret-bearing variable in `docker-compose.prod.yml` uses the `${VAR:?message}` syntax,
so Compose refuses to start any service whose required variable is missing, with a message
naming the variable — there is no way to accidentally launch this stack with a blank
`AUTH_JWT_SIGNING_SECRET`, `DATABASE_PASSWORD`, or `REDIS_PASSWORD`. Independently of Compose,
`in.bachatsetu.backend.configuration.production.ProductionEnvironmentGuard` performs the same
check (plus rejecting known development placeholder values) inside the JVM under the `prod`
Spring profile, so the same class of misconfiguration is caught even outside Compose (e.g. if
the backend image is run directly on ECS with a task-definition environment block instead).

`.env` must never be committed — it already matches the repository's `.gitignore` pattern
(`.env*` excluded, `.env.prod.example` explicitly re-included as the template).

## 3. Build and start the stack

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

This builds the backend and frontend images from their Dockerfiles (see
[docker-guide.md](docker-guide.md)), starts Postgres, Redis, the backend, the frontend, and
the Nginx edge, in dependency order (`depends_on: condition: service_healthy` — each service's
`HEALTHCHECK` gates the next).

## 4. Verify the deployment

```bash
# Every container should show "healthy", not "starting" (allow the configured start_period
# — 90s for the backend, since it also runs Flyway migrations on startup) or "unhealthy".
docker compose -f docker-compose.prod.yml ps

# From the host, or from wherever the load balancer will reach this instance:
curl -f http://localhost/actuator/health/liveness
curl -f http://localhost/actuator/health/readiness
curl -f http://localhost/
```

If any container reports `unhealthy`, see
[recovery-guide.md](recovery-guide.md#container-fails-its-healthcheck) before proceeding.

## 5. Put it behind TLS

`nginx.conf` listens on plain HTTP port 80 by design — TLS termination is expected to happen
in front of this host:

- **AWS Application Load Balancer** (the path documented in
  [infrastructure-guide.md](infrastructure-guide.md)) with an ACM-issued certificate,
  forwarding plain HTTP to this instance's port 80. This is the recommended approach: no
  certificate files to manage on the host, automatic renewal.
- **Self-managed** (e.g. a single EC2 instance with no load balancer): terminate TLS with
  Certbot + an additional `server { listen 443 ssl; }` block added to `nginx.conf`, or run a
  separate TLS-terminating proxy in front of the `nginx` container. Not configured by this
  sprint — the certificate paths would be environment/host-specific and are not fabricated
  here.

Either way, set `AUTH_CORS_ALLOWED_ORIGINS` and `NEXT_PUBLIC_API_BASE_URL` to the final public
HTTPS URLs, not the plain-HTTP internal ones, before the first real deployment.

## 6. Deploying an update

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

Compose rebuilds only the images whose Dockerfile or build context changed, and replaces
containers one at a time as their images become ready; `depends_on: condition: service_healthy`
still gates startup order. There is no blue/green or rolling-update mechanism at this stage
(that requires more than one host, which is out of scope per
[infrastructure-guide.md §1](infrastructure-guide.md#1-target-architecture)) — expect a brief
window of unavailability while the backend and frontend containers restart. Flyway migrations
run automatically as part of backend startup; see
[runbook.md](runbook.md#database-migrations) for what to check before deploying a change that
includes one.

## 7. Rolling back

```bash
git checkout <previous-known-good-commit-or-tag>
docker compose -f docker-compose.prod.yml up -d --build
```

This rebuilds and redeploys the previous version's images. It does **not** roll back a
database migration that already ran — Flyway migrations in this codebase are forward-only
(`spring.flyway.clean-disabled: true` everywhere prevents accidental resets); see
[recovery-guide.md](recovery-guide.md#rolling-back-a-bad-deployment) for what to do if the bad
deployment included a schema change that needs to be undone.
