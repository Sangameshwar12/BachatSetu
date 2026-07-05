# Release Process

Version: 1.0
Effective date: 2026-07-05
Status: Authoritative

## Versioning

BachatSetu uses Semantic Versioning: `MAJOR.MINOR.PATCH`.

- `MAJOR`: incompatible public contract, data, or operational change requiring coordinated migration
- `MINOR`: backward-compatible capability
- `PATCH`: backward-compatible defect or security correction

Release tags use `vMAJOR.MINOR.PATCH`; approved pre-releases use SemVer suffixes such as `v1.2.0-rc.1`. `CHANGELOG.md` is the release-note source.

## Release Readiness

A release candidate is cut from `develop` as `release/{version}` only after planned scope is complete. The release branch accepts stabilization changes, not features.

Required evidence:

- Linked scope and acceptance results
- Successful backend and architecture quality gates
- Dependency and secret scans
- Migration compatibility and backup/restore assessment when data changes exist
- Security review for changed exposure or sensitive flows
- Performance evidence for material workload changes
- Deployment and rollback instructions
- Updated changelog and operator documentation

## Release Checklist

### Prepare

- [ ] Confirm version and included issues.
- [ ] Create `release/{version}` from `develop`.
- [ ] Move completed changelog entries from `Unreleased` to the dated version.
- [ ] Freeze new feature work on the release branch.

### Validate

- [ ] `mvn clean verify` passes with Docker-backed integration tests executed.
- [ ] Dependency Review, secret scanning, static analysis, architecture tests, and coverage gates pass.
- [ ] Flyway migrations are forward compatible and validated against a production-like PostgreSQL version.
- [ ] Security, privacy, audit, performance, observability, and support impacts are approved.
- [ ] Backup status and rollback or forward-fix plan are confirmed.

### Approve And Release

- [ ] Required code-owner, security, database, and release approvals are present.
- [ ] Merge the release branch to `main` without bypassing protection.
- [ ] Create the semantic version tag and confirm release-validation workflow success.
- [ ] Deploy through the approved environment progression and approval gates.
- [ ] Run post-deployment health, smoke, migration, and critical workflow checks.
- [ ] Merge the released state back into `develop`.

### Close

- [ ] Publish release notes and known limitations.
- [ ] Confirm monitoring and support ownership for the stabilization window.
- [ ] Delete the completed release branch.
- [ ] Record incidents, rollback, or follow-up work.

## Rollback Strategy

Application releases must support rapid rollback to the last known-good immutable artifact. Configuration must be versioned and compatible with both the new and rollback artifact during the deployment window.

Database rollback defaults to forward-fix migrations. Never edit or reverse an applied Flyway migration casually. Destructive or incompatible changes require expand-and-contract deployment, verified backups, restore evidence, explicit approval, and a maintenance or isolation plan.

Rollback triggers include failed critical smoke tests, financial integrity risk, authorization bypass, uncontrolled error rate, data corruption, or an incident commander decision. Preserve logs, metrics, deployment identifiers, and timestamps for review.

## Hotfix Process

1. Open a high-priority issue with impact, scope, and incident link when applicable.
2. Create `hotfix/{issue}-{description}` from the affected `main` revision.
3. Implement the smallest safe correction and regression test.
4. Run all mandatory quality, security, migration, and release checks.
5. Obtain expedited code-owner and security/database approval as applicable.
6. Merge to `main`, increment the patch version, tag, and deploy.
7. Merge the hotfix into `develop` and active release branches.
8. Update `CHANGELOG.md` and complete incident review when production was affected.

## Release Failure

Stop promotion on any failed required check. Do not retag a released version; correct the cause and issue a new version. A partial or failed deployment is an operational event and must have a named owner until systems and data are verified stable.
