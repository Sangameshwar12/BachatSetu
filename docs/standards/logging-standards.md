# Logging Standards

Logs are operational tools, not data stores. They must help diagnose production issues without exposing sensitive information.

## Principles

- Use structured logs.
- Include correlation identifiers.
- Never log secrets, passwords, OTPs, full tokens, or payment credentials.
- Mask PII by default.
- Log financial state transitions carefully and consistently.
- Prefer business event IDs over raw payloads.

## Log Levels

| Level | Usage |
| --- | --- |
| ERROR | Unexpected failures requiring investigation |
| WARN | Recoverable issues or suspicious conditions |
| INFO | Important lifecycle and business events |
| DEBUG | Developer diagnostics in non-production |
| TRACE | Rare deep diagnostics, disabled by default |

## Required Log Context

Where available, include:

- `requestId`
- `tenantId`
- `userId`
- `actorType`
- `module`
- `operation`
- `resourceId`
- `clientVersion`
- `platform`

## Sensitive Data Rules

Never log:

- Passwords
- OTPs
- JWTs or refresh tokens
- Full mobile numbers
- Full email addresses when avoidable
- Bank account details
- Payment provider secrets
- Raw KYC documents
- Full request or response bodies for sensitive APIs

Mask examples:

```text
mobile: 98******10
email: a***y@example.com
token: [REDACTED]
```

## Financial Logging

Financial operations should log:

- Operation type
- Internal transaction ID
- Idempotency key hash
- Payment provider reference
- Previous state
- New state
- Reconciliation source

Do not log full provider payloads unless safely redacted and explicitly required for support.

## Audit Logs vs Application Logs

Application logs are for engineering operations.

Audit logs are product records for sensitive actions. They must be stored separately, retained according to policy, and queryable for compliance and support investigations.

