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
| `REDIS_PASSWORD` | Redis `AUTH` password. Has a safe blank default under `application.yml` for local development, but `application-prod.yml` removes that default (`${REDIS_PASSWORD}`, no fallback) — the app will not start under `SPRING_PROFILES_ACTIVE=prod` without it. |
| `AUTH_JWT_SIGNING_SECRET` | HMAC signing secret for access/refresh JWTs. Generate with `openssl rand -base64 64`. Rejected if blank by `ProductionEnvironmentGuard`. |
| `AUTH_CORS_ALLOWED_ORIGINS` | Comma-separated list of exact browser origins allowed to call the API. Rejected if blank or still the `http://localhost:3000` development default. |
| `TENANT_DEFAULT_ID` | The single tenant this deployment serves (`TenantScopeProviderConfig`) — used for every pre-authentication flow (signup, OTP request, login-completion) that needs a tenant before a JWT exists. Provision the matching row in `platform.tenants` before go-live. Rejected if blank or still the local placeholder `00000000-0000-0000-0000-000000000000` by `ProductionEnvironmentGuard`. |
| `AUDIT_SYSTEM_ACTOR_ID` | Recorded as `created_by`/`updated_by` only for the pre-authentication requests above (`SecurityContextCurrentAuditorProvider`) — every other write uses the real signed-in user's id automatically. Rejected if blank or still the local placeholder `00000000-0000-0000-0000-000000000001` by `ProductionEnvironmentGuard`. |

## 3. Backend — optional, with a safe default

