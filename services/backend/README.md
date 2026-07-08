# BachatSetu Backend

For running the application locally (prerequisites, environment variables, Flyway, seed data, Swagger, health checks, and troubleshooting), see the [repository root README](../../README.md#backend-development).

This document describes the domain layer's internal package structure and architectural decisions. The domain model is framework-independent and organized as a modular monolith; each top-level business package is a bounded module with its own aggregate, entities, value objects, events, exceptions, factories, and repository ports. Application services, REST controllers, and persistence adapters (which consume these domain ports) live in the corresponding `application`, `interfaces`, and `infrastructure` packages alongside each module.

## Package Structure

```text
in.bachatsetu.backend
  shared/domain
  auth/domain/{model,event,exception,port,factory}
  user/domain/{model,event,exception,port,factory}
  group/domain/{model,event,exception,port,factory}
  member/domain/{model,event,exception,port,factory}
  payment/domain/{model,event,exception,port,factory}
  receipt/domain/{model,event,exception,port,factory}
  draw/domain/{model,event,exception,port,factory}
  notification/domain/{model,event,exception,port,factory}
```

`auction` is not listed above because it has no domain package of its own: "Auction" is the pre-existing
`Draw` aggregate (`draw.domain.model.Draw`) operating in `DrawType.AUCTION` mode. The `auction` top-level
package contains only `application` and `interfaces` layers — a business-vocabulary-aligned facade over
`draw.domain`/`draw.application` — documented in
[docs/application/auction-application.md](docs/application/auction-application.md).

## Architectural Decisions

- Domain code uses only the Java standard library and has no Spring, persistence, HTTP, or serialization annotations.
- Aggregate roots protect state transitions and collect domain events. Event publication is handled by each module's application layer.
- References between aggregates use `AggregateId`; aggregates do not retain objects owned by another module.
- Repository interfaces are outbound domain ports, implemented by JPA-backed infrastructure adapters under each module's `infrastructure`/`interfaces.rest.config` packages.
- Constructors support reconstitution, while factories create new aggregates using an injected `Clock` for deterministic time handling.
- `Money` stores integer minor units with an explicit ISO currency. Floating-point financial values are not accepted.
- `AuditInfo` and aggregate versions are domain metadata only; persistence mapping and optimistic-lock annotations belong to adapters.
- Auth currently covers OTP-based verification, JWT access/refresh tokens (opt-in via `bachatsetu.authentication.token.enabled`), and Spring Security wiring. There is no self-service user registration/provisioning flow yet — see the root README's troubleshooting section.

## Dependency Rule

Business modules may depend on `shared.domain`. A business module must not import another module's aggregate or entity; cross-module relationships are represented by identifiers and domain events.

Persistence foundation decisions are documented in [docs/persistence/README.md](docs/persistence/README.md).

Build quality gates are documented in [docs/quality/build-quality.md](docs/quality/build-quality.md).
