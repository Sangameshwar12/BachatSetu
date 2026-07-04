# Business Domain Design

BachatSetu is India's Community Savings Platform. The first product domain is Bhishi, also known as ROSCA or Committee. The broader product vision is to support trusted community-based collections, savings, contributions, and fund management across many Indian social and civic contexts.

This document defines the product domain using Domain Driven Design principles. It intentionally contains no application code.

## 1. Business Domain

### Domain Statement

BachatSetu enables trusted groups of people to organize recurring contributions, track obligations, manage payouts, send reminders, reconcile payments, and maintain transparent financial records.

### Initial Domain

The initial domain is Bhishi group management:

- A group is created by an organizer.
- Members join or are added to the group.
- The group agrees on contribution amount, frequency, duration, payout rules, penalties, and operating terms.
- Members contribute on schedule.
- One or more members receive payouts according to the group rules.
- Payments, dues, reminders, and history are tracked transparently.

### Strategic Domain Goal

BachatSetu should become the trusted operating system for community money collection in India.

### Ubiquitous Language

| Term | Meaning |
| --- | --- |
| Tenant | A customer organization or operating unit using BachatSetu |
| Community | A real-world group of people connected by trust, locality, work, family, society, temple, NGO, or shared goal |
| Member | A person participating in a community or group |
| Organizer | A person responsible for operating a group |
| Bhishi Group | A recurring savings group with agreed contribution and payout rules |
| Contribution | A member's expected payment for a cycle |
| Collection Cycle | A scheduled period in which contributions are due |
| Payout | Distribution of collected funds to an eligible member |
| Due | An unpaid expected contribution |
| Penalty | Additional amount applied when a rule is violated, usually for late payment |
| Ledger Entry | Immutable financial record of value movement |
| Reconciliation | Process of matching internal payment state with external provider state |
| Audit Trail | Tamper-resistant history of sensitive actions |

## 2. Core Entities

### Tenant

Represents a SaaS customer boundary.

Responsibilities:

- Owns configuration, users, groups, and operational settings.
- Defines enabled product modules.
- Provides tenant-level reporting and administration.

### User

Represents a login-capable person.

Responsibilities:

- Authenticates into the platform.
- Holds profile, contact, session, and verification information.
- May have roles across tenants or groups.

### Member

Represents a person participating in a community financial activity.

Responsibilities:

- Participates in one or more groups.
- Has member-level status, contact preferences, and financial obligations.
- May or may not have completed full digital onboarding at first.

### Community

Represents a real-world social or operating collection unit.

Responsibilities:

- Groups members under a shared purpose.
- Provides a reusable structure for future modules beyond Bhishi.

### Bhishi Group

Aggregate root for the Bhishi module.

Responsibilities:

- Defines group rules, contribution amount, cycle frequency, duration, start date, and payout method.
- Controls lifecycle from draft to active to completed or cancelled.
- Owns member participation within the group.

### Group Membership

Represents a member's participation in a specific group.

Responsibilities:

- Tracks member status.
- Defines role within the group.
- Holds join date, exit date, and participation metadata.

### Collection Cycle

Represents one scheduled contribution period.

Responsibilities:

- Defines due date.
- Tracks cycle status.
- Holds expected contributions and payout eligibility.

### Contribution Obligation

Represents what a member owes for a cycle.

Responsibilities:

- Tracks expected amount, due date, paid amount, due status, penalties, and waivers.

### Payment

Represents an attempted or completed external or manual payment.

Responsibilities:

- Tracks payment initiation, provider reference, status, failure reason, and reconciliation state.

### Payout

Represents a distribution to a member.

Responsibilities:

- Tracks beneficiary, amount, status, approval, release, and reconciliation.

### Ledger Account

Represents an accounting account for a member, group, platform, or settlement bucket.

Responsibilities:

- Provides structure for immutable financial entries.

### Ledger Entry

Represents immutable value movement.

Responsibilities:

- Records debit or credit, amount, currency, source, reference, and timestamp.

### Notification

Represents an outbound communication.

Responsibilities:

- Tracks recipient, channel, message category, delivery status, and provider response.

### Audit Record

