# BachatSetu Documentation

This folder contains the foundational engineering documentation for BachatSetu. It is intended to guide repository setup, product architecture, engineering execution, governance, security, and delivery.

No application code should be added until the foundation is reviewed and approved.

## Folder Structure

```text
docs/
  README.md
  api/
    rest-api-contract.md
  architecture/
    repository-structure.md
    documentation-structure.md
    system-architecture.md
    api-standards.md
    database-standards.md
    security-standards.md
  database/
    postgresql-database-architecture.md
  deployment/
    README.md
    docker-guide.md
    environment-variables-guide.md
    infrastructure-guide.md
    production-deployment-guide.md
    production-checklist.md
    runbook.md
    recovery-guide.md
  operations/
    production-readiness.md
  product/
    README.md
    business-domain-design.md
    platform-overview.md
    vision-and-implementation-status.md
    system-architecture-and-modules.md
    data-model-and-database-schema.md
    backend-module-and-api-reference.md
    business-processes.md
    state-machines.md
    frontend-experience.md
    security-and-compliance.md
    non-functional-and-production-readiness.md
    roadmap-and-future-work.md
    glossary.md
  roadmap/
    development-roadmap.md
    project-milestones.md
    sprint-planning.md
  standards/
    coding-standards.md
    naming-conventions.md
    folder-naming-conventions.md
    logging-standards.md
    exception-handling.md
    git-commit-conventions.md
  tooling/
    ide-extensions.md
  workflow/
    git-branching-strategy.md
    development-workflow.md
    ci-cd-strategy.md
    testing-strategy.md
```

## Recommended Reading Order

1. Repository Structure
2. Documentation Structure
3. System Architecture
4. Business Domain Design
5. Development Roadmap
6. Security Standards
7. Database Standards
8. PostgreSQL Database Architecture
9. API Standards
10. REST API Contract
11. Development Workflow
12. CI/CD Strategy
13. Testing Strategy
14. Coding and Naming Standards

## Product Documentation (Implementation-Grounded)

Items 3, 4, 6–10 above (`system-architecture.md`, `business-domain-design.md`, `security-standards.md`, `database-standards.md`, `postgresql-database-architecture.md`, `api-standards.md`, `rest-api-contract.md`) were written **before implementation began**, as forward-looking specifications — each says so explicitly. Now that `services/backend` and `services/web` are built out, **[`docs/product/README.md`](product/README.md)** is the complete, implementation-verified documentation set covering what the platform actually does today, reconciled explicitly against every document above. Read the foundational documents above for original intent and standards; read `docs/product/` for ground truth.

## Deployment Documentation

**[`docs/deployment/README.md`](deployment/README.md)** covers running the application in
production: Docker images, environment configuration, target AWS infrastructure, the
deployment procedure, a pre-launch checklist, day-2 operations, and failure recovery. It is
operational documentation, not product documentation — see `docs/product/` for what the
platform does, `docs/deployment/` for how it runs.

## Decision Log

Architecture decisions should be captured as ADRs once implementation begins.

Future ADR location:

```text
docs/architecture/adr/
  0001-use-modular-monorepo.md
  0002-use-modular-monolith-before-microservices.md
```
