# Environment Variables Guide

> **Audience:** DevOps Engineers, Developers
> **Prerequisite reading:** [docker-guide.md](docker-guide.md)

Every configuration value the backend and frontend need in production is supplied through
environment variables layered on top of `services/backend/src/main/resources/application.yml`
(backend) and build-time `NEXT_PUBLIC_*` variables (frontend) — none of it is hardcoded, and
the `prod` Spring profile has no working defaults for anything security-sensitive. This
chapter is the single reference for every variable; `.env.prod.example` (repository root) is
the fill-in-the-blanks companion to copy from.

## 1. How backend configuration resolution works

`application.yml` is the base configuration, active in every profile. `application-{profile}.yml`
files (`local`, `dev`, `test`, `prod`) layer on top, per
`SPRING_PROFILES_ACTIVE`. Almost every value in `application.yml` is written as
`${ENV_VAR:default}` — a default only ever safe for local development. `application-dev.yml`
and `application-prod.yml` remove the default for anything security-sensitive
(`${DATABASE_URL}` with no `:default`), so the application **fails to start** rather than
silently falling back to a development value if the operator forgets to set it. See
[production-deployment-guide.md](production-deployment-guide.md) for what
`in.bachatsetu.backend.configuration.production.ProductionEnvironmentGuard` additionally
verifies at startup under the `prod` profile.

## 2. Backend — required in `prod` (no default, startup fails if unset)

| Variable | Used for |
| --- | --- |
| `DATABASE_URL` | JDBC connection string, e.g. `jdbc:postgresql://<host>:5432/bachatsetu` |
| `DATABASE_USERNAME` | Database user |
| `DATABASE_PASSWORD` | Database password. `ProductionEnvironmentGuard` also rejects known development placeholder values (`bachatsetu`, `password`, `postgres`, blank). |
| `REDIS_HOST` | Redis host |
| `AUTH_JWT_SIGNING_SECRET` | HMAC signing secret for access/refresh JWTs. Generate with `openssl rand -base64 64`. Rejected if blank by `ProductionEnvironmentGuard`. |
| `AUTH_CORS_ALLOWED_ORIGINS` | Comma-separated list of exact browser origins allowed to call the API. Rejected if blank or still the `http://localhost:3000` development default. |

