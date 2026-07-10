# Signup Module

## Purpose

Lets a brand-new person create a BachatSetu account without any manual database work: submit name,
mobile number, optional email, and preferred language; verify a dispatched OTP; receive an access
token and a refresh token, ready to call every other authenticated endpoint immediately.

## Architecture

Purely additive packages inside the existing Auth module:

```
auth
 ├── domain/port          ProfileProvisioningPort (auth-owned, see "Two aggregates" below)
 ├── application/signup    command, query, exception, usecase, service
 ├── application/port      PasswordHashGeneratorPort, DomainEventPublisherPort (new, generic)
 └── interfaces/rest       SignupController, signup DTOs, SignupApiMapper, SignupApplicationConfig
```

`POST /api/v1/auth/signup` creates the account (`PENDING_VERIFICATION`) and dispatches a
`REGISTRATION`-purpose OTP. `POST /api/v1/auth/signup/verify` verifies the OTP, activates the
account, assigns the default `GROUP_MEMBER` role, and issues the first access/refresh token pair
via the pre-existing (previously unwired) token subsystem.

## Two Aggregates, One Table

`auth.domain.model.User` (authentication: email/mobile/passwordHash/status/roles) and
`user.domain.model.UserProfile` (profile: name/contact/preferredLanguage/status) both persist onto
the same `identity.users` row, through two separate repository ports. Critically:
`AuthUserRepositoryAdapter.save(User)` can only **update** an existing row — it throws
`PersistenceResourceNotFoundException` if the row doesn't exist yet — while
`UserRepositoryAdapter.save(UserProfile)` can **insert** a fresh row. Signup therefore provisions
the profile first (creates the row), then populates the auth columns on that same row: zero changes
to either existing repository, adapter, or mapper.

**Why this isn't a direct `auth → user` dependency.** The user module's own onboarding REST layer
(Sprint 13.4 Part 2) depends on `auth.application.security.CurrentUserProvider`, like every
controller in the codebase. If `auth`'s signup application services also depended on `user`'s
repository/domain types directly, the two modules would form a package cycle (`auth → user → auth`),
which the `PackageDependencyArchitectureTest` ArchUnit rule forbids. Instead, `auth` defines its own
domain port, `auth.domain.port.ProfileProvisioningPort`, using only auth-owned and shared types
(`Email`, `PhoneNumber`, `AggregateId`, `String` for the preferred-language code). The concrete
adapter, `infrastructure.persistence.adapter.AuthProfileProvisioningAdapter`, lives in **general**
infrastructure — not `infrastructure.auth` — specifically so it's free to depend on
`user.domain.port.UserRepository` and `user.domain.model.*`: general infrastructure may depend on
any module's domain layer, but a module's own application/domain code may not depend on another
module's domain or application layer. This is the same "invert the dependency through a
module-owned port implemented by a neutral infrastructure adapter" pattern used for `TenantProvider`
below.

## Password Invariant

This platform is 100% OTP-based; no password is ever set or checked. `User.register()` still
requires a bcrypt/Argon2-formatted `PasswordHash` as a domain invariant, so
`PasswordHashGeneratorPort` (implemented by `RandomPasswordHashGeneratorAdapter`) bcrypt-encodes 32
random bytes at signup time — a hash nobody will ever authenticate against.

## Tenant Resolution Before Login

Issuing the first access/refresh token pair requires a tenant ID, but no JWT exists yet at this
point in the flow. `auth.domain.port.TenantProvider` (a domain port, for the same
general-infrastructure-may-depend-on-any-domain-layer reason as above) wraps the existing
`infrastructure.persistence.adapter.TenantScopeProvider` — currently hardcoded to a single
placeholder tenant in the `local` profile, pending a real multi-tenant resolution strategy.

## Audit and Domain Events

`User.register()` already emits `UserRegistered`; the signup services publish it (and any other
pulled events) through a new, generic `auth.application.port.DomainEventPublisherPort` /
`ApplicationEventDomainEventPublisherAdapter` pair, mirroring every other module's own
`DomainEventPublisherPort`. `audit.interfaces.rest.event.SignupAuditListener` reacts to
`UserRegistered` and records a `USER_REGISTERED` audit entry — the same "publish an event, let a
listener in the dependent module react" pattern already used for `LOGIN` auditing, needed because
Audit's own REST layer already depends on `auth` (for `CurrentUserProvider`), so a direct call the
other way would also form a cycle.

## Validation and Errors

`mobileNumber` — Indian E.164 (`+91[6-9]\d{9}`); `preferredLanguage` — `ENGLISH|HINDI|MARATHI`;
`acceptedTerms` — must be `true`. Failures map to RFC 7807 problem details:

| Failure | Status | `code` |
|---|---|---|
| Terms not accepted | 400 | `terms-not-accepted` |
| Mobile already registered | 409 | `mobile-already-registered` |
| Email already registered | 409 | `email-already-registered` |
| OTP expired | 410 | `otp-expired` |
| OTP invalid | 422 | `otp-invalid` |
| OTP attempts exceeded | 429 | `otp-verification-limit-exceeded` |

## Configuration

`bachatsetu.authentication.token.enabled` (default `true`) gates both the token subsystem and the
signup use cases — signup cannot function without token issuance, so `SignupController` requires
**both** `bachatsetu.authentication.rest.enabled` and `bachatsetu.authentication.token.enabled` to
be `true` (`matchIfMissing = true` on both). Minimal-context tests that don't need signup disable
`bachatsetu.authentication.token.enabled=false` explicitly, matching the precedent set by every
other flag added in this session.
