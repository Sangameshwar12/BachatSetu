# Login & Session Management Module

## Purpose

Lets an existing, already-`ACTIVE` user sign back in on a new device or after their session has
lapsed — mobile number → OTP → access/refresh token pair — and keeps that session alive
indefinitely (while the refresh token remains valid) through a REST-exposed token-refresh and
logout API. This sprint (LR-2) is purely additive: it wires the returning-user flow on top of the
same OTP/JWT primitives [`signup`](signup.md) already uses, and exposes Sprint 8.6's refresh-token
application layer — built but never reachable over HTTP — through a real REST API.

## Architecture

Purely additive packages inside the existing Auth module:

```
auth
 ├── application/login     command, query, exception, usecase, service
 ├── application/token      (Sprint 8.6, pre-existing) — now REST-reachable
 └── interfaces/rest        LoginController, TokenController, DTOs, mappers, config
```

`POST /api/v1/auth/login/start` looks the user up by mobile number, requires `UserStatus.ACTIVE`,
and dispatches a `SIGN_IN`-purpose OTP. `POST /api/v1/auth/login/verify` verifies that OTP,
re-checks the account is still `ACTIVE`, and issues the first access/refresh token pair for this
session via the pre-existing token subsystem — no new token-generation logic, no new JWT signing
code. `POST /api/v1/auth/token/refresh` and `POST /api/v1/auth/logout` are thin REST wrappers
around Sprint 8.6's `RefreshAccessTokenApplicationService` and `RevokeRefreshTokenApplicationService`
— their rotation, expiry, and reuse-detection behavior is unchanged.

## No Duplicate Token Logic

`CompleteLoginApplicationService` issues tokens the same way `CompleteSignupApplicationService`
does: `GenerateAccessTokenUseCase` + `GenerateRefreshTokenUseCase` with a fresh
`TokenSessionId.newId()`. Login does not re-run profile activation or default role assignment —
the user is already `ACTIVE` with roles from signup — it only re-authenticates them.

## Actor Identity Before Authentication

Three of the four endpoints in this sprint run before any bearer JWT exists, so each command needs
an actor identity that isn't "the current authenticated user":

- **Login start** — the looked-up `User`'s own ID (`user.userId().toAggregateId()`), the same
  self-actor pattern signup uses for its own pre-auth OTP dispatch.
- **Token refresh / logout** — the presented refresh token's own ID
  (`RefreshTokenCredential.parse(rawToken).tokenId().toAggregateId()`). A refresh token acting on
  its own behalf mirrors the domain aggregate's existing internal convention: `RefreshToken` already
  self-actors during `markReused()`/`revoke()`. `TokenApiMapper.actorId(String)` centralizes the
  parse and maps a malformed token to `TokenFailureReason.INVALID_REFRESH_TOKEN` (401) rather than
  letting an `IllegalArgumentException` escape.

## Audit Wiring — No False Signals

Four new audit events, each deliberately scoped to fire from exactly one path so audit history
stays trustworthy:

| Event | Listener | Fires on | Does **not** fire on |
|---|---|---|---|
| `LOGIN` | `LoginAuditListener` (pre-existing, previously dead code) | `OtpVerified` filtered to `purpose == SIGN_IN` | Signup's `REGISTRATION`-purpose OTP verification |
| `LOGIN_FAILED` | `LoginFailedAuditListener` (new) | `OtpRejected` / `OtpExpired` filtered to `purpose == SIGN_IN` | Signup OTP failures |
| `TOKEN_REFRESH` | `TokenRefreshAuditListener` (new) | `RefreshTokenCreated`, published only from `RefreshAccessTokenApplicationService.refresh()`'s rotation path | Initial token issuance at login/signup (`GenerateRefreshTokenApplicationService` deliberately left un-instrumented) |
| `LOGOUT` | `LogoutAuditListener` (new) | `RefreshTokenRevoked`, published only from the legitimate `ACTIVE → REVOKED` branch of `RevokeRefreshTokenApplicationService` | The internal security-driven revoke that fires when `RefreshAccessTokenApplicationService` detects a reused, already-rotated token — that revoke is defensive, not a user action, and must never look like a real logout in the audit trail |

