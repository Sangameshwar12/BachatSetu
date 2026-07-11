# Roadmap and Future Work

> **Audience:** Investors, Product Managers, Future Employees, Engineering Leadership
> **Prerequisite reading:** [Vision and Implementation Status](vision-and-implementation-status.md) (this chapter consolidates every gap flagged there and throughout this documentation set)

[`docs/roadmap/development-roadmap.md`](../roadmap/development-roadmap.md) and [`docs/roadmap/project-milestones.md`](../roadmap/project-milestones.md) define seven phases and eight milestones from Phase 0 (Foundation) through Phase 6 (Expansion Modules). This chapter maps actual delivered work against that plan and lists every concrete gap surfaced elsewhere in this documentation set, so it can be read on its own as a single "what's left" reference.

## 1. Where the Platform Stands Today

| Roadmap Phase | Status | Evidence |
| --- | --- | --- |
| Phase 0: Foundation | ✅ Complete | This entire `docs/` tree, plus `services/backend` and `services/web` scaffolding |
| Phase 1: Platform Core | 🟡 Mostly complete for backend/web; mobile not started | Identity, roles, audit, and configuration are all implemented (see [Data Model §3, §7, §8](data-model-and-database-schema.md)); no mobile project exists |
| Phase 2: Bhishi MVP | ✅ Complete | Group setup, member management, contribution schedules and tracking, reminders, admin oversight, and core reports (Admin Analytics) are all implemented — see [Vision and Implementation Status §3](vision-and-implementation-status.md#3-mvp-scope-reconciliation) |
| Phase 3: Payments and Ledger | 🟡 Payments complete; Ledger not started | Payment provider integration, webhook handling, and idempotency are implemented; **no ledger model, no reconciliation jobs, no dedicated payment-mismatch support workflow exist** — see [§2](#2-the-ledger-gap) below |
| Phase 4: Private Beta | ⛔ Not started | No staging environment, no monitoring dashboards, no security review, no pilot onboarding |
| Phase 5: Public Launch | ⛔ Not started | No production environment, no deployment pipeline, no mobile release process |
| Phase 6: Expansion Modules | ⛔ Not started (architecturally prepared) | `Group.moduleType` already supports `BHISHI`, `SELF_HELP_GROUP`, `SOCIETY_COLLECTION`, `COMMUNITY_FUND` as data values, but only `BHISHI` has a working product experience |

## 2. The Ledger Gap

This is the single most consequential gap in the platform today, discussed in full in [Vision and Implementation Status §5](vision-and-implementation-status.md#5-what-a-ledger-would-add). Building `finance.ledger_accounts` and `finance.ledger_entries` as an append-only accounting trail — independent of `Payment.status` — is the prerequisite for: representing partial refunds and fee adjustments without mutating history, computing per-member/per-group balances from first principles rather than on-demand aggregation, and giving Finance Operators (a role already seeded in `identity.roles` but with no dedicated UI) a real reconciliation workflow instead of a single status column.

## 3. Consolidated Future Work by Theme

### Product and Domain

| Item | Source |
| --- | --- |
| Dedicated Ledger (accounts, entries, reconciliation cases) | [Vision and Implementation Status §1, §5](vision-and-implementation-status.md) |
| Independent Payout aggregate with its own approval/release workflow | [Vision and Implementation Status §1](vision-and-implementation-status.md#1-ubiquitous-language-reconciliation) |
| Community as a distinct entity from Group | [Vision and Implementation Status §1](vision-and-implementation-status.md#1-ubiquitous-language-reconciliation) |
| Self-service tenant onboarding (today: platform-admin-only suspend/activate/archive) | [Vision and Implementation Status §3](vision-and-implementation-status.md#3-mvp-scope-reconciliation) |
| Organizer-recorded manual/cash payment on a member's behalf | [Vision and Implementation Status §3](vision-and-implementation-status.md#3-mvp-scope-reconciliation) |
| Notification templates and delivery-attempt history as data (today: rendered in code, single status field) | [Vision and Implementation Status §7](vision-and-implementation-status.md#7-database-schema-reconciliation) |
| Reporting exports / export-job infrastructure | [Vision and Implementation Status §2](vision-and-implementation-status.md#2-bounded-context-reconciliation) |
| Cross-tenant audit search for platform administrators | [Security and Compliance §5](security-and-compliance.md#5-tenant-isolation), [Backend Module and API Reference — Audit Trail](backend-module-and-api-reference.md#audit-trail--audit) |
| Group-scoped member-list endpoint (blocks a real Members tab and a draw-winner picker) | [Frontend Experience §5, §7](frontend-experience.md) |
| Server-side text search on admin group listing | [Frontend Experience §6](frontend-experience.md#6-screen-catalog--platform-administrator) |
| Dedicated UI for Tenant Admin, Support Operator, Finance Operator, Auditor roles (all already seeded/modeled) | [Frontend Experience §1](frontend-experience.md#1-personas), [Security and Compliance §3](security-and-compliance.md#3-authorization) |
| Editing group rules/contribution schedule after creation | [Frontend Experience §5](frontend-experience.md#5-screen-catalog--organizer) |

### Platform and Infrastructure

| Item | Source |
| --- | --- |
| Native mobile app (Flutter, per the original architecture intent) | [Vision and Implementation Status §4](vision-and-implementation-status.md#4-system-architecture-reconciliation) |
| Cloud deployment (AWS account, network boundaries, RDS, ElastiCache, CloudFront/ALB, WAF, KMS) | [System Architecture and Modules §2](system-architecture-and-modules.md#2-high-level-system-diagram) |
| Centralized logging, metrics/alerting, and error-tracking provider (frontend seam already exists in `src/lib/logger.ts`) | [Non-Functional Requirements and Production Readiness §3](non-functional-and-production-readiness.md#3-what-production-ready-still-requires) |
| Automated frontend test suite (backend already has one) | [Non-Functional Requirements and Production Readiness §4](non-functional-and-production-readiness.md#4-testing-strategy) |
| Runbooks (deployment, rollback, DB restore, provider outage, payment mismatch, admin compromise, secret rotation) | [Non-Functional Requirements and Production Readiness §3](non-functional-and-production-readiness.md#3-what-production-ready-still-requires) |
| Database backup/restore drills | [Non-Functional Requirements and Production Readiness §3](non-functional-and-production-readiness.md#3-what-production-ready-still-requires) |
| Custom PWA install/update-available UI (today: native browser prompt only) | [Non-Functional Requirements and Production Readiness §2](non-functional-and-production-readiness.md#2-frontend-production-readiness-sprint-fe-6) |

### Security and Compliance

| Item | Source |
| --- | --- |
| Multi-factor authentication for admin/internal operations | [Security and Compliance §1](security-and-compliance.md#1-authentication) |
| PII masking in logs (not yet confirmed as implemented) | [Security and Compliance §7](security-and-compliance.md#7-data-protection) |
| Formal secrets management (Secrets Manager/SSM) — currently environment variables only | [Security and Compliance §7](security-and-compliance.md#7-data-protection) |
| SAST, secret-scanning-in-CI, container scanning, penetration test | [Security and Compliance §9](security-and-compliance.md#9-compliance-posture) |
| Third-party security audit / compliance certification (SOC 2, ISO 27001, PCI-DSS scope assessment, DPDP Act readiness) | [Security and Compliance §9](security-and-compliance.md#9-compliance-posture) |
| Legal review of Privacy Policy and Terms of Service copy (currently early-access placeholder text) | [Frontend Experience §3](frontend-experience.md#3-screen-catalog--visitor-marketing) |

## 4. Future Product Modules (Phase 6)

Both [`business-domain-design.md` §11](business-domain-design.md#11-future-scope) and [`system-architecture.md`](../architecture/system-architecture.md#future-product-modules) list the same ten candidate expansion modules, which this documentation reaffirms without change: **Women's Self Help Groups, Society Maintenance, Apartment Collection, Temple Collection, Festival Collection, Office Contribution, Wedding Collection, Travel Saving Groups, NGO Collection, and Community Funds.** The `Group.moduleType` column already anticipates this — `SELF_HELP_GROUP`, `SOCIETY_COLLECTION`, and `COMMUNITY_FUND` are valid database values today — but no product experience beyond `BHISHI` has been built for any of them.

## 5. Monetization

No billing, subscription, or platform-fee mechanism exists in the codebase today. How BachatSetu will monetize is not yet defined in any reviewed document and is intentionally left open here rather than invented — this is a business decision for product/leadership, not an engineering gap.

## Closing Note

This concludes the Product Documentation set's implementation-focused chapters. [Glossary](glossary.md) provides a single reference for every term used across all chapters, and [`docs/product/README.md`](README.md) is the index and recommended reading order for the whole set.
