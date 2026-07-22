# Backend Module and API Reference

> **Audience:** Developers, QA Engineers
> **Prerequisite reading:** [System Architecture and Modules](system-architecture-and-modules.md), [Data Model and Database Schema](data-model-and-database-schema.md)

Every endpoint below was read directly from the `@RequestMapping`/`@GetMapping`/`@PostMapping`/`@PatchMapping`/`@DeleteMapping` annotations in `services/backend/src/main/java/in/bachatsetu/backend/**/interfaces/rest/controller/`. All paths are relative to the API base `http://localhost:8080` (local) — no `/api/v1` prefix is added twice; it is already part of every path shown. For request/response field-level detail and business rules per module, see the deep-dive documents under `services/backend/docs/application/` linked from each section. Live, always-current API documentation is also available at `/swagger-ui/index.html` when the backend is running (per `services/backend/README.md`).

**Auth column key:** *Public* = no bearer token required. *User* = any authenticated user. *Owner* = authenticated user acting on their own resource. *Organizer* = a `GroupMember` with `roleInGroup = ORGANIZER`. *PLATFORM_ADMIN* = a user holding the seeded `PLATFORM_ADMIN` role, enforced via Spring Security `@PreAuthorize("hasRole('PLATFORM_ADMIN')")`. *Provider* = signed webhook call from a payment gateway, not a logged-in user. *Refresh token* = no bearer access token; the request body itself carries the opaque refresh token being acted on.

## Authentication and Signup — `auth`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/otp/request` | Request an OTP for a given purpose (registration, sign-in, password reset, mobile change) | Public |
| POST | `/api/v1/auth/otp/verify` | Verify an OTP and (for sign-in) receive access/refresh tokens | Public |
| POST | `/api/v1/auth/otp/resend` | Resend an OTP for a pending challenge | Public |
| POST | `/api/v1/auth/otp/invalidate` | Invalidate a pending OTP challenge | Public |
| POST | `/api/v1/auth/signup` | Start signup: create a `PENDING_VERIFICATION` user and send the first OTP | Public |
| POST | `/api/v1/auth/signup/verify` | Verify signup OTP and receive the initial token pair | Public |
| POST | `/api/v1/auth/login/start` | Returning-user login: look up an `ACTIVE` user by mobile number and send a `SIGN_IN` OTP (Sprint LR-2) | Public |
| POST | `/api/v1/auth/login/verify` | Verify login OTP and receive an access/refresh token pair (Sprint LR-2) | Public |
| POST | `/api/v1/auth/token/refresh` | Rotate a refresh token and issue a new access/refresh token pair (Sprint LR-2, wires the Sprint 8.6 refresh-token layer) | Refresh token |
| POST | `/api/v1/auth/logout` | Revoke a refresh token, ending the session (Sprint LR-2) | Refresh token |

See [`login.md`](../../services/backend/docs/application/login.md) for the full returning-user login and session-management flow.

## Profile Onboarding — `user`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/users/me/onboarding` | Complete profile onboarding (city, state, photo, notification preference); sets `users.onboarded = true` | Owner |

## Savings Groups — `group`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/groups` | Create a group | User (becomes organizer) |
| GET | `/api/v1/groups/{groupId}` | Get one tenant-scoped group | Group participant |
| GET | `/api/v1/groups` | List groups | User |
| PATCH | `/api/v1/groups/{groupId}/activate` | Activate an inactive/suspended group | Organizer |
| PATCH | `/api/v1/groups/{groupId}/suspend` | Suspend an active group | Organizer |
| PATCH | `/api/v1/groups/{groupId}/close` | Permanently close a group | Organizer |
| POST | `/api/v1/groups/{groupId}/members` | Add a member to a group | Organizer |
| DELETE | `/api/v1/groups/{groupId}/members/{memberId}` | Remove a member from a group | Organizer |

