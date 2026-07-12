# REST API Contract

This document defines the REST API contract for BachatSetu. It is an endpoint catalog and API design specification only. It does not contain Java code, Flutter code, SQL, OpenAPI YAML, or implementation details.

BachatSetu APIs must feel consistent, predictable, audit-friendly, and safe for financial workflows. The API style should be closer to Stripe than to a generic CRUD backend: stable resources, explicit lifecycle actions, idempotent financial commands, clear errors, and strong backward compatibility.

## 1. API Design Principles

- Design contract first, implementation second.
- Keep APIs resource-oriented.
- Use explicit command endpoints for lifecycle transitions that require audit, authorization, or idempotency.
- Make every financial side effect idempotent.
- Return stable machine-readable error codes.
- Prefer predictable field names and pagination patterns over endpoint-specific creativity.
- Never expose internal implementation details.
- Never trust client-calculated financial totals.
- Keep tenant and object-level authorization mandatory.
- Make APIs mobile-friendly for low-bandwidth and intermittent networks.

## 2. REST Standards

Base URL:

```text
https://api.bachatsetu.com/api/v1
```

Resource naming:

- Use plural nouns.
- Use kebab-case paths.
- Use camelCase query parameters.
- Use camelCase JSON field names in future OpenAPI specs.

Common path shape:

```text
/api/v1/{resources}
/api/v1/{resources}/{resourceId}
/api/v1/{resources}/{resourceId}/{subResources}
/api/v1/{resources}/{resourceId}/{commands}
```

Common headers:

| Header | Required | Purpose |
| --- | --- | --- |
| Authorization | Most endpoints | Bearer access token |
| X-Request-Id | Recommended | Client-generated request correlation |
| Idempotency-Key | Required for side-effecting financial commands | Replay protection |
| X-Client-Version | Recommended | Mobile or admin version |
| X-Platform | Recommended | android, ios, web, admin |
| Accept-Language | Optional | Response localization |

## 3. Authentication APIs

