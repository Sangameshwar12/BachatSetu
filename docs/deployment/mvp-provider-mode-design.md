# MVP Provider Mode: Email/SMS/Payment Gateway Deployment-Mode Switches

> **Audience:** Backend Engineers, DevOps Engineers, Engineering Leadership
> **Status:** Implemented and verified
> **Prerequisite reading:** [environment-variables-guide.md](environment-variables-guide.md)

## 1. Root cause

`EmailInfrastructureConfig`/`SmsInfrastructureConfig` (the real-provider wiring) and
`LocalEmailSenderConfig`/`LocalOtpSenderConfig` (the log-only fallback) were gated on Spring
**profiles** — `@Profile({"dev", "prod"})` on the real configs, `@Profile({"local", "test"})` on
the logging ones. This conflated two independent questions into one mechanism:

- **Which environment is this?** (local dev machine, CI, a real deployment) — what
  `SPRING_PROFILES_ACTIVE` is actually for: connection strings, actuator exposure, CORS
  strictness, `ProductionEnvironmentGuard`'s fail-fast checks.
- **Is a real email/SMS provider configured?** — a deployment-mode question, orthogonal to the
  first. An MVP closed beta on AWS EC2 legitimately wants `prod`'s environment behavior (strict
  CORS, no Swagger, a real production database) while having no real email/SMS provider yet.

Because both questions were answered by the same `@Profile` list, `prod` could only mean "use
the real provider" — there was no way to ask for `prod`'s environment behavior *and* log-only
delivery at the same time. The proximate failure (`Found 2: retryingEmailSenderAdapter,
loggingEmailSenderAdapter`) was `LocalEmailSenderConfig` having had `"prod"` added directly to
its own `@Profile` list at some point, alongside `EmailInfrastructureConfig`'s pre-existing
`@Profile({"dev", "prod"})` — both configs became active under `prod` simultaneously, producing
two `EmailSenderPort` beans. That specific edit is symptomatic, not the disease: profile-based
gating for this concern was one accidental edit away from either this duplicate-bean failure
(as happened to Email) or the opposite failure — `prod` requiring real credentials it doesn't
have (as was still true for SMS, which never got the same accidental edit).

## 2. Clean architecture: feature flags, not a new Spring profile

The fix replaces `@Profile` with `@ConditionalOnProperty` on all four classes, keyed off a new
boolean property per module — `bachatsetu.email.enabled` (`EMAIL_PROVIDER_ENABLED`) and
`bachatsetu.sms.enabled` (`SMS_PROVIDER_ENABLED`), both defaulting to `false`. A third,
pre-existing property (`bachatsetu.payment.gateway.enabled` / `PAYMENT_GATEWAY_ENABLED`,
already gating the payment-gateway REST controllers) had its own default corrected from `true`
to `false` for the same reason — see §3.4.

**Why a feature flag, not a new `mvp` Spring profile:**

- Every other per-module capability in this codebase — `STORAGE_ENABLED`, `ADMIN_ENABLED`,
  `PAYMENT_GATEWAY_ENABLED`, every `*_REST_ENABLED` flag — already uses exactly this pattern:
  a `bachatsetu.<module>.enabled` boolean, `@ConditionalOnProperty`-gated, with a safe default
  in the base `application.yml`. Using it for Email/SMS is not a new paradigm; it corrects the
  two modules that had drifted from the codebase's own established convention.
- An `mvp` Spring profile would have needed either (a) duplicating every legitimately-`prod`
  setting (connection pool sizing, CORS strictness, actuator exposure,
  `ProductionEnvironmentGuard`) into a new `application-mvp.yml` — copy-paste, drift risk,
  exactly what the refactor rules exclude — or (b) requiring `SPRING_PROFILES_ACTIVE=prod,mvp`
  as a mandatory combination, which is easy to get wrong and makes "which profile is active"
  answer two unrelated questions at once.