## Members — `member`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/members` | Create a member record | User |
| GET | `/api/v1/members/{memberId}` | Get one member | Owner, organizer, or admin |
| GET | `/api/v1/members` | List members | User |
| GET | `/api/v1/members/{memberId}/participations` | List a member's group participations | Owner or organizer |
| POST | `/api/v1/members/{memberId}/participations` | Record a new group participation for a member | Organizer |
| PATCH | `/api/v1/members/{memberId}` | Update member metadata | Owner or organizer |

## Group Invitations and Join — `invitation`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/groups/{groupId}/invite` | Create (or reissue) the group's active invitation (QR/code/link) | Organizer |
| GET | `/api/v1/groups/{groupId}/invite` | Get the group's current active invitation | Organizer |
| DELETE | `/api/v1/groups/{groupId}/invite` | Revoke the active invitation | Organizer |
| GET | `/api/v1/join/{token}` | Public preview of an invitation by its secure token (link/QR join) | Public |
| POST | `/api/v1/groups/join` | Join a group using an invitation code or token | User |

## Payments — `payment`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/payments` | Create a payment (idempotent — see `idempotency_key_hash` in [Data Model](data-model-and-database-schema.md#5-schema-finance)) | Owner (payer) |
| GET | `/api/v1/payments/{paymentId}` | Get one payment | Payer, organizer, or admin |
| GET | `/api/v1/payments` | List payments | User |
| PATCH | `/api/v1/payments/{paymentId}/status` | Transition a payment's status (e.g. to `VERIFIED`, `FAILED`, `REFUNDED`) | Organizer or admin |

## Collection — `payment` (Collection endpoints)

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| GET | `/api/v1/groups/{groupId}/collection` | Get the group's current-cycle contribution collection summary (per-member paid/pending status, percent collected) | Group participant |
| POST | `/api/v1/groups/{groupId}/collection/members/{memberId}/mark-paid` | Organizer records a member's current-cycle contribution as collected in cash, on the member's behalf | Organizer |

## Payment Gateway Integration — `paymentgateway`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/payments/{paymentId}/gateway-orders` | Create a gateway order for a payment against the configured provider | Owner |
| POST | `/api/v1/payments/{paymentId}/gateway-orders/sync` | Sync the local payment status with the gateway's current order status | Owner |
| POST | `/api/v1/payments/{paymentId}/refunds` | Initiate a refund through the gateway | Organizer or admin |
| POST | `/api/v1/payments/webhooks/razorpay` | Razorpay webhook ingestion | Provider (signature-verified) |
| POST | `/api/v1/payments/webhooks/stripe` | Stripe webhook ingestion | Provider (signature-verified) |
| POST | `/api/v1/payments/webhooks/cashfree` | Cashfree webhook ingestion | Provider (signature-verified) |

## Draws — `draw`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/draws` | Schedule a draw for a cycle | Organizer |
| GET | `/api/v1/draws/{drawId}` | Get one draw | Group participant |
| GET | `/api/v1/draws` | List draws | User |
| PATCH | `/api/v1/draws/{drawId}/conduct` | Open/conduct the draw (random or fixed-rotation selection) | Organizer |
| PATCH | `/api/v1/draws/{drawId}/close` | Close the draw, recording the selected winner and payout amount | Organizer |

## Auctions — `auction`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/auctions` | Start an auction-type payout round for a draw | Organizer |
| POST | `/api/v1/auctions/{auctionId}/bids` | Submit a bid | Eligible group member |
| POST | `/api/v1/auctions/{auctionId}/close` | Close the auction and settle the winning bid | Organizer |
| GET | `/api/v1/auctions` | List auctions | User |
| GET | `/api/v1/auctions/{auctionId}` | Get one auction | Group participant |
| GET | `/api/v1/auctions/{auctionId}/winner` | Get the auction's winning bid | Group participant |

## Receipts — `receipt`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/receipts` | Generate a receipt for a verified payment | System / organizer |
| GET | `/api/v1/receipts/{receiptId}` | Get one receipt | Owner or admin |
| GET | `/api/v1/receipts` | List receipts | User |
| GET | `/api/v1/receipts/{receiptId}/pdf` | Stream the receipt as a rendered PDF | Owner or admin |
| GET | `/api/v1/receipts/{receiptId}/pdf/storage-url` | Get a storage-backed download URL for the receipt PDF | Owner or admin |

