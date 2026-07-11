# Recovery Guide

> **Audience:** DevOps Engineers, on-call responders
> **Prerequisite reading:** [runbook.md](runbook.md)

Failure-scenario procedures for a `docker-compose.prod.yml` deployment. Each section states
what's actually true about the current setup (what recovers automatically, what doesn't) —
several items describe manual procedures that would benefit from automation this sprint does
not add (see the closing note).

## Container fails its healthcheck

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs --tail 200 <service>
```

- **`postgres`/`redis` unhealthy**: check the container actually started (`docker compose logs`
  for a crash on startup) versus is merely slow (cold volume, first-run initialization) —
  the `start_period` in each healthcheck already allows for the latter.
- **`backend` unhealthy**: the most common cause is a failed Flyway migration or an unreachable
  database/Redis at startup — check the log for a `FlywayException` or a connection-refused
  stack trace near the top of the log (Spring Boot fails fast on startup errors, so the
  relevant error is usually within the first ~50 lines). See
  [Failed database migration](#failed-database-migration) below if it's a Flyway error
  specifically.
- **`frontend` unhealthy**: check it can reach `NEXT_PUBLIC_API_BASE_URL` from a browser's
  perspective (not from inside the container — that variable is baked into the client bundle,
  not read at runtime) and that the image was built with the correct value (§5 of
  [environment-variables-guide.md](environment-variables-guide.md)).
- **`nginx` unhealthy or returning 502**: almost always means `backend` or `frontend` isn't
  actually healthy yet — `depends_on: condition: service_healthy` should prevent Nginx from
  starting before they are, but a service that becomes unhealthy *after* startup (e.g. the
  backend loses its database connection) will still show as a 502 from Nginx even though
  Nginx itself is fine. Check the upstream service first.

## Failed database migration

Flyway is forward-only by design (`spring.flyway.clean-disabled: true`) — there is no
automatic rollback. If a migration in `services/backend/src/main/resources/db/migration/`
fails partway through against production data:

1. The backend will not start (Flyway blocks application startup on a failed migration) — the
   previous container version, if `docker compose up` is re-run without the new image, will
   also refuse to start once Flyway's schema history table records the failed attempt,
   because Flyway checksums are validated on every startup.
2. Manually inspect `flyway_schema_history` in the database to see exactly which migration
   failed and at what statement.
3. Fix the underlying data issue the migration didn't anticipate, or write a new forward
   migration that corrects the partial change the failed one left behind — do not hand-edit
   `flyway_schema_history` to mark the failed migration as successful unless you have manually
   verified the schema now matches what that migration was supposed to produce.
4. This is why [runbook.md §Database migrations](runbook.md#database-migrations) says to test
   migrations against production-shaped data before deploying them — this recovery path is
   slow and manual by nature; the goal is to avoid needing it.

## Rolling back a bad deployment

- **No schema change involved**: `git checkout <previous-tag>` and redeploy per
  [production-deployment-guide.md §7](production-deployment-guide.md#7-rolling-back) — clean
  and immediate.
- **Schema change involved**: the previous application version's code does not know about the
  new columns/tables, but a purely additive migration (new nullable column, new table) is
  typically harmless to leave in place — the old code simply doesn't use it. Only write a
  compensating "down" migration if the change was genuinely destructive (renamed/dropped a
  column the old code needs) — in that case, roll forward with a new migration that restores
  what's needed, rather than trying to make Flyway run backwards.

## Database data loss / corruption

No automated backup-and-restore drill exists yet (see
[production-checklist.md](production-checklist.md)) — this is the manual procedure until one
is built:

1. RDS: restore from the most recent automated snapshot or a point-in-time recovery target,
   into a **new** RDS instance (never restore over the live one) — see
   [infrastructure-guide.md §3](infrastructure-guide.md#3-rds-postgresql).
2. Point `DATABASE_URL` at the restored instance, verify data with a read-only query pass
   before cutting traffic over.
3. Any writes that happened between the snapshot and the incident are lost — the acceptable
   window (RPO) has not been formally decided; until it is, treat every snapshot's age as the
   worst-case data-loss window.
4. Self-managed Postgres (the `postgres` container in `docker-compose.prod.yml`, if RDS isn't
   in use): the `postgres-prod-data` named volume is the only copy of the data — it is not
   backed up by anything in this repository. Set up `pg_dump` on a schedule, shipped off-host,
   before relying on this path for anything real.

## Redis data loss

Redis in this deployment backs only the generic cache infrastructure added in this sprint
(`in.bachatsetu.backend.infrastructure.cache.CacheConfiguration`) — no business module reads
or writes through it yet (see
[non-functional-and-production-readiness.md](../product/non-functional-and-production-readiness.md)).
Losing all Redis data today has **no business-data impact** — restart the `redis` container
(or ElastiCache node) and the application continues correctly, just without whatever was
cached. This changes the moment a business module starts depending on Redis for anything
beyond performance (e.g. if a future rate limiter relies on it for correctness, not just
speed) — re-evaluate this section then.

## Container / host crash

`restart: unless-stopped` on every service in `docker-compose.prod.yml` means Docker restarts
a crashed container automatically, and restarts the whole stack on host reboot (as long as the
Docker daemon itself is set to start on boot, which is the default on most Linux
distributions' Docker packages). Named volumes (`postgres-prod-data`, `redis-prod-data`)
survive a container restart or recreation — they do not survive the *host* being terminated
(e.g. an EC2 instance replaced rather than rebooted), which is exactly why RDS/ElastiCache are
recommended over the Dockerized Postgres/Redis for anything beyond a first
deployment (§1 of [infrastructure-guide.md](infrastructure-guide.md)).

## Closing note

Every manual procedure in this chapter is a candidate for automation once there's a team and
traffic to justify it — see
[roadmap-and-future-work.md](../product/roadmap-and-future-work.md) for "Runbooks" and
"Database backup/restore drills," both listed there as still-open Platform and Infrastructure
work. This chapter documents the procedure that exists today, not the automation that should
eventually replace parts of it.