The reuse-vs-legitimate-revoke distinction is enforced with explicit Mockito `never()`/`times()`
assertions in `TokenApplicationServiceTest`, not just by code inspection — see
`detectsRotationReuseAndRevokesActiveReplacement()` (asserts the publisher is **never** called) and
`revokesActiveTokenIdempotentlyAndRejectsInvalidStates()` (asserts a second, idempotent revoke does
**not** re-publish).

Every new module-owned event flows through `DomainEventPublisherPort` /
`ApplicationEventDomainEventPublisherAdapter`, the same generic mechanism signup uses — Auth never
depends on Audit directly (Audit's REST layer already depends on Auth for `CurrentUserProvider`, so
the reverse would form a package cycle); Audit's listeners react to Auth's own domain events via
plain Spring `@EventListener`.

## Session Lifecycle (Frontend)

`AuthProvider` (`services/web/src/contexts/auth-context.tsx`) owns three independent mechanisms
that together satisfy "never ask the user to log in again while the refresh token is valid":

1. **Login** — `login(tokens)` decodes the access token's claims, stores the full session
   (including the refresh token) to `localStorage`, and pushes the access token into the
   axios-facing `token-store` singleton.
2. **Hydration (browser refresh)** — on mount, if the persisted access token is still valid it's
   reused as-is; if it's expired but the refresh token isn't, a refresh is attempted before
   falling back to logged-out; if both are expired, the stored session is discarded.
3. **Proactive silent refresh** — a `useEffect` keyed on `session` schedules a `setTimeout` for
   `accessTokenExpiresAt - 60s` (`REFRESH_MARGIN_MS`), calls `refreshAccessToken`, and reschedules
   itself against the new session's expiry. A failure here (revoked/expired refresh token) calls
   `forceLogoutToLogin`, which clears the session, shows a toast, and redirects to `/login`.

A fourth, pre-existing mechanism — the axios response interceptor's reactive 401 handler — is left
unchanged as a fallback safety net for the case a request slips through with an already-invalid
token (clock skew, out-of-band revocation); it was deliberately **not** upgraded to a
refresh-and-retry interceptor in this sprint, since the proactive timer already covers the normal
case and a second, overlapping refresh-triggering path would risk racing the scheduled one.

## Validation and Errors

`mobileNumber` — Indian E.164 (`+91[6-9]\d{9}`), matching signup. Failures map to RFC 7807 problem
details:

**Login**

| Failure | Status | `code` |
|---|---|---|
| Mobile not registered | 404 | `mobile-not-registered` |
| Account not active | 403 | `account-not-active` |
| OTP expired | 410 | `otp-expired` |
| OTP invalid | 422 | `otp-invalid` |
| OTP attempts exceeded | 429 | `otp-verification-limit-exceeded` |

**Token refresh / logout** (Sprint 8.6 reasons, now REST-reachable)

| Failure | Status | `code` |
|---|---|---|
| Refresh token not found / malformed | 401 | `invalid-refresh-token` |
| Refresh token expired | 401 | `refresh-token-expired` |
| Refresh token reused | 401 | `refresh-token-reused` |
| Refresh token revoked | 401 | `refresh-token-revoked` |

## Security Decisions

- Refresh tokens are never accepted as a query parameter or URL segment — both `/token/refresh` and
  `/logout` take the raw token only in the JSON request body.
- No JWT or refresh-token value is ever logged; error responses surface only the machine-readable
  `code` and a human-readable `detail`, never the token itself.
- Refresh tokens keep rotating on every use (Sprint 8.6 behavior, unchanged): each `/token/refresh`
  call returns a new refresh token and revokes the one just presented, so a leaked-but-unused old
  token is a dead end and reuse of an already-rotated token is detected and treated as a compromise
  signal (see `state-machines.md#refresh-token-session`).

## Configuration

Gated identically to signup: `LoginApplicationConfig` and the token-refresh/logout beans require
both `bachatsetu.persistence.repositories.enabled` and `bachatsetu.authentication.token.enabled`
(both `matchIfMissing = true`). No new configuration properties were introduced.
