# Engineering Governance

Version: 1.0
Effective date: 2026-07-05
Status: Authoritative

## Purpose

This policy defines decision authority, ownership, review expectations, quality gates, and exceptions for BachatSetu engineering. It coordinates existing standards without restating them.

## Policy Hierarchy

When guidance conflicts, apply this order:

1. Legal, regulatory, and approved security requirements
2. This governance policy and the other documents in `docs/governance`
3. Approved architecture decision records
4. Architecture, database, API, and security standards
5. Development, testing, CI/CD, coding, naming, and commit standards
6. Team conventions that are not yet documented

The [Branching Policy](branching-policy.md) supersedes the earlier trunk-based recommendation in `docs/workflow/git-branching-strategy.md`.

## Roles

| Role | Accountability |
| --- | --- |
| Repository owner | Repository settings, CODEOWNERS, branch protection, final policy and emergency decisions |
| Code owner | Technical correctness and maintainability within owned paths |
| Author | Scope, implementation quality, tests, documentation, risk disclosure, and review response |
| Reviewer | Independent verification of correctness, architecture, security, tests, compatibility, and operability |
| Security owner | Vulnerability triage, risk acceptance, disclosure coordination, and security exceptions |
| Release manager | Version, release evidence, approvals, deployment coordination, rollback readiness, and release record |

One person may hold several roles in an early-stage team, but high-risk changes still require an independent reviewer whenever practicable.

## Ownership

`CODEOWNERS` is the executable ownership map. It covers backend code, documentation, workflows, shared configuration, and AI context. Repository branch protection must require code-owner approval for owned paths.

Ownership means responsibility for review and stewardship; it does not permit bypassing CI, security, or release policy.

## Change Governance

Every non-trivial change begins with an issue that defines outcome, scope, dependencies, acceptance criteria, risks, and verification. Pull requests must remain traceable to that issue.

Required specialist review:

- Financial or aggregate invariants: backend/domain owner
- Flyway, schema, indexes, or retention: backend/database owner
- Authentication, authorization, secrets, PII, cryptography, or public exposure: security owner
- Architecture boundaries or new dependencies: architecture owner
- Workflows, deployment, cloud, or release controls: repository/infrastructure owner
- Public API or mobile/admin contracts: owning producers and consumers

## Merge Gates

A pull request is mergeable only when:

- Scope and acceptance criteria are satisfied.
- Required CODEOWNERS approvals are present.
- `mvn clean verify` and applicable GitHub checks pass.
- Dependency review and secret scanning pass.
- Architecture, database, security, performance, and compatibility impacts are resolved.
- Owning documentation and `CHANGELOG.md` are updated when applicable.
- Rollback or forward-fix handling is credible for operationally significant changes.
- No unresolved critical or high-severity defect remains unless formally accepted by the repository and security owners.

Authors must not dismiss failed checks by rerunning until green without understanding the failure.

## Decision Records

Use an architecture decision record when a change alters a durable boundary, platform dependency, data ownership rule, security model, deployment topology, or operational strategy. The decision must record context, options, decision, consequences, owner, and review date.

Routine implementation details belong in issues, pull requests, and owning technical documentation rather than ADRs.

## Exceptions

An exception must be:

- Narrowly scoped
- Time-bound with an expiry date
- Owned by a named role
- Supported by risk and compensating controls
- Linked to a remediation issue
- Approved by the relevant code owner and repository or security owner

Exceptions cannot authorize committing secrets, bypassing mandatory financial integrity controls, rewriting applied Flyway migrations, or concealing a known incident.

## Audit And Review

Governance documents are reviewed before each production release and after a significant incident, architecture change, or control failure. Updates use semantic document versions and are reviewed like code.

## References

- [Architecture Protection](../architecture/architecture-protection.md)
- [Security Standards](../architecture/security-standards.md)
- [Development Workflow](../workflow/development-workflow.md)
- [CI/CD Strategy](../workflow/ci-cd-strategy.md)
- [Testing Strategy](../workflow/testing-strategy.md)
- [Git Commit Conventions](../standards/git-commit-conventions.md)
