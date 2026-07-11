# BachatSetu

BachatSetu is India's Community Savings Platform. The first supported product module is Bhishi, also known as ROSCA or Committee groups. The platform is designed to grow into a broader community collections and savings SaaS for self-help groups, apartment collections, society maintenance, temple collections, festival collections, office contributions, travel saving groups, NGO collections, and community funds.

This repository defines the production foundation, engineering standards, architecture, delivery workflow, and project roadmap, alongside a working backend implementation (`services/backend`) covering OTP authentication, savings groups, members, payments, draws, auctions, receipts, and notifications.

## Current Repository Contents

```text
BachatSetu/
  README.md
  docker-compose.dev.yml     # Full stack (Postgres, Redis, backend, frontend) for local Docker development
  docker-compose.prod.yml    # Full stack, production images, behind an Nginx edge — see docs/deployment/
  .env.prod.example          # Template for docker-compose.prod.yml's required secrets
  deploy/
    nginx/
      nginx.conf             # Edge reverse proxy used by docker-compose.prod.yml
  docs/
    README.md
    api/
    architecture/
    database/
    deployment/               # Docker, environment, AWS infrastructure, runbook, recovery
    operations/
    product/
    roadmap/
    standards/
    tooling/
    workflow/
  services/
    backend/   # Spring Boot backend (see "Backend Development" below)
    web/       # Next.js frontend (see services/web/README.md)
```

## Intended Product Architecture

- Backend: Java 21, Spring Boot 3, PostgreSQL, Redis, Flyway, Spring Security, JWT, Maven
- Mobile: Flutter
- Admin Portal: React, TypeScript
- Cloud: AWS
- CI/CD: GitHub Actions

## Backend Development

The backend lives in [`services/backend`](services/backend). All commands below are run from that directory.

### Prerequisites

| Tool | Version |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9+ |
| PostgreSQL | 16.x (17.x also verified working) |
| Redis | 7.x (any Redis-protocol-compatible server, e.g. Memurai on Windows, works too) |

The quickest way to get PostgreSQL and Redis running locally is the bundled Compose file:

```bash
cd services/backend
docker compose up -d postgres redis
```

This starts Postgres on `localhost:5432` (db/user/password all `bachatsetu`) and Redis on `localhost:6379`, matching the application's default configuration — no environment variables are required if you use it as-is. If you already run PostgreSQL/Redis natively (not via Docker), just export the environment variables below to point at them instead.

### Configuration

The app is configured via environment variables layered on top of `src/main/resources/application.yml`. Defaults target the Docker Compose services above:

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/bachatsetu` | JDBC connection string |
| `DATABASE_USERNAME` | `bachatsetu` | Database user |
| `DATABASE_PASSWORD` | `bachatsetu` | Database password |
| `DATABASE_MAX_POOL_SIZE` | `10` | HikariCP max pool size |
| `DATABASE_MIN_IDLE` | `2` | HikariCP min idle connections |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `SERVER_PORT` | `8080` | HTTP port the app listens on |

Spring profiles (`SPRING_PROFILES_ACTIVE`):

- `local` (default when no profile is set) — smaller connection pool, verbose actuator output, `DEBUG` application logging. See `application-local.yml`.
- `dev` — requires explicit `DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`/`REDIS_HOST` (no defaults).
- `prod` — same as `dev` plus a larger default connection pool and minimal actuator detail.
- `seed` — additive; combine with `local` (e.g. `SPRING_PROFILES_ACTIVE=local,seed`) to load sample development data (see "Seed Data" below). Do not use in `dev`/`prod`.

### Running the App

```bash
cd services/backend
mvn spring-boot:run
```

Or build and run the jar directly:

```bash
mvn clean package
java -jar target/bachatsetu-backend-0.1.0-SNAPSHOT.jar
```

On startup the app automatically runs any pending Flyway migrations against the configured database, then starts Tomcat on `SERVER_PORT` (default `8080`).

### Flyway Migrations

Migrations live in `src/main/resources/db/migration` (`V1` through `V6`) and run automatically on every application startup (`spring.flyway.enabled: true`). There is nothing to run manually:

- **Fresh database**: all migrations apply in order and Hibernate's `ddl-auto: validate` check passes against the resulting schema.
- **Existing, already-migrated database**: Flyway validates the applied migration checksums and history, finds nothing pending, and the app starts normally.

`spring.flyway.clean-disabled: true` is set everywhere, so `flyway:clean` cannot accidentally wipe a database through this project's own configuration.

### Seed Data

An opt-in Flyway location at `src/main/resources/db/seed` (`V900__local_development_seed_data.sql`) inserts a small, self-consistent sample dataset: one organizer and two members, a savings group, group memberships, a monthly cycle, a payment, a receipt, and a completed draw — all under the fixed local placeholder tenant `00000000-0000-0000-0000-000000000000`.

This location is **not** included by default. Activate it by adding the `seed` profile:

```bash
SPRING_PROFILES_ACTIVE=local,seed mvn spring-boot:run
```

It is never combined into `dev`/`prod`, and it never runs as part of the default `local` profile, so it cannot affect existing tests or non-local environments.

### Swagger / OpenAPI

Once the app is running:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Raw OpenAPI document: `http://localhost:8080/v3/api-docs`

