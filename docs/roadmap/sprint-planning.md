# Sprint Planning

BachatSetu should begin with two-week sprints. The team may move to one-week sprints during early discovery or hot launch periods.

## Sprint Cadence

Recommended recurring ceremonies:

- Sprint planning
- Daily async or live standup
- Mid-sprint risk check
- Backlog refinement
- Sprint review
- Retrospective

## Sprint Planning Inputs

Each sprint should consider:

- Product priorities
- Customer feedback
- Security requirements
- Compliance tasks
- Technical debt
- Production issues
- Team capacity
- Dependencies

## Sprint Capacity

Reserve capacity:

- 70 percent planned product work
- 15 percent bugs and hardening
- 10 percent technical debt
- 5 percent operational and documentation work

Adjust after private beta based on real support load.

## Story Sizing

Use relative sizing:

```text
1, 2, 3, 5, 8, 13
```

Rules:

- Split 13-point stories before sprint commitment.
- Treat security and financial correctness as sizing multipliers.
- Include testing and documentation in estimates.

## Sprint 0 Recommendation

Sprint 0 should produce:

- Final repository setup
- Backend skeleton
- Mobile skeleton
- Admin skeleton
- Local development environment
- CI baseline
- First ADRs
- Product flow diagrams
- Threat model draft

No business feature should be considered complete in Sprint 0.

## Backlog Themes

Initial backlog themes:

- Platform foundation
- Identity and access
- Tenant management
- Bhishi group lifecycle
- Contribution lifecycle
- Payments and reconciliation
- Notifications
- Admin operations
- Reporting and audit
- Security and compliance
- Observability and support

## Release Planning

Every release should include:

- Release goal
- Scope list
- Known risks
- Migration notes
- Rollback plan
- Support notes
- Monitoring checks

