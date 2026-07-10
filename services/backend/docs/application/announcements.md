# Platform Announcements

## Overview

Platform announcements let the BachatSetu platform team publish time-windowed notices (e.g. scheduled
maintenance) that a future mobile/web frontend can display to end users. Announcements live inside the
`platformoperations` module (see [platform-operations.md](platform-operations.md) for the module's overall
architecture); this document covers the announcement feature specifically.

## Domain Model

`Announcement` is an aggregate root with:

- `title`, `message` — free text.
- `startAt`, `endAt` — the inclusive active window (`endAt` must not precede `startAt`).
- `severity` — `INFO`, `WARNING`, or `CRITICAL`.

There is no persisted `ACTIVE`/`EXPIRED` status column: `Announcement.isActive(Instant now)` derives the
state from `now` against `[startAt, endAt]` at read time. This avoids a scheduled job to flip a stored status
and keeps the aggregate's invariant (a window's activity is a pure function of time) trivially correct
regardless of when it is queried.

## Publishing Flow

```
Platform Admin      AnnouncementController   PublishAnnouncementUseCase   AnnouncementRepository   Audit
 │ POST /announcements       │                        │                          │                  │
 ├───────────────────────────►                        │                          │                  │
 │                           │ toPublishCommand(user, req)                       │                  │
 │                           ├───────────────────────►│                          │                  │
 │                           │                        │ Announcement.publish()  │                  │
 │                           │                        │ save()                  │                  │
 │                           │                        ├─────────────────────────►                  │
 │                           │                        │ publish(domain events)  │                  │
 │                           │                        │ createAuditEntry.execute(ANNOUNCEMENT_PUBLISHED)
 │                           │                        ├──────────────────────────────────────────────►
 │                           │◄───────────────────────┤                          │                  │
 │ 200 OK                    │                        │                          │                  │
 │◄───────────────────────────                        │                          │                  │
```

Audit recording happens directly from `PublishAnnouncementApplicationService` (best-effort, wrapped in a
`try`/`catch` that never lets an audit failure affect an already-published announcement) rather than through
an Audit event listener — see
[platform-operations.md's module-cycle-avoidance section](platform-operations.md#module-cycle-avoidance) for
why every Tenant/Announcement/Broadcast audit entry in this module is recorded this way.

## API Summary

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/v1/platform-operations/announcements` | `PLATFORM_ADMIN` | Publish an announcement |
| GET | `/api/v1/platform-operations/announcements` | `PLATFORM_ADMIN` | List every announcement (paginated) |
| GET | `/api/v1/platform-operations/announcements/active` | Any authenticated user | List currently active announcements |

`GET /announcements/active` is intentionally open to any authenticated user (not `PLATFORM_ADMIN`-gated)
since its purpose is for every user's client to display active notices — publishing and browsing the full
history remain platform-administrator-only.

## Response Shape

```json
{
  "announcementId": "…",
  "title": "Scheduled maintenance",
  "message": "The app will be briefly unavailable at 2am UTC.",
  "startAt": "2026-07-15T02:00:00Z",
  "endAt": "2026-07-15T03:00:00Z",
  "severity": "WARNING",
  "active": false
}
```

`active` is always computed at response time from the current server clock — it is never a stored value.

## Errors

| Condition | HTTP status | Problem code |
|---|---|---|
| `endAt` before `startAt` | 422 | `platform-operations-validation-failed` |
| Request validation failure | 400 | `validation-error` |
| Not authenticated | 401 | `authentication-required` |
| Missing `PLATFORM_ADMIN` role (publish/list) | 403 | `platform-administrator-required` |

## Known Limitations

- **No edit or unpublish action.** The sprint specification asks only for publishing and listing; an
  announcement, once published, cannot be edited or withdrawn early. If needed, that would be additive
  future work (e.g. an `unpublish()` domain method plus a `DELETE`/`PATCH` endpoint), not a redesign.
- **No per-tenant targeting.** Every announcement is platform-wide; there is no concept of "for tenant X
  only," matching the sprint's scope.
