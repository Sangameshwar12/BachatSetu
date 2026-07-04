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
  operations/
    production-readiness.md
  product/
    business-domain-design.md
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

## Decision Log

Architecture decisions should be captured as ADRs once implementation begins.

Future ADR location:

```text
docs/architecture/adr/
  0001-use-modular-monorepo.md
  0002-use-modular-monolith-before-microservices.md
```
