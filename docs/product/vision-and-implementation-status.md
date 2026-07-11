# Vision and Implementation Status

## Purpose of This Document

BachatSetu's foundational documents — [`business-domain-design.md`](business-domain-design.md), [`system-architecture.md`](../architecture/system-architecture.md), [`rest-api-contract.md`](../api/rest-api-contract.md), and [`postgresql-database-architecture.md`](../database/postgresql-database-architecture.md) — were written **before implementation began**, as forward-looking specifications. Each says so explicitly ("It is intentionally documentation-only," "It does not contain Java code... or implementation DDL"). They describe the platform BachatSetu is designed to grow into.

Since those documents were written, `services/backend` (Spring Boot, Java 21) and `services/web` (Next.js 16, React 19) have been built out through Sprint 13.5 (backend) and Sprint FE-6 (frontend). Implementation did not follow the specification documents field-for-field: some concepts were simplified during construction, some were renamed, and some (a dedicated Ledger, a separate Payout aggregate, a Flutter mobile app, self-service tenant onboarding) were never built at all.

This document is the **single authoritative reconciliation** between the original vision and what actually runs in production today. Every other chapter in this Product Documentation set describes the *implemented* system and links back here whenever its terminology or scope diverges from the earlier specs. Where this document says a concept is **Not Implemented**, treat every mention of that concept elsewhere in the older `docs/` tree as aspirational, not current fact.

## How to Read the Status Markers

| Marker | Meaning |
| --- | --- |
| ✅ Implemented | Exists in `services/backend` and/or `services/web` today, verified against source code and the applied Flyway migrations (`V1`–`V14`). |
| 🟡 Partially Implemented | Some part of the concept exists, but materially narrower than the original spec describes. |
| ⛔ Not Implemented | Described in the pre-implementation specs but has no corresponding code, table, or endpoint. Tracked in [Roadmap and Future Work](roadmap-and-future-work.md).
| 🔁 Renamed / Restructured | The concept exists but under different terminology or a different shape than originally specified. |

## 1. Ubiquitous Language Reconciliation

