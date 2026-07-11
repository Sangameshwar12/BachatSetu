# BachatSetu Product Documentation

This is the complete, implementation-grounded Product Documentation for BachatSetu — India's Community Savings Platform, starting with Bhishi (Committee/ROSCA) group management. It documents the system **as built** in `services/backend` (Java 21, Spring Boot 3) and `services/web` (Next.js 16, React 19), verified directly against source code, applied database migrations, and REST controllers as of Sprint 13.5 (backend) and Sprint FE-6 (frontend).

This documentation set sits alongside, and does not replace, [`business-domain-design.md`](business-domain-design.md) — the original pre-implementation Domain-Driven Design specification. That document remains valuable as a record of product intent; this set exists because implementation did not follow it field-for-field, and every reader needs one place that says, precisely, what is real today.

## Reading Order

The chapters are written to be read in order — each one builds on the terminology and facts established before it, and links back rather than repeating itself.

1. **[Platform Overview](platform-overview.md)** — Executive summary, press release, and FAQ. Start here.
2. **[Vision and Implementation Status](vision-and-implementation-status.md)** — The authoritative reconciliation between the original design vision and what is actually implemented. Every later chapter depends on the terminology and status markers established here.
3. **[System Architecture and Modules](system-architecture-and-modules.md)** — How the backend and frontend are structured, the real module inventory, and the tech stack in use.
4. **[Data Model and Database Schema](data-model-and-database-schema.md)** — Every table, every schema, entity-relationship diagrams, drawn from the applied Flyway migrations.
5. **[Backend Module and API Reference](backend-module-and-api-reference.md)** — Every real REST endpoint, organized by module, with authorization requirements.
6. **[Business Processes](business-processes.md)** — Sequence diagrams for every real end-to-end flow: signup, onboarding, group creation, invitations, payments, draws, auctions, receipts, notifications, support, and platform operations.
7. **[State Machines](state-machines.md)** — Every entity lifecycle as a state diagram, with the exact enum values enforced by the database.
8. **[Frontend Experience](frontend-experience.md)** — Every real screen, organized by the four personas the product actually supports.
9. **[Security and Compliance](security-and-compliance.md)** — Authentication, authorization, tenant isolation, audit trail, and data protection, as implemented.
10. **[Non-Functional Requirements and Production Readiness](non-functional-and-production-readiness.md)** — Performance, accessibility, SEO, PWA, monitoring, and what remains before a production launch.
11. **[Roadmap and Future Work](roadmap-and-future-work.md)** — Every gap flagged across this documentation set, consolidated into one list, mapped against the phased roadmap.
12. **[Glossary](glossary.md)** — Every term used across this documentation set, in one alphabetized reference.

## How to Use This Documentation by Role

| Role | Suggested path |
| --- | --- |
| **New backend developer** | 1 → 2 → 3 → 4 → 5, then the deep-dive docs under `services/backend/docs/` |
| **New frontend developer** | 1 → 2 → 3 → 8, then `services/web/README.md` |
| **QA engineer** | 5 → 6 → 7 |
| **UI/UX designer** | 1 → 8 |
| **DevOps engineer** | 3 → 9 → 10 |
| **Product manager** | 1 → 2 → 6 → 11 |
| **Investor** | 1 → 2 → 11 |
| **Legal reviewer** | 9 |
| **Future employee (any role)** | Start at 1 and read straight through — the set is short enough to read end to end in one sitting |

## Relationship to the Rest of `docs/`

This product documentation set cross-references, and is cross-referenced by, the rest of the engineering documentation tree — see the top-level [`docs/README.md`](../README.md) for the complete index (architecture, standards, workflow, roadmap, governance, database, and operations documentation). Where this set and an older architecture/design document disagree, [Vision and Implementation Status](vision-and-implementation-status.md) is the deciding authority, because it is the only document in the tree verified directly against running code.
