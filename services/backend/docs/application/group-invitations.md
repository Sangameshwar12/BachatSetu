# Group Invitations & Joining Module

## Purpose

Lets a group organizer invite people by shareable code, QR, or link, and lets an invitee preview the
group and join it themselves — without becoming the group owner, and without the organizer manually
adding each member. Completes Sprint 13.4's onboarding journey: signup → profile onboarding →
create-or-join a group → dashboard.

## Architecture

An entirely new top-level module, `invitation`, following the same package shape as every other
module:

```
invitation
 ├── domain/model    GroupInvitation aggregate, InvitationCode, InvitationToken, InvitationType, InvitationStatus
 ├── domain/event    InvitationCreated, InvitationRevoked, InvitationAccepted
 ├── domain/port     GroupInvitationRepository
 ├── application     command, query, exception, port, usecase, service
 └── interfaces/rest InvitationController (organizer), JoinController (member), DTOs, mapper, config, adapters
```

`V13__group_invitations.sql` adds `community.group_invitations` — one row per invitation, with a
partial unique index (`WHERE status = 'ACTIVE'`) enforcing "at most one active invitation per group"
at the database level, not just in application code.

## One Aggregate, Every Sharing Channel

Rather than three separate invitation types, `GroupInvitation` always carries both an
`InvitationCode` (short, human-typeable, e.g. `AB3D9F2K`) and an `InvitationToken` (a
cryptographically random ~43-character string). `POST /groups/{id}/invite` generates both at once;
`type` records which channel the organizer *primarily* intends to share (`QR`, `CODE`, or `LINK`),
but either the code or the token can be redeemed regardless. The response's `joinLink` field
(`/join/{token}`) is what a client encodes into a QR image or shares as a link — the token, not the
invitation's own database ID, satisfying "no raw database IDs inside QR."

**Generating a new invitation replaces the old one.** `CreateInvitationApplicationService` revokes
any existing active invitation for the group before creating the new one, matching the unique-active-
invitation-per-group database constraint and "GET /groups/{id}/invite (current)" reading as "the one
current invitation," not "one of several."

## Reusing `SavingsGroup.joinMember` for Self-Service Join

The group module already had `JoinGroupUseCase`, but it requires the *caller* to already be the
group's owner (`GroupAuthorizationService.requireOwner`) — it models an organizer directly adding a
member, not a self-service join. `AcceptInvitationApplicationService` instead calls the pre-existing
`SavingsGroup.joinMember(...)` aggregate method **directly**, bypassing that owner-only
authorization check, since a valid, unexpired, unused invitation *is* the authorization for a
self-service join. This required no changes to `SavingsGroup`, `GroupMember`, or the existing
organizer-add-member flow: `joinMember` already enforces "not already a member"
(`DuplicateMemberException`) and "not at capacity" (`GroupCapacityExceededException`) as domain
invariants, so Part 7's validation requirements ("capacity available, user not already member") come
free from code that already existed and was already tested.

**Ordering guarantees a rejected join never consumes the invitation.** Both
`GroupInvitation.accept(...)` and `SavingsGroup.joinMember(...)` are called in-memory before either
aggregate is saved; if the group-side call throws (capacity/duplicate), nothing has been persisted
yet for either aggregate, so the invitation remains `ACTIVE` and redeemable.

## Security

- **Unguessable:** the token is 32 cryptographically random bytes (`SecureRandom`), base64url-encoded
  (~43 characters) — `RandomInvitationTokenGeneratorAdapter`.
- **Single-use:** `GroupInvitation.accept(...)` transitions `ACTIVE → USED`; a second attempt (by
  anyone) fails with `InvitationDomainException` → `422 invitation-validation-failed`.
- **Expiring:** `bachatsetu.invitation.validity` (default `P7D`, an ISO-8601 duration) sets each
  invitation's `expiresAt` at creation time; `accept(...)` rejects an expired invitation without ever
  transitioning it to `USED`.
