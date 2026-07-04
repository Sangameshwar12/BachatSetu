# Repository Structure

This document defines the intended repository structure for BachatSetu. The current repository should remain documentation-only until project initialization is explicitly approved.

## Repository Strategy

BachatSetu should begin as a modular monorepo.

Reasons:

- Shared product vocabulary across backend, mobile, admin portal, infrastructure, and docs.
- Easier startup velocity with one source of truth.
- Stronger atomic changes across API contracts, admin UI, and mobile flows.
- Clear migration path to polyrepo or service-specific repositories when team size and deployment complexity justify it.

## Intended Top-Level Structure

```text
BachatSetu/
  README.md
  backend/
    README.md
    pom.xml
    src/
    config/
    scripts/
  mobile/
    README.md
    pubspec.yaml
    lib/
    test/
  admin-portal/
    README.md
    package.json
    src/
    public/
    tests/
  infrastructure/
    README.md
    aws/
    terraform/
    docker/
    environments/
  database/
    README.md
    flyway/
    seed/
    reference-data/
  docs/
    README.md
    architecture/
    operations/
    roadmap/
    standards/
    tooling/
    workflow/
  contracts/
    openapi/
    asyncapi/
    examples/
  scripts/
    local/
    ci/
    release/
  .github/
    workflows/
    pull_request_template.md
    ISSUE_TEMPLATE/
  .vscode/
    extensions.json
    settings.json
  .editorconfig
  .gitignore
```

## Backend Structure

The backend should use Java 21, Spring Boot 3, Maven, PostgreSQL, Redis, Flyway, Spring Security, and JWT.

Recommended package model:

```text
backend/src/main/java/in/bachatsetu/
  BachatSetuApplication.java
  shared/
    config/
    security/
    web/
    persistence/
    observability/
    exception/
    validation/
  modules/
    identity/
    tenant/
    member/
    bhishi/
    wallet/
    ledger/
    payment/
    notification/
    audit/
    reporting/
```

The backend should begin as a modular monolith with strict module boundaries. A module can become a separate service only after clear scale, ownership, deployment, or reliability pressure exists.

## Mobile Structure

Flutter should use a feature-first structure:

```text
mobile/lib/
  app/
  core/
    config/
    network/
    storage/
    security/
    analytics/
    localization/
  features/
    auth/
    onboarding/
    dashboard/
    bhishi/
    payments/
    profile/
    notifications/
  shared/
    widgets/
    theme/
    utils/
```

## Admin Portal Structure

React and TypeScript should use a domain-oriented structure:

```text
admin-portal/src/
  app/
  routes/
  components/
  features/
    auth/
    tenants/
    bhishi/
    payments/
    users/
    audit/
    reports/
  services/
  hooks/
  types/
  styles/
  tests/
```

## Infrastructure Structure

Infrastructure should be environment-driven and reviewable:

```text
infrastructure/
  aws/
    network/
    compute/
    database/
    cache/
    storage/
    security/
    observability/
  terraform/
    modules/
    environments/
      dev/
      staging/
      production/
  docker/
    backend/
    admin-portal/
    local-compose/
```

## Documentation Structure

Documentation should stay close to engineering execution:

```text
docs/
  architecture/
  operations/
  roadmap/
  standards/
  tooling/
  workflow/
```

## Repository Rules

- No business logic in documentation commits.
- No secrets in the repository.
- No generated build artifacts committed.
- All financial and security-sensitive behavior must be covered by tests before release.
- Architecture changes must be documented before or with implementation.

