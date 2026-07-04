# Documentation Structure

BachatSetu documentation should be treated as a product asset. It must stay close to engineering decisions and evolve with the platform.

## Current Documentation Structure

```text
docs/
  README.md
  architecture/
    repository-structure.md
    documentation-structure.md
    system-architecture.md
    api-standards.md
    database-standards.md
    security-standards.md
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

## Future Documentation Areas

Add these folders when implementation begins:

```text
docs/
  architecture/
    adr/
    diagrams/
    threat-models/
  product/
    requirements/
    user-flows/
    release-notes/
  operations/
    runbooks/
    incidents/
    support-playbooks/
  compliance/
    policies/
    data-retention/
    vendor-risk/
```

## Documentation Ownership

| Area | Owner |
| --- | --- |
| Architecture | CTO or principal architect |
| API standards | Backend lead |
| Database standards | Backend lead or data owner |
| Security standards | CTO or security owner |
| Product requirements | Product owner |
| Runbooks | Engineering owner for the system |
| Incident documents | Incident commander |

## Documentation Rules

- Keep docs in Markdown unless a richer format is required.
- Update docs in the same pull request as behavior-changing code.
- Use ADRs for durable architecture decisions.
- Use diagrams for complex flows, but keep text explanations authoritative.
- Do not store secrets, credentials, or sensitive customer data in docs.
- Review security and compliance docs before production launch.