BachatSetu is OTP-only — there is no password, OAuth, social, biometric, or passkey login anywhere
in the product. The table below reflects the endpoints actually implemented; see
[`login.md`](../../services/backend/docs/application/login.md) and
[`signup.md`](../../services/backend/docs/application/signup.md) for the full flows.

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/signup` | Register a new account and dispatch a `REGISTRATION` OTP | Public | Recommended |
| POST | `/api/v1/auth/signup/verify` | Verify signup OTP, activate account, issue first token pair | Public | Recommended |
| POST | `/api/v1/auth/login/start` | Look up a returning user by mobile number and dispatch a `SIGN_IN` OTP | Public | Recommended |
| POST | `/api/v1/auth/login/verify` | Verify login OTP and issue an access/refresh token pair | Public | Recommended |
| POST | `/api/v1/auth/token/refresh` | Rotate a refresh token and issue a new access/refresh token pair | Refresh token | No |
| POST | `/api/v1/auth/logout` | Revoke a refresh token | Refresh token | No |

## 4. User APIs

> **From here on, this document is a design specification, not an as-built catalog.** Sections 4
> onward describe the target API surface this contract was originally written against; most of
> these endpoints have not been implemented as specified (the real, shipped equivalent is often
> narrower — e.g. the `user` module only exposes `POST /api/v1/users/me/onboarding`, not the
> `/users/me/*` family below). For what actually exists in the running backend, see
> [`backend-module-and-api-reference.md`](../product/backend-module-and-api-reference.md) (read
> directly from the controller source) and the reconciliation in
> [`vision-and-implementation-status.md`](../product/vision-and-implementation-status.md).

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/users/me` | Get current user profile | User | No |
| PATCH | `/users/me` | Update current user profile | User | No |
| GET | `/users/me/devices` | List current user's devices | User | No |
| DELETE | `/users/me/devices/{deviceId}` | Revoke a device | User | Required |
| GET | `/users/me/roles` | List current user's effective roles | User | No |
| GET | `/users/me/activity` | List current user's visible activity | User | No |
| POST | `/users/me/contact-methods` | Add email or mobile contact method | User | Required |
| POST | `/users/me/contact-methods/{contactMethodId}/verify` | Verify contact method | User | Required |
| GET | `/users/{userId}` | Get user by ID for authorized operators | Admin or support | No |
| PATCH | `/users/{userId}/status` | Lock, suspend, or reactivate user | Admin | Required |

## 5. Organizer APIs

Organizer endpoints provide convenience views and workflows for users managing groups. They do not replace canonical group, cycle, installment, or payment resources.

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/organizer/groups` | List groups organized by current user | Organizer | No |
| GET | `/organizer/groups/{groupId}/summary` | Get organizer summary for a group | Organizer | No |
| GET | `/organizer/groups/{groupId}/collections` | View collection progress | Organizer | No |
| POST | `/organizer/groups/{groupId}/reminders` | Send reminders to due members | Organizer | Required |
| GET | `/organizer/groups/{groupId}/exceptions` | List group exceptions needing action | Organizer | No |
| POST | `/organizer/groups/{groupId}/manual-payments` | Record approved manual payment | Organizer with permission | Required |
| GET | `/organizer/tasks` | List pending organizer tasks | Organizer | No |

## 6. Member APIs

Member endpoints optimize the mobile member experience.

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/member/groups` | List groups where current user is a member | Member | No |
| GET | `/member/groups/{groupId}` | Get member-facing group detail | Member | No |
| GET | `/member/groups/{groupId}/installments` | List member installments in group | Member | No |
| GET | `/member/installments/due` | List current user's due installments | Member | No |
| GET | `/member/payments` | List current user's payments | Member | No |
| GET | `/member/receipts` | List current user's receipts | Member | No |
| GET | `/member/payouts` | List current user's payout or draw status | Member | No |
| POST | `/member/groups/{groupId}/leave-requests` | Request exit from group where policy allows | Member | Required |

## 7. Group APIs

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/groups` | List groups visible to current actor | User | No |
| POST | `/groups` | Create group draft | Organizer | Required |
| GET | `/groups/{groupId}` | Get group detail | Group participant or admin | No |
| PATCH | `/groups/{groupId}` | Update draft or editable group fields | Organizer | No |
| DELETE | `/groups/{groupId}` | Delete draft group or request cancellation | Organizer or admin | Required |
| POST | `/groups/{groupId}/activate` | Activate group after validation | Organizer | Required |
| POST | `/groups/{groupId}/suspend` | Suspend group operations | Admin or authorized organizer | Required |
| POST | `/groups/{groupId}/resume` | Resume suspended group | Admin or authorized organizer | Required |
| POST | `/groups/{groupId}/complete` | Mark group completed | Organizer or admin | Required |
| GET | `/groups/{groupId}/rules` | Get group rules | Group participant | No |
| PUT | `/groups/{groupId}/rules` | Replace draft group rules | Organizer | Required |
| GET | `/groups/{groupId}/members` | List group members | Group participant | No |
| POST | `/groups/{groupId}/members` | Add group member | Organizer | Required |
| GET | `/groups/{groupId}/members/{groupMemberId}` | Get group member detail | Group participant | No |
| PATCH | `/groups/{groupId}/members/{groupMemberId}` | Update member role or metadata | Organizer | No |
| POST | `/groups/{groupId}/members/{groupMemberId}/remove` | Remove or exit member according to rules | Organizer | Required |

## 8. Monthly Cycle APIs

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/groups/{groupId}/monthly-cycles` | List cycles for a group | Group participant | No |
| POST | `/groups/{groupId}/monthly-cycles` | Create or schedule a cycle manually where allowed | Organizer | Required |
| GET | `/monthly-cycles/{cycleId}` | Get cycle detail | Group participant | No |
| PATCH | `/monthly-cycles/{cycleId}` | Update editable cycle fields before close | Organizer | No |
| POST | `/monthly-cycles/{cycleId}/open` | Open scheduled cycle | Organizer or system actor | Required |
| POST | `/monthly-cycles/{cycleId}/close` | Close cycle after validations | Organizer | Required |
| POST | `/monthly-cycles/{cycleId}/cancel` | Cancel cycle before financial activity where allowed | Admin | Required |
| GET | `/monthly-cycles/{cycleId}/summary` | Get collection and payout summary | Group participant | No |

## 9. Installment APIs

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/installments` | List installments visible to actor | User | No |
| GET | `/installments/{installmentId}` | Get installment detail | Owner, organizer, or admin | No |
| GET | `/monthly-cycles/{cycleId}/installments` | List installments for cycle | Organizer or admin | No |
| POST | `/monthly-cycles/{cycleId}/installments/generate` | Generate installment obligations | Organizer or system actor | Required |
| POST | `/installments/{installmentId}/mark-paid` | Mark installment paid through approved manual path | Organizer with permission | Required |
| POST | `/installments/{installmentId}/waive` | Waive installment or penalty | Organizer or admin | Required |
| POST | `/installments/{installmentId}/dispute` | Raise dispute | Member or organizer | Required |
| POST | `/installments/{installmentId}/resolve-dispute` | Resolve installment dispute | Admin or organizer | Required |

## 10. Payment APIs

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/payments` | List payments visible to actor | User | No |
| POST | `/payments` | Create payment intent | User | Required |
| GET | `/payments/{paymentId}` | Get payment detail | Payer, organizer, finance, or admin | No |
| POST | `/payments/{paymentId}/confirm` | Confirm client-side payment handoff where provider requires it | User | Required |
| POST | `/payments/{paymentId}/cancel` | Cancel pending payment | Payer or finance | Required |
| POST | `/payments/{paymentId}/retry` | Create retry attempt for failed or expired payment | User | Required |
| POST | `/payments/{paymentId}/reconcile` | Trigger reconciliation for payment | Finance or admin | Required |
| GET | `/payments/{paymentId}/attempts` | List payment attempts | Payer, finance, or admin | No |
| POST | `/payment-provider-webhooks/{provider}` | Receive payment provider webhook | Provider-signed | Provider event ID |

## 11. Receipt APIs

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/receipts` | List receipts visible to actor | User | No |
| GET | `/receipts/{receiptId}` | Get receipt detail | Receipt owner, organizer, or admin | No |
| GET | `/payments/{paymentId}/receipt` | Get receipt for payment | Payer or admin | No |
| POST | `/receipts/{receiptId}/send` | Send or resend receipt | User or organizer | Required |
| POST | `/receipts/{receiptId}/cancel` | Cancel receipt due to approved correction | Finance or admin | Required |
| GET | `/receipts/{receiptId}/download` | Download receipt document | Receipt owner or admin | No |

## 12. Draw APIs

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/groups/{groupId}/draws` | List draws for group | Group participant | No |
| POST | `/monthly-cycles/{cycleId}/draws` | Schedule draw for cycle | Organizer | Required |
| GET | `/draws/{drawId}` | Get draw detail | Group participant | No |
| POST | `/draws/{drawId}/open` | Open draw | Organizer | Required |
| POST | `/draws/{drawId}/complete` | Complete draw and select winner | Organizer | Required |
| POST | `/draws/{drawId}/cancel` | Cancel draw | Admin or organizer | Required |
| POST | `/draws/{drawId}/approve-payout` | Approve payout from draw result | Organizer or finance | Required |

## 13. Auction APIs

Auction APIs are used for auction-based Bhishi payout models.

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/draws/{drawId}/auction-bids` | List auction bids for draw | Organizer or participant where rules allow | No |
| POST | `/draws/{drawId}/auction-bids` | Submit bid | Eligible group member | Required |
| GET | `/auction-bids/{bidId}` | Get bid detail | Bid owner or organizer | No |
| POST | `/auction-bids/{bidId}/withdraw` | Withdraw bid before auction close where allowed | Bid owner | Required |
| POST | `/auction-bids/{bidId}/accept` | Accept winning bid | Organizer | Required |
| POST | `/draws/{drawId}/auction/close` | Close auction round | Organizer | Required |

## 14. Notification APIs

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/notifications` | List notifications for current user | User | No |
| GET | `/notifications/{notificationId}` | Get notification detail | Recipient or admin | No |
| POST | `/notifications/{notificationId}/read` | Mark notification read | Recipient | Required |
| POST | `/notifications/read-all` | Mark all current user's notifications read | User | Required |
| GET | `/notification-preferences` | Get current user's notification preferences | User | No |
| PUT | `/notification-preferences` | Replace current user's notification preferences | User | Required |
| POST | `/notification-test-messages` | Send test message for admin verification | Admin | Required |
| GET | `/admin/notification-templates` | List templates | Admin | No |
| POST | `/admin/notification-templates` | Create template | Admin | Required |
| PATCH | `/admin/notification-templates/{templateId}` | Update template | Admin | No |

## 15. Dashboard APIs

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/dashboards/member` | Current member dashboard summary | Member | No |
| GET | `/dashboards/organizer` | Current organizer dashboard summary | Organizer | No |
| GET | `/dashboards/admin` | Tenant admin dashboard summary | Tenant admin | No |
| GET | `/dashboards/finance` | Finance operations summary | Finance | No |
| GET | `/groups/{groupId}/dashboard` | Group-specific dashboard | Group participant or admin | No |
| GET | `/monthly-cycles/{cycleId}/dashboard` | Cycle-specific collection dashboard | Organizer or admin | No |

## 16. Admin APIs

Admin endpoints are tenant-scoped unless explicitly platform-scoped.

| Method | Endpoint | Purpose | Auth | Idempotency |
| --- | --- | --- | --- | --- |
| GET | `/admin/tenants` | List tenants | Platform admin | No |
| POST | `/admin/tenants` | Create tenant | Platform admin | Required |
| GET | `/admin/tenants/{tenantId}` | Get tenant | Platform admin | No |
| PATCH | `/admin/tenants/{tenantId}` | Update tenant | Platform admin | No |
| POST | `/admin/tenants/{tenantId}/suspend` | Suspend tenant | Platform admin | Required |
| POST | `/admin/tenants/{tenantId}/activate` | Activate tenant | Platform admin | Required |
| GET | `/admin/users` | Search tenant users | Tenant admin or support | No |
| POST | `/admin/users` | Invite or create user | Tenant admin | Required |
| PATCH | `/admin/users/{userId}/roles` | Replace assigned roles | Tenant admin | Required |
| GET | `/admin/groups` | Search groups across tenant | Tenant admin | No |
| GET | `/admin/payments` | Search payments | Finance or admin | No |
| GET | `/admin/reconciliation-cases` | List reconciliation cases | Finance or admin | No |
| POST | `/admin/reconciliation-cases/{caseId}/resolve` | Resolve reconciliation case | Finance or admin | Required |
| GET | `/admin/audit-logs` | Search audit logs | Auditor or admin | No |
| GET | `/admin/activity-logs` | Search product activity logs | Admin or support | No |
| GET | `/admin/settings` | Get tenant settings | Tenant admin | No |
| PUT | `/admin/settings` | Replace tenant settings | Tenant admin | Required |

## 17. Error Response Format

All errors must use a consistent envelope.

| Field | Required | Purpose |
| --- | --- | --- |
| error.code | Yes | Stable machine-readable error code |
| error.message | Yes | Human-readable summary safe for clients |
| error.details | No | Field-level or domain-specific validation details |
| error.type | Yes | validation, authentication, authorization, conflict, rate_limit, provider, internal |
| meta.requestId | Yes | Request correlation ID |
| meta.timestamp | Yes | Server timestamp |

Common error codes:

| Code | Meaning |
| --- | --- |
| VALIDATION_FAILED | Request failed validation |
| AUTHENTICATION_REQUIRED | Missing or invalid authentication |
| ACCESS_DENIED | Authenticated actor lacks permission |
| RESOURCE_NOT_FOUND | Resource not found or not visible |
| RESOURCE_CONFLICT | Request conflicts with current state |
| IDEMPOTENCY_CONFLICT | Same idempotency key used with different request |
| RATE_LIMIT_EXCEEDED | Client exceeded allowed request rate |
| PAYMENT_PROVIDER_UNAVAILABLE | External payment provider unavailable |
| PAYMENT_STATUS_UNKNOWN | Payment requires reconciliation |
| INTERNAL_SERVER_ERROR | Unexpected server failure |

## 18. Validation Rules

Global validation:

- Reject unknown enum values.
- Reject malformed UUIDs.
- Reject invalid dates and timestamps.
- Reject negative money amounts unless an approved reversal type allows it.
- Reject unbounded list requests.
- Reject unsupported filters and sort fields.
- Validate tenant, group, member, and resource visibility before returning data.

Financial validation:

- Amounts are in paise.
- Currency must be explicit.
- Payment amount must match server-calculated obligation.
- Idempotency key is required for financial commands.
- Provider status must be verified before final settlement.

Lifecycle validation:

- Draft groups may be edited more freely than active groups.
- Active groups cannot have rule changes that invalidate completed cycles.
- Closed cycles cannot be mutated except through approved correction flows.
- Paid installments cannot be marked unpaid without correction and audit.

## 19. Pagination

Default style:

```text
?limit=25&cursor=opaqueCursor
```

Response pagination metadata:

| Field | Purpose |
| --- | --- |
| meta.page.limit | Requested page size |
| meta.page.nextCursor | Cursor for next page |
| meta.page.hasMore | Whether more records exist |

Rules:

- Default limit: 25.
- Maximum limit: 100 for app APIs.
- Maximum limit: 250 for approved admin APIs.
- Cursor pagination is preferred for high-volume resources.
- Offset pagination may be used only for small admin lookup tables.

## 20. Filtering

Filtering syntax:

```text
?status=active
?fromDate=2026-01-01
?toDate=2026-01-31
?groupId={groupId}
```

Rules:

- Filters must be whitelisted per endpoint.
- Date ranges must have maximum allowed windows for high-volume resources.
- Search endpoints must rate limit broad queries.
- Sensitive identifiers such as mobile number should use secure search rules, not direct broad matching.

Common filters:

| Filter | Purpose |
| --- | --- |
| status | Filter by lifecycle state |
| groupId | Restrict to group |
| cycleId | Restrict to monthly cycle |
| memberId | Restrict to member |
| fromDate | Inclusive start date |
| toDate | Inclusive end date |
| moduleType | Bhishi or future module |
| paymentStatus | Payment lifecycle state |

## 21. Sorting

Sorting syntax:

```text
?sort=createdAt:desc
?sort=dueDate:asc
```

Rules:

- Sort fields must be whitelisted.
- Default sort should be deterministic.
- Use `createdAt:desc` for activity-like resources.
- Use `dueDate:asc` for installment and obligation views.
- Add stable tie-breakers internally for cursor pagination.

Common sort fields:

| Field | Direction |
| --- | --- |
| createdAt | asc, desc |
| updatedAt | asc, desc |
| dueDate | asc, desc |
| paidAt | asc, desc |
| amount | asc, desc |
| status | asc, desc |

## 22. API Versioning

Version path:

```text
/api/v1
```

Rules:

- Breaking changes require a new major version.
- Backward-compatible optional fields may be added in the same version.
- Field removals, type changes, semantic changes, and enum meaning changes are breaking changes.
- Deprecated endpoints must include sunset documentation.
- Mobile clients must be supported for a defined version window.

Deprecation headers for future use:

| Header | Purpose |
| --- | --- |
| Deprecation | Indicates endpoint is deprecated |
| Sunset | Planned removal date |
| Link | Migration documentation |

## 23. Idempotency Rules

Required for:

- Group creation.
- Group activation, suspension, resume, completion, and cancellation.
- Member addition and removal.
- Installment generation.
- Manual payment recording.
- Payment creation, retry, cancel, and reconciliation.
- Receipt send and cancel.
- Draw and auction lifecycle commands.
- Notification bulk read or send operations.
- Admin tenant lifecycle changes.

Rules:

- Use `Idempotency-Key` header.
- Keys are scoped to actor, tenant, endpoint, and request body fingerprint.
- Replaying the same key with the same request returns the original result.
- Replaying the same key with a different request returns `IDEMPOTENCY_CONFLICT`.
- Idempotency records should be retained long enough for mobile retries and provider delays.

Provider webhooks:

- Use provider event ID as the idempotency identity.
- Duplicate webhook delivery must not duplicate payment, receipt, or ledger effects.

## 24. Rate Limiting Strategy

Rate limit dimensions:

- IP address.
- User ID.
- Tenant ID.
- Device fingerprint.
- Endpoint family.
- Provider webhook source.

High-protection endpoint families:

- OTP request and verification.
- Login and refresh.
- Payment creation.
- Payment webhook ingestion.
- Manual payment recording.
- Notification sending.
- Admin search.

Rate limit response:

| Header | Purpose |
| --- | --- |
| X-RateLimit-Limit | Allowed requests in window |
| X-RateLimit-Remaining | Remaining requests |
| X-RateLimit-Reset | Reset time |
| Retry-After | Required wait time after 429 |

## 25. HTTP Status Code Guidelines

| Status | Use |
| --- | --- |
| 200 | Successful read or command returning a resource |
| 201 | Resource created |
| 202 | Accepted for asynchronous processing |
| 204 | Successful command with no body |
| 400 | Malformed request |
| 401 | Missing, expired, or invalid authentication |
| 403 | Authenticated but forbidden |
| 404 | Resource missing or hidden by authorization |
| 409 | Conflict with current resource state |
| 410 | Resource intentionally no longer available |
| 422 | Valid syntax but failed domain validation |
| 429 | Rate limit exceeded |
| 500 | Unexpected server error |
| 502 | Upstream provider failure |
| 503 | Temporary service unavailable |

Guidance:

- Use 404 instead of 403 when revealing resource existence would leak data.
- Use 409 for duplicate active membership, duplicate idempotency conflicts, or invalid current state.
- Use 422 for business rule validation, such as activating a group with insufficient members.
- Use 202 for reconciliation, exports, bulk sends, and provider-dependent async work.

## 26. OpenAPI Folder Structure

OpenAPI specifications should be added later as contract files. Do not generate them until implementation planning begins.

Recommended future structure:

```text
contracts/
  openapi/
    v1/
      bachatsetu-api.yaml
      paths/
        auth.yaml
        users.yaml
        organizer.yaml
        member.yaml
        groups.yaml
        monthly-cycles.yaml
        installments.yaml
        payments.yaml
        receipts.yaml
        draws.yaml
        auctions.yaml
        notifications.yaml
        dashboards.yaml
        admin.yaml
      components/
        schemas.yaml
        parameters.yaml
        responses.yaml
        errors.yaml
        security.yaml
      examples/
        requests/
        responses/
```

Rules:

- OpenAPI must be reviewed before endpoint implementation.
- API contract changes must be backward-compatible unless versioned.
- Generated clients must not be manually edited.
- Examples must avoid real PII and financial data.