## 3. Backend — optional, with a safe default

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_MAX_POOL_SIZE` | `10` (`30` under `prod`) | HikariCP max pool size |
| `DATABASE_MIN_IDLE` | `2` (`5` under `prod`) | HikariCP min idle connections |
| `DATABASE_BATCH_SIZE` | `50` | Hibernate JDBC batch size |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | *(blank)* | Redis `AUTH` password — required in practice for any non-local Redis; blank only works against an unauthenticated instance |
| `SERVER_PORT` | `8080` | HTTP port the app listens on |
| `MANAGEMENT_SERVER_PORT` | same as `SERVER_PORT` | Optional separate port for actuator endpoints — see [infrastructure-guide.md §5](infrastructure-guide.md#5-monitoring-network-layout) |
| `MANAGEMENT_ENDPOINTS_EXPOSURE` | `health,info,metrics,prometheus` | Actuator endpoints exposed over HTTP |
| `AUTH_OTP_BCRYPT_STRENGTH` | `12` | bcrypt cost factor for OTP hashes |
| `AUTH_ACCESS_TOKEN_EXPIRY` / `AUTH_REFRESH_TOKEN_EXPIRY` | `15m` / `30d` | JWT lifetimes |
| `AUTH_JWT_ISSUER` / `AUTH_JWT_AUDIENCE` | `bachatsetu` / `bachatsetu-api` | JWT claims |
| `CACHE_ENABLED` | `true` | Enables the Redis cache infrastructure (`in.bachatsetu.backend.infrastructure.cache.CacheConfiguration`) |
| `CACHE_OTP_TTL` / `CACHE_RATE_LIMIT_TTL` / `CACHE_SESSION_TTL` / `CACHE_CONFIG_TTL` | `5m` / `1m` / `30m` / `10m` | Per-cache-region time-to-live. No business module reads or writes through these caches yet — see [non-functional-and-production-readiness.md](../product/non-functional-and-production-readiness.md). |
| `SMS_PROVIDER` | `MSG91` | `MSG91`, `FAST2SMS`, or `TWILIO` — selects the active OTP delivery provider. The selected provider's own credential variables below are enforced as required by `SmsProviderProperties`'s fail-fast startup validation, not by Spring's own binding — the app refuses to start if they are blank. |
| `SMS_RETRY_COUNT` / `SMS_CONNECT_TIMEOUT` / `SMS_READ_TIMEOUT` | `2` / `3s` / `5s` | Retry count (after the first attempt) and HTTP timeouts for the shared SMS `RestClient` |
| `MSG91_AUTH_KEY` / `MSG91_TEMPLATE_ID` / `MSG91_SENDER_ID` | *(blank)* | Required only if `SMS_PROVIDER=MSG91` |
| `FAST2SMS_API_KEY` | *(blank)* | Required only if `SMS_PROVIDER=FAST2SMS` |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_PHONE_NUMBER` | *(blank)* | Required only if `SMS_PROVIDER=TWILIO` |
| `EMAIL_PROVIDER` | `AWS_SES` | `AWS_SES`, `RESEND`, or `SENDGRID` — selects the active email delivery provider. Same fail-fast pattern as `SMS_PROVIDER`, enforced by `EmailProviderProperties`. |
| `EMAIL_FROM_ADDRESS` | *(blank)* | Sending address — required regardless of which provider is selected |
| `EMAIL_REPLY_TO` | defaults to `EMAIL_FROM_ADDRESS` | Reply-to address on every outbound message |
| `EMAIL_RETRY_COUNT` / `EMAIL_CONNECT_TIMEOUT` / `EMAIL_READ_TIMEOUT` | `2` / `3s` / `5s` | Retry count and HTTP/SDK timeouts for the shared email client |
| `AWS_SES_REGION` / `AWS_ACCESS_KEY` / `AWS_SECRET_KEY` | *(blank)* | Required only if `EMAIL_PROVIDER=AWS_SES` |
| `RESEND_API_KEY` | *(blank)* | Required only if `EMAIL_PROVIDER=RESEND` |
| `SENDGRID_API_KEY` | *(blank)* | Required only if `EMAIL_PROVIDER=SENDGRID` |
| `RAZORPAY_KEY_ID` / `RAZORPAY_SECRET` / `RAZORPAY_WEBHOOK_SECRET` | *(blank)* | Razorpay payment gateway credentials |
| `STRIPE_API_KEY` / `STRIPE_WEBHOOK_SECRET` | *(blank)* | Stripe payment gateway credentials |
| `CASHFREE_CLIENT_ID` / `CASHFREE_CLIENT_SECRET` / `CASHFREE_WEBHOOK_SECRET` | *(blank)* | Cashfree payment gateway credentials |
| `STORAGE_DEFAULT_PROVIDER` | `LOCAL` | `LOCAL`, `AWS_S3`, `AZURE_BLOB`, or `GOOGLE_CLOUD_STORAGE` — see [infrastructure-guide.md §2](infrastructure-guide.md#2-amazon-s3) before using `LOCAL` in a multi-instance deployment |
| `STORAGE_AWS_BUCKET` / `STORAGE_AWS_REGION` / `STORAGE_AWS_ACCESS_KEY_ID` / `STORAGE_AWS_SECRET_ACCESS_KEY` | *(blank)* | Required only if `STORAGE_DEFAULT_PROVIDER=AWS_S3` |
| Every `*_REST_ENABLED` / `*_ENABLED` flag (one per module — see `application.yml`) | `true` | Per-module kill switch, unrelated to this sprint; listed here only because they are also environment variables |

## 4. Backend — profile-specific files

| File | Applies under | Notable overrides |
| --- | --- | --- |
| `application-local.yml` | `SPRING_PROFILES_ACTIVE=local` (default when unset) | Small connection pool, all actuator detail (`show-details: always`), `DEBUG` application logging |
| `application-dev.yml` | `SPRING_PROFILES_ACTIVE=dev` | Requires `DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`/`REDIS_HOST` explicitly (no defaults) |
| `application-test.yml` | `SPRING_PROFILES_ACTIVE=test` | Lower bcrypt cost for fast tests, `bachatsetu.automation.enabled=false`, all actuator detail. New in this sprint — additive; no existing test currently activates it (see [docker-guide.md](docker-guide.md)) |
| `application-prod.yml` | `SPRING_PROFILES_ACTIVE=prod` | Larger connection pool, minimal actuator detail (`show-details: never`), `ProductionEnvironmentGuard` active |

## 5. Frontend

| Variable | Where it's read | Purpose |
| --- | --- | --- |
| `NEXT_PUBLIC_API_BASE_URL` | Build time (`docker build --build-arg`) **and** container start (`docker run -e`) | Base URL the browser calls the backend at. Must be a public HTTPS URL reachable from the browser, not an internal container hostname — every API call is made client-side. Because Next.js inlines `NEXT_PUBLIC_*` values into the client JavaScript bundle at build time, changing it after the image is built has no effect; the image must be rebuilt. |

`services/web/.env.example` documents the same variable for local (non-Docker) development.

## 6. Compose / orchestration-level

| Variable | Consumed by | Purpose |
| --- | --- | --- |
| `DATABASE_NAME` | `docker-compose.prod.yml` | Passed through to both `POSTGRES_DB` and the backend's `DATABASE_URL` |
| `JAVA_OPTS` | `services/backend/Dockerfile` `ENTRYPOINT` | Extra JVM flags, appended to the container-aware heap-sizing defaults already baked into the image |

## 7. What is intentionally not here yet

`SMS_*` (Sprint PI-2.1) and `EMAIL_*` (Sprint PI-2.2) provider variables are documented in
§3 above — both are implemented and wired through `docker-compose.prod.yml` as of Sprint LR-1.
No secrets-manager integration variables exist yet (`AWS_SECRETS_MANAGER_*` or similar) — see
[infrastructure-guide.md §6](infrastructure-guide.md#6-secrets-management) for why plain
environment variables are still this project's scope and what the next step looks like.
