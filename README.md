# BachatSetu

BachatSetu is India's Community Savings Platform. The first supported product module is Bhishi, also known as ROSCA or Committee groups. The platform is designed to grow into a broader community collections and savings SaaS for self-help groups, apartment collections, society maintenance, temple collections, festival collections, office contributions, travel saving groups, NGO collections, and community funds.

This repository is currently documentation-first. It defines the production foundation, engineering standards, architecture, delivery workflow, and project roadmap. It intentionally contains no application source code yet.

## Current Repository Contents

```text
BachatSetu/
  README.md
  docs/
    README.md
    architecture/
    operations/
    product/
    roadmap/
    standards/
    tooling/
    workflow/
```

## Intended Product Architecture

- Backend: Java 21, Spring Boot 3, PostgreSQL, Redis, Flyway, Spring Security, JWT, Maven
- Mobile: Flutter
- Admin Portal: React, TypeScript
- Cloud: AWS
- CI/CD: GitHub Actions

## Documentation Index

- [Documentation Home](docs/README.md)
- [Repository Structure](docs/architecture/repository-structure.md)
- [Documentation Structure](docs/architecture/documentation-structure.md)
- [System Architecture](docs/architecture/system-architecture.md)
- [API Standards](docs/architecture/api-standards.md)
- [Database Standards](docs/architecture/database-standards.md)
- [Security Standards](docs/architecture/security-standards.md)
- [Business Domain Design](docs/product/business-domain-design.md)
- [Coding Standards](docs/standards/coding-standards.md)
- [Naming Conventions](docs/standards/naming-conventions.md)
- [Folder Naming Conventions](docs/standards/folder-naming-conventions.md)
- [Logging Standards](docs/standards/logging-standards.md)
- [Exception Handling Standards](docs/standards/exception-handling.md)
- [Git Commit Conventions](docs/standards/git-commit-conventions.md)
- [Git Branching Strategy](docs/workflow/git-branching-strategy.md)
- [Development Workflow](docs/workflow/development-workflow.md)
- [CI/CD Strategy](docs/workflow/ci-cd-strategy.md)
- [Testing Strategy](docs/workflow/testing-strategy.md)
- [Development Roadmap](docs/roadmap/development-roadmap.md)
- [Project Milestones](docs/roadmap/project-milestones.md)
- [Sprint Planning](docs/roadmap/sprint-planning.md)
- [Recommended IDE Extensions](docs/tooling/ide-extensions.md)

## Guiding Principles

- Build trust before scale.
- Treat money movement, balances, identity, and audit trails as critical infrastructure.
- Prefer simple, modular architecture first; evolve into distributed services only when the product and scale require it.
- Make every financial state change traceable, reversible by controlled process, and observable.
- Keep compliance, privacy, security, and operational readiness as first-class engineering concerns.
"# BachatSetu" 
