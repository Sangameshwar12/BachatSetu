# Development Workflow

This workflow is designed for a small startup team that must move quickly without compromising financial correctness or security.

## Standard Flow

1. Create or select a ticket.
2. Confirm acceptance criteria.
3. Create a short-lived branch.
4. Implement changes with tests.
5. Update documentation when behavior or architecture changes.
6. Open a pull request.
7. Pass CI checks.
8. Complete code review.
9. Merge to `main`.
10. Deploy through the approved pipeline.

## Definition of Ready

A ticket is ready when it has:

- Clear user or business outcome
- Acceptance criteria
- Known dependencies
- Security or compliance notes if relevant
- Test expectations
- Product owner approval for scope

## Definition of Done

A ticket is done when:

- Acceptance criteria are met
- Tests are added or updated
- CI passes
- Documentation is updated if needed
- Logs and errors follow standards
- Security-sensitive changes are reviewed
- No known critical or high defects remain

## Pull Request Standards

Every pull request should include:

- What changed
- Why it changed
- How it was tested
- Screenshots for UI changes
- Migration notes for database changes
- Rollback notes for risky changes

## Review Ownership

Required specialist reviews:

- Security-sensitive changes: security reviewer or CTO
- Financial logic: backend lead or CTO
- Database migrations: backend lead or data owner
- Infrastructure changes: DevOps owner or CTO
- Public API changes: backend and frontend/mobile consumers

## Local Development Expectations

Before pushing:

- Format code.
- Run relevant unit tests.
- Run static checks.
- Verify migrations locally when database changes exist.
- Update OpenAPI contracts when APIs change.

## Environment Flow

```text
local -> dev -> staging -> production
```

Environment purpose:

- `local`: developer machine
- `dev`: shared integration testing
- `staging`: production-like validation
- `production`: live users