Every completed REST module registers its endpoints here: OTP Authentication (`/api/v1/auth/otp/*`), Savings Groups (`/api/v1/groups/**`), Members (`/api/v1/members/**`), Payments (`/api/v1/payments/**`), Draws (`/api/v1/draws/**`), Receipts (`/api/v1/receipts/**`), Notifications (`/api/v1/notifications/**`), and Auctions (`/api/v1/auctions/**`).

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

Returns `{"status":"UP", ...}` once the database and Redis connections are healthy. Under the `local` profile, `management.endpoints.web.exposure.include` is set to `*` for easier troubleshooting (all actuator endpoints, not just `health`/`info`).

### Build Verification

```bash
mvn clean verify
```

Runs the full test suite plus Checkstyle, PMD, SpotBugs, ArchUnit, and JaCoCo coverage gates. Some persistence integration tests use Testcontainers and are automatically skipped (`disabledWithoutDocker = true`) if Docker isn't available locally — this is expected and does not indicate a failure.

### Troubleshooting

- **`NumberFormatException` on startup mentioning a Hikari property**: HikariCP's own timeout properties (`spring.datasource.hikari.connection-timeout`, `validation-timeout`, `idle-timeout`, `max-lifetime`) take plain milliseconds, not Spring's `30s`/`10m` duration shorthand. If you override these, use integer milliseconds.
- **`password authentication failed` connecting to Postgres**: confirm `DATABASE_USERNAME`/`DATABASE_PASSWORD` match the target database, and that nothing else is already listening on the port you think Postgres owns (e.g. a second local Postgres install on `5432` vs `5433`).
- **Auth endpoints return "no qualifying bean" style errors, or user lookups fail**: the OTP authentication adapters are tenant-scoped, but no request carries a tenant identifier yet and no multi-tenant resolution strategy has been designed. A fixed placeholder tenant (`in.bachatsetu.backend.infrastructure.persistence.adapter.LocalTenantScopeProviderConfig`) is wired **only under the `local` profile** to unblock local development; it intentionally does not activate under `dev`/`prod`. Real tenant resolution (header, subdomain, etc.) and a user-provisioning/signup flow are not yet implemented — see "Remaining Known Limitations" in the Sprint 11.6 report.
- **App won't start / a module's beans seem to silently disappear**: application configuration classes in this codebase are gated with `@ConditionalOnPersistenceRepositories` (backed by `bachatsetu.persistence.repositories.enabled`, default `true`) rather than `@ConditionalOnBean` checks against sibling configuration classes — the latter has no guaranteed evaluation order for regular (non-auto-configuration) `@Configuration` classes and can skip a bean even when its dependencies are present. If you add a new module, follow the same pattern rather than reintroducing cross-class `@ConditionalOnBean`.

## Documentation Index

- [Documentation Home](docs/README.md)
- [**Product Documentation (start here for what's actually built)**](docs/product/README.md)
- [**Deployment Documentation (Docker, environment, AWS, runbook)**](docs/deployment/README.md)
- [SMS Provider Integration (MSG91 / Fast2SMS / Twilio)](docs/integrations/sms-provider.md)
- [Repository Structure](docs/architecture/repository-structure.md)
- [Documentation Structure](docs/architecture/documentation-structure.md)
- [System Architecture](docs/architecture/system-architecture.md)
- [API Standards](docs/architecture/api-standards.md)
- [REST API Contract](docs/api/rest-api-contract.md)
- [Database Standards](docs/architecture/database-standards.md)
- [PostgreSQL Database Architecture](docs/database/postgresql-database-architecture.md)
- [Security Standards](docs/architecture/security-standards.md)
- [Business Domain Design](docs/product/business-domain-design.md)
- [Coding Standards](docs/standards/coding-standards.md)
- [Naming Conventions](docs/standards/naming-conventions.md)
- [Folder Naming Conventions](docs/standards/folder-naming-conventions.md)
- [Logging Standards](docs/standards/logging-standards.md)
- [Exception Handling Standards](docs/standards/exception-handling.md)
- [Git Commit Conventions](docs/standards/git-commit-conventions.md)
- [Git Branching Strategy](docs/workflow/git-branching-strategy.md)
- [Development Workflow](docs/workflow/development-workflow.md)
- [CI/CD Strategy](docs/workflow/ci-cd-strategy.md)
- [Testing Strategy](docs/workflow/testing-strategy.md)
- [Development Roadmap](docs/roadmap/development-roadmap.md)
- [Project Milestones](docs/roadmap/project-milestones.md)
- [Sprint Planning](docs/roadmap/sprint-planning.md)
- [Recommended IDE Extensions](docs/tooling/ide-extensions.md)

## Guiding Principles

- Build trust before scale.
- Treat money movement, balances, identity, and audit trails as critical infrastructure.
- Prefer simple, modular architecture first; evolve into distributed services only when the product and scale require it.
- Make every financial state change traceable, reversible by controlled process, and observable.
- Keep compliance, privacy, security, and operational readiness as first-class engineering concerns.
