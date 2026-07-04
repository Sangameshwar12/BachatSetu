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

- API latency
- API error rate
- Authentication failures
- Payment initiation count
- Payment success and failure rates
- Webhook failures
- Reconciliation mismatches
- Database CPU, memory, connections, slow queries
- Redis memory and eviction
- Background job failures

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

