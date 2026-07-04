# CI/CD Strategy

BachatSetu should use GitHub Actions for CI/CD. Pipelines must be fast for pull requests and strict for releases.

## Pipeline Principles

- Every pull request must run validation.
- `main` must always be releasable.
- Production deployments require explicit approval.
- Security checks are mandatory.
- Database migrations must be validated before deployment.
- Build artifacts must be traceable to commit SHA.

## Pull Request CI

Required checks:

- Backend compile
- Backend unit tests
- Backend static analysis
- Backend dependency vulnerability scan
- Flyway migration validation
- Admin TypeScript type check
- Admin lint and tests
- Flutter analyze
- Flutter tests
- Secret scan
- Formatting check

## Main Branch CI

On merge to `main`:

- Run full validation
- Build backend artifact
- Build admin portal artifact
- Build mobile test artifact when needed
- Build container images
- Scan containers
- Publish artifacts to registry
- Deploy automatically to dev

## Staging Deployment

Staging should require:

- Tagged release candidate
- Successful main build
- Migration dry run
- Smoke tests
- Product acceptance checks

## Production Deployment

Production should require:

- Manual approval
- Verified release notes
- Backup status check
- Migration compatibility review
- Rollback plan
- Post-deployment smoke tests

## Recommended GitHub Actions Workflows

```text
.github/workflows/
  backend-ci.yml
  admin-ci.yml
  mobile-ci.yml
  security-scan.yml
  migration-check.yml
  build-and-publish.yml
  deploy-dev.yml
  deploy-staging.yml
  deploy-production.yml
```

## Deployment Strategy

Initial recommendation:

- Blue-green or rolling deployments for backend
- Static artifact deployment for admin portal
- Store mobile release artifacts separately
- Database migrations run before app deployment only when backward-compatible

## Rollback Strategy

- Application rollback must be one command or one approved workflow.
- Database rollback should use forward-fix migrations by default.
- Destructive migrations require special approval and maintenance windows.
- Feature flags should protect risky launches.