## Notifications — `notification`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/notifications` | Queue a notification | System |
| GET | `/api/v1/notifications/{notificationId}` | Get one notification | Recipient or admin |
| GET | `/api/v1/notifications` | List notifications | User |
| PATCH | `/api/v1/notifications/{notificationId}/delivered` | Mark a notification delivered (provider callback) | System |
| PATCH | `/api/v1/notifications/{notificationId}/failed` | Mark a notification failed (provider callback) | System |

`automation` schedules reminder notifications on a cron trigger and has **no REST controller of its own** — it calls into `notification` and `payment`/`group` read ports directly.

## File Storage — `storage`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/storage/files` | Upload a file (multipart) | User |
| GET | `/api/v1/storage/files/{fileId}` | Get file metadata | Owner or admin |
| GET | `/api/v1/storage/files/{fileId}/download` | Download the raw file bytes | Owner or admin |
| DELETE | `/api/v1/storage/files/{fileId}` | Delete a file | Owner or admin |

## Audit Trail — `audit`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/audit` | Manually record an audit entry for the caller's own tenant | User |
| GET | `/api/v1/audit/{auditId}` | Get one tenant-scoped audit entry | User (own tenant only) |
| GET | `/api/v1/audit` | Search audit entries — **always scoped to the caller's own tenant**, page by page; filters: `actor`, `module`, `event`, `dateFrom`, `dateTo` | User (own tenant only) |

There is **no cross-tenant audit search endpoint** — even a `PLATFORM_ADMIN` calling this endpoint only sees their own tenant's entries. This is a known, documented limitation surfaced honestly in the Admin Portal's Monitoring screen (see [Frontend Experience](frontend-experience.md)).

## Composed Dashboards — `dashboard`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| GET | `/api/v1/dashboard/member` | Composed summary for the member home screen: current group, recent notifications, upcoming draw | User (any user with a member profile — including organizers viewing their own membership) |
| GET | `/api/v1/dashboard/organizer` | Composed summary for the organizer home screen: every group the caller owns, with member counts, invitation status, next draw, and contribution progress per group | Organizer |

## Platform Administration — `admin`

Every endpoint below requires the `PLATFORM_ADMIN` role.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/admin/statistics` | Platform-wide totals (users, groups, payments, receipts, notifications, files), computed on demand |
| GET | `/api/v1/admin/users` | Search users across every tenant, paginated; filters: `status`, `email`, `phone`, `createdAfter`, `createdBefore` |
| GET | `/api/v1/admin/groups` | Search groups across every tenant, paginated; filters: `status`, `createdAfter`, `createdBefore` |
| GET | `/api/v1/admin/tenants` | List tenants with per-tenant user/group counts, paginated |
| POST | `/api/v1/admin/users/{id}/enable` | Enable a platform user, across any tenant |
| POST | `/api/v1/admin/users/{id}/disable` | Disable a platform user, across any tenant |

### Analytics — `admin.analytics`

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/admin/analytics/overview` | Platform-wide overview snapshot |
| GET | `/api/v1/admin/analytics/payments` | Payment analytics, including a 30-day trend |
| GET | `/api/v1/admin/analytics/groups` | Savings-group analytics (average members, contribution, draw completion rate, monthly new-group trend) |
| GET | `/api/v1/admin/analytics/users` | User analytics (monthly registrations, preferred-language distribution, users per tenant) |
| GET | `/api/v1/admin/analytics/storage` | Storage analytics, including a 30-day upload trend |
| GET | `/api/v1/admin/analytics/notifications` | Notification analytics (delivery-status and type distributions) |

