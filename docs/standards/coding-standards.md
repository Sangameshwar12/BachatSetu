# Coding Standards

These standards apply once application code is introduced. They define engineering expectations without creating implementation code.

## Universal Standards

- Code must be simple, readable, and testable.
- Prefer explicit names over clever abstractions.
- Keep modules cohesive and boundaries clear.
- Avoid business logic in controllers, UI widgets, or persistence adapters.
- Every financial rule must be covered by tests.
- Every security-sensitive flow must be reviewed.
- Dead code should be removed, not commented out.

## Backend Standards

Technology:

- Java 21
- Spring Boot 3
- Maven
- PostgreSQL
- Redis
- Flyway
- Spring Security

Guidelines:

- Use constructor injection.
- Keep controllers thin.
- Keep transaction boundaries in application services.
- Use DTOs for API input and output.
- Do not expose persistence entities directly through APIs.
- Use validation annotations and explicit domain validation.
- Keep module dependencies one-directional.
- Prefer immutable value objects where practical.
- Use records for simple immutable DTOs when appropriate.

## Flutter Standards

Guidelines:

- Use feature-first folder organization.
- Keep UI widgets free of business rules.
- Centralize API clients and error mapping.
- Use consistent state management selected during implementation planning.
- Keep secrets out of the mobile app.
- Support localization from the beginning.
- Design for low-bandwidth and intermittent network conditions.

## React TypeScript Standards

Guidelines:

- Use TypeScript strict mode.
- Prefer functional components.
- Keep API access behind typed services.
- Keep route-level components separate from reusable UI components.
- Avoid storing sensitive tokens in unsafe browser storage unless explicitly accepted by security design.
- Use accessible components and keyboard-friendly interactions.

## Infrastructure Standards

Guidelines:

- Infrastructure must be reproducible.
- Environment differences must be explicit.
- Production changes must go through review.
- Use least-privilege IAM.
- Prefer managed AWS services unless custom operation is justified.
- Keep infrastructure documentation near the configuration.

## Code Review Expectations

Reviewers must check:

- Correctness
- Security
- Tenant isolation
- Error handling
- Logging hygiene
- Test coverage
- Backward compatibility
- Operational impact

