# Identity Domain

Version: 1.3
Sprint: 8.1, amended by Sprints 8.2, 8.3, and 8.6
Status: Implemented

## Purpose

The identity domain defines authentication-user identity, role and permission assignment, OTP verification, encoded password state, and refresh-token lifecycle. It is pure Java and has no Spring, JWT, HTTP, JPA, repository, database, or infrastructure dependency.

Broader architecture boundaries remain defined by [Architecture Protection](../../../../docs/architecture/architecture-protection.md). User profile, address, language, and community membership remain owned by the separate `user` and `member` bounded contexts.

## Aggregate Design

| Aggregate | Identity | Responsibility |
| --- | --- | --- |
| `User` | `UserId` | Authentication contact identifiers, encoded password hash, status, and unique role assignments |
| `Role` | `RoleId` | Canonical role name and unique permission-ID assignments |
| `Permission` | `PermissionId` | Canonical permission name |
| `RefreshToken` | `RefreshTokenId` | Hashed credential ownership, session uniqueness, rotation, reuse, revocation, and expiry |
| `OtpVerification` | `AggregateId` | OTP purpose, verification result, and expiry lifecycle |

Aggregates expose immutable snapshots of collections. State changes occur only through invariant-preserving methods and update shared audit/version metadata. Equality and hash codes use aggregate identity rather than mutable attributes.

Sprint 8.2 adds event-free `rehydrate(...)` constructors and domain-owned repository ports. Rehydration applies the same constructor invariants, restores audit metadata and optimistic-lock versions, and never replays or emits domain events. Persistence implementation details are documented in [Identity Persistence](identity-persistence.md).

## Value Objects

- `UserId`, `RoleId`, `PermissionId`, and `RefreshTokenId` provide UUID-backed type safety.
- `Email` reuses the canonical shared-kernel value object and normalizes valid addresses to lowercase.
- `MobileNumber` accepts only canonical Indian mobile numbers in `+91XXXXXXXXXX` form, with subscriber numbers beginning from 6 through 9.
- `PasswordHash` accepts recognized bcrypt or Argon2 encodings and cannot represent ordinary plain text. Its string representation is redacted.
- `OtpCode` accepts exactly six digits and redacts its string representation. It is ephemeral and never belongs to persisted aggregate state.
- `OtpHash` and `RefreshTokenHash` accept opaque encoded values between 32 and 255 characters and redact string rendering.
- `TokenSessionId` identifies one device/session independently of a rotating credential identifier.

No raw password or refresh-token credential is modeled. The domain stores only an encoded password hash and refresh-token lifecycle identity.

## Domain Rules

### User

- Registration requires a valid email, Indian mobile number, encoded password hash, actor, and timestamp.
- New users begin in `PENDING_VERIFICATION`.
- Assigning the same `RoleId` twice fails.
- Changing to the current password hash fails.
- Password-change events never contain password or hash material.

### Role And Permission

- Role names are trimmed, uppercase, and limited to letters, digits, and underscores.
- Permission names are trimmed, lowercase, and use canonical segmented names such as `group:read`.
- A role cannot contain the same permission ID twice.
- `PermissionFactory` rejects a canonical name already present in the authoritative permission collection supplied by its caller.
- Cross-transaction permission-name uniqueness is protected by the persistence boundary's database constraint.

### OTP

- Generation and expiry times are explicit, and expiry must follow generation.
- The aggregate stores only `OtpHash`; hash generation and comparison are delegated through the application boundary.
- Verification succeeds only while status is `PENDING`, before expiry, and for a matching hash result.
- Each unsuccessful comparison increments the verification-attempt count. The fifth failure moves the verification to `FAILED`.
- Verification at or after expiry moves it to `EXPIRED`.
- Terminal OTP states cannot be verified again.
- Explicit invalidation moves a pending OTP to `INVALIDATED`.
- Replacement invalidates the previous OTP immediately and carries a resend count capped at three.
- OTP values are excluded from domain events.

### Refresh Token

- Expiry must follow issue time.
- Only a one-way `RefreshTokenHash` is retained; plaintext credential material never enters the aggregate.
- A token is usable only while `ACTIVE` and before expiry, with one active token per user/session enforced by persistence.
- Only an active, unexpired token can be revoked.
- Evaluation at or after expiry moves an active token to `EXPIRED`.
- Rotation moves the old token to `ROTATED`, links its replacement, and makes it immediately unusable.
- Presentation of a rotated token moves it to `REUSED`; the application layer revokes the active replacement for that session.
- Token credential generation, hashing, signing, and parsing remain outside the domain and are documented in [JWT Authentication](jwt-authentication.md).

## Domain Events

| Event | Aggregate | Sensitive-data policy |
| --- | --- | --- |
| `UserRegistered` | User | Contains normalized contact identifiers, no password hash |
| `PasswordChanged` | User | Contains identity and time only |
| `OtpGenerated` | OTP verification | Contains purpose and expiry, never the OTP code |
| `OtpVerified` | OTP verification | Contains identity and time only |
| `RefreshTokenCreated` | Refresh token | Contains lifecycle identity and expiry, no token credential |
| `RefreshTokenRevoked` | Refresh token | Contains lifecycle identity and time only |

Events implement the shared `DomainEvent` contract and use UUID event identifiers. Aggregate events remain in memory until pulled by a future application boundary.

## Factories

Factories generate identifiers and obtain time through an injected `Clock`, keeping tests deterministic and the domain independent of frameworks:

- `UserFactory`
- `RoleFactory`
- `PermissionFactory`
- `RefreshTokenFactory`
- `OtpVerificationFactory`

OTP factories accept an externally generated `OtpHash`; secure code generation, hashing, and delivery are application ports defined by Sprint 8.3.

## Validation And Testing

Validation is performed at value-object and aggregate boundaries. Invalid formats use `IllegalArgumentException`; lifecycle and uniqueness violations use `IdentityDomainException`.

Unit tests cover successful behavior, invalid formats, duplicate assignments, immutable collection views, equality, event safety, clock-based factories, OTP failure/expiry, and refresh-token revocation/expiry. JaCoCo enforces 100% line coverage for every class under `in.bachatsetu.backend.auth.domain` during `mvn clean verify`.

## Explicitly Out Of Scope

- Spring Security and security configuration
- JWT and refresh-token credential implementation, which remains behind domain-independent application ports
- Password encoding and comparison adapters
- OTP transport, hashing, clock, and secure-random provider implementations
- Controllers, DTOs, APIs, application services, and workflows
- JPA entities, Flyway migrations, and database constraints
