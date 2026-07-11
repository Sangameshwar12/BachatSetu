# Runbook

> **Audience:** DevOps Engineers, on-call responders
> **Prerequisite reading:** [production-deployment-guide.md](production-deployment-guide.md)

Day-2 operational procedures for a `docker-compose.prod.yml` deployment. This is not an
incident-response contact list (none exists yet — see
[roadmap-and-future-work.md](../product/roadmap-and-future-work.md)); it is the set of
commands and checks an operator runs.

## Checking service status

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f frontend
docker compose -f docker-compose.prod.yml logs -f nginx
```

Backend and frontend logs are single-line JSON under the `dev`/`prod` profiles (see
`services/backend/src/main/resources/logback-spring.xml`) — pipe through `jq` for readability:

```bash
docker compose -f docker-compose.prod.yml logs backend | jq -r '.message'
```

Every log line carries `requestId` and `correlationId` fields
(`in.bachatsetu.backend.observability.CorrelationIdFilter`) — to trace one request across
backend log lines and the `X-Request-Id` response header a client received:

```bash
docker compose -f docker-compose.prod.yml logs backend | jq -c 'select(.requestId == "<id-from-response-header>")'
```

## Restarting a service

```bash
docker compose -f docker-compose.prod.yml restart backend
```

`restart: unless-stopped` (set on every service in `docker-compose.prod.yml`) means Docker
also restarts a crashed container automatically without operator action; this command is for
a deliberate restart (e.g. after a configuration change to `.env` — restart is required for
new environment variables to take effect, since they're read once at container start).

## Scaling

Single-host, single-instance-per-service today (see
[infrastructure-guide.md §1](infrastructure-guide.md#1-target-architecture)) — there is no
`docker compose ... scale` story until the backend/frontend run behind a load balancer with
more than one host, which is Future Work. The immediate lever is vertical: adjust the
`deploy.resources.limits.memory` values in `docker-compose.prod.yml` and the backend's
`JAVA_OPTS`/`-XX:MaxRAMPercentage` (already proportional to the container's memory limit, so
raising the memory limit alone increases usable heap without any other change).

## Rotating a secret

1. Update the value in `.env` (or wherever the secret is sourced from in a non-Compose
   deployment).
2. `docker compose -f docker-compose.prod.yml up -d` — Compose recreates only the containers
   whose configuration changed.
3. For `AUTH_JWT_SIGNING_SECRET` specifically: rotating it invalidates every access and
   refresh token issued under the old secret — every signed-in user is signed out. There is no
   dual-secret grace-period mechanism (`bachatsetu.authentication.token.jwt-version` exists in
   the token claims but nothing in the codebase currently checks multiple valid secrets at
   once) — plan rotation for a low-traffic window.
4. For `DATABASE_PASSWORD`/`REDIS_PASSWORD`: change the credential at the database/cache
   provider first (RDS/ElastiCache console, or `ALTER USER` for a self-managed Postgres),
   *then* update `.env` and restart — reversing the order causes a connection outage between
   the credential change and the restart.

## Database migrations

Flyway migrations (`services/backend/src/main/resources/db/migration/V*.sql`) run
automatically on backend startup — `spring.flyway.enabled: true` everywhere, and
`spring.flyway.clean-disabled: true` everywhere prevents `flyway:clean` from ever running
through this application's own configuration. Before deploying a change that adds a new
migration:

- Confirm the migration is additive (new table/column/index) rather than destructive
  (dropping/renaming a column a running previous version still reads) — during the brief
  window in a deployment where the old backend version may still be handling in-flight
  requests against the new schema (§6 of
  [production-deployment-guide.md](production-deployment-guide.md) has no blue/green
  mechanism), a destructive migration can break the outgoing version's queries.
- Confirm it was tested against a copy of production-shaped data, not just the seed dataset
  (`SPRING_PROFILES_ACTIVE=local,seed`, `services/backend/README.md`) — Flyway checksums the
  migration on first apply; a migration that fails partway through on a data shape the seed
  data didn't cover requires the manual recovery procedure in
  [recovery-guide.md](recovery-guide.md#failed-database-migration).

## Checking current configuration without a restart

```bash
curl -s http://localhost/actuator/info | jq
```

Returns the running application name and version
(`in.bachatsetu.backend.observability` correlation headers apply to this request too). Deeper
configuration values are intentionally not exposed here — `management.endpoint.health.show-details: never`
and no `/actuator/env` exposure under `prod` (`MANAGEMENT_ENDPOINTS_EXPOSURE` defaults to
`health,info,metrics,prometheus`, not `env` or `configprops`) — see
[environment-variables-guide.md](environment-variables-guide.md).

## Maintenance mode

The platform-configuration module (Sprint 13.3) already supports a maintenance-mode flag,
enforced by `in.bachatsetu.backend.security.filter.MaintenanceModeFilter` — this is existing
functionality, not new in PI-1, but is the correct tool for a planned-maintenance window (e.g.
a database migration expected to take more than a few seconds). Set it through the Platform
Admin Portal's Configuration screen (`/dashboard/admin/configuration`) or the underlying
`PUT /api/v1/admin/config` endpoint (`maintenanceEnabled`/`maintenanceMessage`/
`maintenanceStartAt`/`maintenanceEndAt` fields on the full-replace
`UpdateConfigurationRequest`) — see
[backend-module-and-api-reference.md](../product/backend-module-and-api-reference.md) for the
full contract. Requests to `/actuator/health*`, `/api/v1/auth/**`, and everything under
`/api/v1/admin/**` are always allowed through, and a caller already authenticated as
`PLATFORM_ADMIN` bypasses maintenance mode entirely, so an operator is never locked out of
their own tools by turning it on.
