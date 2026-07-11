# Platform Overview

> **Audience:** Investors, Product Managers, Future Employees, Legal Team
> **Reading prerequisite:** None — this is the entry point to the Product Documentation set. See [Vision and Implementation Status](vision-and-implementation-status.md) for how the claims here map to source code.

## Press Release

**BachatSetu launches to bring India's Bhishi groups online, replacing notebooks and WhatsApp threads with a transparent, auditable digital ledger of who owes what and who has paid.**

Millions of Indian households participate in a Bhishi — also called a Committee or ROSCA (Rotating Savings and Credit Association) — a group of trusted people who each contribute a fixed amount on a fixed schedule, with the pooled amount paid out to one member per cycle until everyone has received a payout once. Today, this is run on paper registers, WhatsApp groups, and memory. Disputes over who paid, who's next in line, and how much is owed are common, and there is no independent record either side can point to.

BachatSetu is a digital platform purpose-built for this exact workflow. An organizer creates a group, sets the contribution amount and schedule, and invites members with a QR code, a short join code, or a shareable link. Every contribution is tracked against its cycle. Payments made through an integrated payment gateway (Razorpay, Stripe, or Cashfree) are automatically verified and receipted; the organizer can review who has paid at a glance. When it's time for a payout, the platform runs a fair draw or an auction, records the result, and notifies the group.

"We built BachatSetu because every family we talked to had a Bhishi story that ended in an argument," said the founding team. "The group already trusts each other — they just need a shared, honest record. That's the whole product."

BachatSetu is available today as a web application, with an admin portal for platform operations built directly into the same product. A native mobile app is planned as future work (see [Roadmap and Future Work](roadmap-and-future-work.md)).

## Frequently Asked Questions

### For Customers (Organizers and Members)

**What is a Bhishi, and why does BachatSetu start there?**
A Bhishi (also called a Committee or ROSCA) is a recurring group savings arrangement: members contribute a fixed amount every cycle, and the pooled contribution is paid out to one member per cycle in turn, until every member has received a payout. It's a deeply established practice across Indian communities, and it is the first product module BachatSetu supports — see [Vision and Implementation Status §1](vision-and-implementation-status.md#1-ubiquitous-language-reconciliation) for how "Bhishi Group" maps to the `Group` entity in the running system.

**Do I need to give BachatSetu my bank account or UPI PIN?**
No. Payments are processed by a third-party payment gateway (Razorpay, Stripe, or Cashfree, selected per deployment) through their own secure checkout — BachatSetu never stores card numbers, UPI PINs, or bank credentials. See [Security and Compliance](security-and-compliance.md).

**How do I join a group?**
An organizer invites you with a QR code, a short alphanumeric join code, or a shareable link. Scanning, entering, or clicking any of the three brings you to a join screen with the group's details already filled in. See [Business Processes — Invitation and Join](business-processes.md#invitation-and-join).

**What happens if I can't pay on time?**
Your installment is marked `OVERDUE` and visible to you and your organizer. Depending on the group's rules, a late penalty may apply, or the organizer may waive it. This is a manual, organizer-driven decision today — see [Vision and Implementation Status §3](vision-and-implementation-status.md#3-mvp-scope-reconciliation) for what's automated versus manual.

