# API Standards

BachatSetu APIs must be stable, predictable, secure, and easy to consume from Flutter, the admin portal, and future partner integrations.

## API Style

- Use REST for primary product APIs.
- Use JSON request and response bodies.
- Use OpenAPI specifications for every public and app-consumed API.
- Use HTTPS only.
- Use UTF-8 encoding.

## URL Structure

```text
/api/v1/{resource}
/api/v1/{resource}/{id}
/api/v1/{resource}/{id}/{sub-resource}
```

Examples:

```text
/api/v1/bhishi-groups
/api/v1/bhishi-groups/{groupId}/members
/api/v1/payments/{paymentId}
```

## Versioning

- Major API versions belong in the path, such as `/api/v1`.
- Minor backward-compatible changes do not require a new path version.
- Breaking changes require a new major version.
- Deprecated APIs must include a documented removal timeline.

## HTTP Methods

| Method | Usage |
| --- | --- |
| GET | Read resources |
| POST | Create resources or trigger commands |
| PUT | Replace complete resources |
| PATCH | Partially update resources |
| DELETE | Delete or deactivate resources |

## Status Codes

| Status | Meaning |
| --- | --- |
| 200 | Successful read or update |
| 201 | Resource created |
| 202 | Accepted for async processing |
| 204 | Successful operation with no body |
| 400 | Invalid request |
| 401 | Missing or invalid authentication |
| 403 | Authenticated but not authorized |
| 404 | Resource not found |
| 409 | Conflict or duplicate request |
| 422 | Valid JSON but failed domain validation |
| 429 | Rate limit exceeded |
| 500 | Unexpected server error |
| 503 | Temporary service unavailable |

## Request Standards

- Use camelCase JSON fields.
- Validate all external input.
- Reject unknown enum values.
- Use ISO 8601 timestamps.
- Use amounts in the smallest currency unit, such as paise for INR.
- Never accept client-calculated financial totals without server verification.

## Response Envelope

Standard success response:

```json
{
  "data": {},
  "meta": {
    "requestId": "string"
  }
}
```

Standard error response:

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed",
    "details": []
  },
  "meta": {
    "requestId": "string"
  }
}
```

These snippets define API shape only. They are not application implementation.

## Pagination

Use cursor pagination for large or user-facing lists.

```text
?limit=25&cursor=opaqueCursor
```

Use offset pagination only for small admin tables where consistency is not critical.

## Sorting and Filtering

```text
?sort=createdAt:desc
?status=active
?fromDate=2026-01-01&toDate=2026-01-31
```

Rules:

- Whitelist sortable fields.
- Whitelist filter fields.
- Use server-side validation for date ranges and limits.

## Idempotency

Required for:

- Payment initiation
- Contribution recording
- Payout initiation
- Provider webhook handling
- Any API that can create financial side effects

Use the `Idempotency-Key` header for client-initiated operations.

## Headers

| Header | Purpose |
| --- | --- |
| Authorization | Bearer JWT |
| X-Request-Id | Client or gateway request correlation |
| Idempotency-Key | Replay protection for financial commands |
| X-Client-Version | Mobile or admin app version |
| X-Platform | android, ios, web |

## Backward Compatibility

APIs may add optional fields. APIs must not remove fields, rename fields, change field types, or change enum meanings without a major version change.