- **Tenant-safe:** the invitation *code* (short, more guessable, meant to be typed by a known
  invitee) is looked up scoped to the caller's own tenant — `GroupInvitationRepository.findByCode(
  tenantId, code)`. The invitation *token* (long, random, meant to work from a cold QR scan/link click
  before the app necessarily knows which tenant) is looked up tenant-agnostically via a
  database-wide unique constraint on `secure_token` — there is no tenant ambiguity to resolve since
  the token's entropy alone makes cross-tenant collision or guessing infeasible.

## Join Flow

1. **Preview** — `GET /api/v1/join/{token}` (public, no authentication) returns group name,
   organizer's display name (looked up via the `user` module), contribution amount, frequency,
   member count, and capacity. No group or organizer database IDs are exposed.
2. **Join** — `POST /api/v1/groups/join`, authenticated, body `{code|token, channel}`. The caller
   self-reports `channel` (`CODE`, `QR`, or `LINK`) — the client already knows whether the user typed
   a code, scanned a QR, or clicked a link, so the backend doesn't need to (and structurally cannot)
   infer it purely from a redeemed token, since QR and LINK both redeem the same token.

## Audit and Notifications

`InvitationCreated`, `InvitationRevoked`, and `InvitationAccepted` are published through the new
`invitation.application.port.DomainEventPublisherPort`, exactly mirroring every other module's
generic event-publisher pattern. Three new audit listeners record `INVITATION_CREATED`,
`INVITATION_REVOKED`, and — for acceptance — `GROUP_JOINED`, `QR_JOINED`, or `LINK_JOINED` depending
on `InvitationAccepted.channel()`. Joining a group also fires the pre-existing `MemberJoined` domain
event (from `SavingsGroup.joinMember`), which the already-existing `SavingsGroupNotificationListener`
already turns into an organizer notification — Part 14's "organizer notified on new member join" is
satisfied without any new notification code.

## Errors

| Failure | Status | `code` |
|---|---|---|
| Group not found | 404 | `group-not-found` |
| No active invitation | 404 | `no-active-invitation` |
| Invitation not found (bad code/token) | 404 | `invitation-not-found` |
| Only the group owner may invite/revoke | 403 | `access-denied` |
| Invitation expired, used, or cancelled | 422 | `invitation-validation-failed` |
| Already a member / group at capacity | 422 | `invitation-validation-failed` (same domain-exception family as expiry) |

## Dashboards (Parts 10 & 11)

`GET /api/v1/dashboard/member` and `GET /api/v1/dashboard/organizer` compose data that already
existed across the group, member, payment, draw, notification, and invitation modules — the
dashboard module itself owns no aggregate. Three small, additive repository methods were needed to
avoid returning placeholder data:

- `SavingsGroupRepository.findByOwnerId(tenantId, ownerId)` — there was no existing "groups I own"
  query.
- `DrawRepository.findNextScheduledByGroup(tenantId, groupId)` / `PaymentRepository
  .findLatestByGroupAndMember(tenantId, groupId, memberId)` / `NotificationRepository
  .findRecentForRecipient(tenantId, recipientUserId)` — none of these three modules had any
  group- or recipient-scoped filter before this sprint; every existing listing was either
  cross-tenant-only (analytics) or entirely unfiltered.

**Known simplifications, not placeholders:** "Upcoming Installment" is the group's per-cycle
contribution amount (there is no separate per-cycle due-date schedule to query against — see
`ContributionSchedule`, which exists on `GroupRule` but isn't tied to individual installment due
dates anywhere in the codebase). "Contribution Progress" is the share of active members with at
least one `VERIFIED` payment ever recorded against the group (there is no per-cycle payment tracking
to compare against). "Pending Join Requests" from Part 11's spec has no backing concept in this
codebase — group joining has always been either organizer-direct-add or (as of this sprint)
invitation-redemption, never a request/approval queue — so it is represented as `hasActiveInvitation`
(whether the group currently has a live, unredeemed invitation) rather than invented as fake data.

## Configuration

`bachatsetu.invitation.rest.enabled` (default `true`) gates `InvitationController`/`JoinController`.
`bachatsetu.invitation.validity` (default `P7D`) configures invitation expiry.
`bachatsetu.dashboard.rest.enabled` (default `true`) gates `DashboardController`. `/api/v1/join/**`
is a public security endpoint (preview only — joining itself requires authentication).