Represents a sensitive action performed by a user, system, or support actor.

Responsibilities:

- Captures who did what, when, where, why, and on which resource.

## 3. Bounded Contexts

### Identity and Access Context

Purpose:

- Authentication
- Authorization
- Sessions
- Roles
- Permissions

Key concepts:

- User
- Role
- Permission
- Session
- Verification

### Tenant and Community Context

Purpose:

- Tenant setup
- Community grouping
- Module enablement
- Tenant-level settings

Key concepts:

- Tenant
- Community
- Tenant Setting
- Module Subscription

### Member Context

Purpose:

- Member profiles
- Contact preferences
- Participation across groups

Key concepts:

- Member
- Member Contact
- Member Status
- Member Consent

### Bhishi Context

Purpose:

- Bhishi group lifecycle
- Group rules
- Member participation
- Collection cycles
- Payout eligibility

Key concepts:

- Bhishi Group
- Group Rule
- Group Membership
- Collection Cycle
- Contribution Obligation
- Payout Plan

### Payment Context

Purpose:

- Payment initiation
- Payment provider integration
- Payment status tracking
- Webhook handling
- Reconciliation

Key concepts:

- Payment
- Payment Attempt
- Provider Transaction
- Reconciliation Case

### Ledger Context

Purpose:

- Immutable accounting records
- Financial correctness
- Balances derived from entries

Key concepts:

- Ledger Account
- Ledger Entry
- Journal
- Adjustment
- Reversal

### Notification Context

Purpose:

- Reminders
- Receipts
- Alerts
- Delivery tracking

Key concepts:

- Notification
- Template
- Channel
- Delivery Attempt

### Audit and Compliance Context

Purpose:

- Sensitive action tracking
- Compliance support
- Investigation history

Key concepts:

- Audit Record
- Actor
- Action
- Reason
- Evidence

### Reporting Context

Purpose:

- Operational visibility
- Financial summaries
- Exports
- Admin dashboards

Key concepts:

- Report
- Dashboard Metric
- Export Job

## 4. User Roles

| Role | Description |
| --- | --- |
| Platform Owner | BachatSetu internal owner with full governance visibility |
| Platform Admin | Manages tenants, configuration, incidents, and support escalation |
| Support Operator | Assists users with restricted, audited access |
| Tenant Admin | Manages one tenant and its enabled modules |
| Community Manager | Oversees multiple groups inside a tenant or community |
| Group Organizer | Creates and manages a Bhishi group |
| Group Co-Organizer | Assists organizer with limited group management permissions |
| Group Member | Participates in contribution and payout activity |
| Auditor | Reviews records without operational mutation rights |
| Finance Operator | Handles payment, payout, and reconciliation workflows |

## 5. Domain Relationships

```text
Tenant
  owns Communities
  owns Users
  enables Product Modules

Community
  contains Members
  contains Bhishi Groups

User
  may represent one Member
  may hold Roles
  may act as Organizer or Admin

Bhishi Group
  has Group Rules
  has Group Memberships
  has Collection Cycles
  has Payouts

Collection Cycle
  has Contribution Obligations
  may produce Payout

Contribution Obligation
  may be settled by Payments
  may create Penalties

Payment
  produces Ledger Entries after confirmation
  may require Reconciliation

Payout
  produces Ledger Entries after approval or release

Sensitive Actions
  produce Audit Records

Important State Changes
  produce Domain Events
```

## 6. Business Rules

### Tenant Rules

- Every business resource must belong to a tenant.
- A user may access tenant data only through assigned roles and permissions.
- Tenant configuration controls enabled modules and operational limits.

### Bhishi Group Rules

- A group must have an organizer before activation.
- A group must define contribution amount, frequency, start date, and duration before activation.
- A group cannot be activated without at least the minimum required members.
- Active group rules cannot be changed if the change would invalidate completed cycles.
- Group cancellation after activation must preserve financial history.
- Completed groups are read-only except for approved corrections and audit notes.

### Membership Rules

- A member can belong to multiple groups.
- A member must have active membership to owe contributions in a group.
- Removing a member from an active group requires settlement or approved exception.
- Role changes inside a group must be audited.

