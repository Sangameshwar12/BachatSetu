# BachatSetu Backend Domain Layer

This service contains a framework-independent domain model organized as a modular monolith. Each top-level business package is a bounded module with its own aggregate, entities, value objects, events, exceptions, factories, and repository ports.

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

## Architectural Decisions

- Domain code uses only the Java standard library and has no Spring, persistence, HTTP, or serialization annotations.
- Aggregate roots protect state transitions and collect domain events. Event publication belongs to a future application layer.
- References between aggregates use `AggregateId`; aggregates do not retain objects owned by another module.
- Repository interfaces are outbound domain ports. Infrastructure adapters will implement them later.
- Constructors support reconstitution, while factories create new aggregates using an injected `Clock` for deterministic time handling.
- `Money` stores integer minor units with an explicit ISO currency. Floating-point financial values are not accepted.
- `AuditInfo` and aggregate versions are domain metadata only; persistence mapping and optimistic-lock annotations belong to adapters.
- Auth models account and verification state only. Credential verification, sessions, JWT, and security configuration are intentionally absent.

## Dependency Rule

Business modules may depend on `shared.domain`. A business module must not import another module's aggregate or entity; cross-module relationships are represented by identifiers and domain events.

Persistence foundation decisions are documented in [docs/persistence/README.md](docs/persistence/README.md).

Build quality gates are documented in [docs/quality/build-quality.md](docs/quality/build-quality.md).