| Variable | Default | Purpose |
| --- | --- | --- |
| `DATABASE_MAX_POOL_SIZE` | `10` (`30` under `prod`) | HikariCP max pool size |
| `DATABASE_MIN_IDLE` | `2` (`5` under `prod`) | HikariCP min idle connections |
| `DATABASE_BATCH_SIZE` | `50` | Hibernate JDBC batch size |
| `REDIS_PORT` | `6379` | Redis port |
| `SERVER_PORT` | `8080` | HTTP port the app listens on |
| `MANAGEMENT_ENDPOINTS_EXPOSURE` | `health,info,metrics,prometheus` | Actuator endpoints exposed over HTTP |
| `AUTH_OTP_BCRYPT_STRENGTH` | `12` | bcrypt cost factor for OTP hashes |
| `AUTH_ACCESS_TOKEN_EXPIRY` / `AUTH_REFRESH_TOKEN_EXPIRY` | `15m` / `30d` | JWT lifetimes |
| `AUTH_JWT_ISSUER` / `AUTH_JWT_AUDIENCE` | `bachatsetu` / `bachatsetu-api` | JWT claims |
| `AUTH_JWT_CLOCK_SKEW` | `30s` | Allowed clock drift when validating JWT `exp`/`nbf` |
| `AUTH_PASSWORD_HASH_STRENGTH` | `12` | bcrypt cost factor for user password hashes |
| `AUTH_REFRESH_HASH_STRENGTH` | `12` | bcrypt cost factor for stored refresh-token hashes |
| `AUTH_HEADER_NAME` | `Authorization` | Header name the auth filter reads the bearer token from |
| `AUTH_BEARER_PREFIX` | `Bearer ` (trailing space) | Prefix stripped from the auth header before JWT parsing |
| `AUTH_CORS_ALLOWED_METHODS` | `GET,POST,PUT,PATCH,DELETE,OPTIONS` | Allowed HTTP methods on CORS preflight |
| `AUTH_CORS_ALLOWED_HEADERS` | `Authorization,Content-Type,Accept,X-Request-ID` | Allowed request headers on CORS preflight |
| `AUTH_CORS_EXPOSED_HEADERS` | `X-Request-ID` | Response headers exposed to the browser |
| `AUTH_CORS_ALLOW_CREDENTIALS` | `false` | Whether cookies/credentials are allowed cross-origin |
| `AUTH_CORS_MAX_AGE` | `1h` | Preflight cache duration |
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
| `PAYMENT_GATEWAY_DEFAULT_PROVIDER` | `RAZORPAY` | `RAZORPAY`, `STRIPE`, or `CASHFREE` — selects which provider's credentials below are active. Unlike `SMS_PROVIDER`/`EMAIL_PROVIDER`, there is no fail-fast validation on the selected provider's credentials — a blank/misconfigured secret only surfaces at request/webhook time, not at startup. |
| `RAZORPAY_KEY_ID` / `RAZORPAY_SECRET` / `RAZORPAY_WEBHOOK_SECRET` | *(blank)* | Razorpay payment gateway credentials |
| `STRIPE_API_KEY` / `STRIPE_WEBHOOK_SECRET` | *(blank)* | Stripe payment gateway credentials |
| `CASHFREE_CLIENT_ID` / `CASHFREE_CLIENT_SECRET` / `CASHFREE_WEBHOOK_SECRET` | *(blank)* | Cashfree payment gateway credentials |
| `STORAGE_DEFAULT_PROVIDER` | `LOCAL` | `LOCAL`, `AWS_S3`, `AZURE_BLOB`, or `GOOGLE_CLOUD_STORAGE` — see [infrastructure-guide.md §2](infrastructure-guide.md#2-amazon-s3) before using `LOCAL` in a multi-instance deployment. **Only `LOCAL` and `AWS_S3` are backed by a real implementation today** — `AZURE_BLOB` and `GOOGLE_CLOUD_STORAGE` are simulated adapters (no real SDK calls), and there is no fail-fast validation on any provider's credentials, unlike SMS/Email. |
| `STORAGE_LOCAL_PATH` | `./data/storage` | Filesystem directory used when `STORAGE_DEFAULT_PROVIDER=LOCAL`. In `docker-compose.prod.yml` this resolves inside the backend container and is backed by the `backend-prod-storage` named volume so it survives redeploys — but not horizontal scaling to more than one backend instance. |
| `STORAGE_AWS_BUCKET` / `STORAGE_AWS_REGION` / `STORAGE_AWS_ACCESS_KEY_ID` / `STORAGE_AWS_SECRET_ACCESS_KEY` | *(blank)* | Required only if `STORAGE_DEFAULT_PROVIDER=AWS_S3` |
| `STORAGE_AZURE_ACCOUNT_NAME` / `STORAGE_AZURE_ACCOUNT_KEY` / `STORAGE_AZURE_CONTAINER_NAME` | *(blank)* | Read only if `STORAGE_DEFAULT_PROVIDER=AZURE_BLOB` — simulated adapter, not yet wired through `docker-compose.prod.yml` |
| `STORAGE_GCP_BUCKET` / `STORAGE_GCP_PROJECT_ID` / `STORAGE_GCP_CREDENTIALS_JSON` | *(blank)* | Read only if `STORAGE_DEFAULT_PROVIDER=GOOGLE_CLOUD_STORAGE` — simulated adapter, not yet wired through `docker-compose.prod.yml` |
| `INVITATION_VALIDITY` | `P7D` (ISO-8601 duration) | Group-invitation link/code validity period |
| `AUTOMATION_DRAW_CRON` | `0 */15 * * * *` | Cron schedule for the auto-draw job |
| `AUTOMATION_PAYMENT_REMINDER_CRON` | `0 0 9 * * *` | Cron schedule for the payment-due reminder job |
| `AUTOMATION_PAYMENT_REMINDER_DAYS_AHEAD` | `3` | How many days before the due date the reminder fires |
| `AUTOMATION_OVERDUE_REMINDER_CRON` | `0 30 9 * * *` | Cron schedule for the overdue-payment reminder job |
| `AUTOMATION_CLEANUP_CRON` | `0 0 3 * * *` | Cron schedule for the housekeeping/cleanup job |
| `ADMIN_PAGE_SIZE_DEFAULT` | `20` | Default page size for admin list endpoints |
| `ADMIN_PAGE_SIZE_MAX` | `100` | Max page size an admin caller may request |
| Every `*_REST_ENABLED` / `*_ENABLED` flag (one per module — see `application.yml`) | `true` (except `RECEIPT_STORAGE_UPLOAD_ENABLED`, which defaults to `false`) | Per-module kill switch, unrelated to this sprint; listed here only because they are also environment variables |

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
