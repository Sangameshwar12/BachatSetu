# Git Commit Conventions

BachatSetu uses Conventional Commits to keep history readable and support automated release notes.

## Format

```text
type(scope): short description
```

Examples:

```text
docs(architecture): define modular monorepo foundation
feat(bhishi): add group creation use case
fix(payments): handle provider timeout reconciliation
test(ledger): cover reversal entry validation
```

## Types

| Type | Usage |
| --- | --- |
| feat | New user-facing capability |
| fix | Bug fix |
| docs | Documentation-only change |
| test | Tests only |
| refactor | Behavior-preserving code change |
| perf | Performance improvement |
| chore | Tooling, maintenance, configuration |
| ci | CI/CD change |
| build | Build system or dependency change |
| security | Security-specific change |

## Scopes

Recommended scopes:

```text
architecture
backend
mobile
admin
infrastructure
database
security
payments
ledger
bhishi
identity
notifications
docs
ci
```

## Rules

- Use imperative mood.
- Keep the subject under 72 characters when practical.
- Do not end the subject with a period.
- Include a body for non-trivial changes.
- Reference issues or tickets when available.
- Use `BREAKING CHANGE:` footer for breaking changes.

## Examples

```text
docs(standards): add API and database conventions
```

```text
security(identity): rotate refresh token after every use

Adds refresh token rotation and revocation support for active sessions.

Refs: BS-104
```

