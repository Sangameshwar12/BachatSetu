# Docker Guide

> **Audience:** DevOps Engineers, Developers
> **Prerequisite reading:** [docs/product/system-architecture-and-modules.md](../product/system-architecture-and-modules.md)

This guide documents the Docker images and Compose files added in Sprint PI-1 (Production
Infrastructure Foundation). It covers what each file does, why it's built the way it is, and
how to build and run it locally. It does not cover *deploying* those images to AWS — see
[infrastructure-guide.md](infrastructure-guide.md) for that.

## 1. Images

### 1.1 Backend — `services/backend/Dockerfile`

A two-stage build:

1. **`build`** (`maven:3.9.9-eclipse-temurin-21`) — resolves dependencies into their own
   Docker layer (`mvn dependency:go-offline`, cached separately from source changes), then
   packages the application jar with `mvn package -DskipTests` (tests already ran under
   `mvn clean verify` in CI; the image build does not re-run them).
2. **runtime** (`eclipse-temurin:21-jre-alpine`) — copies only the built jar. Runs as a
   dedicated non-root `bachatsetu` user. Declares `HEALTHCHECK` against
   `/actuator/health/liveness` (public per
   `bachatsetu.authentication.security.public-endpoints` — see
   [environment-variables-guide.md](environment-variables-guide.md)). JVM heap sizing uses
   `-XX:MaxRAMPercentage=70.0`/`-XX:InitialRAMPercentage=50.0` rather than a fixed `-Xmx`, so
   the container's memory limit (set by Compose or the orchestrator) is what actually bounds
   heap size — no hardcoded value to keep in sync with deployment configuration. 30% of the
   container limit is left as non-heap headroom (metaspace, thread stacks, JIT code cache,
   native SDK buffers) — `docker-compose.prod.yml` sets the backend's limit to `2048m`
   accordingly. The image also creates `/app/data/storage` owned by the non-root `bachatsetu`
   user at build time, since `STORAGE_DEFAULT_PROVIDER=LOCAL`'s default directory needs to be
   writable by that user, not `root`.

Build and run it standalone:

```bash
cd services/backend
docker build -t bachatsetu-backend:local .
docker run --rm -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/bachatsetu \
  -e DATABASE_USERNAME=bachatsetu -e DATABASE_PASSWORD=bachatsetu \
  -e REDIS_HOST=host.docker.internal \
  -e AUTH_JWT_SIGNING_SECRET=local-only-secret \
  -e SPRING_PROFILES_ACTIVE=dev \
  bachatsetu-backend:local
```

### 1.2 Frontend — `services/web/Dockerfile`

A three-stage build:

1. **`deps`** — `npm ci` into its own cached layer.
2. **`builder`** — copies source, runs `npm run build`. `next.config.ts` sets
   `output: "standalone"`, so the build produces a self-contained `.next/standalone` server
   bundle (a minimal `node_modules` subset plus `server.js`) instead of requiring the full
   `node_modules` tree in the runtime image. `NEXT_PUBLIC_API_BASE_URL` is accepted as a
   build `ARG` — Next.js inlines `NEXT_PUBLIC_*` variables into the client bundle at build
   time, so it must be known when the image is built, not just at container start.
