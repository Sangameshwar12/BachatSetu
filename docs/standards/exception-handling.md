# Exception Handling Standards

Exception handling must produce safe, consistent user-facing errors while preserving enough internal detail for engineering diagnosis.

## Principles

- Do not leak internal implementation details.
- Map exceptions to consistent API error responses.
- Preserve root causes in internal logs.
- Use domain-specific error codes.
- Treat validation, authorization, conflict, and system failures differently.

## Error Categories

| Category | HTTP Status | Example Code |
| --- | --- | --- |
| Validation | 400 or 422 | VALIDATION_FAILED |
| Authentication | 401 | AUTHENTICATION_REQUIRED |
| Authorization | 403 | ACCESS_DENIED |
| Not Found | 404 | RESOURCE_NOT_FOUND |
| Conflict | 409 | DUPLICATE_REQUEST |
| Rate Limit | 429 | RATE_LIMIT_EXCEEDED |
| External Provider | 502 or 503 | PAYMENT_PROVIDER_UNAVAILABLE |
| Internal | 500 | INTERNAL_SERVER_ERROR |

## API Error Shape

```json
{
  "error": {
    "code": "ACCESS_DENIED",
    "message": "You do not have permission to perform this action",
    "details": []
  },
  "meta": {
    "requestId": "string"
  }
}
```

This snippet defines response shape only. It is not application implementation.

## Backend Guidelines

- Use centralized exception mapping.
- Use domain exceptions for expected business failures.
- Avoid catching broad exceptions unless translating or adding context.
- Do not swallow exceptions silently.
- Use retries only for safe, idempotent operations.
- Wrap external provider failures with provider-neutral application errors.

## Mobile and Admin Guidelines

- Show user-friendly messages.
- Preserve machine-readable error codes.
- Do not expose stack traces.
- Provide retry actions only when retry is safe.
- Handle offline and timeout states explicitly.

## Financial Error Handling

Financial operations require extra care:

- Unknown payment status must not be treated as failed or successful without reconciliation.
- Duplicate requests must return the original result when possible.
- Provider timeouts should create a pending state and reconciliation job.
- Reversals must be explicit records.