- A boolean flag defaulting to `false` makes **the safe, credential-free state the default**
  everywhere, including `prod` — an operator opts *in* to needing real credentials, rather than
  the previous design where `prod` opted *out* of working without them. This is what makes
  "future migration to real providers require only configuration changes" literally true: flip
  `EMAIL_PROVIDER_ENABLED=true` (or `SMS_PROVIDER_ENABLED`/`PAYMENT_GATEWAY_ENABLED`), fill in
  that provider's credentials, redeploy — no code change, no profile change.

**Why exactly one bean is guaranteed, not just conventionally true:**

`@ConditionalOnProperty(..., havingValue = "true")` on the real-provider config and
`@ConditionalOnProperty(..., havingValue = "false", matchIfMissing = true)` on the logging
config are complementary and exhaustive over every value the property can hold (`true`,
`false`, or literally absent) — there is no value for which both, or neither, evaluate true.
This is a stronger guarantee than the previous `@Profile` lists ever gave (profile membership
is just two independently-maintained lists of strings with no structural relationship
enforced by the compiler or the framework), and it is exactly why the original duplicate-bean
failure was possible in the first place.

## 3. Files changed and why

### 3.1 Email

| File | Change |
| --- | --- |
| `services/backend/src/main/java/.../infrastructure/email/config/EmailInfrastructureConfig.java` | `@Profile({"dev", "prod"})` → `@ConditionalOnProperty(prefix = "bachatsetu.email", name = "enabled", havingValue = "true")` |
| `services/backend/src/main/java/.../infrastructure/email/config/LocalEmailSenderConfig.java` | `@Profile({"local", "test", "prod"})` (the duplicate-bean bug) → `@ConditionalOnProperty(prefix = "bachatsetu.email", name = "enabled", havingValue = "false", matchIfMissing = true)` |

### 3.2 SMS

| File | Change |
| --- | --- |
| `services/backend/src/main/java/.../infrastructure/auth/config/SmsInfrastructureConfig.java` | `@Profile({"dev", "prod"})` → `@ConditionalOnProperty(prefix = "bachatsetu.sms", name = "enabled", havingValue = "true")` |
| `services/backend/src/main/java/.../infrastructure/auth/config/LocalOtpSenderConfig.java` | `@Profile({"local", "test"})` → `@ConditionalOnProperty(prefix = "bachatsetu.sms", name = "enabled", havingValue = "false", matchIfMissing = true)` |

### 3.3 Configuration wiring

| File | Change |
| --- | --- |
| `services/backend/src/main/resources/application.yml` | Added `bachatsetu.email.enabled: ${EMAIL_PROVIDER_ENABLED:false}` and `bachatsetu.sms.enabled: ${SMS_PROVIDER_ENABLED:false}`, siblings of the existing `provider` keys. Also corrected `bachatsetu.payment.gateway.enabled`'s own default from `${PAYMENT_GATEWAY_ENABLED:true}` to `${PAYMENT_GATEWAY_ENABLED:false}` — see §3.4. |
| `docker-compose.prod.yml` | Passed `SMS_PROVIDER_ENABLED`, `EMAIL_PROVIDER_ENABLED`, `PAYMENT_GATEWAY_ENABLED` through to the backend container, all defaulting to `false`. |
| `.env.prod.example` | Added the three flags with explanatory comments; corrected the SMS/Email section headers, which previously (incorrectly, as of this fix) said real credentials were "required." |
| `docs/deployment/environment-variables-guide.md` | Documented all three flags in §3 (optional, safe default); corrected the `SMS_PROVIDER`/`EMAIL_PROVIDER`/`PAYMENT_GATEWAY_DEFAULT_PROVIDER` rows to note they are only read once the corresponding `*_ENABLED` flag is `true`. |
| `docs/deployment/production-checklist.md` | Replaced a checklist item that implied SMS/Email credentials were mandatory for any go-live with one reflecting the real, now-supported MVP deployment mode — including an explicit call-out that `SMS_PROVIDER_ENABLED=false` means end users cannot self-serve signup/login without an operator reading the application log for their OTP (see §5). |
| `docs/integrations/email-provider.md`, `docs/integrations/sms-provider.md` | Corrected the "for every profile except local/test" framing in each doc's introduction to describe the actual deployment-mode-switch behavior. |