3. **runtime** (`node:22-alpine`) — copies only `.next/standalone`, `.next/static`, and
   `public/`. Runs as a non-root `nextjs` user. `HEALTHCHECK` hits `/` on the container's own
   port using `wget --spider` (already present in Alpine's BusyBox, no extra package needed).

`compress: true` in `next.config.ts` enables gzip on the standalone server's own responses.
Long-lived cache headers for hashed static assets (`/_next/static/**`) are *not* set via
`next.config.ts`'s `headers()` — Next.js already sends its own immutable Cache-Control for
those paths in production, and setting a duplicate one there produces a build-time warning
about interfering with `next dev`. The Nginx edge (§1.3) sets an explicit
`Cache-Control: public, max-age=31536000, immutable` for `/_next/static/**` as a
belt-and-braces measure when the app runs behind it.

```bash
cd services/web
docker build -t bachatsetu-web:local --build-arg NEXT_PUBLIC_API_BASE_URL=http://localhost:8080 .
docker run --rm -p 3000:3000 bachatsetu-web:local
```

### 1.3 Nginx edge — `deploy/nginx/nginx.conf`

Not a custom image — `docker-compose.prod.yml` mounts this file straight into the stock
`nginx:1.27-alpine` image. It is the only service in `docker-compose.prod.yml` that publishes
a host port. Responsibilities:

- Routes `/api/**`, `/v3/api-docs/**`, `/swagger-ui/**` to the backend; everything else to the
  frontend.
- Routes only `/actuator/health*` to the backend publicly — deeper actuator paths
  (`/actuator/metrics`, `/actuator/prometheus`) are deliberately **not** proxied here. See
  [infrastructure-guide.md §5](infrastructure-guide.md#5-monitoring-network-layout) for how
  those are reached instead.
- gzip compression for text/JSON responses (including responses proxied through an optional
  CloudFront CDN, via `gzip_proxied any`), and an explicit long-cache header for
  `/_next/static/**` (belt-and-braces alongside the Next.js server's own headers).
- A small set of defense-in-depth response headers
  (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`) — nginx strips
  (`proxy_hide_header`) whatever the upstream (Spring Security, Next.js's own
  `next.config.ts` `headers()`) already set for these before re-adding its own, so clients
  never see the same header twice. `Content-Security-Policy` is set at the Next.js layer
  instead (`services/web/next.config.ts`), since only the frontend origin serves HTML and the
  policy needs build-time knowledge of `NEXT_PUBLIC_API_BASE_URL` for `connect-src`.
- A `X-Request-Id` header that is preserved if the client (or the ALB) supplied one, generated
  otherwise, and forwarded to both upstreams — the backend's own
  `in.bachatsetu.backend.observability.CorrelationIdFilter` picks it up from there. Named
  `$req_id` internally, not `$request_id`, to avoid shadowing nginx's own built-in variable of
  that name.
- `X-Forwarded-Proto` is forwarded from whatever the ALB set on the original request (falling
  back to nginx's own `$scheme` only when absent) rather than being overwritten with
  `$scheme` directly — nginx itself always speaks plain HTTP, so using `$scheme` unconditionally
  would make every request look insecure to the backend and silently suppress its
  `Strict-Transport-Security` response header.
- TLS termination happens **in front of** this proxy (an AWS Application Load Balancer with
  an ACM certificate — see [infrastructure-guide.md](infrastructure-guide.md)), not inside it.
  `nginx.conf` only listens on port 80.

## 2. Compose files

Three Compose files exist, each with a distinct purpose — none replaces another:

| File | Purpose |
| --- | --- |
| `services/backend/docker-compose.yml` | Unchanged from before this sprint. Postgres + Redis only, for running the backend with `mvn spring-boot:run` on the host. See `services/backend/README.md`. |
| `docker-compose.dev.yml` (repo root) | The full stack (Postgres, Redis, backend, frontend) in containers, frontend running `next dev` with a bind mount for hot reload. Fixed, non-secret development credentials. |
| `docker-compose.prod.yml` (repo root) | The full stack built from the production Dockerfiles above, plus the Nginx edge. Every secret is required from the environment (`${VAR:?message}` — Compose refuses to start if unset) — see [environment-variables-guide.md](environment-variables-guide.md). |

### 2.1 Running the dev stack

```bash
docker compose -f docker-compose.dev.yml up --build
```

Frontend on `http://localhost:3000`, backend on `http://localhost:8080`, Postgres on `5432`,
Redis on `6379` — the same ports `services/backend/README.md` already documents for the
host-based workflow, so both approaches are interchangeable.

### 2.2 Running the prod stack

```bash
cp .env.prod.example .env   # then fill in every value — see environment-variables-guide.md
docker compose -f docker-compose.prod.yml up -d --build
```

Only Nginx publishes a host port (`80`). Postgres, Redis, the backend, and the frontend are
reachable only on the internal `bachatsetu-internal` Docker network. See
[production-deployment-guide.md](production-deployment-guide.md) for the full procedure,
including what has to be true about the host/environment before this command is safe to run.

Since Sprint 9.1, the same file also starts `prometheus` and `grafana` — scraping/dashboarding
the backend's `/actuator/prometheus` metrics. Both are on `bachatsetu-internal` like every other
service, and additionally bind their host ports to `127.0.0.1` only (not `0.0.0.0`), so neither
is reachable outside the host itself. See [monitoring-guide.md](monitoring-guide.md) for how to
reach Grafana from a workstation, default credentials, and how to add a dashboard.

## 3. What this sprint does not include

- No image is published to a registry (ECR, Docker Hub) — that is a CI/CD pipeline concern,
  out of scope for PI-1 (see [roadmap-and-future-work.md](../product/roadmap-and-future-work.md)
  for GitHub Actions CI/CD, still marked not started).
- No Kubernetes manifests — the target deployment shape for PI-1 is Compose-on-EC2 (or an
  equivalent single-host/ECS deployment), documented in
  [infrastructure-guide.md](infrastructure-guide.md).
- Docker builds were **not executed** in the environment this sprint was implemented in (no
  Docker daemon available) — every Dockerfile and Compose file was written and statically
  reviewed, and is covered by
  `services/backend/src/test/java/in/bachatsetu/backend/deployment/DockerConfigurationValidationTest.java`,
  but an actual `docker build`/`docker compose up` run against these files has not yet been
  performed. Run the commands in §1 and §2 as the first verification step in any environment
  with Docker available before relying on these images.
