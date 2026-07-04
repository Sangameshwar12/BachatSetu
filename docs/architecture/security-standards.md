# Security Standards

BachatSetu handles identity, community financial activity, payments, and sensitive personal information. Security must be designed into the platform from the first implementation sprint.

## Security Principles

- Default deny.
- Least privilege.
- Defense in depth.
- Strong tenant isolation.
- No secrets in source control.
- No sensitive data in logs.
- Financial operations require auditability.

## Authentication

- Use Spring Security for backend authentication.
- Use JWT access tokens with short expiry.
- Use refresh tokens with rotation and revocation.
- Store password hashes using a strong adaptive hashing algorithm.
- Require MFA for admin and internal operations before production launch.
- Support device/session management for mobile users.

## Authorization

Authorization must be enforced at three levels:

- API route access
- Domain use case access
- Object-level tenant and group ownership

Role-based access control is required for:

- Platform super admins
- Tenant admins
- Group organizers
- Group members
- Support operators
- Auditors

## Tenant Isolation

Every tenant-owned operation must verify tenant scope.

No API should trust tenant IDs supplied by the client without validating membership and access rights.

## Secrets Management

- Store secrets in AWS Secrets Manager or SSM Parameter Store.
- Use separate secrets per environment.
- Rotate production secrets on a defined schedule.
- Never commit `.env` files containing real secrets.
- Use GitHub Actions secrets only for CI/CD credentials.

## Data Protection

- Use TLS for all network traffic.
- Encrypt production databases and backups at rest.
- Encrypt sensitive application-level fields where needed.
- Mask PII in logs, exports, and support tooling.
- Restrict production data access through approval and audit.

## Payment Security

- Validate all provider webhooks.
- Make all payment commands idempotent.
- Reconcile payment provider state with internal state.
- Never treat client-side payment success as final.
- Store provider payloads safely with PII minimization.

## API Security

- Rate limit authentication, OTP, payment, and webhook endpoints.
- Validate all input.
- Use CORS allowlists.
- Apply secure HTTP headers.
- Reject oversized request bodies.
- Use request correlation IDs.

## Admin Security

- Admin portal must require MFA.
- Support roles must be restricted and auditable.
- Sensitive actions require explicit reason capture.
- Production support access should be time-bound.

## Security Testing

Minimum required checks:

- Dependency vulnerability scanning
- Static application security testing
- Secret scanning
- Container scanning
- API authorization tests
- OWASP Top 10 review
- Pre-production penetration test before public launch

## Incident Response

Before production launch, create runbooks for:

- Account takeover
- Payment mismatch
- Suspicious admin activity
- Data exposure
- Provider webhook abuse
- Credential leakage
- Production secret rotation