### 3.4 Payment gateway — a second instance of the same root cause

While verifying the fix end-to-end (§5), the live MVP boot revealed that
`bachatsetu.payment.gateway.enabled` still defaulted to `true` at the `application.yml` level —
only `docker-compose.prod.yml`'s *override* had been set to `false`, meaning any deployment path
that doesn't go through that specific Compose file (a direct `java -jar` run, `mvn
spring-boot:run`, a future CI/CD pipeline) would still activate the payment-gateway REST
endpoints by default, contradicting "Payment: Disabled" as a genuine MVP-mode property rather
than a Docker-Compose-specific override. Fixed by correcting the actual `application.yml`
default (`payment.rest.enabled`, which gates core payment recording/viewing — genuinely core
business functionality — was deliberately left untouched at `true`).

This surfaced two test classes (`PaymentGatewayControllerTest`, `PaymentWebhookControllerTest`)
that relied on the old `true` default without setting the property explicitly — both controllers
are themselves `@ConditionalOnProperty`-gated on this same flag. Fixed by adding
`@TestPropertySource(properties = "bachatsetu.payment.gateway.enabled=true")` to both, matching
what those tests actually exercise (the controllers' behavior *when* the gateway is enabled).

### 3.5 Tests

| File | Change |
| --- | --- |
| `AuthenticationInfrastructureConfigTest.java` | Replaced a test asserting the logging OTP sender is *absent* under `prod` (no longer true — see §2) with one confirming it's present under `prod` when `sms.enabled` is unset, plus a new test confirming it's absent when `sms.enabled=true`. |
| `SmsInfrastructureConfigTest.java` | Added `bachatsetu.sms.enabled=true` to the shared property defaults (the class under test is now conditionally gated); added a new test confirming it contributes no beans when `sms.enabled=false`. |
| `EmailInfrastructureConfigTest.java` (new) | Did not previously exist. Mirrors `SmsInfrastructureConfigTest`'s structure: one test per provider (AWS SES/Resend/SendGrid), a fail-fast test, and an "enabled=false contributes nothing" test. |
| `LocalEmailSenderConfigTest.java` (new) | Did not previously exist (the equivalent OTP-side coverage lived only inside `AuthenticationInfrastructureConfigTest`). Tests the logging sender is active by default, active under `prod` specifically, and absent when `email.enabled=true`. |
| `PaymentGatewayControllerTest.java`, `PaymentWebhookControllerTest.java` | Added `@TestPropertySource(properties = "bachatsetu.payment.gateway.enabled=true")` — see §3.4. |

No business logic, domain model, or use-case code was touched anywhere in this change — every
edit is in `infrastructure`/`interfaces.rest.config` classes, `application.yml`, or tests.

## 4. Verification performed

- `mvn clean verify`: **BUILD SUCCESS** — 1873 tests (10 new), 0 Checkstyle/PMD/SpotBugs
  violations, all JaCoCo coverage gates met, full ArchUnit suite passed.
- `npm run lint` / `npm run build`: clean, all 37 routes build (frontend untouched by this
  change; run to confirm no incidental regression).
- **Live boot, `SPRING_PROFILES_ACTIVE=prod`, zero email/SMS/payment environment variables
  set** (confirmed via `env | grep` before starting the process) — the exact MVP scenario:
  - `Started BachatSetuBackendApplication in 16.858 seconds` (and `20.415 seconds` on a second,
    fully independent run after the payment-gateway fix) — clean startup, zero errors, no
    `BeanDefinitionException`, no `UnsatisfiedDependencyException`, no duplicate beans.
  - `/actuator/health` → `{"status":"UP"}`.
  - `POST /api/v1/auth/signup` → `202 Accepted`; the OTP was logged via
    `in.bachatsetu.backend.infrastructure.auth.adapter.LoggingOtpSenderAdapter` with the phone
    number masked (`+91******0002`) — never sent to a real SMS provider, never logging the raw
    code.
  - `POST /api/v1/auth/signup/verify` (using a known bcrypt hash set directly via `psql`, never
    the raw OTP over the API) → `200 OK` with a real JWT.
  - Both the `WELCOME` and `SIGNUP_COMPLETED` emails were logged via
    `in.bachatsetu.backend.infrastructure.email.adapter.LoggingEmailSenderAdapter` with the
    address masked (`mv***********@example.com`) — never sent to a real email provider.
  - `POST /api/v1/users/me/onboarding` (an authenticated write, using the JWT above) →
    `200 OK` — JWT authentication genuinely works end-to-end.
  - The payment-gateway/webhook endpoints, confirmed disabled: their live response is
    byte-for-byte identical to a request against a path that was never mapped at all (both
    return Spring Security's generic `401 authentication-required`, since an unmapped path
    never reaches MVC to produce a distinguishable 404) — the correct, expected signature for
    "this controller bean does not exist," and confirmed unambiguously by the isolated
    `@WebMvcTest` slice tests (§3.5), where Security filters are disabled and the raw `404` is
    directly observable.
- Docker itself could not be exercised — no Docker daemon is available in this environment
  (consistent with every previous sprint's own documented limitation in this repository). The
  live `SPRING_PROFILES_ACTIVE=prod` boot above, using the exact same environment variables
  `docker-compose.prod.yml` passes through, is the same honest substitute used throughout this
  project's deployment-readiness work.

## 5. Operational note: MVP mode and real users

With `SMS_PROVIDER_ENABLED=false` (the default), every OTP is written to the application log,
not delivered to the end user's phone. This is correct and intentional for internal testing or
an operator-mediated beta, but **a real end user cannot self-serve signup or login** without
someone reading the backend log and relaying the code to them. Before inviting real users who
expect to receive an SMS, this specific flag needs `true` plus real MSG91/Fast2SMS/Twilio
credentials — everything else in this MVP mode (email, payment gateway) can safely stay
log-only/disabled for longer, since neither blocks a user from completing the core signup →
join a group → make a payment (recorded, not gateway-processed) flow on their own.

## 6. Future migration path

Every one of the three flags follows the identical pattern — enabling a real provider is a
configuration change, never a code change:

1. **Email:** set `EMAIL_PROVIDER_ENABLED=true`, `EMAIL_PROVIDER` to `AWS_SES`/`RESEND`/
   `SENDGRID`, and that provider's credentials (`EMAIL_FROM_ADDRESS` plus the provider-specific
   keys). `EmailProviderProperties`'s existing fail-fast startup validation (unchanged by this
   refactor) rejects a blank required credential immediately rather than at first send.
2. **SMS:** set `SMS_PROVIDER_ENABLED=true`, `SMS_PROVIDER` to `MSG91`/`FAST2SMS`/`TWILIO`, and
   that provider's credentials. Same fail-fast validation via `SmsProviderProperties`.
3. **Payment gateway:** set `PAYMENT_GATEWAY_ENABLED=true`, `PAYMENT_GATEWAY_DEFAULT_PROVIDER`,
   and that provider's credentials. Note (pre-existing, unrelated to this refactor): unlike
   Email/SMS, there is no fail-fast validation on payment-gateway credentials — a
   blank/misconfigured secret only surfaces at request/webhook time.

No `@Profile` list, `docker-compose.prod.yml` service definition, or Dockerfile needs to change
for any of the three — only environment variable values.