### Contribution Rules

- Each active member receives a contribution obligation for each applicable cycle.
- Contributions become due according to the cycle due date.
- Partial payments may be allowed only if group rules allow them.
- Late penalties apply only according to configured group rules.
- Waivers require organizer or authorized admin approval.

### Payment Rules

- Payment initiation must be idempotent.
- Client-side payment success is not final until verified by provider callback or reconciliation.
- Unknown payment status must remain pending until resolved.
- Duplicate provider callbacks must not duplicate ledger entries.
- Failed payments do not settle obligations.

### Payout Rules

- Payout eligibility is determined by group rules.
- Payout cannot be released without required approval.
- Payout cannot exceed available eligible group funds unless explicitly supported by product policy.
- Payout release must create auditable financial records.

### Ledger Rules

- Ledger entries are append-only.
- Reversals and corrections must be represented as new entries.
- Balances must be derived from ledger entries.
- Financial state changes require traceable source references.

### Notification Rules

- Members should receive reminders before and after due dates according to communication preferences.
- Payment receipts should be sent only after confirmed payment.
- Failed delivery should be tracked and retried according to channel policy.

### Audit Rules

- Sensitive actions must produce audit records.
- Audit records must not be editable by normal users.
- Support actions require actor identity and reason.

## 7. User Journeys

### Organizer Creates a Bhishi Group

1. Organizer signs in.
2. Organizer creates a new group.
3. Organizer defines contribution amount, cycle frequency, start date, duration, and payout rules.
4. Organizer adds members.
5. System validates activation readiness.
6. Organizer activates the group.
7. System creates the first collection cycle.

### Member Joins and Tracks Contributions

1. Member receives invite or is added by organizer.
2. Member verifies contact details.
3. Member views group rules and obligations.
4. Member receives upcoming due reminders.
5. Member pays contribution.
6. Member receives confirmation after payment is verified.
7. Member views history and payout status.

### Organizer Manages Monthly Collection

1. Organizer opens active group dashboard.
2. Organizer reviews current cycle collection status.
3. Organizer sends reminders to unpaid members.
4. Organizer records manual collection if allowed.
5. System updates contribution status.
6. Organizer reviews payout readiness.

### Finance Operator Resolves Payment Mismatch

1. Finance operator opens reconciliation queue.
2. Operator reviews payment, provider status, and internal state.
3. Operator triggers reconciliation or records approved resolution.
4. System updates payment state.
5. System emits audit and domain events.

### Support Operator Assists Member

1. Support operator searches member using safe identifiers.
2. Operator views limited profile and group context.
3. Operator reviews contribution/payment status.
4. Operator performs permitted action with reason.
5. System records audit trail.

## 8. User Stories

### Group Organizer

- As a group organizer, I want to create a Bhishi group so that I can manage recurring savings digitally.
- As a group organizer, I want to add members so that everyone in the group can be tracked.
- As a group organizer, I want to view contribution status so that I know who has paid and who is due.
- As a group organizer, I want to send reminders so that members pay on time.
- As a group organizer, I want to manage payouts so that the correct member receives funds.

### Group Member

- As a member, I want to view my active groups so that I understand my obligations.
- As a member, I want to see upcoming dues so that I can pay on time.
- As a member, I want to receive payment confirmation so that I trust the system.
- As a member, I want to see contribution history so that I can verify records.
- As a member, I want to know my payout status so that I can plan financially.

### Tenant Admin

- As a tenant admin, I want to manage organizers so that group operations are controlled.
- As a tenant admin, I want to view group performance so that I can monitor community collections.
- As a tenant admin, I want to review exceptions so that sensitive actions are governed.

### Finance Operator

- As a finance operator, I want to review unresolved payments so that mismatches are corrected.
- As a finance operator, I want to see provider references so that I can investigate payment issues.
- As a finance operator, I want actions audited so that financial operations remain accountable.

### Platform Admin

- As a platform admin, I want to manage tenants so that BachatSetu can onboard customers safely.
- As a platform admin, I want to monitor incidents so that operational risks are visible.
- As a platform admin, I want to restrict support access so that user trust is protected.