**How is the payout winner chosen?**
Either by a random or fixed-rotation **draw**, or by an **auction** where members bid a discount to receive the payout early — whichever the organizer configured for the group. See [Business Processes — Draw and Auction](business-processes.md#draw-and-auction).

**Is there a mobile app?**
Not yet. The web application works on mobile browsers today; a native app is tracked in [Roadmap and Future Work](roadmap-and-future-work.md).

### For Investors and Business Stakeholders

**What is the market?**
Community savings groups (Bhishi/Committee/ROSCA) are informal and largely undocumented, but the practice is common across urban and semi-urban Indian households, self-help groups, offices, and social circles. `business-domain-design.md`'s stated strategic goal is for BachatSetu to become "the trusted operating system for community money collection in India," starting with Bhishi and expanding into adjacent use cases — see the Future Modules list in [Roadmap and Future Work](roadmap-and-future-work.md).

**What's actually built today, versus planned?**
This is answered precisely, module by module, in [Vision and Implementation Status](vision-and-implementation-status.md) — the single source of truth for implemented-vs-future scope across this entire documentation set. In short: OTP-based signup and authentication, group creation and QR/code/link invitations, contribution tracking, integrated payment gateways, draws and auctions, receipts, notifications, and a full platform admin portal are built and running. A dedicated accounting ledger, a native mobile app, and self-service multi-tenant onboarding are not yet built.

**How does BachatSetu make money?**
Not yet defined in the codebase or existing documentation — no billing, subscription, or platform-fee module exists today. This is intentionally left open in this documentation rather than invented; see [Roadmap and Future Work](roadmap-and-future-work.md).

**Who are the platform's users?**
Three personas actually exist in the running product today: the **Group Member** (contributes, views obligations, receives payouts), the **Group Organizer** (creates and runs groups — a role a Member takes on, not a separate account type), and the **Platform Administrator** (operates the whole platform: tenants, configuration, analytics, support, monitoring). See [Frontend Experience](frontend-experience.md) for the screen-by-screen breakdown per persona, and [Vision and Implementation Status §4](vision-and-implementation-status.md#4-system-architecture-reconciliation) for the fuller role list originally envisioned (Tenant Admin, Finance Operator, Auditor) that does not yet have a dedicated UI.

### For Engineering and Technical Stakeholders

**What's the technology stack?**
Backend: Java 21, Spring Boot 3, PostgreSQL, Redis, Flyway, Spring Security, Maven, built as a modular monolith following Domain-Driven Design and hexagonal architecture. Frontend: Next.js 16 (App Router), React 19, TypeScript, TailwindCSS v4, TanStack React Query. Full detail in [System Architecture and Modules](system-architecture-and-modules.md) and [Frontend Experience](frontend-experience.md).

**Is the codebase production-ready?**
The frontend completed a dedicated production-readiness sprint (performance, accessibility, SEO, security hardening, PWA support, and monitoring scaffolding) — see [Non-Functional Requirements and Production Readiness](non-functional-and-production-readiness.md). The backend enforces layered architecture boundaries via ArchUnit, and every module has an associated test suite and application-layer documentation under `services/backend/docs/`. Neither service has been deployed to a production environment or load-tested at scale as of this writing.

**Why isn't there a public API or partner integration story yet?**
Because none has been built. The REST API exists to serve BachatSetu's own web client, not as a published third-party integration surface. See [Backend Module and API Reference](backend-module-and-api-reference.md).

## Who This Documentation Is For

| Audience | Start Here |
| --- | --- |
| Developers (backend) | [System Architecture and Modules](system-architecture-and-modules.md), [Backend Module and API Reference](backend-module-and-api-reference.md), then the per-module docs under `services/backend/docs/` |
| Developers (frontend) | [Frontend Experience](frontend-experience.md), then `services/web/README.md` |
| QA Engineers | [Business Processes](business-processes.md), [State Machines](state-machines.md) |
| UI/UX Designers | [Frontend Experience](frontend-experience.md) |
| DevOps Engineers | [System Architecture and Modules](system-architecture-and-modules.md), [Non-Functional Requirements and Production Readiness](non-functional-and-production-readiness.md) |
| Investors | This document, [Vision and Implementation Status](vision-and-implementation-status.md), [Roadmap and Future Work](roadmap-and-future-work.md) |
| Product Managers | [Vision and Implementation Status](vision-and-implementation-status.md), [Business Processes](business-processes.md), [Roadmap and Future Work](roadmap-and-future-work.md) |
| Legal Team | [Security and Compliance](security-and-compliance.md) |
| Future Employees | [`docs/product/README.md`](README.md) for the full reading order |

The complete chapter list and recommended reading order live in [`docs/product/README.md`](README.md).