[`business-domain-design.md` §1](business-domain-design.md#ubiquitous-language) defines the project's ubiquitous language. The table below maps each original term to what actually exists in code.

| Original Term (business-domain-design.md) | Status | Implementation Term | Notes |
| --- | --- | --- | --- |
| Tenant | ✅ Implemented | `Tenant` (`platform.tenants`) | Added late (migration `V14`); every other table's `tenant_id` has no database-level foreign key to it — tenancy is enforced only at the application layer (see [Security and Compliance](security-and-compliance.md)). |
| Community | ⛔ Not Implemented | — | No `community.communities` table, no `Community` aggregate. `community` is only a PostgreSQL **schema name** grouping group-related tables, not a business entity. |
| Member | 🔁 Renamed | `GroupMember` | There is no platform-wide `Member` entity independent of a group. Participation is modeled directly as `GroupMember`, a join between `User` and `Group`. |
| Organizer | ✅ Implemented | `GroupMember.roleInGroup = ORGANIZER` | Not a separate account type — any `User` who creates a group becomes its organizer via their `GroupMember` row. |
| Bhishi Group | 🔁 Renamed | `Group` (`community.groups`, `moduleType` column) | `Group` is generic and reusable across module types (`BHISHI`, `SELF_HELP_GROUP`, `SOCIETY_COLLECTION`, `COMMUNITY_FUND`), matching the "reusable structure for future modules" goal — but only `BHISHI` has a working product experience today. |
| Group Rule | 🔁 Restructured | Inline columns on `Group` | The spec describes a separate `group_rules` table/aggregate. The actual `community.groups` table holds contribution amount, frequency, duration, capacity, and payout method directly as columns — there is no separate rules aggregate or rule-versioning history. |
| Collection Cycle | 🔁 Renamed | `MonthlyCycle` (`community.monthly_cycles`) | Same concept, different name throughout the codebase. |
| Contribution Obligation | 🔁 Renamed | `Installment` (`community.installments`) | Same concept, different name. |
| Payment | ✅ Implemented | `Payment` (`finance.payments`) | Matches the spec closely, including idempotency and reconciliation-status fields. |
| Payout | 🟡 Partially Implemented | Fields on `Draw` (`payoutAmountPaise`, `selectedGroupMemberId`) | There is no independent `Payout` aggregate with its own approval/release workflow. A draw's payout is just two columns on the `Draw` record, set when the draw completes. |
| Ledger Account / Ledger Entry | ⛔ Not Implemented | — | No `finance.ledger_accounts` or `finance.ledger_entries` tables exist. `Payment` and `Receipt` records are the system's actual financial trail today; see [§5](#5-what-a-ledger-would-add). |
| Notification | ✅ Implemented | `Notification` (`notification.notifications`) | Matches the spec. |
| Audit Record | 🔁 Restructured | `AuditEntry` (`audit.audit_entries`) | An earlier table, `audit.audit_logs` (migration `V1`), was superseded in practice by `audit.audit_entries` (migration `V9`, with a JSONB `metadata` column and an evolving `event_type` enum now at 41 values). Both tables still exist in the schema; only `audit_entries` is written to by current application code. |
| Reconciliation | 🟡 Partially Implemented | `Payment.reconciliationStatus` column | The spec describes a full `Reconciliation Case` workflow with its own table and investigation lifecycle. The real implementation is a single status enum (`NOT_REQUIRED`, `PENDING`, `MATCHED`, `MISMATCHED`, `RESOLVED`) directly on `Payment` — there is no case-management UI or table. |

## 2. Bounded Context Reconciliation

[`business-domain-design.md` §3](business-domain-design.md#3-bounded-contexts) defines nine bounded contexts. The table below maps each to its actual backend module (Java package under `in.bachatsetu.backend`).

| Original Bounded Context | Status | Actual Module(s) |
| --- | --- | --- |
| Identity and Access Context | ✅ Implemented | `auth`, `user` |
| Tenant and Community Context | 🟡 Partially Implemented | `platformoperations` (tenant lifecycle only — suspend/activate/archive by a platform admin; no self-service tenant onboarding, no Community concept) |
| Member Context | 🔁 Restructured | Folded into `group` and `member` (no standalone member profile outside group participation) |
| Bhishi Context | ✅ Implemented | `group`, `member`, `invitation` |
| Payment Context | ✅ Implemented | `payment`, `paymentgateway` |
| Ledger Context | ⛔ Not Implemented | — |
| Notification Context | ✅ Implemented | `notification`, plus scheduled reminders in `automation` |
| Audit and Compliance Context | ✅ Implemented | `audit` |
| Reporting Context | 🟡 Partially Implemented | `admin` (statistics), `admin.analytics` (trend/distribution analytics) — read-only dashboards exist; there is no export-job infrastructure |

Two modules exist in the real system with **no equivalent bounded context** in `business-domain-design.md`, because they were identified as necessary during implementation rather than anticipated up front:

| Module | Purpose | Why it wasn't anticipated |
| --- | --- | --- |
| `storage` | Stores uploaded files (profile photos, generated receipt PDFs) behind a provider-agnostic port, with `LOCAL`, `AWS_S3`, `AZURE_BLOB`, and `GOOGLE_CLOUD_STORAGE` adapters | File storage wasn't called out as a distinct context in the original design |
| `support` | Tenant-scoped support ticket intake and triage (`SupportTicket` aggregate) | The original design mentions a "Support Operator" role and journey but no dedicated ticketing system |
| `draw`, `auction` | Payout-selection mechanics (random/fixed-rotation draws, auction-style bidding) | These were treated as part of the Bhishi context conceptually, but are implemented as their own top-level modules with their own REST controllers |
| `dashboard` | Aggregated read-only summaries for the member and organizer home screens | A cross-cutting query module with no equivalent bounded context in the original design — it composes data from `group`, `payment`, and `notification` rather than owning any data itself |
| `platformconfig` (implemented as `admin.configuration`) | Global platform configuration singleton, feature flags, and system limits | Not anticipated as its own context; ended up living inside the `admin` module rather than as a peer bounded context |

## 3. MVP Scope Reconciliation

[`business-domain-design.md` §10](business-domain-design.md#10-mvp-scope) lists what was planned for MVP. The table below marks the actual build status of every item, as of Sprint 13.5 (backend) / Sprint FE-6 (frontend).

| Planned MVP Item | Status |
| --- | --- |
| User authentication foundation | ✅ Implemented — mobile-number + OTP, JWT access/refresh tokens with rotation and reuse detection |
| Tenant setup | 🟡 Partially Implemented — platform admins can suspend/activate/archive a tenant; there is no tenant self-onboarding flow |
| Basic roles and permissions | ✅ Implemented — `identity.roles`, `identity.permissions`, `identity.user_roles`, `identity.role_permissions`, seeded with `PLATFORM_ADMIN`, `SUPPORT_OPERATOR`, `TENANT_ADMIN`, `GROUP_ORGANIZER`, `GROUP_MEMBER` |
| Member profile management | ✅ Implemented — signup, OTP verification, and a profile-onboarding step (city, state, photo, notification preference) |
| Bhishi group creation | ✅ Implemented |
| Group member management | ✅ Implemented — including QR/code/link invitations |
| Contribution schedule setup | ✅ Implemented — contribution amount, frequency, duration, capacity, payout method are all set at group creation |
| Collection cycle tracking | ✅ Implemented (`MonthlyCycle`) |
| Contribution due tracking | ✅ Implemented (`Installment`) |
| Manual payment recording with audit | 🟡 Partially Implemented — payments can be recorded and are audited, but there is no dedicated "organizer records a manual/cash payment on a member's behalf" workflow; all payment creation is currently payer-initiated |
| Payment provider integration | ✅ Implemented — Razorpay, Stripe, and Cashfree adapters behind a common gateway port, with webhook ingestion |
| Basic ledger records | ⛔ Not Implemented (see [§1](#1-ubiquitous-language-reconciliation)) |
| Member reminders | ✅ Implemented — scheduled installment-due reminders via the `automation` module |
| Organizer dashboard | ✅ Implemented |
| Admin dashboard | ✅ Implemented — Platform Dashboard, Analytics, User/Group/Tenant Management, Configuration, Monitoring, Support |
| Audit trail for sensitive actions | ✅ Implemented |
| Basic reports | 🟡 Partially Implemented — Admin Analytics provides read-only summary cards, trend charts, and distributions; there is no export/download capability |

## 4. System Architecture Reconciliation

[`system-architecture.md` §"Core Domains"](../architecture/system-architecture.md#core-domains) lists ten domains. Reconciliation:

| Spec Domain | Status | Notes |
| --- | --- | --- |
| Identity | ✅ Implemented | |
| Tenant | 🟡 Partially Implemented | See §2, §3 above |
| Member | 🔁 Restructured | See §1 above |
| Bhishi | ✅ Implemented | |
| Wallet | ⛔ Not Implemented | No user-visible balance/wallet concept exists; a member's financial position is read directly off `Installment` and `Payment` records, not a derived balance |
| Ledger | ⛔ Not Implemented | See §5 |
| Payment | ✅ Implemented | |
| Notification | ✅ Implemented | |
| Audit | ✅ Implemented | |
| Reporting | 🟡 Partially Implemented | See §2 above |

The spec's ["High-Level Components"](../architecture/system-architecture.md#high-level-components) diagram shows a **Mobile App** and an **Admin Portal** as separate clients in front of the same API Gateway. The actual system has **one** web client (`services/web`, Next.js) serving marketing pages, member/organizer dashboards, and the admin portal from a single deployable app, gated by route protection and role checks rather than being separate applications. No mobile app (Flutter or otherwise) has been built — see [Roadmap and Future Work](roadmap-and-future-work.md).

The layered-architecture pattern described in the spec (`API Controller → Application Service → Domain Model → Repository Interface → Persistence Adapter`) **is** exactly what every backend module follows — this part of the spec was implemented faithfully and is documented in detail in [System Architecture and Modules](system-architecture-and-modules.md).

## 5. What a Ledger Would Add

Because "no Ledger" is the single biggest gap between vision and reality, it is worth being precise about the consequence: today, `Payment.status = VERIFIED` **is** the system's source of financial truth for "did this contribution get paid." There is no independent, append-only accounting trail that a Payment/Receipt pair is checked against. This is acceptable for the platform's current scale and MVP scope, but it means:

- There is no way to represent a partial refund, a fee adjustment, or a correction as a *new* record without mutating the original `Payment` row's status.
- There is no per-member or per-group running balance computed from first principles — every "how much has this member paid so far" view is computed on demand from `Installment.paidAmountPaise`.
- Financial reconciliation (`Payment.reconciliationStatus`) flags a mismatch but does not produce a structured investigation record.

This is flagged as the top item in [Roadmap and Future Work](roadmap-and-future-work.md).

## 6. API Contract Reconciliation

[`rest-api-contract.md`](../api/rest-api-contract.md) specifies an idealized, fully resource-oriented API (separate `/organizer/*` and `/member/*` namespaces, `/monthly-cycles/{cycleId}/installments/generate`, `/payments/{paymentId}/attempts`, a full `/admin/reconciliation-cases` sub-resource, cursor-based pagination everywhere). The real API, cataloged endpoint-by-endpoint in [Backend Module and API Reference](backend-module-and-api-reference.md), differs in three consistent ways:

1. **Fewer, coarser endpoints.** Where the spec proposes a full CRUD + lifecycle-command resource family per concept, the real API often exposes only the operations actually needed by a screen — e.g., there is one `GET /api/v1/dashboard/organizer` that returns a composed summary, rather than the spec's six separate dashboard endpoints.
2. **Offset pagination, not cursor pagination.** Real list endpoints (`GET /api/v1/admin/users`, `/admin/groups`, `/platform-operations/tenants`, `/audit`) use `page`/`size` query parameters and return `PageResponse<T>` (`content`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`), not the spec's `?limit=25&cursor=...` shape.
3. **No `Idempotency-Key` header anywhere.** Idempotency for payment creation is instead enforced server-side via `Payment.idempotencyKeyHash`, computed from the request body rather than a client-supplied header. No endpoint in the real system reads an `Idempotency-Key` request header.

The spec's principles that **were** followed faithfully: RFC-7807-style problem-detail error responses (`error.code`-equivalent via a typed `ApiError` on the frontend), `/api/v1` versioning, bearer-token authentication, and role-based authorization enforced via Spring Security `@PreAuthorize`.

## 7. Database Schema Reconciliation

[`postgresql-database-architecture.md` §5](../database/postgresql-database-architecture.md#5-complete-entity-list) lists a "Complete Entity List" of 27 tables across five schemas. The verified, actually-applied schema (14 Flyway migrations, `V1`–`V14`) has 20 tables across nine schemas. Full detail is in [Data Model and Database Schema](data-model-and-database-schema.md); the headline differences:

| Spec Table | Status |
| --- | --- |
| `platform.tenant_settings`, `platform.system_settings` | ⛔ Not Implemented — `config.platform_configuration` (a singleton row) plus `config.feature_flags` and `config.platform_limits` cover global configuration instead; there is no per-tenant settings table |
| `identity.user_profiles` | ⛔ Not Implemented — profile fields (`city`, `state`, `photo_file_id`, `notifications_enabled`, `onboarded`) were added directly to `identity.users` instead |
| `identity.user_devices` | ⛔ Not Implemented — no device-registration/trust concept exists |
| `identity.otp_challenges` | 🔁 Renamed — `identity.otp_verifications` |
| `identity.login_sessions` | 🔁 Renamed — `identity.refresh_tokens` |
| `community.communities`, `community.group_rules` | ⛔ Not Implemented — see §1 |
| `finance.payment_attempts`, `finance.ledger_accounts`, `finance.ledger_entries`, `finance.reconciliation_cases` | ⛔ Not Implemented — see §1, §5 |
| `notification.notification_templates`, `notification.notification_delivery_attempts` | ⛔ Not Implemented — templates are rendered in application code, not stored as data; delivery is fire-and-forget with a single `status` column on `Notification` itself |
| `audit.activity_logs` | ⛔ Not Implemented — `audit.audit_entries` serves both the compliance-audit and product-activity-timeline purposes described separately in the spec |

Tables that exist in the real schema with **no equivalent** in the original spec at all: `community.group_invitations` (QR/code/link invitations), `community.membership_history`, `finance.payment_gateway_orders`, `storage.stored_files`, `support.support_tickets`, `platform.announcements`.

## 8. Reading This Document Alongside the Rest of the Set

Every subsequent chapter in this Product Documentation set — [System Architecture and Modules](system-architecture-and-modules.md), [Data Model and Database Schema](data-model-and-database-schema.md), [Backend Module and API Reference](backend-module-and-api-reference.md), [Business Processes](business-processes.md), [State Machines](state-machines.md), [Frontend Experience](frontend-experience.md), [Security and Compliance](security-and-compliance.md), and [Non-Functional Requirements and Production Readiness](non-functional-and-production-readiness.md) — describes the **implemented system only**, using implementation terminology (the right-hand column of the tables above), and links back to this document rather than repeating these reconciliation notes. [Roadmap and Future Work](roadmap-and-future-work.md) is where every ⛔ and 🟡 item above is tracked going forward.