## 9. Acceptance Criteria

### Bhishi Group Creation

- Given an organizer has required permissions, when they create a group with valid details, then the group is saved in draft status.
- Given required group fields are missing, when the organizer tries to save, then the system rejects the request with clear validation errors.
- Given the group lacks minimum members, when activation is attempted, then activation is blocked.
- Given all activation requirements are met, when the organizer activates the group, then the group status becomes active and the first cycle is scheduled.

### Member Management

- Given an active organizer, when they add a member with valid contact details, then the member is added to the group.
- Given a duplicate member is added to the same group, when the organizer submits, then the system prevents duplicate membership.
- Given a member has financial obligations, when removal is attempted, then the system requires settlement or authorized exception.

### Contribution Tracking

- Given a cycle is active, when obligations are generated, then every active member has one expected contribution.
- Given a member pays successfully, when payment confirmation is received, then the obligation reflects paid status.
- Given payment status is unknown, when provider confirmation is unavailable, then the obligation remains pending.

### Payment Reconciliation

- Given a provider webhook is received twice, when the system processes it, then financial records are created only once.
- Given internal and provider statuses differ, when reconciliation runs, then the mismatch is flagged or corrected according to policy.

### Audit

- Given a sensitive action is performed, when the action succeeds or fails, then an audit record is created.
- Given a support operator changes a sensitive state, when they submit the action, then a reason is required.

## 10. MVP Scope

### Included in MVP

- User authentication foundation
- Tenant setup
- Basic roles and permissions
- Member profile management
- Bhishi group creation
- Group member management
- Contribution schedule setup
- Collection cycle tracking
- Contribution due tracking
- Manual payment recording with audit
- Payment provider integration if launch readiness requires it
- Basic ledger records
- Member reminders
- Organizer dashboard
- Admin dashboard
- Audit trail for sensitive actions
- Basic reports

### Excluded from MVP

- Multiple advanced payout algorithms
- Marketplace or discovery of groups
- Public group joining
- Full lending or credit products
- Cross-border payments
- Complex investment products
- AI-based risk scoring
- Multi-currency support
- White-label tenant branding beyond basics

## 11. Future Scope

Future modules:

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

Future capabilities:

- Advanced payout methods
- Automated bank transfer payouts
- WhatsApp-first workflows
- Multi-language support
- Offline-first mobile experience
- Digital receipts and certificates
- Group reputation and trust scoring
- Advanced analytics
- Regulatory reporting support
- Partner APIs
- Bulk import for societies and NGOs
- Multi-tenant white-labeling

## 12. Functional Requirements

### Identity and Access

- Users must be able to authenticate securely.
- Users must have roles scoped to tenant, community, or group.
- Admin and support actions must be permission controlled.

### Tenant and Community

- Platform admins must be able to onboard tenants.
- Tenant admins must configure enabled modules and basic settings.
- Communities must organize members and groups.

### Member Management

- Organizers must add and manage members.
- Members must have contact and status information.
- Members must be searchable by authorized users.

### Bhishi Management

- Organizers must create and configure Bhishi groups.
- Groups must support lifecycle states.
- Groups must define contribution and payout rules.
- Groups must generate collection cycles.

### Contribution Management

- System must create contribution obligations.
- System must track paid, unpaid, partial, overdue, waived, and disputed states where supported.
- Organizers must view collection progress.

### Payment Management

- System must track payment attempts.
- System must verify payment provider status.
- System must support reconciliation.
- System must avoid duplicate financial effects.

### Ledger and Reporting

- System must maintain immutable financial records.
- System must provide member, organizer, and admin summaries.
- System must support exports for authorized users.

### Notifications

- System must send reminders and receipts.
- System must track delivery status.
- System must respect communication preferences where applicable.

### Audit

- System must record sensitive actions.
- System must support investigation workflows for authorized users.

## 13. Non Functional Requirements

### Security

- Strong authentication and authorization.
- Tenant isolation.
- Encrypted sensitive data.
- No sensitive data in logs.
- Audited support access.

### Reliability

- Payment workflows must be idempotent.
- Provider failures must not corrupt financial state.
- Reconciliation must recover from network and provider inconsistencies.

