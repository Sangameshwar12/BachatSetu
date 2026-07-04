# System Architecture

BachatSetu is an enterprise-grade fintech SaaS platform for community savings and collections. The initial module is Bhishi. The architecture must support future community finance modules without rewriting the foundation.

## Architectural Style

Start with a modular monolith backend and a monorepo.

This gives the startup speed of a single deployable backend while preserving boundaries that can later evolve into independent services.

## High-Level Components

```text
Mobile App      Admin Portal
   |                |
   +------- API Gateway / Load Balancer
                    |
              Spring Boot Backend
                    |
    +---------------+----------------+
    |               |                |
 PostgreSQL       Redis         External Providers
    |               |          Payments, SMS, Email,
    |               |          WhatsApp, KYC, Analytics
    |
 Flyway Migrations
```

## Core Domains

| Domain | Purpose |
| --- | --- |
| Identity | Authentication, user accounts, sessions, credentials, JWT lifecycle |
| Tenant | SaaS tenant boundaries, organization settings, module enablement |
| Member | Community members, profiles, roles, relationships |
| Bhishi | Group creation, contribution cycles, auction/draw rules, payouts |
| Wallet | User-visible balances and payment state summaries |
| Ledger | Immutable financial accounting records |
| Payment | Payment initiation, status reconciliation, provider webhooks |
| Notification | SMS, email, WhatsApp, push notifications |
| Audit | Traceability for sensitive user, financial, and admin actions |
| Reporting | Operational reports, compliance reports, exports |

## Future Product Modules

Future modules should plug into the same shared foundation:

- Women's Self Help Groups
- Society Maintenance
- Apartment Collection
- Temple Collection
- Festival Collection
- Office Contribution
- Wedding Collection
- Travel Saving Groups
- NGO Collection
- Community Funds

Each module should own its domain behavior but use common identity, tenant, payment, ledger, notification, audit, and reporting capabilities.

## Backend Architecture

Use layered architecture inside each module:

```text
API Controller
  Application Service
    Domain Model / Domain Service
      Repository Interface
        Persistence Adapter
```

Rules:

- Controllers only handle HTTP concerns.
- Application services coordinate use cases and transactions.
- Domain services hold business rules that do not naturally belong to one entity.
- Repositories hide persistence details.
- External integrations must be behind adapter interfaces.
- Modules should not directly access another module's database tables.

## Data Architecture

PostgreSQL is the system of record.

Redis is used only for cache, rate limiting, distributed locks where justified, short-lived verification tokens, and ephemeral state. Redis must not become the source of truth for money or identity.

Financial records must be ledger-first:

- Never overwrite financial history.
- Prefer append-only ledger entries.
- Store reversals and adjustments as new records.
- Use immutable audit trails for sensitive actions.

## Multi-Tenancy

Initial recommendation: shared database, shared schema, tenant-scoped rows.

Every tenant-owned table must include:

- `tenant_id`
- `created_at`
- `updated_at`
- `created_by`
- `updated_by`

Future high-scale or regulated tenants can move to isolated schemas or databases if needed.

## Security Architecture

Security must be layered:

- TLS everywhere.
- Strong authentication and JWT session controls.
- Role-based access control at API and domain levels.
- Object-level authorization for tenant and group access.
- Encryption for sensitive data at rest.
- PII masking in logs and exports.
- Rate limiting and abuse protection.
- Secure webhook validation for payment providers.

## AWS Target Architecture

Initial production architecture:

- Route 53 for DNS
- CloudFront for admin portal static delivery
- S3 for admin portal build artifacts and controlled document storage
- Application Load Balancer for backend traffic
- ECS Fargate or EKS for backend workloads
- RDS PostgreSQL with Multi-AZ in production
- ElastiCache Redis
- Secrets Manager or SSM Parameter Store
- CloudWatch logs and metrics
- AWS WAF for public endpoints
- KMS for encryption keys

## Scalability Path

Phase 1:

- Modular monolith
- Single PostgreSQL primary with read replica later
- Redis caching where measured
- Horizontal backend scaling

Phase 2:

- Read replicas for reporting
- Background workers for notifications, payment reconciliation, exports
- Event-driven internal workflows

Phase 3:

- Split high-pressure modules into services
- Dedicated ledger service if transaction volume demands it
- Dedicated reporting warehouse
- Data lake for analytics

## Reliability Principles

- All payment operations must be idempotent.
- All provider callbacks must be verifiable and replay-safe.
- Scheduled reconciliation must correct provider or network inconsistencies.
- Critical workflows must emit audit and domain events.
- Background jobs must be retryable and observable.

