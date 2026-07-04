# Testing Strategy

BachatSetu must test for correctness, trust, and operability. Financial and security-critical paths require deeper test coverage than ordinary CRUD flows.

## Testing Pyramid

```text
Few end-to-end tests
More integration and contract tests
Many unit and domain tests
```

## Backend Testing

Required test types:

- Unit tests for domain logic
- Application service tests for use cases
- Repository integration tests
- API controller tests
- Security authorization tests
- Flyway migration tests
- Payment provider adapter tests with mocks
- Ledger correctness tests
- Idempotency tests

Minimum expectations:

- Critical financial logic: near-complete branch coverage
- Authentication and authorization: positive and negative coverage
- Public APIs: contract coverage
- Migrations: validated in CI

## Flutter Testing

Required test types:

- Unit tests for state and services
- Widget tests for key screens
- Golden tests for critical UI states when practical
- Integration tests for onboarding, login, group view, and payment flows
- Offline and retry behavior tests

## Admin Portal Testing

Required test types:

- Unit tests for utilities and hooks
- Component tests for reusable UI
- Route-level tests for important workflows
- API contract tests
- Accessibility checks
- Role-based access tests

## End-to-End Testing

Critical E2E scenarios:

- User onboarding
- Login and session refresh
- Create Bhishi group
- Add group member
- Record contribution
- Initiate payment
- Payment status reconciliation
- Admin audit lookup

## Non-Functional Testing

Before production launch:

- Load testing
- Spike testing
- Security testing
- Mobile performance testing
- Database migration rehearsal
- Backup and restore drill
- Disaster recovery tabletop exercise

## Test Data Standards

- Do not use real customer data in development.
- Use synthetic data for test environments.
- Mask production data before any lower-environment use.
- Keep test data deterministic where possible.

