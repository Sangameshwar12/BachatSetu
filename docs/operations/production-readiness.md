# Production Readiness

Production readiness must be treated as an engineering milestone, not a final checklist completed at the end.

## Required Before Production

- Production AWS account structure approved
- Network boundaries configured
- Secrets managed outside source control
- Database backups active
- Restore drill completed
- Centralized logging active
- Metrics and alerts active
- Error tracking active
- Security scans clean or accepted
- Incident response process documented
- Support escalation process documented

## Observability

Minimum dashboards:

- API latency ✅ delivered (Sprint 9.1 — `deploy/monitoring/grafana/dashboards/bachatsetu-overview.json`, see [monitoring-guide.md](../deployment/monitoring-guide.md))
- API error rate ✅ delivered (Sprint 9.1, same dashboard)
- Authentication failures — not delivered; needs a purpose-built counter in the auth module
- Payment initiation count — not delivered; needs a purpose-built counter in the payment module
- Payment success and failure rates — not delivered; needs a purpose-built counter in the payment module
- Webhook failures — not delivered; needs a purpose-built counter in the payment-gateway module
- Reconciliation mismatches — not delivered; no reconciliation job exists yet
- Database CPU, memory, connections, slow queries — connections ✅ delivered (HikariCP pool metrics, Sprint 9.1); CPU/memory/slow-queries need a PostgreSQL exporter, not yet deployed (see [monitoring-guide.md §7](../deployment/monitoring-guide.md#7-known-limitations))
- Redis memory and eviction — not delivered; needs a Redis exporter, not yet deployed (Sprint 9.1 delivers Redis *client* command latency instead, see [monitoring-guide.md §7](../deployment/monitoring-guide.md#7-known-limitations))
- Background job failures — not delivered; needs a purpose-built counter in the automation module

JVM heap/non-heap, GC, and thread-count dashboards — not listed above, but also delivered in
Sprint 9.1 — round out the infrastructure/runtime half of this list. Every "not delivered" item
above is a business-metric gap, not an infrastructure one; see
[monitoring-guide.md](../deployment/monitoring-guide.md) for exactly what Sprint 9.1 covers.

## Alerts

Alert on:

- High 5xx rate
- Payment webhook failure spike
- Reconciliation mismatch spike
- Database connection exhaustion
- Queue backlog growth
- Authentication attack indicators
- Unusual admin activity
- Failed backup

## Runbooks

Required runbooks:

- Production deployment
- Production rollback
- Database restore
- Payment provider outage
- Payment mismatch investigation
- Admin account compromise
- Secret rotation
- Incident communication

## Support Readiness

Support tooling must provide:

- User lookup by safe identifiers
- Tenant and group lookup
- Payment status visibility
- Audit trail visibility
- Reason capture for sensitive support actions
- Strict role-based access

