# JPA Entity Mapping

## Scope

This sprint maps `User`, `Role`, `Permission`, `Group`, `Member`, `MonthlyCycle`, `Installment`, `Payment`, `Receipt`, `Draw`, `AuctionBid`, `Notification`, and `AuditLog`. It adds no repositories, services, controllers, authentication, or business workflows.

## Mapping Decisions

- Every entity uses an assigned UUID inherited from `BaseJpaEntity`; aggregate identity is created before persistence.
- Audit timestamps, audit actors, optimistic `@Version`, and soft-delete metadata are inherited from the mapped superclass.
- Enums use `EnumType.STRING` so database values remain readable and ordinal changes cannot corrupt meaning.
- To-one and collection associations are explicitly lazy.
- Aggregate-child persistence uses only `PERSIST` and `MERGE`. `REMOVE` cascade and orphan removal are intentionally disabled for membership, cycle, installment, draw, bid, payment, and receipt history.
- Tenant references remain UUID columns because no tenant entity belongs to this sprint.
- Monetary values use integer minor units plus ISO currency codes. Floating-point columns are not used.
- Schema and table names follow the database catalog: `identity`, `community`, `finance`, `notification`, and `audit`.
- `user_roles` and `role_permissions` contain assignment metadata in the database design. They are not modeled as lossy `ManyToMany` collections; dedicated association entities belong to a later access-control sprint.
- Audit logs expose no mutation methods and use only non-updatable business columns. Retention and archival are infrastructure policies outside this mapping sprint.

## Domain Mapping

MapStruct interfaces map entities to existing domain counterparts for users, groups, members, payments, receipts, draws, auction bids, and notifications. Association references are supplied through `JpaReferenceProvider`; mappers never execute database queries.

`Role`, `Permission`, `MonthlyCycle`, `Installment`, and `AuditLog` currently have no domain model in the repository. Their JPA mappings are complete, but fake domain types and fake mappers were deliberately not introduced. Domain mappings should be added when those bounded-context models exist.

Some existing aggregate models are intentionally coarser than the database catalog. Mapper documentation and signatures make those boundaries explicit: one member persistence row maps to one participation, receipt totals map to a single contribution line, and delivery/payment attempt histories remain separate future mappings.
