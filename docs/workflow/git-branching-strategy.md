# Git Branching Strategy

BachatSetu should use trunk-based development with short-lived branches.

## Core Branches

| Branch | Purpose |
| --- | --- |
| main | Always releasable production branch |
| release/* | Stabilization branch for planned releases |
| hotfix/* | Emergency production fixes |
| feature/* | Short-lived feature work |
| bugfix/* | Non-emergency fixes |
| chore/* | Maintenance and tooling work |

## Branch Naming

```text
feature/{ticket-id}-{short-description}
bugfix/{ticket-id}-{short-description}
hotfix/{ticket-id}-{short-description}
release/{yyyy-mm-dd}
chore/{short-description}
```

Examples:

```text
feature/bs-101-bhishi-group-setup
bugfix/bs-208-payment-status-display
hotfix/bs-301-admin-login-failure
release/2026-08-01
```

## Rules

- Protect `main`.
- Require pull requests into `main`.
- Require CI checks before merge.
- Require at least one approval for normal changes.
- Require security or architecture review for sensitive changes.
- Keep branches short-lived, ideally under three days.
- Use feature flags for incomplete production-safe work.

## Release Branches

Release branches are for final hardening only:

- Bug fixes
- Version updates
- Release notes
- Deployment configuration checks

No new feature development should happen on release branches.

## Hotfix Flow

1. Branch from `main`.
2. Apply minimal fix.
3. Run required tests.
4. Merge to `main`.
5. Deploy.
6. Back-merge to any active release branch.
7. Document incident if production was impacted.