### Scalability

- Platform must support growth to millions of users.
- Read-heavy dashboards should be optimized through measured caching and reporting views.
- Background processing should handle reminders, reconciliation, and exports.

### Performance

- Common mobile screens should load quickly on low-bandwidth networks.
- Admin dashboards should support filtering and pagination.
- Payment confirmation flows should provide clear pending states when real-time confirmation is unavailable.

### Observability

- Critical business flows must emit logs, metrics, and events.
- Payment, payout, and reconciliation failures must be alertable.
- Audit records must support investigation.

### Compliance and Privacy

- Personal data must be minimized.
- Data retention must be policy-driven.
- Production data access must be restricted and audited.
- Financial records must remain traceable.

### Usability

- Product flows must support non-technical users.
- Mobile experience must support Indian language expansion.
- Error messages must be clear and actionable.

## 14. Event Flow

### Bhishi Group Activation Flow

```text
OrganizerCreatesGroup
  -> BhishiGroupCreated
  -> MembersAddedToGroup
  -> GroupActivationRequested
  -> GroupActivationValidated
  -> BhishiGroupActivated
  -> CollectionCycleScheduled
  -> ContributionObligationsCreated
  -> MemberNotificationsQueued
```

### Contribution Payment Flow

```text
ContributionDueCreated
  -> PaymentInitiated
  -> PaymentProviderCallbackReceived
  -> PaymentVerified
  -> LedgerEntriesRecorded
  -> ContributionMarkedPaid
  -> ReceiptNotificationQueued
```

### Payment Unknown Flow

```text
PaymentInitiated
  -> ProviderTimeoutOrUnknownStatus
  -> PaymentMarkedPendingVerification
  -> ReconciliationScheduled
  -> ProviderStatusFetched
  -> PaymentResolved
  -> LedgerEntriesRecorded or PaymentMarkedFailed
```

### Payout Flow

```text
PayoutEligibilityDetermined
  -> PayoutApprovalRequested
  -> PayoutApproved
  -> PayoutInitiated
  -> PayoutProviderStatusReceived
  -> PayoutCompleted
  -> LedgerEntriesRecorded
  -> MemberNotificationQueued
```

## 15. Domain Events

### Identity Events

- UserRegistered
- UserVerified
- UserLoggedIn
- UserSessionRevoked
- RoleAssigned
- RoleRevoked

### Tenant and Community Events

- TenantCreated
- TenantActivated
- TenantSuspended
- CommunityCreated
- ModuleEnabled
- ModuleDisabled

### Member Events

- MemberCreated
- MemberContactVerified
- MemberAddedToCommunity
- MemberStatusChanged

### Bhishi Events

- BhishiGroupCreated
- BhishiGroupRulesDefined
- GroupMemberAdded
- GroupMemberRemoved
- BhishiGroupActivated
- BhishiGroupSuspended
- BhishiGroupCompleted
- CollectionCycleScheduled
- CollectionCycleOpened
- CollectionCycleClosed

### Contribution Events

- ContributionObligationCreated
- ContributionDueDateReached
- ContributionMarkedPending
- ContributionMarkedPaid
- ContributionPartiallyPaid
- ContributionMarkedOverdue
- ContributionWaived
- PenaltyApplied

### Payment Events

- PaymentInitiated
- PaymentProviderCallbackReceived
- PaymentVerified
- PaymentFailed
- PaymentMarkedPendingVerification
- PaymentReconciliationRequested
- PaymentReconciled
- DuplicatePaymentCallbackIgnored

### Payout Events

- PayoutEligibilityDetermined
- PayoutApprovalRequested
- PayoutApproved
- PayoutRejected
- PayoutInitiated
- PayoutCompleted
- PayoutFailed

### Ledger Events

- LedgerAccountCreated
- LedgerEntriesRecorded
- LedgerAdjustmentRecorded
- LedgerReversalRecorded

### Notification Events

- NotificationQueued
- NotificationSent
- NotificationDelivered
- NotificationFailed

### Audit Events

- SensitiveActionRecorded
- SupportActionRecorded
- AdminActionRecorded
- FinancialExceptionApproved

