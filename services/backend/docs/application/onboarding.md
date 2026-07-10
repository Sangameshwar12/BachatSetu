# Profile Onboarding Module

## Purpose

The post-signup step where a newly verified user records an optional photo, city, state, and
notification preference before reaching the Welcome Screen. Completes exactly once per profile.

## Architecture

The `user` module previously had only a `domain` layer (built for signup's profile provisioning in
Sprint 13.4 Part 1). This sprint stands up its `application` and `interfaces` layers for the first
time, following the exact same package shape as every other module:

```
user
 ├── domain/model          UserProfile extended additively (see below), ProfileCompleted event
 ├── application/port      ClockPort, TransactionPort, DomainEventPublisherPort (new trio)
 ├── application/{command,query,exception,usecase,service}
 └── interfaces/rest       OnboardingController, DTOs, OnboardingApiMapper, config, adapters
```

`POST /api/v1/users/me/onboarding` — authenticated, acts on the caller's own profile (resolved via
`CurrentUserProvider`, never a path parameter).

## Extending UserProfile

Additive fields: `city`, `state` (plain strings — not the existing `Address` value object, which
requires a full postal address `line1`/`postalCode`/`countryCode` onboarding never collects, and
which no other flow in the codebase populates yet), `photoFileId` (nullable `AggregateId`
referencing a Storage-module file), `notificationsEnabled` (boolean), `onboarded` (boolean guard).
`UserProfile.completeOnboarding(...)` enforces the once-only invariant and emits `ProfileCompleted`.
A new, richer constructor overload carries the additive fields; the original constructor delegates
to it with the pre-onboarding defaults, so `UserProfile.register(...)` is unchanged.

`V12__signup_and_profile_onboarding.sql` adds the matching nullable columns (plus
`notifications_enabled`/`onboarded` with `NOT NULL DEFAULT`) to `identity.users`, and
`UserJpaEntity`/`UserJpaMapper` are extended to round-trip them — `address`/`contactPreferences`
remain unpersisted exactly as before this sprint (a pre-existing gap, out of scope here).

## Photo Storage

The request accepts an already-uploaded `photoFileId` (a Storage-module file identifier), not raw
bytes — the client uploads the photo via the existing `POST /api/v1/storage/files` endpoint first,
then submits the returned file ID with the onboarding form. This reuses the Storage module exactly
as instructed ("Use Storage module. Do not store binary in User table.") without adding a second,
onboarding-specific upload path.

## Why the Controller Never Touches Auth's Domain Layer

`OnboardingController` needs the caller's identity, so it depends on
`auth.application.security.CurrentUserProvider` — an **application**-layer type, allowed for any
controller. What it must *not* do is call `.toAggregateId()`/`AggregateId` constructors itself
(domain-layer calls), which the `LayerDependencyArchitectureTest`
`CONTROLLERS_MUST_NOT_DEPEND_ON_DOMAIN_OR_INFRASTRUCTURE` rule forbids for every controller in the
codebase. Following the same pattern as `SavingsGroupApiMapper`/`SavingsGroupController`, the
controller passes the whole `AuthenticatedUser` into `OnboardingApiMapper.toCommand(currentUser,
request)`, and the mapper — not layer-restricted — performs the actual domain-type conversion.

## Audit and Notifications

`ProfileCompleted` is published through the new `user.application.port.DomainEventPublisherPort` and
picked up by `audit.interfaces.rest.event.ProfileCompletedAuditListener`, recording a
`PROFILE_COMPLETED` entry — the same event-listener pattern used throughout this sprint to avoid
module cycles.

## Errors

| Failure | Status | `code` |
|---|---|---|
| No profile exists for this user | 404 | `profile-not-found` |
| Onboarding already completed | 409 | `already-onboarded` |

## Configuration

`bachatsetu.user.rest.enabled` (default `true`) gates `OnboardingController`.
