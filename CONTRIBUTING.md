# Contributing to BachatSetu

Version: 1.0
Governance status: Active

Thank you for contributing to BachatSetu. Changes must preserve financial correctness, security, auditability, and the modular architecture.

## Before Starting

1. Select or create an approved issue with acceptance criteria.
2. Confirm that the work belongs to the current sprint.
3. Read [Engineering Governance](docs/governance/engineering-governance.md) and the standards it references.
4. Branch from `develop` using the naming rules in [Branching Policy](docs/governance/branching-policy.md).

Security vulnerabilities must follow [SECURITY.md](SECURITY.md) and must not be discussed in public issues.

## Local Setup

Backend prerequisites:

- Java 21
- Maven 3.9 or newer
- Docker for PostgreSQL Testcontainers integration tests

Run the backend quality build from `services/backend`:

```shell
mvn clean verify
```

Docker must be available for the complete PostgreSQL integration suite. A local build may report skipped Testcontainers tests when Docker is unavailable, but CI and release validation must execute them.

## Engineering Standards

- Follow [Coding Standards](docs/standards/coding-standards.md) and [Naming Conventions](docs/standards/naming-conventions.md).
- Preserve the dependency rules enforced by [Architecture Protection](docs/architecture/architecture-protection.md).
- Use constructor injection and structured logging.
- Do not add business logic to controllers, repositories, or persistence mappers.
- Do not modify Flyway migrations that have been applied to a shared environment.
- Add tests for production behavior and defects.
- Keep secrets, credentials, and personal data out of source, fixtures, logs, and pull requests.

## Commits

Use Conventional Commits as defined in [Git Commit Conventions](docs/standards/git-commit-conventions.md):

```text
type(scope): imperative summary
```

Keep commits reviewable and avoid mixing unrelated changes.

## Documentation

Update the owning document when behavior, architecture, operations, security, database structure, or public contracts change. Link to existing sources of truth rather than copying policy text into multiple files.

## Pull Requests

- Complete every applicable section of the pull request template.
- Keep scope aligned with the linked issue and sprint.
- Explain architecture, database, security, performance, deployment, and rollback impacts.
- Ensure required checks pass before requesting final approval.
- Resolve review conversations with code, tests, documentation, or an explicit decision record.
- Do not merge your own security-sensitive, financial, migration, or release change without the required owner review.

## Definition of Done

A contribution is complete when acceptance criteria are met, `mvn clean verify` passes, required reviews are approved, documentation is current, no critical or high-risk finding remains unresolved, and the change has an understood rollback path.
