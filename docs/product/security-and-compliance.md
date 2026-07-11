# Security and Compliance

> **Audience:** Legal Team, Security Reviewers, Developers, DevOps Engineers, Investors
> **Prerequisite reading:** [Data Model and Database Schema](data-model-and-database-schema.md), [Backend Module and API Reference](backend-module-and-api-reference.md), [Frontend Experience](frontend-experience.md)

This chapter documents security controls **as implemented**, distinguishing them from the target-state controls in [`security-standards.md`](../architecture/security-standards.md), which was written before implementation began. For the platform's public vulnerability-disclosure process, see [`SECURITY.md`](../../SECURITY.md) at the repository root; for internal triage process, see [`docs/governance/security-process.md`](../governance/security-process.md).

## 1. Authentication

BachatSetu is **OTP-only** — there is no password-based login in the running product (the `/forgot-password` screen is an explicit placeholder; `identity.users.password_hash` exists in the schema but is not exercised by any implemented flow).

```mermaid
sequenceDiagram
    actor U as User
    participant W as Frontend
    participant B as auth module
    participant DB as identity schema

    U->>W: Mobile number
    W->>B: POST /api/v1/auth/otp/request
    B->>DB: Insert otp_verifications (otp_hash only — never plaintext)
    B-->>W: { verificationId, expiresAt }
    U->>W: 6-digit code
    W->>B: POST /api/v1/auth/otp/verify
    B->>DB: Compare bcrypt hash; increment verification_attempts on mismatch
    B->>DB: Issue refresh_tokens (ACTIVE), sign a short-lived JWT access token
    B-->>W: { accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt }
```

Key controls, verified against the schema and controller code:

- **OTP codes are never stored in plaintext.** Migration `V4` explicitly dropped the plaintext `otp_code` column and replaced it with a bcrypt `otp_hash`, enabled via PostgreSQL's `pgcrypto` extension.
- **OTP attempts and resends are capped**: `verification_attempts` (0–5) and `resend_count` (0–3) are enforced by database check constraints, not just application logic.
- **At most one pending OTP per user and purpose** at a time (partial unique index).
- **Refresh tokens rotate on every use**, and reuse of an already-rotated token is a distinct, detectable state (`REUSED` — see [State Machines — Refresh Token](state-machines.md#refresh-token-session)), which is the standard defense against a stolen refresh token being replayed after the legitimate client has already rotated past it.
- **Access tokens are short-lived JWTs** carrying `sub` (user ID), `mobile_number`, `tenant_id`, `roles`, and `permissions` claims, decoded client-side for UX only — the frontend explicitly documents that this decoded payload is "never used for authorization decisions," only display and route-gating convenience (see §3).

**Not implemented, despite being specified:** multi-factor authentication for admin/internal operations ([`security-standards.md` §"Authentication"](../architecture/security-standards.md#authentication) calls for this "before production launch"), device/session management UI, and password-based login entirely.

## 2. Session Handling (Frontend)

The frontend has no server-readable session — the backend issues bearer JWTs only, no session cookie. Consequently:

- The token pair is persisted in `localStorage` (there is no more secure browser storage option available to a pure SPA without a backend-for-frontend layer, which does not exist in this system).
- A module-level `token-store.ts` bridges the React `AuthContext` (the only writer) into the axios request interceptor (which cannot use React hooks) — every outgoing request attaches `Authorization: Bearer <token>` if a token is present.
- On any `401` response, the interceptor calls a registered "unauthorized" handler that clears the session, shows a **"Your session has expired — please log in again"** toast, and redirects to `/login`. This toast was added specifically during the FE-6 production-readiness sprint after identifying that the prior behavior — a silent redirect with no explanation — was confusing.
- `ProtectedRoute` gates every `/dashboard/*` route on `isAuthenticated` (client-side, post-mount, since there is no server-readable session to check during server rendering).

## 3. Authorization

Authorization is enforced at two independent layers, matching [`security-standards.md`](../architecture/security-standards.md#authorization)'s "three levels" principle for the levels that are actually implemented:

1. **API route access** — every admin-scoped controller (`admin`, `admin.analytics`, `admin.configuration`, `platformoperations`) carries `@PreAuthorize("hasRole('PLATFORM_ADMIN')")` at the class level via Spring Security. This is the actual authorization boundary; nothing else in the system can be trusted to enforce it.
2. **Object-level tenant/group ownership** — enforced in application services (e.g. a user cannot fetch a `Payment` or `Receipt` belonging to a different tenant), not by database row-level security. See §5.

**Client-side role gating (UX convenience, not a security boundary):** the frontend's `RoleGuard` component reads the `roles` claim decoded from the JWT and blocks navigation into `/dashboard/admin/*` for non-admin sessions, and the sidebar hides the Admin navigation item entirely for non-admins. This was added during FE-6 after discovering that **any authenticated user could previously reach the Admin Portal's UI shell** (though every underlying API call would still correctly return `403`, since the real authorization boundary is layer 1 above). The fix is explicitly documented in code as "not the authorization boundary... every underlying endpoint independently enforces its own role check server-side."

**Role and permission model** (`identity.roles`, `identity.permissions`, `identity.user_roles`, `identity.role_permissions` — see [Data Model §3](data-model-and-database-schema.md#3-schema-identity)): seeded roles are `PLATFORM_ADMIN`, `SUPPORT_OPERATOR` (platform scope), `TENANT_ADMIN` (tenant scope), `GROUP_ORGANIZER`, `GROUP_MEMBER` (group scope), with 17 seeded permissions across `USER`, `ROLE`, `GROUP`, `MEMBER`, `PAYMENT`, `RECEIPT`, `DRAW`, `NOTIFICATION`, `AUDIT` modules. Only `PLATFORM_ADMIN` has a dedicated frontend surface today; `TENANT_ADMIN` and `SUPPORT_OPERATOR` exist in the data model and role seed but have no corresponding UI (see [Roadmap and Future Work](roadmap-and-future-work.md)). Organizer/member authorization within a group is derived from `GroupMember.roleInGroup`, not from this platform-wide role table.

## 4. Payment Security

- **Client-side payment completion is never treated as final.** A `Payment` only transitions to `VERIFIED` on a signed provider webhook (or an explicit reconciliation sync call) — see [Business Processes — Contribution Payment](business-processes.md#contribution-payment-with-gateway) and [State Machines — Payment](state-machines.md#payment).
- **Idempotency is enforced server-side** via `Payment.idempotencyKeyHash`, unique per `(tenant_id, idempotency_key_hash)` — not via a client-supplied `Idempotency-Key` HTTP header as originally specified (see [Vision and Implementation Status §6](vision-and-implementation-status.md#6-api-contract-reconciliation)).
- **Webhook endpoints are provider-specific** (`/api/v1/payments/webhooks/{razorpay|stripe|cashfree}`) and are documented as "Provider-signed" in [Backend Module and API Reference](backend-module-and-api-reference.md); the signature-verification implementation itself lives in `paymentgateway`'s infrastructure adapters per provider and is not re-derived here — see `services/backend/docs/application/payment-gateway.md` for the verified detail.
- BachatSetu never handles or stores raw card numbers, UPI PINs, or bank credentials — checkout happens entirely on the gateway provider's own hosted UI.

## 5. Tenant Isolation

Every tenant-owned table carries a `tenant_id` column (see [Data Model §1](data-model-and-database-schema.md#1-database-and-multi-tenancy-configuration)), but **no table has a database-level foreign key to `platform.tenants`** — tenant consistency is enforced entirely by application-layer query scoping, not by PostgreSQL constraints. This matches the "shared database, shared schema" model [`system-architecture.md`](../architecture/system-architecture.md#multi-tenancy) recommends as the initial approach, with the caveat the same document states explicitly: *"No API should trust tenant IDs supplied by the client without validating membership and access rights"* — every reviewed application service derives the acting tenant from the authenticated JWT's `tenant_id` claim, not from a client-supplied parameter.

The clearest evidence of tenant scoping in the actual API surface: the `audit` module's `GET /api/v1/audit` search endpoint is documented in its own Javadoc as "Authenticated and tenant-scoped: every request is always scoped to the caller's own tenant, never a client-supplied one" — even a `PLATFORM_ADMIN` cannot search another tenant's audit trail through this endpoint (a known, disclosed gap surfaced honestly in the Admin Portal's Monitoring screen).

## 6. Audit Trail

Every sensitive action across every module writes an immutable `AuditEntry` (see [Data Model §7](data-model-and-database-schema.md#7-schema-audit) for the full 41-value `event_type` enum). Confirmed integration points, read directly from the notification/audit listener classes built across backend sprints: authentication (login), payment verification, receipt generation and PDF download, draw completion, notification sending, storage upload and delete, payment-gateway refund initiation and webhook processing, tenant lifecycle changes (suspend/activate/archive), support ticket lifecycle, and announcement publication.

Audit entries are:

- **Append-only** — no `updated_at` or soft-delete columns; an audit row is never edited.
- **Intentionally unconstrained** on `actor_id` and `resource_id` — no foreign key, so an audit trail survives deletion of the user or resource it describes.
- **Tenant-scoped for search**, as noted in §5 — there is no platform-wide audit view today.

## 7. Data Protection

| Control | Status |
| --- | --- |
| TLS for all network traffic | Deployment-dependent — not something the application code itself enforces; a reverse proxy/load balancer at deploy time is expected to terminate TLS (no such infrastructure has been provisioned yet — see [System Architecture and Modules §2](system-architecture-and-modules.md#2-high-level-system-diagram)) |
| Encryption at rest | Deployment-dependent (would come from the hosting database's own encryption, e.g. RDS) — not configured, since no production database has been provisioned |
| OTP values hashed, never plaintext | ✅ Implemented (see §1) |
| PII masking in logs | Not confirmed in the reviewed code — treat as aspirational per [`security-standards.md`](../architecture/security-standards.md#data-protection) until verified |
| Secrets management (AWS Secrets Manager / SSM) | Not applicable yet — no cloud deployment exists; local/dev configuration uses environment variables per `services/backend/README.md` |
| File upload validation | ✅ Implemented on the frontend (profile photo: `image/*` MIME check and a 5 MB client-side size cap, added in FE-6) and via `storage.stored_files.size`/checksum tracking server-side |

## 8. Frontend-Specific Hardening (FE-6)

A dedicated production-readiness sprint reviewed the frontend specifically for: `dangerouslySetInnerHTML` usage (only one site — static, config-derived JSON-LD structured data on the landing page for SEO, containing no user input); clipboard usage (`navigator.clipboard.writeText` is used only to copy an already-generated, non-secret invitation code/link, never to read the clipboard); and route guards for all three authenticated personas (Member via `ProtectedRoute`, Organizer via the same, Admin via the additional `RoleGuard` described in §3). No XSS injection points, unsafe `eval`, or unvalidated redirect targets were found.

## 9. Compliance Posture

BachatSetu has **not** undergone a third-party security audit, penetration test, or formal compliance certification (SOC 2, ISO 27001, PCI-DSS, or India's DPDP Act readiness assessment) as of this writing. [`security-standards.md` §"Security Testing"](../architecture/security-standards.md#security-testing) calls for dependency scanning, SAST, secret scanning, container scanning, API authorization tests, an OWASP Top 10 review, and a pre-production penetration test before public launch — [`SECURITY.md`](../../SECURITY.md) confirms Dependabot and Dependency Review are active at the repository level today; the remaining items are tracked in [Roadmap and Future Work](roadmap-and-future-work.md). Because payment card/UPI data never touches BachatSetu's own servers (§4), the platform's own PCI-DSS scope is reduced, but this has not been formally assessed.

## Next Chapter

[Non-Functional Requirements and Production Readiness](non-functional-and-production-readiness.md) covers performance, accessibility, SEO, and monitoring — the remaining pillars of launch readiness alongside security.
