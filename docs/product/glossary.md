# Glossary

> **Audience:** Everyone. This is the single reference point for every term used across the Product Documentation set.

Terms marked **(design term)** appear in the pre-implementation specifications ([`business-domain-design.md`](business-domain-design.md), [`system-architecture.md`](../architecture/system-architecture.md)) but are not the term used in running code — see [Vision and Implementation Status](vision-and-implementation-status.md) for the full reconciliation. All other terms are the actual implementation vocabulary, used consistently across this documentation set.

| Term | Definition |
| --- | --- |
| **Announcement** | A platform-wide, time-windowed broadcast message (`platform.announcements`) published by a platform administrator and visible to any authenticated user while active. Not tenant-scoped. |
| **Audit Entry** | An immutable, append-only record of a sensitive action (`audit.audit_entries`), tagged with an `event_type` from a 41-value enum. Search is always scoped to the caller's own tenant. |
| **Auction** | A payout-selection mechanism where eligible group members submit bids (a discount on the payout amount) to receive the cycle's payout early; the organizer closes the auction to settle a winner. |
| **Auction Bid** | One member's bid within an auction (`community.auction_bids`), belonging to exactly one `Draw`. |
| **Bhishi** | A recurring community savings arrangement (also called a Committee or ROSCA) where members contribute a fixed amount per cycle and one member receives the pooled payout per cycle, in turn. BachatSetu's first and currently only fully-supported product module. |
| **Bhishi Group (design term)** | See **Group**. |
| **Bounded Context** | A Domain-Driven Design term for a clearly-scoped area of the business domain with its own model and language; see [System Architecture and Modules §4](system-architecture-and-modules.md#4-backend-module-inventory) for how contexts map to actual backend modules. |
| **Collection Cycle (design term)** | See **Monthly Cycle**. |
| **Community (design term)** | A real-world social grouping container envisioned as a parent of multiple Groups. **Not implemented** — no `Community` entity or table exists. |
| **Contribution Obligation (design term)** | See **Installment**. |
| **Draw** | The event that selects which group member receives a cycle's payout, either by random/fixed-rotation selection or by resolving an auction (`community.draws`). Exactly one draw exists per `Monthly Cycle`. |
| **Feature Flag** | A platform-wide, database-backed on/off toggle (`config.feature_flags`) for one of nine subsystems (`AUTHENTICATION`, `PAYMENTS`, `NOTIFICATIONS`, `STORAGE`, `RECEIPTS`, `AUCTION`, `ANALYTICS`, `AUDIT`, `SIGNUP`), editable by a platform administrator through the Admin Portal's Configuration screen. |
| **Group** | The aggregate root for a savings group (`community.groups`), generic across module types though only `BHISHI` has a working product experience today. Java class name: `SavingsGroup`. Holds contribution amount, frequency, duration, capacity, and payout method directly as columns. |
| **Group Invitation** | A QR-code, short alphanumeric code, or shareable-link invitation issued by an organizer (`community.group_invitations`). At most one `ACTIVE` invitation exists per group at a time. |
| **Group Member** | A `User`'s participation in one `Group` (`community.group_members`), with a role (`ORGANIZER`, `CO_ORGANIZER`, `MEMBER`) and a status. There is no platform-wide "Member" entity independent of this join. |
| **Group Rule (design term)** | A separate rules aggregate envisioned in the original design. **Not implemented as a separate table** — rule fields live directly on `Group`. |
| **Installment** | What one `Group Member` owes for one `Monthly Cycle` (`community.installments`) — expected amount, paid amount, penalty, due date, and status. Exactly one per member per cycle. |
| **Ledger Account / Ledger Entry (design terms)** | An append-only accounting model envisioned in the original design. **Not implemented** — see [Roadmap and Future Work §2](roadmap-and-future-work.md#2-the-ledger-gap). |
| **Member (design term)** | A platform-wide participant entity independent of any group. Not implemented as a standalone entity — see **Group Member**. |
| **Module Type** | The `Group.moduleType` column distinguishing product lines: `BHISHI`, `SELF_HELP_GROUP`, `SOCIETY_COLLECTION`, `COMMUNITY_FUND`. Only `BHISHI` has a built frontend experience. |
| **Monthly Cycle** | One scheduled contribution period for a group (`community.monthly_cycles`), moving through `SCHEDULED → OPEN → COLLECTION_DUE → PAYOUT_PENDING → CLOSED` (or `CANCELLED`). |
| **Notification** | An outbound message to a user over one channel (`EMAIL`, `SMS`, `WHATSAPP`, `PUSH`) — `notification.notifications`. Rendered in application code at send time; not backed by a stored template table. |
| **Organizer** | Not a separate account type — any `User` whose `Group Member` row has `roleInGroup = ORGANIZER` in a given group. |
| **OTP Verification** | A one-time-password challenge (`identity.otp_verifications`) for registration, sign-in, password reset, or mobile-number change. Only a bcrypt hash of the code is ever stored. |
| **Payment** | A payer's payment intent/attempt (`finance.payments`), independent of which gateway processed it. The system's actual financial source of truth today, in the absence of a Ledger. |
| **Payment Gateway Order** | The gateway-specific order created against Razorpay, Stripe, or Cashfree for one `Payment` (`finance.payment_gateway_orders`). One-to-one with `Payment`. |
| **Payout (design term)** | An independent aggregate with its own approval/release workflow, as originally envisioned. **Not implemented** — the real system only has `Draw.payoutAmountPaise` and `Draw.selectedGroupMemberId` as fields. |
| **PLATFORM_ADMIN** | The seeded platform-scoped role granting access to every endpoint under `admin`, `admin.analytics`, `admin.configuration`, and `platformoperations`, and to the `/dashboard/admin/*` routes in the frontend. |
| **Platform Administrator** | The persona holding the `PLATFORM_ADMIN` role; operates tenants, configuration, analytics, monitoring, and support at the platform level. See [Frontend Experience §1](frontend-experience.md#1-personas). |
| **Receipt** | The confirmation document generated automatically when a `Payment` is verified (`finance.receipts`). One receipt per payment; downloadable as a rendered PDF. |
| **Reconciliation Status** | A status column directly on `Payment` (`NOT_REQUIRED`, `PENDING`, `MATCHED`, `MISMATCHED`, `RESOLVED`) tracking whether the internal and provider-reported payment status agree. Narrower than the originally-envisioned `Reconciliation Case` workflow. |
| **RoleGuard** | A frontend-only React component that blocks navigation into `/dashboard/admin/*` for sessions lacking the `PLATFORM_ADMIN` role, decoded from the JWT. A UX convenience, not the actual authorization boundary — see [Security and Compliance §3](security-and-compliance.md#3-authorization). |
| **Stored File** | Any uploaded or generated file (profile photo, rendered receipt PDF) behind a provider-agnostic storage port (`storage.stored_files`), supporting `LOCAL`, `AWS_S3`, `AZURE_BLOB`, and `GOOGLE_CLOUD_STORAGE` adapters. |
| **Support Ticket** | A tenant-scoped support request (`support.support_tickets`), moving through `OPEN → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED`. |
| **Tenant** | The top-level SaaS customer boundary (`platform.tenants`). Every business table's `tenant_id` refers to this table's `id`, but with no database-level foreign key — tenancy is enforced entirely in the application layer. |
| **User** | Any login-capable person (`identity.users`), authenticated by mobile number + OTP (no password-based login exists in the running product). Carries both a legacy `status` column and the operative `auth_status` column — see [State Machines — User Authentication Status](state-machines.md#user-authentication-status). |
| **Wallet (design term)** | A user-visible balance concept from the original architecture spec. **Not implemented** — a member's financial position is read directly from `Installment` and `Payment` records, not a derived balance. |

For every full entity's complete column list, see [Data Model and Database Schema](data-model-and-database-schema.md). For every REST endpoint, see [Backend Module and API Reference](backend-module-and-api-reference.md).
