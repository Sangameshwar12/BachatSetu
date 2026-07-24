# Deployment Documentation

Production infrastructure documentation for BachatSetu, added in Sprint PI-1 (Production
Infrastructure Foundation). This is documentation and configuration for running the existing
application in production — it does not add or change any business functionality. See
[docs/product/README.md](../product/README.md) for what the product itself does.

## Reading order

1. **[Docker Guide](docker-guide.md)** — the backend and frontend Dockerfiles, the Nginx edge
   configuration, and the three Compose files, and how to build/run them.
2. **[Environment Variables Guide](environment-variables-guide.md)** — every environment
   variable the backend and frontend read, what's required versus optional, and what each one
   does.
3. **[Infrastructure Guide](infrastructure-guide.md)** — the target AWS architecture (S3, RDS,
   ElastiCache, EC2, ALB/CloudFront) this sprint's Docker images are built to run on.
   Documentation only — nothing has been provisioned.
4. **[Production Deployment Guide](production-deployment-guide.md)** — the step-by-step
   procedure to stand up the stack on a host, put it behind TLS, deploy an update, and roll
   back.
5. **[Production Checklist](production-checklist.md)** — what to verify before the first real
   deployment, and an honest list of what PI-1 does not cover.
6. **[Runbook](runbook.md)** — day-2 operations: checking status, restarting, rotating
   secrets, database migrations, maintenance mode.
7. **[Recovery Guide](recovery-guide.md)** — failure scenarios: a container failing its
   healthcheck, a failed migration, rolling back a bad deployment, data loss.
8. **[Monitoring Guide](monitoring-guide.md)** — Prometheus and Grafana, added in Sprint 9.1:
   architecture, how to start it, how to reach Grafana, default credentials, how to add a
   dashboard, the full metrics reference, and known limitations (no PostgreSQL/Redis exporter,
   no alerting yet).

## Relationship to `docs/product/`

[Non-Functional Requirements and Production Readiness](../product/non-functional-and-production-readiness.md)
is where the Product Documentation set describes *what exists* for logging, monitoring, and
production readiness across the whole platform (frontend and backend), written from a
reconciliation-against-implementation perspective. This folder is the operational
companion — *how to actually run it* — written from a DevOps perspective. Where the two
overlap, this folder is authoritative for procedure; that chapter is authoritative for status.