### Platform Configuration — `admin.configuration`

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/admin/config` | Get the current platform configuration and maintenance-mode state |
| PUT | `/api/v1/admin/config` | Full-replace update of platform configuration |
| GET | `/api/v1/admin/config/feature-flags` | List every feature flag's enable/disable state |
| PUT | `/api/v1/admin/config/feature-flags` | Partial update — only the keys present in the request body change |
| GET | `/api/v1/admin/config/limits` | List every configurable platform-wide ceiling |
| PUT | `/api/v1/admin/config/limits` | Partial update — only the keys present in the request body change |

Maintenance mode and feature flags set here are enforced platform-wide by dedicated Spring Security filters (`MaintenanceModeFilter`, `FeatureFlagEnforcementFilter`) that reject non-admin requests while active — see [System Architecture and Modules §8](system-architecture-and-modules.md#8-configuration-and-feature-gating).

## Platform Operations — `platformoperations`

Every endpoint below requires the `PLATFORM_ADMIN` role, except where noted.

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/v1/platform-operations/overview` | Platform-wide totals plus today's activity (signups, payments, new groups, notifications, uploads) |
| GET | `/api/v1/platform-operations/tenants` | List every known tenant with statistics and lifecycle status, paginated; filter: `status` |
| GET | `/api/v1/platform-operations/tenants/{tenantId}` | Get statistics and lifecycle status for one tenant |
| POST | `/api/v1/platform-operations/tenants/{tenantId}/suspend` | Suspend a tenant (requires a reason) |
| POST | `/api/v1/platform-operations/tenants/{tenantId}/activate` | Reactivate a suspended tenant |
| POST | `/api/v1/platform-operations/tenants/{tenantId}/archive` | Archive a tenant (terminal state) |
| GET | `/api/v1/platform-operations/health` | Database/storage/notification component health plus JVM and host runtime facts (distinct from the public, unauthenticated `/actuator/health`) |
| POST | `/api/v1/platform-operations/broadcast` | Send a broadcast notification, scoped to `ALL_USERS`, one `TENANT`, `ORGANIZERS`, or `MEMBERS` |
| GET | `/api/v1/platform-operations/announcements` | List every announcement, paginated |
| POST | `/api/v1/platform-operations/announcements` | Publish a platform-wide announcement |
| GET | `/api/v1/platform-operations/announcements/active` | List currently active announcements — **any authenticated user**, not just admins |

## Support Tickets — `support`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| POST | `/api/v1/support/tickets` | Raise a support ticket | User |
| GET | `/api/v1/support/tickets/{ticketId}` | Get one ticket | Ticket owner or support operator |
| GET | `/api/v1/support/tickets` | Search tickets, paginated | Support operator |
| POST | `/api/v1/support/tickets/{ticketId}/assign` | Assign a ticket to an operator | Support operator |
| POST | `/api/v1/support/tickets/{ticketId}/resolve` | Resolve a ticket (requires a resolution note) | Support operator |
| POST | `/api/v1/support/tickets/{ticketId}/close` | Close a resolved ticket | Support operator |

## Error Response Format

Every error follows the RFC 7807 "problem detail" convention (`application/problem+json`), which the frontend decodes into a typed `ApiError` carrying `code`, `message`, and field-level `violations` (see [Frontend Experience](frontend-experience.md)). This is narrower than the bespoke `error.code`/`error.type`/`meta.requestId` envelope proposed in [`rest-api-contract.md` §17](../api/rest-api-contract.md#17-error-response-format), which was not implemented as specified — see [Vision and Implementation Status §6](vision-and-implementation-status.md#6-api-contract-reconciliation).

## Pagination Format

Every paginated list endpoint above returns the same shape:

```json
{
  "content": [ /* items */ ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7,
  "hasNext": true,
  "hasPrevious": false
}
```

Requested via `page` (zero-based) and `size` query parameters — offset pagination, not the cursor-based `?limit=25&cursor=...` scheme proposed in the original API contract spec.

## Next Chapter

[Business Processes](business-processes.md) shows these endpoints composed into the real end-to-end flows a user or administrator actually walks through.
