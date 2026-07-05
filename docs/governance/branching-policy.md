# Branching Policy

Version: 1.0
Effective date: 2026-07-05
Status: Authoritative

## Model

BachatSetu uses Git Flow Lite. This policy supersedes the trunk-based recommendation in `docs/workflow/git-branching-strategy.md`.

```text
feature/* ----> develop ----> release/* ----> main
bugfix/*  ----> develop          |              |
                           merge back       hotfix/*
                                |              |
                                +<-------------+
```

## Protected Branches

| Branch | Purpose | Rules |
| --- | --- | --- |
| `main` | Production and released history | Pull requests only, required checks and reviews, no force push, no deletion |
| `develop` | Integrated state for the next release | Pull requests only, required checks and reviews, no force push |

`main` must remain releasable. `develop` may contain completed next-release work but must remain buildable and deployable to a non-production environment.

## Working Branches

| Pattern | Created from | Merges into | Purpose |
| --- | --- | --- | --- |
| `feature/{issue}-{description}` | `develop` | `develop` | Approved product capability |
| `bugfix/{issue}-{description}` | `develop` | `develop` | Non-production or next-release defect |
| `release/{version}` | `develop` | `main`, then back to `develop` | Stabilization and release metadata |
| `hotfix/{issue}-{description}` | `main` | `main`, then `develop` and any active release branch | Urgent production correction |

Use lowercase ASCII and hyphens. Keep names concise and traceable, for example `feature/bs-142-group-invitations` or `release/1.2.0`.

## Standard Flow

1. Start from the latest `develop`.
2. Create a short-lived `feature/*` or `bugfix/*` branch.
3. Keep the branch focused on one approved issue.
4. Rebase or merge `develop` as team policy requires before final review; never rewrite a branch another contributor is using without coordination.
5. Open a pull request into `develop` and satisfy all merge gates.
6. Delete the working branch after merge.

Feature and bugfix branches should normally live for fewer than five working days. Split work or use production-safe feature flags when a capability cannot be completed in that window.

## Release Branches

Create `release/{version}` only when planned scope is complete. Allowed changes are version updates, release notes, documentation, configuration validation, and release-blocking fixes. New features are forbidden.

After validation:

1. Merge the release branch into `main`.
2. Create the signed or protected semantic version tag.
3. Merge the release result back into `develop`.
4. Delete the release branch after deployment and stabilization.

## Hotfixes

Create a hotfix from the affected `main` revision. Keep the change minimal, add regression coverage, run release validation, and obtain expedited owner review. After merging and tagging `main`, merge the correction into `develop` and any active release branch.

## Prohibited Practices

- Direct pushes to `main` or `develop`
- Force pushes or history rewrites on protected branches
- Combining unrelated issues in one branch
- Long-lived personal integration branches
- Merging a failed or skipped required check
- Adding features to release or hotfix branches
- Deleting an unmerged branch that contains audit or incident evidence

## Branch Protection Baseline

Require pull requests, at least one approval, code-owner review, resolved conversations, current required checks, linear or merge history selected by repository policy, and prevention of branch deletion and force pushes. Sensitive changes may require additional security, database, or architecture approval.
